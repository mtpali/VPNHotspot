package be.mygod.vpnhotspot.root

import android.os.RemoteException
import android.provider.Settings
import be.mygod.librootkotlinx.RootCommandNoResult
import be.mygod.librootkotlinx.io.awaitExit
import be.mygod.librootkotlinx.io.openReadChannel
import be.mygod.librootkotlinx.io.startPipes
import be.mygod.vpnhotspot.util.Services
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class SettingsGlobalPut(val name: String, val value: String) : RootCommandNoResult {
    companion object {
        suspend fun int(name: String, value: Int) {
            try {
                check(Settings.Global.putInt(Services.context.contentResolver, name, value))
            } catch (e: SecurityException) {
                try {
                    RootManager.use { it.execute(SettingsGlobalPut(name, value.toString())) }
                } catch (eRoot: Exception) {
                    eRoot.addSuppressed(e)
                    throw eRoot
                }
            }
        }
    }

    override suspend fun execute() = null.also {
        val (exit, output) = ProcessBuilder("/system/bin/settings", "put", "global", name, value).apply {
            redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
        }.startPipes(stdin = false).use { pipes ->
            var stdout: ByteReadChannel? = null
            var stderr: ByteReadChannel? = null
            try {
                stdout = pipes.requireStdout().openReadChannel(Services.mainHandler)
                stderr = pipes.requireStderr().openReadChannel(Services.mainHandler)
                coroutineScope {
                    val stdoutText = async { stdout.toByteArray().decodeToString() }
                    val stderrText = async { stderr.toByteArray().decodeToString() }
                    pipes.process.awaitExit() to stdoutText.await() + stderrText.await()
                }
            } finally {
                stdout?.cancel(null)
                stderr?.cancel(null)
            }
        }
        if (exit != 0 || output.isNotEmpty()) throw RemoteException("Process exited with $exit: $output")
    }
}
