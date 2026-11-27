package com.shinku.reader.core.preference

import com.shinku.reader.core.common.preference.InMemoryPreferenceStore
import com.shinku.reader.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemoryStringSetTest {
    @Test
    fun `loads stored sets and distinguishes them from defaults`() {
        val store = InMemoryPreferenceStore(
            sequenceOf<InMemoryPreference<*>>(
                InMemoryPreference("stored", setOf("a"), emptySet()),
                InMemoryPreference("empty", emptySet<String>(), setOf("fallback")),
                InMemoryPreference("wrong-type", setOf(1), emptySet()),
            ),
        )
        val fallback = setOf("fallback")

        assertEquals(setOf("a"), store.getStringSet("stored", fallback).get())
        val empty = store.getStringSet("empty", fallback)
        assertTrue(empty.isSet())
        assertEquals(emptySet<String>(), empty.get())
        val missing = store.getStringSet("missing", fallback)
        assertFalse(missing.isSet())
        assertEquals(fallback, missing.get())
        assertEquals(fallback, store.getStringSet("wrong-type", fallback).get())
        missing.set(setOf("updated"))
        assertEquals(setOf("updated"), missing.get())
        missing.delete()
        assertEquals(fallback, missing.get())
    }
}
