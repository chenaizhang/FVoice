package com.fvoice.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * TODO: Add real instrumented tests for:
 * - UI navigation flows
 * - File processing end-to-end
 * - Permission handling
 * - Theme switching
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.fvoice.app", appContext.packageName)
    }
}
