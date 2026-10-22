#!/usr/bin/env bash
set -euo pipefail

APK="dist/Mashxall-3.1.3.apk"
APP_ID="com.kosratdahmad.mashxall.repair2"
mkdir -p dist
: > dist/emulator-test.txt
: > dist/emulator-logcat.txt
exec > >(tee -a dist/emulator-test.txt) 2>&1

echo "=== Android emulator identity ==="
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

echo "=== Install standalone APK ==="
adb install --no-streaming "$APK"

echo "=== Confirm installed package ==="
adb shell pm path "$APP_ID"

echo "=== Launch app launcher activity ==="
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1

echo "=== Wait for startup ==="
sleep 12

echo "=== Capture Android logcat ==="
adb logcat -d -v brief -t 2500 > dist/emulator-logcat.txt || true

PID="$(adb shell pidof "$APP_ID" | tr -d '\r')"
if [[ -z "$PID" ]]; then
  echo "FAIL: app process did not remain running after launch."
  grep -E "FATAL EXCEPTION|AndroidRuntime|UnsatisfiedLinkError|dlopen failed|Fatal signal|Process: ${APP_ID}" dist/emulator-logcat.txt || true
  exit 1
fi

echo "PASS: app installed and remained running; pid=$PID"
