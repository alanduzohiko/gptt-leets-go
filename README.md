# مەشخەڵ — original-app reverse engineering and Android rebuild

## What the original source actually is

The supplied source is the compiled, three-part Android APK bundle in Release `source-3.1.3`; the repository initially did not contain the publisher's original Gradle/Kotlin source project. The original APK has five DEX files, 14 manifest activities (including dependency activities), 16 services, 18 receivers, three providers, about 1,126 resource files, and a large offline database. Its compiled app contains prayer screens and widgets, Azkar and favorites, Names of Allah, Qibla compass/map, language/location onboarding, detailed settings, azan/notification tones, alarms, and scheduled notifications.

## Primary workflow: full original-app rebuild

`Rebuild safe Mashxall split set` now decodes the original DEX into smali, updates only the application-ID literal and hardcoded `android.resource://` audio URIs that must follow a separate app ID, reassembles the DEX, preserves the original component class names, label resources, original offline database, app assets, and ARM64 native libraries, then merges the three split APKs into one standalone APK. Static checks verify the DEX file set, original class descriptors, package ID, launcher icon resources, original database bytes/integrity, and four native libraries. The Android 11 emulator job attempts an actual install and launch; a static build alone is not a runtime guarantee.

- Original app ID: `com.kosratdahmad.myprayers`
- Full rebuild test app ID: `com.kosratdahmad.mashxall.full`
- Exact source bundle SHA-256: `bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3`

## Separate Native prototype

`native-app/` is a separate clean Java/Android Views implementation. It was created when the original APK builds repeatedly failed, but it is **not** feature-equivalent to the original compiled app. It uses selected original offline data/audio and demonstrates a native alternative. The original-app rebuild workflow is the correct route when preserving the existing app's full screen/component set is the priority.

## Known limits

The original APK contains Firebase/Maps/Huawei integrations. A re-signed clone with a different application ID can be restricted by the original service owner's package ID, signing certificate, or API-key settings; private backend behavior cannot be guaranteed from APK code alone. No claim of complete runtime parity should be made unless the Android 11 installation/startup test passes and the affected service flows are checked.

## Repository boundary

All work stays in `alanduzohiko/gptt-leets-go`. No other repository is modified.
