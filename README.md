# مـــەشــخـــەڵ — Mashxall Unified

A new native Android Islamic-library app, designed independently in Kotlin + Jetpack Compose. This is **not** a repackaging of one source app and does not modify any other GitHub repository.

## UI and languages

- New emerald-and-gold visual identity, custom launcher mark, animated home surface and responsive module cards.
- Sorani Kurdish (کوردی), Arabic (العربية), and English (English); Sorani/Arabic use RTL layout, English uses LTR.
- Native Android UI; minimum Android 7.0 (API 24), designed to be tested on Android 11.

## Current source implementation

The new app source currently includes a UI and repository layer for:

- Quran browsing, searching, surah selection, and verse bookmarks.
- Sorani translation from Noor's Quran database and separate English/Arabic translation sources.
- Tafsir selector for the accessible verse-indexed SQLite tafsir sources.
- Hadith search from the Noor database.
- Adhkar/dua browsing, repetition counter, and Names of Allah.
- Static city-based prayer times and Qibla bearing calculation.
- Local bundled audio playback plus an online reciter list sourced from the Noor audio index.
- Book catalogue, lexicon lookup, word-by-word Quran grammar/root data, Farmuda records, and a persistent tasbih counter.
- A validated extraction script that catalogs SQLite databases, audio, fonts, PDFs and source assets.

**Important limitations:** this source project has not yet produced a confirmed, data-enabled release APK, and it has not been tested on a physical phone. Some Khairah tafsir pages use a separate binary page store referenced by offsets/hashes; those raw files are preserved, but their custom page decoder still needs reverse-engineering before those pages can be shown in the unified UI. Some book/PDF assets are currently catalogued but do not yet have a full built-in reader. Do not treat the current UI skeleton as containing every feature of all source apps yet.

## Input bundle and safe build

The three original user-supplied APK sets are kept as inputs, not treated as original source-code projects. Their SHA-256 fingerprints are checked by the pipeline:

- Salawati 3.1.3: `bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3`
- Khairah 1.2: `36b605314a66e5809c111565e7452f91578d4b0de3e009801118d40f1def85b8`
- Tafseeri Noor 1.2.2: `fa9259501abd4b1c7f870109a1ad49567badb27792510cee3d5ce2ee28e11ce8`

The unified build workflow expects a GitHub Release tagged `unified-source-v1` with one asset named `Mashxall-Unified-Source-Bundle.zip`. It extracts verified assets into the new project's Android assets, checks every SQLite database, then runs Gradle tests/lint/build and official APK signing checks.

### Publish the source bundle (phone-only)

1. **Make this repository Private first**: https://github.com/alanduzohiko/gptt-leets-go/settings/general . Under “Danger Zone” / repository visibility, choose Private. The supplied source APK sets may contain copyrighted data and app assets; do not upload them to a public repository.
2. Download the source bundle supplied in the ChatGPT conversation. Its SHA-256 is `9430a52d73bfff7d897320e36173ce9d6f72f7c8d24b24dcc45b3dd4e70aba96`.
3. Open https://github.com/alanduzohiko/gptt-leets-go/releases/new .
4. Set tag `unified-source-v1`, title `Mashxall Unified Source Bundle`, attach exactly `Mashxall-Unified-Source-Bundle.zip`, and publish.
5. Open the Actions tab. Watch `Build the new unified Mashxall Android app`. If the build succeeds, download its artifact `mashxall-unified-android11-build-…`.

The build artifact is a debug APK. Static checks do not prove launch success. The app must still be installed and tested on Android 11; if it closes, capture a crash log before changing code again.

## Source data and rights

The app is being developed from user-supplied APK sets and their embedded databases/resources. Before redistribution or public release, verify the original apps' licenses, permissions to redistribute content, audio, fonts and Quran/tafsir translations, and include all required notices. This repository contains newly written UI/source code, not the original app source code.
