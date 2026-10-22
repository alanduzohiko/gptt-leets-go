# Mashxall repair workspace

This is a separate workspace created specifically to diagnose and repair the startup crash in the rebranded Android app based on the original user-provided file صلواتي_3.1.3.apks.

## Strict repository boundary

- All work for this task stays in alanduzohiko/gptt-leets-go.
- No code, branch, workflow, or pull request is being sent to alanduzohiko/masheqal or any other repository.
- Do not declare the app fixed unless it has been installed and launched on a real Android 11 device.
- The repair build uses a new, unique test package ID to avoid conflicts with previously installed test builds, while preserving the original DEX bytes, offline database, native libraries, and app components. It changes the visible label and launcher-icon palette.
- APK alignment and signing use the official Android SDK tools. The workflow does not manually construct APK signing blocks.

## Why this is a different repair attempt

This repair changes only manifest package identity/provider authorities plus the label and icon palette at the resource layer. It does not edit DEX code. Static validation checks that DEX bytes, the offline database, and native libraries remain identical; runtime installation and launch must still pass before calling the app fixed.

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

This repair build uses the new package ID com.kosratdahmad.mashxall.repair2.repair2. It is distinct from both the official app (com.kosratdahmad.myprayers) and the prior test build (com.kosratdahmad.mashxall.repair2), so it should avoid an update-signature conflict with either. Do not uninstall the official app. Each test build still needs a stable signing key for in-place updates; while no stable key is configured, treat each unique-package build as a separate test installation.

The previous standalone APK was statically validated, but its Android 11 install/launch test never completed. That was not enough to call it fixed. The current workflow must install and launch the new package in an Android emulator; if that test is unavailable or fails, the app remains unverified and crash logs are required.

## Public-repository privacy note

This repository is currently public. A release asset and GitHub Actions artifacts in a public repository may be accessible to others. If you do not want the original app bundle or output build to be public, change this repository to Private before uploading the source asset.
