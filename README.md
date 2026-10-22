# مـــەشــخـــەڵ — Mashxall Unified

A new native Android Islamic-library app, designed independently in Kotlin + Jetpack Compose. This is **not** a repackaging of one source app and does not modify any other GitHub repository.

## UI and languages

- New emerald-and-gold visual identity, custom launcher mark, pulsing hero panel, staggered fade/slide-in module cards, and softly tinted icon tiles.
- Sorani Kurdish (کوردی), Arabic (العربية), and English (English); Sorani/Arabic use RTL layout, English uses LTR.
- Native Android UI; minimum Android 7.0 (API 24), designed to be tested on Android 11.

## Current source implementation

The new app source currently includes a UI and repository layer for:

- Quran browsing/search, persistent verse bookmarks/reading position, plus an offline, page-by-page SVG Mushaf viewer (604 original pages) with integrity-checked on-demand decompression.
- Sorani translation from Noor's Quran database and separate English/Arabic translation sources.
- Tafsir selector for Noor's verse-indexed SQLite collection plus all four Khairah book tafsirs, decoded on demand with page-range and SHA-256 checks.
- Hadith search from the Noor database.
- Adhkar/dua browsing with persistent repetition counters, Salawati Kurdish/English text enrichment and references, and Names of Allah.
- Foreground GPS/Network location request with visible reported accuracy, source-city timetable lookup from both embedded databases where nearby, calculated times from precise coordinates elsewhere, selectable prayer calculation methods and Asr school, and Qibla using GPS coordinates plus device compass sensors.
- Local bundled audio playback, a dedicated filter for included adhan/mosque audio, and 80 reciter entries from Noor's audio index.
- Book catalogue with an in-app PDF page reader, lexicon lookup, word-by-word Quran grammar/root data, Farmuda records, and a persistent haptic tasbih counter.
- A validated extraction script that catalogs SQLite databases, audio, fonts, PDFs and source assets.

**Important limitations:** the current data-enabled APK passed extraction, Gradle tests/lint/assembleDebug, APK integrity/signature checks and artifact upload; it still needs to be installed and tested on a physical Android 11 phone. Khairah's four legacy tafsir binaries and the 604-page SVG Mushaf are now decoded on demand using build-generated offsets and SHA-256 verification. The source apps' complete internal behavior has not been independently verified screen-by-screen, so do not treat this as a certified 1:1 replacement yet.

## Input bundle and safe build

The three original user-supplied APK sets are kept as inputs, not treated as original source-code projects. Their SHA-256 fingerprints are checked by the pipeline:

- Salawati 3.1.3: `bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3`
- Khairah 1.2: `36b605314a66e5809c111565e7452f91578d4b0de3e009801118d40f1def85b8`
- Tafseeri Noor 1.2.2: `fa9259501abd4b1c7f870109a1ad49567badb27792510cee3d5ce2ee28e11ce8`

The unified build workflow expects a GitHub Release tagged `unified-source-v1` with one asset named `Mashxall-Unified-Source-Bundle.zip`. It extracts verified assets into the new project's Android assets, checks every SQLite database, then runs Gradle tests/lint/build and official APK signing checks.

### Source bundle and phone-only build

The source bundle is already uploaded to the `unified-source-v1` release and the automated workflow has verified its SHA-256 as `9430a52d73bfff7d897320e36173ce9d6f72f7c8d24b24dcc45b3dd4e70aba96`. Do not upload the bundle a second time.

- Release: https://github.com/alanduzohiko/gptt-leets-go/releases/tag/unified-source-v1
- Build runs: https://github.com/alanduzohiko/gptt-leets-go/actions
- When the most recent `Build the new unified Mashxall Android app` run succeeds, open that run's **Artifacts** and download `mashxall-unified-android11-build-…` to the Android phone.

This repository remains public at the user's direction. The supplied apps' audio, translations and bundled assets may have redistribution restrictions; verify the rights before public distribution of a production build.

The build artifact is a debug APK. Static checks do not prove launch success. The app must still be installed and tested on Android 11; if it closes, capture a crash log before changing code again.

## Source data and rights

The app is being developed from user-supplied APK sets and their embedded databases/resources. Before redistribution or public release, verify the original apps' licenses, permissions to redistribute content, audio, fonts and Quran/tafsir translations, and include all required notices. This repository contains newly written UI/source code, not the original app source code.
