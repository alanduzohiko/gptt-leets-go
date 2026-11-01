#!/usr/bin/env bash
set -euo pipefail

APK="dist/Mashxall-3.1.3.apk"
SPLIT_ZIP="dist/Mashxall-3.1.3-signed-parts.zip"
APP_ID="${MASHXALL_TARGET_PACKAGE:-com.kosratdahmad.mashxall.full}"
mkdir -p dist
: > dist/emulator-test.txt
: > dist/emulator-logcat.txt
: > dist/emulator-activities.txt
exec > >(tee -a dist/emulator-test.txt) 2>&1

echo "=== Android emulator identity ==="
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

launch_check() {
  local mode="$1"
  echo "=== Launch app launcher activity ($mode) ==="
  if ! adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1; then
    echo "FAIL: launcher activity could not start ($mode)."
    return 1
  fi
  sleep 12
  local pid
  pid="$(adb shell pidof "$APP_ID" | tr -d '\r')"
  if [[ -z "$pid" ]]; then
    echo "FAIL: app process did not remain running after launch ($mode)."
    adb logcat -d -v brief -t 2500 >> dist/emulator-logcat.txt || true
    grep -E "FATAL EXCEPTION|AndroidRuntime|UnsatisfiedLinkError|dlopen failed|Fatal signal|Process: ${APP_ID}" dist/emulator-logcat.txt || true
    return 1
  fi
  adb shell dumpsys activity activities > dist/emulator-activities.txt || true
  echo "PASS: app installed and remained running using $mode; pid=$pid"
  return 0
}

echo "=== Try standalone APK first ==="
if adb install --no-streaming "$APK" > dist/standalone-install.txt 2>&1; then
  cat dist/standalone-install.txt
  if launch_check "standalone APK"; then
    exit 0
  fi
  adb shell am force-stop "$APP_ID" || true
  adb uninstall "$APP_ID" || true
else
  cat dist/standalone-install.txt
  echo "Standalone install rejected; will try the native split APK set."
  adb logcat -d -v brief -t 2500 >> dist/emulator-logcat.txt || true
fi

echo "=== Install original split layout with all three package-consistent APKs ==="
if [[ ! -s "$SPLIT_ZIP" ]]; then
  echo "FAIL: split installation archive is missing."
  exit 1
fi
mkdir -p dist/split-apks
unzip -o "$SPLIT_ZIP" -d dist/split-apks >/dev/null
if ! adb install-multiple --no-streaming \
    dist/split-apks/base.apk \
    dist/split-apks/split_config.arm64_v8a.apk \
    dist/split-apks/split_config.xxhdpi.apk > dist/split-install.txt 2>&1; then
  cat dist/split-install.txt
  adb logcat -d -v brief -t 2500 >> dist/emulator-logcat.txt || true
  echo "FAIL: both standalone APK installation and split-set installation were rejected."
  exit 1
fi
cat dist/split-install.txt
if ! launch_check "three-split install set"; then
  exit 1
fi
