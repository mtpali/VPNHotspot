package be.mygod.vpnhotspot.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Html
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.mygod.vpnhotspot.App.Companion.app
import be.mygod.vpnhotspot.R
import be.mygod.vpnhotspot.RoutingManager
import be.mygod.vpnhotspot.net.Routing.Ipv6Mode
import be.mygod.vpnhotspot.net.TetherOffloadManager
import be.mygod.vpnhotspot.net.monitor.Upstream
import be.mygod.vpnhotspot.net.monitor.Upstreams
import be.mygod.vpnhotspot.net.wifi.WifiDoubleLock
import be.mygod.vpnhotspot.root.daemon.MasqueradeMode
import be.mygod.vpnhotspot.ui.theme.VpnHotspotPreviewSurface
import be.mygod.vpnhotspot.util.ContactRoute
import be.mygod.vpnhotspot.util.Services
import be.mygod.vpnhotspot.util.allInterfaceNames
import be.mygod.vpnhotspot.util.allRoutes
import be.mygod.vpnhotspot.util.globalNetworkRequestBuilder
import be.mygod.vpnhotspot.util.readableMessage
import java.net.InetAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun SettingsScreen(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var offloadEnabled by remember { mutableStateOf(!inspectionMode && TetherOffloadManager.enabled) }
    var offloadChanging by remember { mutableStateOf(false) }
    val primaryUpstream by if (inspectionMode) {
        remember { mutableStateOf<Upstream?>(null) }
    } else Upstreams.primary.collectAsStateWithLifecycle()
    val fallbackUpstream by if (inspectionMode) {
        remember { mutableStateOf<Upstream?>(null) }
    } else Upstreams.fallback.collectAsStateWithLifecycle()
    val primaryPreference by if (inspectionMode) {
        remember { mutableStateOf<String?>(null) }
    } else rememberPreferenceString(Upstreams.KEY_PRIMARY)
    val fallbackPreference by if (inspectionMode) {
        remember { mutableStateOf<String?>(null) }
    } else rememberPreferenceString(Upstreams.KEY_FALLBACK)
    var masqueradeMode by remember {
        mutableStateOf(if (inspectionMode) {
            MasqueradeMode.MASQUERADE_MODE_SIMPLE
        } else RoutingManager.masqueradeMode)
    }
    var ipv6Mode by remember {
        mutableStateOf(if (inspectionMode) Ipv6Mode.Block else RoutingManager.ipv6Mode)
    }
    var wifiLockMode by remember {
        mutableStateOf(if (inspectionMode) WifiDoubleLock.Mode.None else WifiDoubleLock.mode)
    }
    if (!inspectionMode) {
        LaunchedEffect(Unit) {
            WifiDoubleLock.mode = WifiDoubleLock.mode
            wifiLockMode = WifiDoubleLock.mode
            RoutingManager.masqueradeMode = RoutingManager.masqueradeMode
            masqueradeMode = RoutingManager.masqueradeMode
            RoutingManager.ipv6Mode = RoutingManager.ipv6Mode
            ipv6Mode = RoutingManager.ipv6Mode
        }
        DisposableEffect(lifecycleOwner, offloadChanging) {
            fun refresh() {
                masqueradeMode = RoutingManager.masqueradeMode
                ipv6Mode = RoutingManager.ipv6Mode
                wifiLockMode = WifiDoubleLock.mode
                if (!offloadChanging) offloadEnabled = TetherOffloadManager.enabled
            }
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) = refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refresh()
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
    SettingsList {
        preferenceGroup(key = R.string.settings_service_clean) {
            row(R.string.settings_service_clean) {
                PreferenceRow(
                    icon = R.drawable.ic_cleaning_services,
                    title = stringResource(R.string.settings_service_clean),
                    summary = stringResource(R.string.settings_service_clean_summary),
                    onClick = { if (!inspectionMode) RoutingManager.clean() },
                )
            }
        }
        preferenceGroup(title = R.string.settings_upstream) {
            row(R.string.settings_service_upstream) {
                val fallback = stringResource(R.string.settings_service_upstream_auto)
                TextPreferenceRow(
                    icon = R.drawable.ic_route,
                    title = R.string.settings_service_upstream,
                    value = primaryPreference.orEmpty(),
                    summary = upstreamSummary(
                        fallback = fallback,
                        preference = primaryPreference,
                        upstream = primaryUpstream,
                    ),
                    description = annotatedStringResource(R.string.settings_service_upstream_help),
                    placeholder = fallback,
                    onValueChange = {
                        if (!inspectionMode) app.pref.edit { putString(Upstreams.KEY_PRIMARY, it.ifBlank { null }) }
                    },
                )
            }
            row(R.string.settings_upstream_fallback) {
                val fallbackLabel = stringResource(R.string.settings_upstream_fallback_auto)
                TextPreferenceRow(
                    icon = R.drawable.ic_alt_route,
                    title = R.string.settings_upstream_fallback,
                    value = fallbackPreference.orEmpty(),
                    summary = upstreamSummary(
                        fallback = fallbackLabel,
                        preference = fallbackPreference,
                        upstream = fallbackUpstream,
                    ),
                    description = annotatedStringResource(R.string.settings_upstream_fallback_help),
                    placeholder = fallbackLabel,
                    onValueChange = {
                        if (!inspectionMode) {
                            app.pref.edit { putString(Upstreams.KEY_FALLBACK, it.ifBlank { null }) }
                        }
                    },
                )
            }
        }
        preferenceGroup(title = R.string.settings_downstream) {
            row(R.string.settings_service_masquerade) {
                ListPreferenceRow(
                    icon = R.drawable.ic_nat,
                    title = R.string.settings_service_masquerade,
                    entries = listOf(
                        stringResource(R.string.settings_service_masquerade_none),
                        stringResource(R.string.settings_service_masquerade_simple),
                        stringResource(R.string.settings_service_masquerade_netd),
                    ),
                    entrySummaries = listOf(
                        annotatedStringResource(R.string.settings_service_masquerade_none_summary),
                        annotatedStringResource(R.string.settings_service_masquerade_simple_summary),
                        annotatedStringResource(R.string.settings_service_masquerade_netd_summary),
                    ),
                    values = listOf(
                        MasqueradeMode.MASQUERADE_MODE_NONE,
                        MasqueradeMode.MASQUERADE_MODE_SIMPLE,
                        MasqueradeMode.MASQUERADE_MODE_NETD,
                    ),
                    selectedValue = masqueradeMode,
                    onValueChange = {
                        masqueradeMode = it
                        if (!inspectionMode) RoutingManager.masqueradeMode = it
                    },
                )
            }
            row(R.string.settings_service_ipv6_mode) {
                ListPreferenceRow(
                    icon = R.drawable.ic_counter_6,
                    title = R.string.settings_service_ipv6_mode,
                    entries = listOf(
                        stringResource(R.string.settings_service_ipv6_mode_system),
                        stringResource(R.string.settings_service_ipv6_mode_block),
                        stringResource(R.string.settings_service_ipv6_mode_nat),
                    ),
                    entrySummaries = listOf(
                        annotatedStringResource(R.string.settings_service_ipv6_mode_system_summary),
                        annotatedStringResource(R.string.settings_service_ipv6_mode_block_summary),
                        annotatedStringResource(R.string.settings_service_ipv6_mode_nat_summary),
                    ),
                    values = listOf(Ipv6Mode.System, Ipv6Mode.Block, Ipv6Mode.Nat),
                    selectedValue = ipv6Mode,
                    onValueChange = {
                        ipv6Mode = it
                        if (!inspectionMode) RoutingManager.ipv6Mode = it
                    },
                )
            }
            row(R.string.settings_system_tether_offload) {
                SwitchPreferenceRow(
                    icon = R.drawable.ic_speed,
                    title = R.string.settings_system_tether_offload,
                    summary = stringResource(R.string.settings_system_tether_offload_summary),
                    checked = offloadEnabled,
                    enabled = !offloadChanging,
                    onCheckedChange = { enabled ->
                        if (inspectionMode) {
                            offloadEnabled = enabled
                            return@SwitchPreferenceRow
                        }
                        if (TetherOffloadManager.enabled == enabled) return@SwitchPreferenceRow
                        scope.launch {
                            offloadChanging = true
                            try {
                                TetherOffloadManager.setEnabled(enabled)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.w(e)
                                snackbarHostState.showLongSnackbar(e.readableMessage)
                            } finally {
                                offloadEnabled = TetherOffloadManager.enabled
                                offloadChanging = false
                            }
                        }
                    },
                )
            }
        }
        preferenceGroup(title = R.string.settings_misc) {
            row(R.string.settings_service_wifi_lock) {
                ListPreferenceRow(
                    icon = R.drawable.ic_wifi_lock,
                    title = R.string.settings_service_wifi_lock,
                    entries = listOf(
                        stringResource(R.string.settings_service_wifi_lock_none),
                        stringResource(R.string.settings_service_wifi_lock_high_perf_v29),
                        stringResource(R.string.settings_service_wifi_lock_low_latency),
                    ).let { if (Build.VERSION.SDK_INT < 29) it.take(2) else it },
                    entrySummaries = listOf(
                        annotatedStringResource(R.string.settings_service_wifi_lock_none_summary),
                        annotatedStringResource(R.string.settings_service_wifi_lock_high_perf_v29_summary),
                        annotatedStringResource(R.string.settings_service_wifi_lock_low_latency_summary),
                    ).let { if (Build.VERSION.SDK_INT < 29) it.take(2) else it },
                    values = listOf(
                        WifiDoubleLock.Mode.None,
                        WifiDoubleLock.Mode.HighPerf,
                        WifiDoubleLock.Mode.LowLatency,
                    ).let { if (Build.VERSION.SDK_INT < 29) it.take(2) else it },
                    selectedValue = wifiLockMode,
                    onValueChange = {
                        wifiLockMode = it
                        if (!inspectionMode) WifiDoubleLock.mode = it
                    },
                )
            }
        }
        preferenceGroup(key = "developer") {
            row("contact") {
                PreferenceRow(
                    icon = R.drawable.ic_code,
                    title = remember { ContactRoute.label },
                    onClick = { if (!inspectionMode) ContactRoute.open(context) },
                )
            }
        }
    }
}

@Composable
private fun <T> ListPreferenceRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    entries: List<String>,
    entrySummaries: List<AnnotatedString>,
    values: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
) {
    var selecting by rememberSaveable { mutableStateOf(false) }
    val selectedIndex = values.indexOf(selectedValue)
    PreferenceRow(
        icon = icon,
        title = stringResource(title),
        summary = entries.getOrElse(selectedIndex) { "" },
        onClick = { selecting = true },
    )
    if (selecting) PreferenceSelectionSheet(
        title = stringResource(title),
        entryCount = entries.size,
        selectedIndex = selectedIndex,
        entryLabel = entries::get,
        entrySummary = entrySummaries::getOrNull,
        onDismissRequest = { selecting = false },
        onSelect = { values.getOrNull(it)?.let(onValueChange) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TextPreferenceRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    value: String,
    summary: AnnotatedString,
    description: AnnotatedString,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberTextFieldValueAtEnd(value, editing)
    var suggestionsExpanded by remember(editing) { mutableStateOf(false) }
    val suggestions by rememberInterfaceNameSuggestions(!LocalInspectionMode.current && editing)
    val filteredSuggestions = remember(suggestions, draft.text) {
        suggestions.filter { draft.text.isBlank() || it.contains(draft.text, ignoreCase = true) }
    }
    val titleText = stringResource(title)
    LaunchedEffect(editing, suggestions) {
        if (editing && suggestions.isNotEmpty()) suggestionsExpanded = true
    }
    PreferenceRow(
        icon = icon,
        title = titleText,
        summaryContent = { Text(summary) },
        onClick = { editing = true },
    )
    if (editing) {
        val focusRequester = rememberDialogFocusRequester()
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(titleText) },
            text = {
                val menuExpanded = suggestionsExpanded && filteredSuggestions.isNotEmpty()
                val menuScrollState = rememberScrollState()
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { suggestionsExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                suggestionsExpanded = true
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .semantics { contentDescription = titleText },
                            placeholder = { Text(placeholder) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = menuExpanded,
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                                )
                            },
                            colors = OutlinedTextFieldDefaults.tonalColors(),
                            shape = OutlinedTextFieldDefaults.roundedShape,
                            singleLine = true,
                        )
                        ExposedDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { suggestionsExpanded = false },
                            modifier = Modifier.nonInteractiveVerticalScrollbar(menuScrollState.scrollIndicatorState),
                            scrollState = menuScrollState,
                        ) {
                            for (suggestion in filteredSuggestions) DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    draft = TextFieldValue(suggestion, TextRange(suggestion.length))
                                    suggestionsExpanded = false
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                DialogConfirmButton(onClick = {
                    onValueChange(draft.text)
                    editing = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                DialogDismissButton(onClick = { editing = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun rememberInterfaceNameSuggestions(active: Boolean): State<List<String>> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState(initialValue = emptyList(), active, lifecycleOwner) {
        if (!active) return@produceState
        val interfaceNames = mutableMapOf<Network, List<String>>()
        fun update() {
            value = interfaceNames.values.flatten().distinct().sorted()
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
                interfaceNames[network] = properties.allInterfaceNames
                update()
            }

            override fun onLost(network: Network) {
                interfaceNames.remove(network)
                update()
            }
        }
        var registered = false
        fun register() {
            if (!registered) {
                Services.registerNetworkCallback(globalNetworkRequestBuilder().apply {
                    removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                }.build(), callback)
                registered = true
            }
        }
        fun unregister() {
            if (registered) {
                Services.connectivity.unregisterNetworkCallback(callback)
                registered = false
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = register()
            override fun onStop(owner: LifecycleOwner) {
                unregister()
                interfaceNames.clear()
                update()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) register()
        awaitDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

@Composable
private fun SwitchPreferenceRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    PreferenceSwitchRow(
        icon = icon,
        title = stringResource(title),
        summary = summary,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun upstreamSummary(fallback: String, preference: String?, upstream: Upstream?) = AnnotatedString.fromHtml(
    stringResource(R.string.settings_upstream_current_summary,
        Html.escapeHtml(if (preference.isNullOrEmpty()) fallback else preference),
        mutableMapOf<String, Boolean>().let { interfaces ->
            for (route in upstream?.properties?.allRoutes ?: emptyList()) {
                interfaces.compute(route.`interface` ?: continue) { _, internet ->
                    internet == true || try {
                        route.matches(UPSTREAM_INTERNET_V4_ADDRESS) || route.matches(UPSTREAM_INTERNET_V6_ADDRESS)
                    } catch (e: RuntimeException) {
                        Timber.w(e)
                        false
                    }
                }
            }
            if (interfaces.isEmpty()) "\u2205" else buildString {
                interfaces.entries.forEachIndexed { index, (iface, internet) ->
                    if (index > 0) append(", ")
                    val escaped = Html.escapeHtml(iface)
                    if (internet) append("<b>").append(escaped).append("</b>") else append(escaped)
                }
            }
        }))

@Composable
private fun rememberPreferenceString(key: String): State<String?> {
    val pref = app.pref
    return produceState(initialValue = pref.getString(key, null), key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changed ->
            if (changed == key) value = sharedPreferences.getString(key, null)
        }
        pref.registerOnSharedPreferenceChangeListener(listener)
        awaitDispose { pref.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

@Composable
private fun rememberPreferenceBoolean(key: String, defaultValue: Boolean): State<Boolean> {
    val pref = app.pref
    return produceState(initialValue = pref.getBoolean(key, defaultValue), key, defaultValue) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changed ->
            if (changed == key) value = sharedPreferences.getBoolean(key, defaultValue)
        }
        pref.registerOnSharedPreferenceChangeListener(listener)
        awaitDispose { pref.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

@Preview(name = "Settings", showBackground = true, widthDp = 420, heightDp = 720)
@Preview(
    name = "Settings - dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsPreview() {
    VpnHotspotPreviewSurface {
        SettingsScreen(snackbarHostState = remember { SnackbarHostState() })
    }
}

private val UPSTREAM_INTERNET_V4_ADDRESS = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
private val UPSTREAM_INTERNET_V6_ADDRESS = InetAddress.getByAddress(byteArrayOf(
    0x20, 0x01, 0x48, 0x60, 0x48, 0x60, 0, 0,
    0, 0, 0, 0, 0, 0, 0x88.toByte(), 0x88.toByte(),
))
