package com.fvoice.app.core.asr

import com.fvoice.app.core.model.TranscriptResult
import com.fvoice.app.core.model.TranscriptSegment
import com.fvoice.app.util.FVoiceLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

class TranscriptExporter {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportTxt(result: TranscriptResult, outputFile: File) {
        val text = result.segments.joinToString("\n\n") { it.text }
        outputFile.writeText(text)
        FVoiceLogger.i("Exported TXT: ${outputFile.absolutePath}")
    }

    fun exportSrt(result: TranscriptResult, outputFile: File) {
        val lines = result.segments.mapIndexed { index, segment ->
            buildString {
                appendLine(index + 1)
                appendLine("${formatSrtTime(segment.startMs)} --> ${formatSrtTime(segment.endMs)}")
                appendLine(segment.text)
            }
        }
        outputFile.writeText(lines.joinToString("\n"))
        FVoiceLogger.i("Exported SRT: ${outputFile.absolutePath}")
    }

    fun exportVtt(result: TranscriptResult, outputFile: File) {
        val lines = buildList {
            add("WEBVTT")
            add("")
            result.segments.forEach { segment ->
                add("${formatVttTime(segment.startMs)} --> ${formatVttTime(segment.endMs)}")
                add(segment.text)
                add("")
            }
        }
        outputFile.writeText(lines.joinToString("\n"))
        FVoiceLogger.i("Exported VTT: ${outputFile.absolutePath}")
    }

    fun exportJson(result: TranscriptResult, outputFile: File) {
        outputFile.writeText(json.encodeToString(result))
        FVoiceLogger.i("Exported JSON: ${outputFile.absolutePath}")
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun formatVttTime(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}
