#!/usr/bin/env bash
set -euo pipefail

: "${APK_PATH:?Candidate APK path is missing}"
: "${BASELINE_APK_PATH:?Build 515 APK path is missing}"
mkdir -p dist/runtime-test/upgrade

ADB_BIN="$(command -v adb)"
adb() {
  timeout --foreground 180s "$ADB_BIN" "$@"
}

adb wait-for-device
echo "Installing the verified Build 515 baseline without clearing data."
adb install --no-streaming "$BASELINE_APK_PATH" | tee dist/runtime-test/upgrade/install-515.txt

# Seed the same kind of already-configured returning-user preferences used by
# the real app. This is a CI-only emulator; it never touches the user's phone.
prefs_xml="<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><string name='language'>ckb</string><boolean name='first_run_setup_started' value='true' /><boolean name='first_run_setup_complete' value='true' /><boolean name='startup_animation_started' value='true' /><boolean name='startup_animation_played' value='true' /><boolean name='dark_mode' value='true' /></map>"
prefs_base64="$(printf '%s' "$prefs_xml" | base64 -w0)"
adb shell "run-as com.mashxall.unified sh -c 'mkdir -p shared_prefs && printf %s $prefs_base64 | base64 -d > shared_prefs/mashxall_preferences.xml'"

adb shell am force-stop com.mashxall.unified
adb shell am start -W -n com.mashxall.unified/.MainActivity > dist/runtime-test/upgrade/launch-515.txt 2>&1
sleep 12
baseline_pid="$(adb shell pidof com.mashxall.unified 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$baseline_pid" ]]; then
  adb logcat -d -v threadtime > dist/runtime-test/upgrade/logcat-515-failure.txt || true
  echo "FAIL: known-good Build 515 did not stay alive."
  exit 1
fi
echo "PASS: Build 515 remained alive (pid $baseline_pid)." | tee dist/runtime-test/upgrade/result-515.txt
adb shell am force-stop com.mashxall.unified

echo "Upgrading in place to candidate APK; app data is preserved."
adb install -r --no-streaming "$APK_PATH" | tee dist/runtime-test/upgrade/install-candidate.txt
adb shell am start -W -n com.mashxall.unified/.MainActivity > dist/runtime-test/upgrade/launch-candidate.txt 2>&1
sleep 15
candidate_pid="$(adb shell pidof com.mashxall.unified 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$candidate_pid" ]]; then
  adb logcat -d -v threadtime > dist/runtime-test/upgrade/logcat-candidate-failure.txt || true
  adb shell dumpsys activity activities > dist/runtime-test/upgrade/activities-candidate-failure.txt || true
  echo "FAIL: candidate APK exited after in-place upgrade."
  grep -E 'FATAL EXCEPTION|AndroidRuntime|Fatal signal|am_crash|Process: com.mashxall' dist/runtime-test/upgrade/logcat-candidate-failure.txt | tail -n 120 || true
  exit 1
fi
adb logcat -d -v threadtime > dist/runtime-test/upgrade/logcat-candidate.txt
if grep -Eq 'FATAL EXCEPTION|NoClassDefFoundError|UnsatisfiedLinkError|Fatal signal' dist/runtime-test/upgrade/logcat-candidate.txt; then
  echo "FAIL: fatal Android runtime error after in-place upgrade."
  grep -E 'FATAL EXCEPTION|AndroidRuntime|Fatal signal|Process: com.mashxall' dist/runtime-test/upgrade/logcat-candidate.txt | tail -n 120 || true
  exit 1
fi
echo "PASS: candidate remained alive after in-place upgrade (pid $candidate_pid)." | tee dist/runtime-test/upgrade/result-candidate.txt

# The comprehensive UI/content test installs the same APK with -r and opens
# the real app sections. It does not uninstall or clear package data.
bash tools/runtime-smoke-android11.sh
