# مـــەشــخـــەڵ — Mashxall Unified

A new native Android Islamic-library app, designed independently in Kotlin + Jetpack Compose. This is **not** a repackaging of one source app and does not modify any other GitHub repository.

## UI and languages

- New emerald-and-gold visual identity, custom launcher mark, pulsing hero panel, staggered fade/slide-in module cards, and softly tinted icon tiles.
- Sorani Kurdish (کوردی), Arabic (العربية), and English (English); Sorani/Arabic use RTL layout, English uses LTR.
- Native Android UI; minimum Android 7.0 (API 24), designed to be tested on Android 11.

## Current source implementation

The native Kotlin/Jetpack Compose app integrates checked content from all three supplied APK sets. Important current paths are:

- **Qur'an and Mushaf:** all 114 surah entries and 6,236 verse records; four Sorani/Badini translations from Noor, English Sahih International, Kurdish transliteration, Arabic-only reading, whole-Qur'an search, diacritic toggle, personal bookmarks/notes, verse notes, related-hadith links, and an offline 604-page original SVG Mushaf. The viewer saves page position and maps page selections back to verses.
- **Tafsir:** 16 source selections from Noor and Khairah, including Roshn, Hazhar, Maisar, Sahih International, Noor, Hawrami, Runahi Badini, Zhian Badini, Saadi, Mukhtasar, Rebar and Tawhid, plus Ibn Kathir, Muyassar, Saadi and Tabari in Khairah. Khairah's compressed page data is decoded on demand and checked using recorded byte counts and SHA-256 hashes.
- **Hadith library:** all 8,202 Noor narrations, grouped under the 13 source collections and searchable across text, kitab/chapter names, notes and footnotes. Results are paginated and show book hierarchy, reference numbers and copy/translation controls. Noor's separate 1,141-record verse-related hadith collection has its own decoder and matching to the referenced surah/ayah.
- **Word-by-word study and lexicon:** all 77,429 morphology records, 45 word-type definitions, multi-segment glyph rendering with the original included Quran font, roots and grammar; the separate 620-entry lexicon has count-aware paging and search.
- **Worship content:** Noor's 11 adhkar categories and 133 chapters are merged with Salawati's translated adhkar, references and unique items. Repeat counters and favourites persist. All built-in tasbih presets are browsable. Allah's names merge Noor's Quranic reference/Badini details with Salawati's 99-name translation set.
- **Prayer location and alerts:** country-aware search against Salawati's 166,404-location catalogue plus Noor's city list, without putting the entire global location catalogue into UI memory. GPS/network fix, manual city coordinates, nearby source timetables from both databases, exact-distance candidate selection and coordinate-based calculated fallback are implemented. Calculation method and Asr school are selectable. Qibla uses the selected coordinates and device sensors. Adhan alerts are optional, select bundled audio, schedule recurring prayers, and reschedule on device boot/time-zone change when Android permissions allow.
- **Audio, books and Farmuda:** 80 reciter entries, bundled audio playback and an adhan/mosque filter; the original 94-book catalogue plus all extracted offline PDFs and an in-app PDF page reader; all 240 Farmuda records with search and persistent favourites.
- **Personal-data safety:** personal verse bookmarks and notes, reading position, prayer/location preferences, adhkar progress/favourites, tasbih counts and Farmuda favourites can be exported to and restored from a JSON backup file. The included Quran, tafsir, audio and books remain inside the APK and are not duplicated into that personal backup.

The extraction workflow checks the SHA-256 identity of each source APK-set, archive integrity, SQLite integrity, the 604-page Mushaf index, and literal asset references from Kotlin. Android build jobs run Gradle unit tests, lint, assembly, APK signing/integrity checks and upload a ZIP containing the APK and verification reports.

**Important limitation:** a successful build and database integrity report do not prove every screen behaves perfectly on a real phone. The latest build has not been manually installed and tested on the user's realme X2 Pro / Android 11 in this environment. Treat the current project as a data-rich working build pending that physical-device smoke test, not as a certified 1:1 clone of all original app behaviour. Public redistribution rights for bundled audio, fonts, translations and books must also be confirmed before production release.

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
