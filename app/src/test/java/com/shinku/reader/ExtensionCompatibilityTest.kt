package com.shinku.reader

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * This test ensures that critical classes required for APK extension compatibility
 * remain in their expected packages. DO NOT rename or move the classes listed here.
 *
 * If this test fails, it means a "Freeze Zone" class has been moved or renamed,
 * which WILL break all installed extensions.
 */
class ExtensionCompatibilityTest {

    @Test
    fun `verify source interfaces exist in legacy package`() {
        assertClassExists("eu.kanade.tachiyomi.source.Source")
        assertClassExists("eu.kanade.tachiyomi.source.model.SManga")
        assertClassExists("eu.kanade.tachiyomi.source.model.SChapter")
        assertClassExists("eu.kanade.tachiyomi.source.model.Page")
    }

    @Test
    fun `verify network helpers exist in legacy package`() {
        assertClassExists("eu.kanade.tachiyomi.network.NetworkHelper")
        assertClassExists("eu.kanade.tachiyomi.network.JavaScriptEngine")
    }

    @Test
    fun `verify model implementations exist in legacy package`() {
        assertClassExists("eu.kanade.tachiyomi.source.model.SMangaImpl")
        assertClassExists("eu.kanade.tachiyomi.source.model.SChapterImpl")
    }

    private fun assertClassExists(className: String) {
        try {
            val clazz = Class.forName(className)
            assertNotNull(clazz, "Class $className should not be null")
        } catch (e: ClassNotFoundException) {
            throw AssertionError("CRITICAL FAILURE: Class $className not found. " +
                "This will break extension compatibility! Ensure the class hasn't been moved or renamed.")
        }
    }
}
