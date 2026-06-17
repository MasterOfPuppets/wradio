package pt.pauloliveira.wradio.service.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight file logger for playback diagnostics.
 *
 * Output file:
 *   <app files>/logs/playback_diagnostics.log
 *
 * Rotation strategy:
 *   - if log grows above MAX_BYTES, rename to .bak and start fresh.
 */
class PlaybackDiagnosticsLogger(context: Context) {

    companion object {
        private const val LOG_DIR = "logs"
        private const val LOG_FILE = "playback_diagnostics.log"
        private const val LOG_BAK_FILE = "playback_diagnostics.log.bak"
        private const val MAX_BYTES = 800_000L
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logsDir: File = File(context.filesDir, LOG_DIR).apply { mkdirs() }
    private val logFile: File = File(logsDir, LOG_FILE)
    private val bakFile: File = File(logsDir, LOG_BAK_FILE)

    @Synchronized
    fun log(event: String, details: Map<String, Any?> = emptyMap()) {
        try {
            rotateIfNeeded()
            val timestamp = dateFormat.format(Date())
            val detailText = if (details.isEmpty()) {
                ""
            } else {
                details.entries.joinToString(", ") { (k, v) -> "$k=${v ?: "null"}" }
            }
            val line = if (detailText.isEmpty()) {
                "$timestamp | $event\n"
            } else {
                "$timestamp | $event | $detailText\n"
            }
            logFile.appendText(line)
        } catch (_: Exception) {
            // Diagnostics logger must never crash playback flow.
        }
    }

    fun getAbsolutePath(): String = logFile.absolutePath

    @Synchronized
    private fun rotateIfNeeded() {
        if (!logFile.exists()) return
        if (logFile.length() <= MAX_BYTES) return

        if (bakFile.exists()) {
            bakFile.delete()
        }
        logFile.renameTo(bakFile)
        logFile.writeText("")
    }
}

