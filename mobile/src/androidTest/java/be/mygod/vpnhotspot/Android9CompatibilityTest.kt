package be.mygod.vpnhotspot

import android.net.LocalServerSocket
import android.os.Build
import android.os.Process
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import be.mygod.vpnhotspot.net.wifi.WifiDoubleLock
import be.mygod.vpnhotspot.root.daemon.ClientEnvelope
import be.mygod.vpnhotspot.root.daemon.DaemonEnvelope
import be.mygod.vpnhotspot.root.daemon.DaemonIpc
import be.mygod.vpnhotspot.root.daemon.ReadTrafficCountersCommand
import dalvik.system.BaseDexClassLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SdkSuppress(minSdkVersion = 28, maxSdkVersion = 28)
class Android9CompatibilityTest {
    @Test
    fun mainActivityStartsOnAndroid9() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertTrue(!it.isFinishing) }
        }
    }

    @Test
    fun restoredLowLatencyPreferenceUsesSupportedMode() {
        val pref = App.app.pref
        val original = pref.getString("service.wifiLock", null)
        try {
            WifiDoubleLock.mode = WifiDoubleLock.Mode.LowLatency
            assertEquals(WifiDoubleLock.Mode.HighPerf, WifiDoubleLock.mode)
        } finally {
            pref.edit().apply {
                if (original == null) remove("service.wifiLock") else putString("service.wifiLock", original)
            }.commit()
        }
    }

    @Test(timeout = 30_000)
    fun extractedNativeDaemonStartsAndRepliesWithoutNewerElfImports() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val path = (context.classLoader as BaseDexClassLoader).findLibrary("vpnhotspotd")
        assertNotNull(path)
        assertTrue("Daemon must be extracted on API 28", File(path!!).isFile)
        val name = "${context.packageName}.android9.test.${Process.myPid()}"
        val acceptor = Executors.newSingleThreadExecutor()
        LocalServerSocket(name).use { server ->
            // A real daemon is launched by the root service, outside the app zygote seccomp filter.
            // The API-28 image loads system UBSan constructors that use readlink during exec.
            val output = instrumentation.uiAutomation.executeShellCommand(
                "$path $name ${Build.VERSION.SDK_INT}")
            val daemonOutput = ParcelFileDescriptor.AutoCloseInputStream(output)
            try {
                acceptor.submit<android.net.LocalSocket> { server.accept() }.get(10, TimeUnit.SECONDS).use { socket ->
                    socket.soTimeout = 5_000
                    val packet = ClientEnvelope(call_id = 1, read_traffic_counters = ReadTrafficCountersCommand()).encode()
                    DataOutputStream(socket.outputStream).apply {
                        writeInt(packet.size)
                        write(packet)
                        flush()
                    }
                    val input = DataInputStream(socket.inputStream)
                    val length = input.readInt()
                    assertTrue(length in 1..DaemonIpc.MAX_FRAME_SIZE)
                    val reply = DaemonEnvelope.ADAPTER.decode(ByteArray(length).also { input.readFully(it) }).reply
                    assertNotNull(reply)
                    assertEquals(1L, reply!!.call_id)
                    assertNotNull(reply.traffic_counters)
                    assertTrue(reply.traffic_counters!!.counters.isEmpty())
                }
                // EOF verifies that the externally launched daemon stopped after IPC disconnect.
                daemonOutput.readBytes()
            } finally {
                daemonOutput.close()
                acceptor.shutdownNow()
            }
        }
    }
}
