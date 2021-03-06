package be.mygod.vpnhotspot.util

import android.util.Log
import be.mygod.vpnhotspot.App.Companion.app
import com.crashlytics.android.Crashlytics
import java.io.IOException
import java.io.InputStream

private const val NOISYSU_TAG = "NoisySU"
private const val NOISYSU_SUFFIX = "SUCCESS\n"

private class SuFailure(msg: String?) : RuntimeException(msg)

fun loggerSuStream(command: String): InputStream? {
    val process = try {
        ProcessBuilder("su", "-c", command)
                .directory(app.deviceStorage.cacheDir)
                .redirectErrorStream(true)
                .start()
    } catch (e: IOException) {
        e.printStackTrace()
        Crashlytics.logException(e)
        return null
    }
    return process.inputStream
}

fun loggerSu(command: String): String? {
    val stream = loggerSuStream(command) ?: return null
    return try {
        stream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        Crashlytics.logException(e)
        null
    }
}

fun noisySu(commands: Iterable<String>, report: Boolean = true): Boolean? {
    var out = loggerSu("""function noisy() { "$@" || echo "$@" exited with $?; }
${commands.joinToString("\n") { if (it.startsWith("quiet ")) it.substring(6) else "noisy $it" }}
echo $NOISYSU_SUFFIX""")
    val result = if (out == null) null else out == NOISYSU_SUFFIX
    out = out?.removeSuffix(NOISYSU_SUFFIX)
    if (!out.isNullOrBlank()) {
        Crashlytics.log(Log.INFO, NOISYSU_TAG, out)
        if (report) Crashlytics.logException(SuFailure(out))
    }
    return result
}
fun noisySu(vararg commands: String, report: Boolean = true) = noisySu(commands.asIterable(), report)
