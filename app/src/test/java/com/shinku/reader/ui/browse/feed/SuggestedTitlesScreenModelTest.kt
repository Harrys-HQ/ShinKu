package com.shinku.reader.ui.browse.feed

import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(InternalVoyagerApi::class)
class SuggestedTitlesScreenModelTest {
    private val models = mutableListOf<SuggestedTitlesScreenModel>()
    private val holderKeys = mutableListOf<String>()

    @AfterEach
    fun cleanup() {
        models.forEach { it.screenModelScope.cancel() }
        holderKeys.forEach(ScreenModelStore::onDisposeNavigator)
        Dispatchers.resetMain()
    }

    private fun source(id: Long) = mockk<CatalogueSource> {
        every { this@mockk.id } returns id
        every { lang } returns "en"
        every { supportsLatest } returns true
    }

    private fun page(vararg ids: Long, more: Boolean) = MangasPage(
        ids.map { id ->
            SManga.create().apply {
                url = "$id"
                title = "Title $id"
            }
        },
        more,
    )

    // Voyager exposes screen registration only internally; register test owners to avoid
    // sharing its fallback scope across otherwise independent model instances.
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun TestScope.model(vararg sources: CatalogueSource): SuggestedTitlesScreenModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val manager = mockk<SourceManager> {
            every { getVisibleCatalogueSources() } returns sources.toList()
        }
        val preferences = mockk<SourcePreferences> {
            every { enabledLanguages() } returns mockk { every { get() } returns setOf("en") }
            every { disabledSources() } returns mockk { every { get() } returns emptySet() }
            every { pinnedSources() } returns mockk { every { get() } returns emptySet() }
        }
        val shinku = mockk<ShinKuPreferences> {
            every { feedSourceFilter() } returns mockk { every { get() } returns "all" }
            every { feedLanguageFilter() } returns mockk { every { get() } returns "all" }
        }
        val health = mockk<SourceHealthRepository> {
            every { subscribeAll() } returns flowOf(emptyList())
        }
        val converter = mockk<NetworkToLocalManga>()
        coEvery { converter.invoke(any<List<Manga>>()) } answers {
            firstArg<List<Manga>>().map { it.copy(id = it.url.toLong()) }
        }
        val holderKey = "pagination-test-${java.util.UUID.randomUUID()}".also(holderKeys::add)
        val screen = mockk<Screen> { every { key } returns holderKey }
        return ScreenModelStore.getOrPut<SuggestedTitlesScreenModel>(screen, null) {
            SuggestedTitlesScreenModel(
                title = "For you",
                initialMangas = listOf(Manga.create().copy(id = 999)),
                mode = "for_you",
                query = "",
                sourceManager = manager,
                sourcePreferences = preferences,
                shinkuPreferences = shinku,
                sourceHealthRepository = health,
                networkToLocalManga = converter,
                getManga = mockk<GetManga>(),
                coroutineDispatcher = dispatcher,
            )
        }.also(models::add)
    }

    @Test
    fun `failed source retries its own page while healthy source advances`() = runTest {
        val flaky = source(1)
        val healthy = source(2)
        coEvery { flaky.getLatestUpdates(1) } throws IllegalStateException("temporary") andThen page(10, more = false)
        coEvery { healthy.getLatestUpdates(1) } returns page(20, more = true)
        coEvery { healthy.getLatestUpdates(2) } returns page(21, more = false)
        val model = model(flaky, healthy)
        model.loadNextPage()
        advanceUntilIdle()
        coVerify(exactly = 1) { flaky.getLatestUpdates(1) }
        assertTrue(model.state.value.hasError, "State: ${model.state.value}")
        assertTrue(model.state.value.hasMore)
        assertEquals(listOf(999L, 20L), model.state.value.mangas.map { it.id })
        model.loadNextPage()
        advanceUntilIdle()
        assertFalse(model.state.value.hasMore)
        assertFalse(model.state.value.hasError)
        assertEquals(setOf(999L, 10L, 20L, 21L), model.state.value.mangas.map { it.id }.toSet())
        coVerify(exactly = 2) { flaky.getLatestUpdates(1) }
    }

    @Test
    fun `duplicate only page continues and final nonempty page ends pagination`() = runTest {
        val source = source(1)
        coEvery { source.getLatestUpdates(1) } returns page(999, more = true)
        coEvery { source.getLatestUpdates(2) } returns page(10, more = false)
        val model = model(source)
        model.loadNextPage()
        advanceUntilIdle()
        coVerify(exactly = 1) { source.getLatestUpdates(2) }
        assertEquals(listOf(999L, 10L), model.state.value.mangas.map { it.id }, "State: ${model.state.value}")
        assertFalse(model.state.value.hasMore)
        model.loadNextPage()
        coVerify(exactly = 0) { source.getLatestUpdates(3) }
    }

    @Test
    fun `repeated taps start one request and disposal rejects late extension results`() = runTest {
        val source = source(1)
        val response = CompletableDeferred<MangasPage>()
        coEvery { source.getLatestUpdates(1) } coAnswers {
            try {
                response.await()
            } catch (_: CancellationException) {
                page(10, more = false)
            }
        }
        val model = model(source)
        model.loadNextPage()
        model.loadNextPage()
        runCurrent()
        coVerify(exactly = 1) { source.getLatestUpdates(1) }
        model.screenModelScope.cancel()
        advanceUntilIdle()
        assertEquals(listOf(999L), model.state.value.mangas.map { it.id })
    }
}
