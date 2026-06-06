package com.fvoice.app.core.asr

import androidx.test.platform.app.InstrumentationRegistry
import com.fvoice.app.core.modelmanager.ModelManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class WhisperCppEngineSmokeTest {

    @Test
    fun transcribesBundledJfkSample() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val testContext = instrumentation.context

        ModelManager.installBundledModels(targetContext)
        val modelManager = ModelManager(targetContext)
        val model = modelManager.currentAsrModel.value
        assertNotNull("Bundled Whisper model should be installed and ready", model)

        val wavFile = File(targetContext.cacheDir, "jfk-whisper-smoke.wav")
        testContext.assets.open("jfk.wav").use { input ->
            wavFile.outputStream().use { output -> input.copyTo(output) }
        }

        val engine = WhisperCppEngine()
        try {
            assert(engine.initialize(model!!)) { "Whisper engine should initialize" }
            val segments = engine.transcribe(
                pcmFile = wavFile,
                sampleRate = 16000,
                language = "en",
                cancellationToken = null
            )
            assertFalse("Whisper should produce at least one segment", segments.isEmpty())
        } finally {
            engine.release()
            wavFile.delete()
        }
    }
}
