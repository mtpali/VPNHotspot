# Attribution and distribution notices

VPNHotspot OLED is based on Mygod's VPNHotspot v3.0.8, commit
`20ed65762517c801f87cb87e17e70316c072b444`, under Apache-2.0.
The original copyright and license remain in `LICENSE` and source files.

The license screen and its Google dependencies have been removed. Notices are
distributed as APK assets instead of adding a license-screen dependency.

Android runtime components include AndroidX/Jetpack Compose, Room, Kotlin,
kotlinx.coroutines, kotlinx.collections.immutable, Ktor, Wire, Okio,
librootkotlinx, LSPosed HiddenApiBypass, Timber and ZXing. Their Apache-2.0
license is included in `mobile/src/main/assets/third-party-notices.txt`.
Dependency coordinates and pinned versions are in `gradle/libs.versions.toml`;
transitive versions are resolved by the Compose BOM and Gradle.

The Rust daemon's pinned dependencies are in
`mobile/src/main/rust/vpnhotspotd/Cargo.lock`. Their license/copyright texts,
including MIT and BSD notices, are collected in the APK asset
`mobile/src/main/assets/rust-notices.txt`. Build dependencies are also listed
conservatively. Use `scripts/collect-rust-notices.py` to refresh that asset.

Offline manufacturer prefixes come from the IEEE Registration Authority MA-L
public listing at <https://standards-oui.ieee.org/oui/oui.csv>, retrieved on
2026-10-01. Only common phone-manufacturer prefixes are included. The names
are identification hints and do not imply endorsement or prove a device model.

Sources:

- VPNHotspot: <https://github.com/Mygod/VPNHotspot>
- AndroidX: <https://android.googlesource.com/platform/frameworks/support/>
- Kotlin/kotlinx: <https://github.com/JetBrains/kotlin>
- Ktor: <https://github.com/ktorio/ktor>
- Wire: <https://github.com/square/wire>
- Okio: <https://github.com/square/okio>
- librootkotlinx: <https://github.com/Mygod/librootkotlinx>
- HiddenApiBypass: <https://github.com/LSPosed/AndroidHiddenApiBypass>
- Timber: <https://github.com/JakeWharton/timber>
- ZXing: <https://github.com/zxing/zxing>
- Rust packages: the source URLs recorded alongside their license notices.
