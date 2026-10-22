#!/usr/bin/env bash
# Run as one shell script so failures are captured rather than terminating
# the emulator-runner after each individual line.
set +e
mkdir -p dist
: > dist/package-manager-install.txt
: > dist/package-manager-result.txt
: > dist/package-probe-logcat.txt
: > dist/package-probe-platform.txt
: > dist/package-manager-exit-code.txt
: > dist/control-install.txt
: > dist/control-install-exit-code.txt
: > dist/probe-build.txt

adb wait-for-device
adb logcat -b all -v threadtime > dist/package-probe-logcat.txt 2>&1 &
LOGCAT_PID=$!
sleep 2

{
  echo "=== Android release ==="
  timeout 8s adb shell getprop ro.build.version.release
  echo "=== Android SDK ==="
  timeout 8s adb shell getprop ro.build.version.sdk
  echo "=== Build fingerprint ==="
  timeout 8s adb shell getprop ro.build.fingerprint
  echo "=== Package service status before installation ==="
  timeout 8s adb shell service check package
  echo "=== Settings service status ==="
  timeout 8s adb shell service check settings
  echo "=== System boot completed ==="
  timeout 8s adb shell getprop sys.boot_completed
} > dist/package-probe-platform.txt 2>&1

timeout 8s adb shell service list > dist/package-probe-services-before.txt 2>&1

service_ready=0
for attempt in $(seq 1 20); do
  timeout 6s adb shell service check package > dist/package-service-check.txt 2>&1
  if grep -qi 'found' dist/package-service-check.txt; then
    service_ready=1
    break
  fi
  sleep 2
done

if [[ "$service_ready" -ne 1 ]]; then
  echo "EMULATOR_INVALID_BEFORE_INSTALL: PackageManager never became available." | tee dist/package-manager-result.txt
  timeout 10s adb logcat -d -b all -v threadtime > dist/package-probe-logcat-final.txt 2>&1
  kill "$LOGCAT_PID" 2>/dev/null
  exit 0
fi

# First install a tiny, freshly built, known-good control APK to prove the AVD's
# PackageManager works before giving it the 108 MB application APK.
BT="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
echo "PWD=$PWD ANDROID_HOME=$ANDROID_HOME BUILD_TOOLS=$BT" > dist/probe-build.txt
mkdir -p dist/probe-res/values
mkdir -p dist/probe-manifest
cat > dist/probe-manifest/AndroidManifest.xml <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.mashxall.pmprobe">
  <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="30"/>
  <application android:label="@string/app_name" android:hasCode="false"/>
</manifest>
XML
cat > dist/probe-res/values/strings.xml <<'XML'
<resources><string name="app_name">PackageManagerProbe</string></resources>
XML
ls -l "$GITHUB_WORKSPACE/dist/probe-manifest/AndroidManifest.xml" "$GITHUB_WORKSPACE/dist/probe-res/values/strings.xml" "$ANDROID_HOME/platforms/android-30/android.jar" >> dist/probe-build.txt 2>&1
"$BT/aapt" package -f -M "$GITHUB_WORKSPACE/dist/probe-manifest/AndroidManifest.xml" -S "$GITHUB_WORKSPACE/dist/probe-res" -I "$ANDROID_HOME/platforms/android-30/android.jar" -F "$GITHUB_WORKSPACE/dist/PackageManagerProbe-unsigned.apk" >> dist/probe-build.txt 2>&1
probe_build_rc=$?
if [[ "$probe_build_rc" -ne 0 ]]; then
  echo "CONTROL_APK_BUILD_FAILED" | tee dist/package-manager-result.txt
  cat dist/probe-build.txt
  kill "$LOGCAT_PID" 2>/dev/null
  exit 0
fi
keytool -genkeypair -noprompt -alias probe -keystore dist/probe.jks -storepass android -keypass android -dname "CN=PackageManagerProbe" -keyalg RSA -keysize 2048 -validity 3650 >> dist/probe-build.txt 2>&1
"$BT/zipalign" -f 4 dist/PackageManagerProbe-unsigned.apk dist/PackageManagerProbe-aligned.apk >> dist/probe-build.txt 2>&1
"$BT/apksigner" sign --ks dist/probe.jks --ks-key-alias probe --ks-pass pass:android --key-pass pass:android --out dist/PackageManagerProbe.apk dist/PackageManagerProbe-aligned.apk >> dist/probe-build.txt 2>&1
"$BT/apksigner" verify --verbose dist/PackageManagerProbe.apk >> dist/probe-build.txt 2>&1

timeout 90s adb install --no-streaming dist/PackageManagerProbe.apk > dist/control-install.txt 2>&1
control_rc=$?
echo "$control_rc" > dist/control-install-exit-code.txt
cat dist/control-install.txt
timeout 8s adb shell service check package > dist/package-service-after-control.txt 2>&1

if [[ "$control_rc" -ne 0 ]]; then
  echo "CONTROL_APK_FAILED: emulator PackageManager is not healthy; target APK was not tested." | tee dist/package-manager-result.txt
  timeout 15s adb logcat -d -b all -v threadtime > dist/package-probe-logcat-final.txt 2>&1
  kill "$LOGCAT_PID" 2>/dev/null
  exit 0
fi

echo "CONTROL_APK_INSTALL_SUCCEEDED" > dist/control-install-result.txt
timeout 180s adb install --no-streaming dist/Mashxall-3.1.3.apk > dist/package-manager-install.txt 2>&1
rc=$?
echo "$rc" > dist/package-manager-exit-code.txt
cat dist/package-manager-install.txt

if [[ "$rc" -eq 0 ]]; then
  echo "PACKAGE_MANAGER_INSTALL_SUCCEEDED" > dist/package-manager-result.txt
elif grep -Eqi 'INSTALL_FAILED_NO_MATCHING_ABIS|NO_MATCHING_ABIS' dist/package-manager-install.txt; then
  echo "PACKAGE_PARSED; STOPPED_ONLY_BY_X86_VS_ARM64_NATIVE_ABI" > dist/package-manager-result.txt
elif grep -Eqi 'INSTALL_PARSE_FAILED|INSTALL_FAILED_INVALID_APK|Failed to parse|Parse error|not a valid APK|INSTALL_FAILED_NO_CERTIFICATES|INSTALL_FAILED_UPDATE_INCOMPATIBLE' dist/package-manager-install.txt; then
  echo "PACKAGE_MANAGER_REPORTED_APK_OR_SIGNATURE_REJECTION" > dist/package-manager-result.txt
elif [[ "$rc" -eq 124 ]]; then
  echo "TARGET_INSTALL_TIMEOUT; inspect package services and logcat" > dist/package-manager-result.txt
else
  echo "TARGET_INSTALL_FAILED; inspect raw PackageManager response" > dist/package-manager-result.txt
fi
timeout 8s adb shell service check package > dist/package-service-after-target.txt 2>&1
timeout 8s adb shell service list > dist/package-probe-services-after.txt 2>&1
timeout 8s adb shell dumpsys package com.kosratdahmad.mashxall.full43a1 > dist/package-probe-dumpsys.txt 2>&1
timeout 15s adb logcat -d -b all -v threadtime > dist/package-probe-logcat-final.txt 2>&1
kill "$LOGCAT_PID" 2>/dev/null
exit 0
