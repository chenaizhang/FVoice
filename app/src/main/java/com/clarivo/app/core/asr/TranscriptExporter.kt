package com.clarivo.app.core.asr

import com.clarivo.app.core.model.TranscriptResult
import com.clarivo.app.core.model.TranscriptSegment
import com.clarivo.app.util.ClarivoLogger
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
        ClarivoLogger.i("Exported TXT: ${outputFile.absolutePath}")
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
        ClarivoLogger.i("Exported SRT: ${outputFile.absolutePath}")
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
        ClarivoLogger.i("Exported VTT: ${outputFile.absolutePath}")
    }

    fun exportJson(result: TranscriptResult, outputFile: File) {
        outputFile.writeText(json.encodeToString(result))
        ClarivoLogger.i("Exported JSON: ${outputFile.absolutePath}")
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
