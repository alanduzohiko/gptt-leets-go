# Mashxall repair workspace

This repository is a **separate workspace** for diagnosing and repairing the startup crash in the rebranded Android app based on the user-provided `صلواتي_3.1.3.apks` set.

## Safety boundary

- All work for this task belongs in this repository only.
- Do not push to, open pull requests against, or merge into any other repository.
- Do not claim the app is fixed until an Android runtime test confirms that it opens.
- Keep the original package ID, original DEX payloads, original resources, and original split structure unless evidence shows a specific change is needed.
- Prefer official Android SDK tooling (`zipalign`, `apksigner`) over manually editing APK signing blocks.
- A failed launch must be diagnosed from a captured exception/log, not by guessing which service to remove.

## Current state

This repository was created empty. A separate repair branch will contain the analysis and build automation. The original input is a split APK set containing:

- `base.apk`
- `split_config.arm64_v8a.apk`
- `split_config.xxhdpi.apk`

Original archive SHA-256:
`bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3`

The binary input is not committed to Git because large binary inputs should be provided as a GitHub Release asset or another controlled input source.

## Required validation before calling a build successful

1. Validate the exact input checksum and split names.
2. Confirm package names and split metadata are consistent.
3. Make the smallest possible change; do not rewrite DEX code as a routine branding step.
4. Align and sign each APK with the official Android SDK tools.
5. Verify APK signatures, ZIP integrity, DEX checksums, and bundled database integrity.
6. Install all required split APKs together on Android 11 and confirm the app remains open. Static validation alone is not proof of a successful launch.

See the repair branch for the current scripts and workflow.
