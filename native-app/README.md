# مەشخەڵ — Native Android rebuild

This directory is a new, native Java Android application. It does not embed a web page or WebView, and it does not load any of the original app's DEX files.

## Why this project exists

The repair repository did not contain an Android source tree (\`src/\`, Kotlin/Java app classes, or a Gradle Android project). It contained a prebuilt split APK bundle plus scripts that edited and merged APK resources. Those APK-only rebuilds did not solve the user's installation problem. This project therefore builds a clean native Android app instead of patching the old binary again.

## What is implemented

- Native Android Views and Activity, no Compose/WebView dependency.
- Sorani Kurdish, Arabic, and English interface choices.
- Offline prayer schedule and city selection from the original SQLite data.
- 11 Azkar categories, chapter navigation, Arabic text, translations, and source references.
- 99 names of Allah with the translations already present in the source database.
- Local Tasbih counter.
- Audio tracks extracted from the original app and a compass-based Qibla screen.
- New icon and separate application ID \`com.kosratdahmad.mashxall.nativeapp\`.
- No network permission, no location permission, and no ABI-specific native libraries. The user picks a city from the bundled offline schedule.

## Build and data integrity

The workflow downloads only the exact original source release \`source-3.1.3\`, checks its archive SHA-256, extracts the original \`assets/database/muslim_db_v2.5.1.db\` and original audio files, checks the database checksum and SQLite integrity, builds the APK with Gradle/Android SDK tools, then validates APK signature, alignment, package ID, Android API metadata, and exact database bytes. The database/audio binaries are not committed to Git; CI inserts them from the checked source bundle.

- Application ID: \`com.kosratdahmad.mashxall.nativeapp\`
- Minimum Android: API 24 (Android 7.0)
- Target API: 32 (Android 12L target; compatible with Android 11)
- Default city: Sulaymaniyah

This is a new native implementation of the available offline content, not a claim that the original APK's entire feature set has been reproduced. Its build is not considered successful unless compilation and all static APK checks pass; the emulator job also attempts installation and launch on Android 11.
