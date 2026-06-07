package com.clarivo.app.util

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Clarivo 本地日志管线。
 *
 * 参考 KernelSU 的日志设计：
 * - 日志仅本地存储，不主动上传任何服务器
 * - 同时输出到 logcat 与本地日志文件
 * - 支持按日期轮转、自动清理旧日志
 * - 提供统一入口便于后续接入发送日志功能
 */
object ClarivoLogger {

    private const val TAG = "Clarivo"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE_PREFIX = "clarivo"
    private const val LOG_FILE_SUFFIX = ".log"
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    private const val MAX_LOG_RETENTION_DAYS = 7
    private const val MAX_BUFFER_SIZE = 256

    private val initialized = AtomicBoolean(false)
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var logDir: File

    @JvmStatic
    fun init(context: Context) {
        if (initialized.getAndSet(true)) return
        logDir = File(context.filesDir, LOG_DIR).apply { mkdirs() }
        scope.launch { flushLoop() }
        cleanOldLogs()
    }

    @JvmStatic
    @JvmOverloads
    fun v(msg: String, tr: Throwable? = null) {
        log(Log.VERBOSE, msg, tr)
    }

    @JvmStatic
    @JvmOverloads
    fun d(msg: String, tr: Throwable? = null) {
        log(Log.DEBUG, msg, tr)
    }

    @JvmStatic
    @JvmOverloads
    fun i(msg: String, tr: Throwable? = null) {
        log(Log.INFO, msg, tr)
    }

    @JvmStatic
    @JvmOverloads
    fun w(msg: String, tr: Throwable? = null) {
        log(Log.WARN, msg, tr)
    }

    @JvmStatic
    @JvmOverloads
    fun e(msg: String, tr: Throwable? = null) {
        log(Log.ERROR, msg, tr)
    }

    private fun log(level: Int, msg: String, tr: Throwable?) {
        // 1. 输出到 logcat
        when (level) {
            Log.VERBOSE -> Log.v(TAG, msg, tr)
            Log.DEBUG -> Log.d(TAG, msg, tr)
            Log.INFO -> Log.i(TAG, msg, tr)
            Log.WARN -> Log.w(TAG, msg, tr)
            Log.ERROR -> Log.e(TAG, msg, tr)
        }

        // 2. 写入内存队列，等待批量刷盘
        if (!initialized.get()) return
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val levelChar = when (level) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            else -> '?'
        }
        val line = buildString {
            append(time)
            append(' ')
            append(levelChar)
            append('/')
            append(TAG)
            append(':')
            append(' ')
            append(msg)
            if (tr != null) {
                append('\n')
                append(tr.stackTraceToString())
            }
        }
        logQueue.offer(line)

        // 队列过大时触发一次同步刷盘
        if (logQueue.size >= MAX_BUFFER_SIZE) {
            scope.launch { flush() }
        }
    }

    private suspend fun flushLoop() {
        while (true) {
            kotlinx.coroutines.delay(2000)
            flush()
        }
    }

    @Synchronized
    private fun flush() {
        if (!::logDir.isInitialized) return
        val currentLog = getCurrentLogFile()
        if (currentLog.length() > MAX_LOG_FILE_SIZE) {
            rotateLog(currentLog)
        }
        FileWriter(currentLog, true).use { writer ->
            while (true) {
                val line = logQueue.poll() ?: break
                writer.appendLine(line)
            }
        }
    }

    private fun getCurrentLogFile(): File {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return File(logDir, "${LOG_FILE_PREFIX}_${date}${LOG_FILE_SUFFIX}")
    }

    private fun rotateLog(current: File) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var index = 1
        while (true) {
            val rotated = File(logDir, "${LOG_FILE_PREFIX}_${date}-${index}${LOG_FILE_SUFFIX}")
            if (!rotated.exists()) {
                current.renameTo(rotated)
                break
            }
            index++
        }
    }

    private fun cleanOldLogs() {
        val cutoff = System.currentTimeMillis() - MAX_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L
        logDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    /** 获取日志目录，供 [LogCollector] 打包使用 */
    fun getLogDir(): File {
        if (!::logDir.isInitialized) throw IllegalStateException("Logger not initialized")
        return logDir
    }

    /** 获取所有日志文件列表 */
    fun getLogFiles(): List<File> {
        if (!::logDir.isInitialized) return emptyList()
        return logDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(LOG_FILE_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /** 获取最近 N 条日志文本（用于 UI 展示） */
    fun getRecentLogs(limit: Int = 500): List<String> {
        val files = getLogFiles().take(3)
        val lines = mutableListOf<String>()
        for (file in files.asReversed()) {
            file.readLines().forEach { lines.add(it) }
        }
        return lines.takeLast(limit)
    }
}
