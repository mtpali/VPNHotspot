#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 4 && $# != 5 ]]; then
    echo "Usage: $0 KEYSTORE PASSWORD_FILE KEY_ALIAS OUTPUT_DIR [COMMA_SEPARATED_ABIS]" >&2
    exit 2
fi
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
build_tools="$sdk_dir/build-tools/37.0.0"
[[ -x "$build_tools/apksigner" && -x "$build_tools/zipalign" ]] || {
    echo "Set ANDROID_HOME to an SDK with build-tools 37.0.0." >&2
    exit 1
}
keystore="$(realpath "$1")"
password_file="$(realpath "$2")"
alias_name="$3"
mkdir -p "$4"
output_dir="$(realpath "$4")"

IFS=',' read -r -a abis <<< "${5:-armeabi-v7a,arm64-v8a}"
for abi in "${abis[@]}"; do
    case "$abi" in
        armeabi-v7a|arm64-v8a) ;;
        *) echo "Unsupported release ABI: $abi" >&2; exit 2 ;;
    esac
    input="$repo_dir/mobile/build/outputs/apk/release/mobile-$abi-release-unsigned.apk"
    [[ -f "$input" ]] || { echo "Build :mobile:assembleRelease first: missing $input" >&2; exit 1; }
    suffix=arm64
    [[ "$abi" != armeabi-v7a ]] || suffix=armv7
    aligned="$output_dir/.aligned-$suffix.apk"
    output="$output_dir/VPNHotspot-OLED-$suffix.apk"
    "$build_tools/zipalign" -P 16 -f 4 "$input" "$aligned"
    "$build_tools/apksigner" sign --ks "$keystore" --ks-key-alias "$alias_name" \
        --ks-pass "file:$password_file" \
        --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true \
        --out "$output" "$aligned"
    "$build_tools/apksigner" verify --verbose --print-certs "$output"
    "$build_tools/zipalign" -c -P 16 4 "$output"
    rm "$aligned"
done
