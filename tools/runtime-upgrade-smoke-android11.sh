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

# Android 11 CI can report the emulator online before PackageManager has
# registered its Binder endpoint. Retry only transient ADB/PM transport errors.
wait_for_package_manager() {
  local attempt
  for attempt in $(seq 1 24); do
    if timeout --foreground 20s "$ADB_BIN" shell pm list packages >/dev/null 2>>dist/runtime-test/upgrade/package-manager-check.txt; then
      echo "PASS: PackageManager ready (check $attempt)." | tee -a dist/runtime-test/upgrade/install-recovery.txt
      return 0
    fi
    sleep 5
  done
  echo "FAIL: PackageManager did not recover within the bounded retry window." | tee -a dist/runtime-test/upgrade/install-recovery.txt
  return 1
}

install_apk_with_retry() {
  local label="$1"
  local apk="$2"
  local log_file="$3"
  local replace_existing="$4"
  local attempt
  for attempt in 1 2 3; do
    echo "$label install attempt $attempt/3" | tee -a "$log_file"
    if [[ "$replace_existing" == "true" ]]; then
      if timeout --foreground 360s "$ADB_BIN" install -r --no-streaming "$apk" 2>&1 | tee -a "$log_file"; then
        return 0
      fi
    else
      if timeout --foreground 360s "$ADB_BIN" install --no-streaming "$apk" 2>&1 | tee -a "$log_file"; then
        return 0
      fi
    fi

    if ! grep -Eqi "Can't find service: package|Failure calling service package|Broken pipe|device offline|Could not access the Package Manager" "$log_file"; then
      echo "FAIL: $label install failed for a non-transient reason." | tee -a "$log_file"
      return 1
    fi
    echo "Transient ADB/PackageManager failure detected; reconnecting before retry." | tee -a dist/runtime-test/upgrade/install-recovery.txt
    timeout --foreground 30s "$ADB_BIN" reconnect device >>dist/runtime-test/upgrade/install-recovery.txt 2>&1 || true
    timeout --foreground 45s "$ADB_BIN" wait-for-device >>dist/runtime-test/upgrade/install-recovery.txt 2>&1 || true
    if ! wait_for_package_manager; then return 1; fi
    if [[ "$attempt" -lt 3 ]]; then sleep 8; fi
  done
  echo "FAIL: $label install exhausted all retries." | tee -a "$log_file"
  return 1
}

wait_for_package_manager
echo "Installing the verified Build 515 baseline without clearing data."
install_apk_with_retry "Build 515 baseline" "$BASELINE_APK_PATH" "dist/runtime-test/upgrade/install-515.txt" "false"

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
install_apk_with_retry "Candidate APK" "$APK_PATH" "dist/runtime-test/upgrade/install-candidate.txt" "true"
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

# Repeat the returning-install launch after force-stop. The user's reported
# regression is an immediate process exit on subsequent opens, so explicitly
# exercise two launches after the in-place upgrade without clearing package data.
adb shell am force-stop com.mashxall.unified
adb shell am start -W -n com.mashxall.unified/.MainActivity > dist/runtime-test/upgrade/launch-returning-install.txt 2>&1
sleep 12
returning_pid="$(adb shell pidof com.mashxall.unified 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$returning_pid" ]]; then
  adb logcat -d -v threadtime > dist/runtime-test/upgrade/logcat-returning-failure.txt || true
  echo "FAIL: candidate closed on the second returning-install launch."
  grep -E 'FATAL EXCEPTION|AndroidRuntime|Fatal signal|am_crash|Process: com.mashxall' dist/runtime-test/upgrade/logcat-returning-failure.txt | tail -n 120 || true
  exit 1
fi
adb shell screencap -p /sdcard/mashxall-home.png
adb pull /sdcard/mashxall-home.png dist/runtime-test/upgrade/home-after-upgrade.png >/dev/null
adb shell dumpsys activity activities > dist/runtime-test/upgrade/activities-after-upgrade.txt
adb logcat -d -v threadtime > dist/runtime-test/upgrade/logcat-final.txt
if grep -Eq 'FATAL EXCEPTION|NoClassDefFoundError|UnsatisfiedLinkError|Fatal signal' dist/runtime-test/upgrade/logcat-final.txt; then
  echo "FAIL: fatal Android runtime error was logged after the in-place upgrade."
  grep -E 'FATAL EXCEPTION|AndroidRuntime|Fatal signal|Process: com.mashxall' dist/runtime-test/upgrade/logcat-final.txt | tail -n 120 || true
  exit 1
fi
echo "PASS: Build 515 upgraded in place to the candidate; the app opened, stayed alive, and reopened successfully." | tee dist/runtime-test/upgrade/RESULT.txt

