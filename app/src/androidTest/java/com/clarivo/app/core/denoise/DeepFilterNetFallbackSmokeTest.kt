package com.clarivo.app.core.denoise

import androidx.test.platform.app.InstrumentationRegistry
import com.clarivo.app.core.model.AudioFormatInfo
import com.clarivo.app.core.modelmanager.ModelManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeepFilterNetFallbackSmokeTest {

    @Test
    fun rnnoiseFallbackProducesDenoisedWavWhenDeepFilterNetIsUnavailable() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val testContext = instrumentation.context

        ModelManager.installBundledModels(targetContext)
        val model = ModelManager(targetContext).models.value.firstOrNull {
            it.type == com.clarivo.app.core.model.ModelType.DENOISE_DEEPFILTERNET
        }
        assertNotNull("Bundled denoise model should be present", model)

        val inputFile = File(targetContext.cacheDir, "df-fallback-input.wav")
        val outputFile = File(targetContext.cacheDir, "df-fallback-output.wav")
        testContext.assets.open("jfk.wav").use { input ->
            inputFile.outputStream().use { output -> input.copyTo(output) }
        }

        val engine = DeepFilterNetEngine()
        try {
            assert(engine.initialize(model!!)) { "Fallback denoise engine should initialize" }
            engine.denoise(
                inputPcmFile = inputFile,
                outputPcmFile = outputFile,
                audioInfo = AudioFormatInfo(sampleRate = 16000, channels = 1, bitDepth = 16),
                cancellationToken = null
            )

            val inputBytes = inputFile.readBytes()
            val outputBytes = outputFile.readBytes()
            assertEquals("Fallback should preserve WAV container size", inputBytes.size, outputBytes.size)
            assertTrue("Fallback should preserve WAV header", inputBytes.copyOfRange(0, 44).contentEquals(outputBytes.copyOfRange(0, 44)))
            assertFalse(
                "RNNoise fallback should alter PCM samples instead of passing audio through",
                inputBytes.copyOfRange(44, inputBytes.size).contentEquals(outputBytes.copyOfRange(44, outputBytes.size))
            )
        } finally {
            engine.release()
            inputFile.delete()
            outputFile.delete()
        }
    }
}
