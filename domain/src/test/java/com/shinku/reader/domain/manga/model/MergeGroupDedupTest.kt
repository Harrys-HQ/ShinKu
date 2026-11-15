package com.shinku.reader.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class MergeGroupDedupTest {

    @Test
    fun `empty membership returns every item`() {
        listOf(1L, 2L, 3L).dedupeByMergeGroup(emptyMap()) { it } shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun `ungrouped items always survive`() {
        val membership = mapOf(2L to 90L)
        listOf(1L, 2L, 3L, 4L).dedupeByMergeGroup(membership) { it } shouldBe listOf(1L, 2L, 3L, 4L)
    }

    @Test
    fun `only the first member of a group survives`() {
        val membership = mapOf(1L to 10L, 2L to 10L)
        listOf(1L, 2L).dedupeByMergeGroup(membership) { it } shouldBe listOf(1L)
    }

    @Test
    fun `input order decides the survivor, not the id`() {
        val membership = mapOf(1L to 10L, 2L to 10L)
        listOf(2L, 1L).dedupeByMergeGroup(membership) { it } shouldBe listOf(2L)
    }

    @Test
    fun `interleaved groups keep the first of each in input order`() {
        val membership = mapOf(1L to 10L, 2L to 20L, 3L to 10L, 4L to 30L, 5L to 20L)
        listOf(1L, 2L, 3L, 4L, 5L).dedupeByMergeGroup(membership) { it } shouldBe listOf(1L, 2L, 4L)
    }

    @Test
    fun `grouped and ungrouped items can be mixed`() {
        val membership = mapOf(1L to 10L, 3L to 10L)
        listOf(1L, 2L, 3L).dedupeByMergeGroup(membership) { it } shouldBe listOf(1L, 2L)
    }

    @Test
    fun `a library sized list collapses to one item per group`() {
        val items = (1L..1000L).toList()
        val membership = items.associateWith { it % 10 }
        val result = items.dedupeByMergeGroup(membership) { it }
        result.size shouldBe 10
        result shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
        items.size shouldBe 1000
    }
}
