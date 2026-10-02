package be.mygod.vpnhotspot

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import be.mygod.vpnhotspot.client.ClientViewModel
import be.mygod.vpnhotspot.net.wifi.WifiDoubleLock
import be.mygod.vpnhotspot.ui.VpnHotspotApp
import be.mygod.vpnhotspot.ui.theme.VpnHotspotTheme
import be.mygod.vpnhotspot.util.launchUrl
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.createConfigurationContext(
            Configuration(newBase.resources.configuration).apply { setLocale(Locale.ENGLISH) },
        ))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        val model by viewModels<ClientViewModel>()
        lifecycle.addObserver(model)
        WifiDoubleLock.ActivityListener(this)
        setContent {
            val context = LocalContext.current
            val uriHandler = remember(context) {
                object : UriHandler {
                    override fun openUri(uri: String) = context.launchUrl(uri)
                }
            }
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                VpnHotspotTheme { VpnHotspotApp(model) }
            }
        }
    }
}
