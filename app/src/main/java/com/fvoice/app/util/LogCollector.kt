package com.fvoice.app.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志收集器。
 *
 * 参考 KernelSU 的 bugreport 设计，收集以下内容并打包为 zip：
 * - 应用本地日志文件（FVoiceLogger 输出）
 * - logcat 中本应用的日志
 * - 设备基本信息
 * - 应用版本与配置信息
 *
 * 所有日志仅本地打包，由用户手动选择分享渠道，不会自动上传。
 */
object LogCollector {

    /**
     * 生成 bugreport 压缩包并返回文件对象。
     * 该函数应在 IO 线程调用。
     */
    fun collect(context: Context): File {
        val reportDir = File(context.cacheDir, "bugreport").apply { mkdirs() }

        // 1. 收集应用本地日志
        val appLogsDir = File(reportDir, "app_logs")
        appLogsDir.mkdirs()
        FVoiceLogger.getLogFiles().forEach { logFile ->
            logFile.copyTo(File(appLogsDir, logFile.name), overwrite = true)
        }

        // 2. 收集 logcat（仅本应用）
        val logcatFile = File(reportDir, "logcat.txt")
        runCatching {
            Runtime.getRuntime().exec("logcat -d --pid=${android.os.Process.myPid()} -v threadtime")
                .inputStream.use { input ->
                    logcatFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
        }.onFailure {
            logcatFile.writeText("Failed to collect logcat: ${it.message}")
        }

        // 3. 收集设备与应用基本信息
        val basicFile = File(reportDir, "basic.txt")
        PrintWriter(FileWriter(basicFile)).use { pw ->
            pw.println("=== FVoice Bugreport ===")
            pw.println("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            pw.println()
            pw.println("--- Device Info ---")
            pw.println("BRAND: ${Build.BRAND}")
            pw.println("MODEL: ${Build.MODEL}")
            pw.println("DEVICE: ${Build.DEVICE}")
            pw.println("PRODUCT: ${Build.PRODUCT}")
            pw.println("MANUFACTURER: ${Build.MANUFACTURER}")
            pw.println("SDK: ${Build.VERSION.SDK_INT}")
            pw.println("RELEASE: ${Build.VERSION.RELEASE}")
            pw.println("FINGERPRINT: ${Build.FINGERPRINT}")
            pw.println()
            pw.println("--- App Info ---")
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pw.println("Package: ${context.packageName}")
            pw.println("VersionName: ${pkgInfo.versionName}")
            pw.println("VersionCode: ${pkgInfo.longVersionCode}")
            pw.println()
            pw.println("--- System ---")
            pw.println("Kernel: ${System.getProperty("os.version")}")
            pw.println("Java VM: ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
        }

        // 4. 收集应用配置信息（脱敏，不包含用户文件路径）
        val prefsFile = File(reportDir, "prefs.txt")
        runCatching {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            PrintWriter(FileWriter(prefsFile)).use { pw ->
                pw.println("=== SharedPreferences (settings) ===")
                prefs.all.forEach { (k, v) ->
                    // 简单脱敏：不输出可能包含个人路径的 key
                    if (!k.contains("path", ignoreCase = true)) {
                        pw.println("$k = $v")
                    } else {
                        pw.println("$k = <redacted>")
                    }
                }
            }
        }

        // 5. 打包为 zip
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
        val current = LocalDateTime.now().format(formatter)
        val targetFile = File(context.cacheDir, "FVoice_bugreport_${current}.zip")

        ZipOutputStream(targetFile.outputStream().buffered()).use { zos ->
            reportDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(reportDir).path.replace("\\", "/")
                    val entry = ZipEntry(entryName)
                    entry.time = file.lastModified()
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // 6. 清理临时目录
        reportDir.deleteRecursively()

        return targetFile
    }
}
