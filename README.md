# مەشخەڵ — Repair and native rebuild workspace

The previous repair path only modified resources in a compiled APK and repeatedly failed on the user's phone. This repository now also contains a real native Android Java/Views project at [`native-app/`](native-app/README.md); it is not a WebView or website wrapper.

## Native app project

- Package ID: `com.kosratdahmad.mashxall.nativeapp`.
- Sorani Kurdish, Arabic, and English.
- Offline prayer schedule and city selection, Azkar categories and reader, 99 names of Allah, Tasbih counter, original audio, and compass-based Qibla.
- No internet or location permission; it uses the locally chosen city.
- The exact original offline SQLite database and 16 original audio tracks are extracted by CI after verifying the source bundle's SHA-256.

## Build and test

Workflow `Build native Mashxall for Android 11` compiles the native project and checks the APK signature, alignment, package ID, API levels, bundled database checksum, and audio assets. A separate job attempts installation and launch on an Android 11 emulator. A successful static build is not by itself proof that runtime launch passes.

## Repository boundary

All changes stay in `alanduzohiko/gptt-leets-go`. Do not modify `alanduzohiko/masheqal` or any other repository.
