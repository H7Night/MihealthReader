package com.jhon.mihealthreader.local

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BackendPreferencesTest {

    @Before
    fun clear() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("settings", 0)
            .edit().clear().commit()
    }

    @Test
    fun `defaults to root`() {
        assertEquals(BackendType.ROOT, BackendPreferences(RuntimeEnvironment.getApplication()).get())
    }

    @Test
    fun `stores shizuku`() {
        val prefs = BackendPreferences(RuntimeEnvironment.getApplication())
        prefs.set(BackendType.SHIZUKU)
        assertEquals(BackendType.SHIZUKU, prefs.get())
    }
}
