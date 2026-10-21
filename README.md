# Mashxall repair workspace

This is a separate workspace created specifically to diagnose and repair the startup crash in the rebranded Android app based on the original user-provided file صلواتي_3.1.3.apks.

## Strict repository boundary

- All work for this task stays in alanduzohiko/gptt-leets-go.
- No code, branch, workflow, or pull request is being sent to alanduzohiko/masheqal or any other repository.
- Do not declare the app fixed unless it has been installed and launched on a real Android 11 device.
- The safe build path preserves the original package ID, manifest semantics, all DEX bytes, the offline database, and the three-split structure. It changes only the visible app-label resource and launcher-icon palette.
- APK alignment and signing use the official Android SDK tools. The workflow does not manually construct APK signing blocks.

## Why this is a different repair attempt

Earlier experiments changed the package ID and edited DEX strings as part of rebranding. That may break package-scoped providers or external-service configuration, and editing DEX caused a checksum problem in one build. This workspace avoids those changes completely. It rebuilds only the resource layer via Apktool, confirms that DEX bytes and the offline database are unchanged, and uses zipalign plus apksigner for signing.

This is a conservative test build, not a guarantee that the runtime crash is fixed. Only a real-device launch test can prove that.

## Source bundle required by the build workflow

The original file is 62 MB and contains exactly these APK parts:

- base.apk
- split_config.arm64_v8a.apk
- split_config.xxhdpi.apk

SHA-256 of the exact original upload:

bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3

The binary is not committed into Git because it is large. To make it available to GitHub Actions:

1. Open the Create a release page for this repository: https://github.com/alanduzohiko/gptt-leets-go/releases/new
2. Create a release with tag source-3.1.3 and title Original source bundle 3.1.3.
3. Attach the original file صلواتي_3.1.3.apks as the only .apks asset in that release, then publish the release.
4. Publish the release. The Rebuild safe Mashxall split set workflow should start automatically for the exact tag source-3.1.3. If it does not start, open Actions, select that workflow, tap Run workflow, and leave the source release tag as source-3.1.3.
5. When the run completes, open its artifact mashxall-android11-repair-... and download it. The artifact contains the signed split set, the three individual APK parts, Arabic install instructions, and verification reports.

The workflow checks the SHA-256 against the exact file previously supplied. If it does not match, the build stops instead of processing an uncertain APK.

## Important install caveat

This safe test keeps the original package ID com.kosratdahmad.myprayers. That is intentional: it avoids breaking app code and service authorities by renaming the package. But a custom signature cannot update the official signed app in place. If the official app with that package ID is installed, you may need to uninstall it before trying the rebranded build. Back up its data first. Do not uninstall the original app unless you are willing and able to restore it. This rebranded build will not coexist with the official app under the same package ID.

If this run opens and stays open, the crash is resolved for this build. If it still closes, the next step is a real crash log; do not guess by removing more services or editing DEX code.

## Public-repository privacy note

This repository is currently public. A release asset and GitHub Actions artifacts in a public repository may be accessible to others. If you do not want the original app bundle or output build to be public, change this repository to Private before uploading the source asset.
