#!/usr/bin/env bash
# Run as one shell script so failures are captured rather than terminating the
# emulator-runner after each individual line.
set +e
mkdir -p dist
: > dist/package-manager-install.txt
: > dist/package-manager-result.txt
: > dist/package-probe-logcat.txt
: > dist/package-probe-platform.txt
: > dist/package-manager-exit-code.txt

{
  echo "=== Android release ==="
  adb shell getprop ro.build.version.release
  echo "=== Android SDK ==="
  adb shell getprop ro.build.version.sdk
  echo "=== Build fingerprint ==="
  adb shell getprop ro.build.fingerprint
  echo "=== Package service status before installation ==="
  adb shell service check package
  echo "=== System boot completed ==="
  adb shell getprop sys.boot_completed
} > dist/package-probe-platform.txt 2>&1

# The emulator occasionally loses system_server while booting; wait for PackageManager
# and record the state. A missing service is an emulator failure, not an APK parse result.
service_ready=0
for attempt in $(seq 1 30); do
  adb shell service check package > dist/package-service-check.txt 2>&1
  if grep -qi 'found' dist/package-service-check.txt; then
    service_ready=1
    break
  fi
  sleep 2
done

if [[ "$service_ready" -ne 1 ]]; then
  echo "EMULATOR_INVALID: Android PackageManager service never became available." | tee dist/package-manager-result.txt
  adb logcat -d -b all -v threadtime > dist/package-probe-logcat.txt 2>&1
  exit 0
fi

echo "=== adb install (raw PackageManager response) ===" > dist/package-manager-install.txt
timeout 180s adb install --no-streaming dist/Mashxall-3.1.3.apk >> dist/package-manager-install.txt 2>&1
rc=$?
echo "$rc" > dist/package-manager-exit-code.txt
cat dist/package-manager-install.txt

if [[ "$rc" -eq 0 ]]; then
  echo "PACKAGE_MANAGER_INSTALL_SUCCEEDED" > dist/package-manager-result.txt
elif grep -Eqi 'INSTALL_FAILED_NO_MATCHING_ABIS|NO_MATCHING_ABIS' dist/package-manager-install.txt; then
  echo "PACKAGE_PARSED; INSTALL_STOPPED_ONLY_BY_X86_VS_ARM64_NATIVE_ABI" > dist/package-manager-result.txt
elif grep -Eqi 'INSTALL_PARSE_FAILED|INSTALL_FAILED_INVALID_APK|Failed to parse|Parse error|not a valid APK' dist/package-manager-install.txt; then
  echo "PACKAGE_MANAGER_PARSE_FAILURE" > dist/package-manager-result.txt
elif [[ "$rc" -eq 124 ]]; then
  echo "EMULATOR_TIMEOUT: adb install did not finish in 180 seconds; inspect service state and logcat." > dist/package-manager-result.txt
else
  echo "PACKAGE_MANAGER_INSTALL_FAILED; inspect raw result and logcat" > dist/package-manager-result.txt
fi

adb logcat -d -b all -v threadtime > dist/package-probe-logcat.txt 2>&1
adb shell dumpsys package com.kosratdahmad.mashxall.full43a1 > dist/package-probe-dumpsys.txt 2>&1
exit 0
