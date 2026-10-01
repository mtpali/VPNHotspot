package be.mygod.vpnhotspot

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.StrictMode
import androidx.core.content.getSystemService
import be.mygod.vpnhotspot.room.AppDatabase
import be.mygod.vpnhotspot.root.RootManager
import be.mygod.vpnhotspot.util.DeviceStorageApp
import be.mygod.vpnhotspot.util.InPlaceExecutor
import be.mygod.vpnhotspot.util.Services
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: App
    }

    init {
        // overhead of debug mode is minimal: https://github.com/Kotlin/kotlinx.coroutines/blob/f528898/docs/debugging.md#debug-mode
        if (BuildConfig.DEBUG) System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
        deviceStorage = DeviceStorageApp(this)
        @Suppress("DEPRECATION")
        deviceStorage.moveSharedPreferencesFrom(this,
            android.preference.PreferenceManager.getDefaultSharedPreferencesName(this))
        deviceStorage.moveDatabaseFrom(this, AppDatabase.DB_NAME)
        Services.init { this }

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().apply {
            if (BuildConfig.DEBUG) detectAll() else detectNetwork()
        }.penaltyListener(InPlaceExecutor) { Timber.w(it, "StrictMode thread policy violation") }.build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().apply {
            if (BuildConfig.DEBUG) detectAll() else detectFileUriExposure()
        }.penaltyListener(InPlaceExecutor) { Timber.w(it, "StrictMode VM policy violation") }.build())
    }

    override fun onCreate() {
        super.onCreate()
        ServiceNotification.updateNotificationChannels()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ServiceNotification.updateNotificationChannels()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_BACKGROUND) GlobalScope.launch {
            RootManager.closeExisting()
        }
    }

    lateinit var deviceStorage: Application
    val english by lazy {
        createConfigurationContext(Configuration(resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        })
    }
    @Suppress("DEPRECATION")
    val pref by lazy { android.preference.PreferenceManager.getDefaultSharedPreferences(deviceStorage) }
    val clipboard by lazy { getSystemService<ClipboardManager>()!! }

    val hasTouch by lazy { packageManager.hasSystemFeature("android.hardware.faketouch") }
}
