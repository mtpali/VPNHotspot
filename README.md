# VPNHotspot OLED

Custom fork of [Mygod/VPNHotspot v3.0.8](https://github.com/Mygod/VPNHotspot/releases/tag/v3.0.8),
based on commit `20ed65762517c801f87cb87e17e70316c072b444`.
Apache-2.0 licensed; original author attribution and third-party notices are retained.

Android 10 (API 29) or newer and root access are required for VPN sharing and traffic accounting.
Two release APKs target `armeabi-v7a` and `arm64-v8a`; both use the same custom release certificate.
This certificate differs from upstream, so the custom APK cannot update the original signed app in place.
Stop active VPN tethering before uninstalling or switching builds.

## Changes

- Removed repeater, temporary Wi-Fi hotspot and static-IP UI, services, quick-setting tiles,
  configuration paths, Supplicant AIDL generation and associated source/resources.
- Removed service auto-start and its boot receiver, debug export, donation and project-homepage UI.
- Removed the Manage system tethering shortcut from the Tethering screen.
- Removed the open-source-license screen; distribution notices remain in the APK assets and this source.
- Removed Firebase analytics, Crashlytics, LeakCanary and its debug telemetry installer,
  the browser helper and Google license dependencies.
- Added the contact action labelled `t.me/VPN963`, with Telegram and web fallback.
  The encrypted label is decoded once when Settings is displayed; destination URLs are decoded on click.
  R8 renames the decoder and prevents it being folded into literal contact strings.
  A public source fork and a locally running APK can always be modified; this is obfuscation, not tamper-proofing.
- OLED black surfaces and white foregrounds, no dynamic colors, instant page transitions, no navigation shadow.
- English only: translated and regional resource folders removed, English-only locale configuration,
  dependency resources filtered to English, and debug pseudo-locales disabled.
  Activity resources use English so platform dialog labels and numbers stay English on non-English devices.
- Launcher uses the supplied icon with a 60dp square inside the 108dp adaptive canvas for wider safe margins.
  The monochrome icon is scaled to match; both are one 12dp step smaller than the previous 72dp release.
- Clients show an estimated brand/model above MAC, from DHCP hostname and 6,614 offline IEEE OUI prefixes.
  Random/local MACs do not get an OUI guess. Phone model identification is best-effort.
  The device name is displayed directly without a label prefix.
- Clients show speed and cumulative recorded upload/download. Counters refresh every 5 seconds while
  Clients is visible, and once per minute in the background. No extra polling service or per-phone requests.
  Usage includes previous recorded sessions; uncounted traffic is described in `docs/vpnhotspotd/traffic.md`.
- R8 full mode, name obfuscation, optimized resource shrinking, ABI splits and stripped Rust native binaries.
  Room schema 3 adds a MAC index while retaining existing statistics.
- Native build input tracking excludes Cargo target caches, avoiding unnecessary rebuilds.

## Build

Install JDK 21, Android platform 37.0/build-tools 37.0.0, NDK 29.0.14206865 and Rust 1.99.0.

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi
cargo install cargo-ndk --version 4.1.2 --locked
./gradlew :mobile:assembleRelease :mobile:testDebugUnitTest :mobile:lintRelease
```

Set `ANDROID_HOME` and `ANDROID_NDK_HOME`, or use Android Studio SDK settings.
The two unsigned release APKs are in `mobile/build/outputs/apk/release/`.
Use `scripts/sign-release-apks.sh` with your private keystore to zipalign, sign and verify both APKs.
No signing key/password is stored in this public repository. Retain the delivered signing backup privately
to produce compatible future updates. R8 mapping is generated in `mobile/build/outputs/mapping/release/`.

## System changes and cleanup

Hotspot configuration edited through the standard Wi-Fi configuration screen is persisted by Android.
The global tethering-hardware-offload setting is also persistent; toggle it back here or in Android Developer options.
VPN routing/firewall/NAT66 state retains upstream reversible Stop/Clean cleanup. No static loopback addresses
are created by this fork. Stop an older build's active services before switching builds.

Removed platform API descriptors and exact CSV checks are recorded in [docs/api-removals.md](docs/api-removals.md).

## Compatibility index

The remaining upstream API assumptions below retain their original classifications.
Removed feature descriptors were checked against the upstream-pinned `hiddenapi-flags.csv` checksum
`9102af02fe6ab68b92464bdff5e5b09f3bd62c65d1130aaf85d3296f17d38074` without changing surviving flag suffixes.

## Private APIs used / Assumptions for Android customizations

_a.k.a. things that can go wrong if this app doesn't work._

This is a list of stuff that might impact this app's functionality if unavailable.
This is only meant to be an index.
You can read more in the source code.
API restrictions are updated up to [SHA-256 checksum `9102af02fe6ab68b92464bdff5e5b09f3bd62c65d1130aaf85d3296f17d38074`](https://github.com/Mygod/hiddenapi/commit/2f90e9da30976febeb0630cba48c4da0116c323d).

Greylisted/blacklisted APIs or internal constants: (some constants are hardcoded or implicitly used)

* (prior to API 30) `Landroid/net/ConnectivityManager;->getLastTetherError(Ljava/lang/String;)I,max-target-r`
* (prior to API 30) `Landroid/net/ConnectivityManager;->EXTRA_ACTIVE_TETHER:Ljava/lang/String;,max-target-r`
* (prior to API 30) `Landroid/net/ConnectivityManager;->EXTRA_AVAILABLE_TETHER:Ljava/lang/String;,max-target-r`
* (prior to API 30) `Landroid/net/ConnectivityManager;->ACTION_TETHER_STATE_CHANGED:Ljava/lang/String;,max-target-r`
* (prior to API 30) `Landroid/net/ConnectivityManager;->EXTRA_ERRORED_TETHER:Ljava/lang/String;,max-target-r`
* (since API 30) `Landroid/net/ConnectivityModuleConnector;->IN_PROCESS_SUFFIX:Ljava/lang/String;`
* (since API 31) `Landroid/net/INetd$Stub;->asInterface(Landroid/os/IBinder;)Landroid/net/INetd;`
* (since API 31) `Landroid/net/INetd;->ipSecUpdateSecurityPolicy(IIILjava/lang/String;Ljava/lang/String;IIII)V`
* (since API 30) `Landroid/net/IIntResultListener$Stub;-><init>()V,blocked`
* (since API 30) `Landroid/net/IIntResultListener;->onResult(I)V,blocked`
* (since API 30) `Landroid/net/ITetheringConnector;->stopTethering(ILjava/lang/String;Landroid/net/IIntResultListener;)V,blocked`
* (since API 30) `Landroid/net/ITetheringConnector;->stopTethering(ILjava/lang/String;Ljava/lang/String;Landroid/net/IIntResultListener;)V,blocked`
* (since API 30) `Landroid/net/TetheringManager$ConnectorConsumer;->onConnectorAvailable(Landroid/net/ITetheringConnector;)V,blocked`
* (since API 30) `Landroid/net/TetheringManager$TetheringEventCallback;->onTetherableInterfaceRegexpsChanged(Landroid/net/TetheringManager$TetheringInterfaceRegexps;)V,blocked`
* (since API 31) `Landroid/net/TetheringManager$TetheringEventCallback;->onSupportedTetheringTypes(Ljava/util/Set;)V,blocked`
* (since API 30) `Landroid/net/TetheringManager;->getConnector(Landroid/net/TetheringManager$ConnectorConsumer;)V,blocked`
* `Landroid/net/TetheringManager;->TETHER_ERROR_*:I,blocked`
* (since API 30) `Landroid/net/TetheringManager;->TETHERING_VIRTUAL:I,blocked`
* (since API 30) `Landroid/net/TetheringManager;->TETHERING_WIGIG:I,blocked`
* (since API 31) `Landroid/net/IpSecManager;->DIRECTION_FWD:I,blocked`
* (since API 31) `Landroid/net/IpSecManager;->INVALID_SECURITY_PARAMETER_INDEX:I,blocked`
* (since API 31) `Landroid/net/wifi/SoftApCapability;->getCountryCode()Ljava/lang/String;,blocked`
* (since API 33) `Landroid/net/wifi/SoftApConfiguration$Builder;->setRandomizedMacAddress(Landroid/net/MacAddress;)Landroid/net/wifi/SoftApConfiguration$Builder;,blocked`
* (since API 31) `Landroid/net/wifi/SoftApConfiguration;->BAND_TYPES:[I,blocked`
* (since API 31) `Landroid/net/wifi/SoftApInfo;->getApInstanceIdentifier()Ljava/lang/String;,blocked`
* `Landroid/net/wifi/ISoftApCallback$Stub;->asInterface(Landroid/os/IBinder;)Landroid/net/wifi/ISoftApCallback;,lo-prio,max-target-o`
* (prior to API 31) `Landroid/net/wifi/IWifiManager;->registerSoftApCallback(Landroid/os/IBinder;Landroid/net/wifi/ISoftApCallback;I)V,max-target-o`
* (since API 31) `Landroid/net/wifi/IWifiManager;->registerSoftApCallback(Landroid/net/wifi/ISoftApCallback;)V,blocked`
* (prior to API 31) `Landroid/net/wifi/IWifiManager;->unregisterSoftApCallback(I)V,max-target-o`
* (since API 31) `Landroid/net/wifi/IWifiManager;->unregisterSoftApCallback(Landroid/net/wifi/ISoftApCallback;)V,blocked`
* (since API 31) `Landroid/net/wifi/WifiClient;->getApInstanceIdentifier()Ljava/lang/String;,blocked`
* (prior to API 30) `Landroid/net/wifi/WifiConfiguration;->AP_BAND_2GHZ:I,lo-prio,max-target-o`
* (prior to API 30) `Landroid/net/wifi/WifiConfiguration;->AP_BAND_5GHZ:I,lo-prio,max-target-o`
* (prior to API 30) `Landroid/net/wifi/WifiConfiguration;->AP_BAND_ANY:I,lo-prio,max-target-o`
* (prior to API 30) `Landroid/net/wifi/WifiConfiguration;->apBand:I,unsupported`
* (prior to API 30) `Landroid/net/wifi/WifiConfiguration;->apChannel:I,unsupported`
* (since API 30) `Landroid/net/wifi/WifiContext;->ACTION_RESOURCES_APK:Ljava/lang/String;,blocked`
* (since API 30) `Landroid/net/wifi/WifiContext;-><init>(Landroid/content/Context;)V,blocked`
* `Landroid/net/wifi/WifiManager$SoftApCallbackProxy;-><init>(Landroid/net/wifi/WifiManager;Landroid/os/Looper;Landroid/net/wifi/WifiManager$SoftApCallback;)V`
* `Landroid/net/wifi/WifiManager$SoftApCallbackProxy;-><init>(Landroid/net/wifi/WifiManager;Ljava/util/concurrent/Executor;Landroid/net/wifi/WifiManager$SoftApCallback;)V,blocked`
* `Landroid/net/wifi/WifiManager$SoftApCallbackProxy;-><init>(Landroid/net/wifi/WifiManager;Ljava/util/concurrent/Executor;Landroid/net/wifi/WifiManager$SoftApCallback;I)V,blocked`
* `Landroid/net/wifi/WifiManager$SoftApCallbackProxy;-><init>(Ljava/util/concurrent/Executor;Landroid/net/wifi/WifiManager$SoftApCallback;I)V`
* (prior to API 30) `Landroid/net/wifi/WifiManager$SoftApCallback;->onNumClientsChanged(I)V,greylist-max-o`
* (since API 33) `Landroid/net/wifi/WifiManager;->EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE:Ljava/lang/String;,blocked`
* `Landroid/net/wifi/WifiManager;->mService:Landroid/net/wifi/IWifiManager;,unsupported`
* (prior to API 30) `Landroid/provider/Settings$Global;->SOFT_AP_TIMEOUT_ENABLED:Ljava/lang/String;,lo-prio,max-target-o`
* (on API 34) `Landroid/service/quicksettings/TileService;->mToken:Landroid/os/IBinder;,lo-prio,max-target-o`
* (prior to API 30) `Lcom/android/internal/R$array;->config_tether_bluetooth_regexs:I,max-target-q`
* (prior to API 30) `Lcom/android/internal/R$array;->config_tether_usb_regexs:I,max-target-q`
* (prior to API 30) `Lcom/android/internal/R$array;->config_tether_wifi_regexs:I,max-target-q`
* (prior to API 30) `Lcom/android/internal/R$integer;->config_wifi_framework_soft_ap_timeout_delay:I,greylist-max-o`
* `Lcom/android/internal/R$string;->config_ethernet_iface_regex:I,lo-prio,max-target-o`
* (since API 31) `Lcom/android/server/IpSecService;->FULL_MASK:I`
* (since API 30) `Lcom/android/server/SystemServer;->TETHERING_CONNECTOR_CLASS:Ljava/lang/String;`
* (prior to API 33) `Ljava/lang/invoke/MethodHandles$Lookup;-><init>(Ljava/lang/Class;I)V,unsupported`
* (prior to API 33) `Ljava/lang/invoke/MethodHandles$Lookup;->ALL_MODES:I,lo-prio,max-target-o`

See [`mobile/src/hiddenApiStubs`](mobile/src/hiddenApiStubs) for hidden whitelisted/system APIs as well as partial SDK-class stubs.

Nonexported system resources:

* (since API 30) `@com.android.networkstack.tethering:array/config_tether_bluetooth_regexs`
* (since API 30) `@com.android.networkstack.tethering:array/config_tether_ncm_regexs`
* (since API 30) `@com.android.networkstack.tethering:array/config_tether_usb_regexs`
* (since API 30) `@com.android.networkstack.tethering:array/config_tether_wifi_regexs`
* (since API 30) `@com.android.networkstack.tethering:array/config_tether_wigig_regexs`
* (since API 31) `@com.android.wifi.resources:integer/config_wifiFrameworkSoftApShutDownIdleInstanceInBridgedModeTimeoutMillisecond`
* (since API 30) `@com.android.wifi.resources:integer/config_wifiFrameworkSoftApShutDownTimeoutMilliseconds`

Other:

* (prior to API 30) Activity `com.android.settings/.Settings$TetherSettingsActivity` is assumed to be exported.
* `IPv6 NAT` mode depends on the iptables `TPROXY` and `NFQUEUE` targets and
  transparent sockets. ICMPv6 Echo interception uses app-owned queue `30000`
  and assumes queued downstream packets expose six-byte source hardware-address
  metadata through `NFQA_HWADDR`.
* (since API 30) Relevant tethering APEX classes used here, including `android.net.ITetheringConnector`,
  may be jarjar-relocated under the optional prefixes
  `android.net.connectivity` or `com.android.connectivity`.
* (since API 31) Relevant netd APEX classes used here, including `android.net.INetd*`,
  may be jarjar-relocated under the optional prefixes
  `android.net.connectivity` or `com.android.connectivity`.
* (since API 30) AOSP dispatches `TetheringEventCallback`
  startup tether-state callbacks from one `executor.execute { ... }` block in `onCallbackStarted`,
  and later tether-state updates from one `executor.execute { ... }` block in
  `onTetherStatesChanged`.
* The Rust DNS proxy submits upstream queries through `android_res_nsend`/`android_res_nresult`.
  To keep daemon tasks nonblocking while still using `android_res_nresult` as the public result
  reader/closer, it waits for `dnsproxyd` to close the one-shot `resnsend` client socket before
  reading the result. This assumes `resnsend` writes the complete resolver result before returning
  and the socket receive buffer can hold that result until the framework socket listener closes the
  client socket.
* For `ip rule` priorities, AOSP local-network/tethering priorities are assumed to be 17000/18000
  on API 29..30 and 20000/21000 on API 31+. VPNHotspot uses the 17500..17900 or 20500..20900
  gap between them.
* For route-table numbers, Android interface tables are assumed to start at ifindex + 1000; `IPv6 NAT`
  TPROXY uses table 900 to stay below that range and away from AOSP fixed tables 97..99 and kernel built-ins.
* Clean flushes table 900 because that table is reserved by VPNHotspot. `IPv6 NAT` also adds its
  deterministic ULA /64 route to Android's shared `local_network` route table 97; Clean never flushes
  that table and only deletes VPNHotspot prefixes reconstructed from current interface names.
* For packet marks, Android fwmark is assumed to use low bits for netId and routing metadata.
* `IPv6 NAT` fwmark fallback for TPROXY uses masked high reserved bits `0x10000000/0x10000000`.
  That fallback is expected on only kernels without effective `FRA_IP_PROTO` policy-rule support, which upstream Linux added in 4.17.
  Probe cleanup deletes at most one rule, omitting `FRA_IP_PROTO` when support or
  the mutation outcome is uncertain; repeated stale-rule deletion is reserved for Clean.
* Daemon reply sockets use the AOSP local-network protected mark `0x00030063`, which assumes
  `LOCAL_NET_ID = 99` plus the `explicitlySelected` and `protectedFromVpn` fwmark bits.

System/root command assumptions:

The following Android system binaries are assumed to be bundled and executable:

* `/system/bin/dumpsys` (`ipsec`);
* `/system/bin/iptables-restore`, `/system/bin/ip6tables-restore` (`-w --noflush`, restore input
  commands including `-I`, `-D`, `-N`, `-nvx -L <chain>`);
* `/system/bin/ndc` (`ipfwd`, `nat`);
* `/system/bin/settings` (`put global`);
* `/system/bin/linker` or `/system/bin/linker64` (`path.zip!/program`).
