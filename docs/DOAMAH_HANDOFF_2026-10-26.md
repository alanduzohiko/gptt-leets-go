# DOAMAH HANDOFF — Mashxall project checkpoint

**Resume keyword:** `دوامة`  
**Checkpoint date:** 2026-10-26 (UTC)  
**Do not restart from scratch. Continue from the current branch and inspect the newest GitHub Actions run first.**

## Project identity and workflow
- Target app: **مەشخەڵ / Mashxall**, a native Android app. It supersedes the older “ئەلەند” concept.
- Repository: https://github.com/alanduzohiko/gptt-leets-go
- Active work branch: `redesign/reader-first-mashxall`
- Open PR: https://github.com/alanduzohiko/gptt-leets-go/pull/5
- Main branch must not be changed directly; keep using the redesign branch until all checks pass.
- User workflow: phone-only, Android 11 on realme X2 Pro; avoid Replit and Vercel.
- Source bundle release: https://github.com/alanduzohiko/gptt-leets-go/releases/tag/unified-source-v1
- Verified source bundle SHA-256 expected by CI: `9430a52d73bfff7d897320e36173ce9d6f72f7c8d24b24dcc45b3dd4e70aba96`

## Core user requirements — keep all of them in scope
The user’s goal is a complete, professional app, not a UI-only demo. Preserve the three languages (Sorani Kurdish, Arabic, English) and incorporate the original source/data/features from the three Islamic apps: **خيرة / Khairah, تفسير نور / Tafsir Noor, and صلواتي / Salawati**. Do not replace their content with invented/general placeholders.

### Home
- Replace “اختيار اليوم” with a beautiful real prayer-times card.
- Six rows/times: Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha.
- 12-hour format is the default; optional 24-hour setting.
- Keep “Quick Access / الوصول السريع” below the prayer times.
- Show source/location clearly; do not claim exactness until checked on the user’s phone.
- Request location permission in context; if Android location services are off, offer to open system location settings. Never claim Android lets the app turn GPS on silently.
- Manual city selection must supersede/remove stale GPS coordinates.
- Verify correct city/country/timezone/date, source schedule, calculation method and Asr school.

### Qur’an / Mushaf — highest priority
- Opening the Qur’an displays all 114 surahs in order, Al-Fatiha to An-Nas, with scrolling and search.
- Selecting a surah opens the **graphic 604-page Mushaf**, not a text-only ayah list.
- Put translation, tafsir, reciter selection, page style, verse text size, and verse tools in the Mushaf reading context.
- Tapping a verse should offer its Arabic text, chosen translation/tafsir, playback, copy, share, bookmark, related hadith and word-by-word study.
- “Continue reading” on Home should return to the last saved graphic Mushaf page. Opening Qur’an through Quick Access should open the surah list.
- Reader modes currently being implemented: normal paper, night mode and warm sepia; verse text size and default reciter/translation/tafsir.
- Do not advertise a full tajweed-coloured or alternate Madinah edition unless its complete page assets/data are present. A single sample page is not an edition.
- Tafsir and translations should not be buried in a generic, unrelated library screen.

### Hadith, adhkar and supplications
- Hadith list: subject/title, book/source and number, concise preview; tap for separate details.
- Clean raw HTML/mark-up so tags or awkward joined text do not display.
- Separate Arabic, translations, notes and footnotes visually; support search, favourites and copy/share.
- Adhkar: category landing page; category opens its own list; each dhikr opens its own detail with Arabic, available translation/source, repeat counter, favourites.
- Morning/evening and favourites filters should work.
- “المأثورات والأدعية / Farmuda” also needs titled preview cards and separate details, preserving source Arabic, Sorani and Badini when present.
- Reciter and muezzin audio libraries/settings belong in the Audio/Settings area, not mixed into generic adhkar cards.

### Audio/settings/customization/data
- Rich settings that truly work: theme, accent colours, time format, Quran reader, tafsir/translation defaults, reciters, adhan/muezzin library, location/calculation preferences, alerts and permissions.
- Opening the reciter library must not autoplay a verse. Autoplay only after an explicit verse playback action.
- Backup/restore must preserve personal bookmarks/notes/preferences. New backups use an authenticated password-protected format; maintain compatibility with legacy JSON exports.
- Keep the app Native and installable on Android 11.

## Work already applied on the active branch
Changes include:
- Home prayer times replacing the daily-pick card; 12-hour default and 12/24 controls.
- Home location request and source labeling.
- Salawati-first preference for a matching city schedule, Noor fallback where relevant; GPS rows are ranked by distance and Salawati wins an exact tie. These need ongoing verification against real-device/reference settings before claiming perfect timings.
- Qur’an surah list → graphic Mushaf page viewer; verse action sheet for copy, playback, bookmark, share, tafsir, linked hadith and word study.
- “Continue reading” hook/shortcut to saved Mushaf page.
- Mushaf night/sepia appearance and adjustable verse font size.
- Hadith category/article cards and separate details; raw HTML cleanup.
- Adhkar category list and separate dhikr details with repetition/favourites.
- Farmuda distinct detail sheet with available language variants, favourite and copy.
- App-wide accent palette choices (gold, emerald, blue, violet, rose and turquoise).
- Separate navigation for book library vs Quran and audio; prevent accidental audio autoplay.
- Portable authenticated backup envelope using AES-GCM; Gradle/JVM-testable crypto tests and legacy JSON import path.
- Arabic redesign spec at `docs/redesign-spec-ar.md`.
- CI source-audit scripts: `tools/audit_source_coverage.py`, `tools/audit_content_coverage.py`, `tools/audit_content_wiring.py` and `tools/audit_prayer_data.py`.
- Test workflow: `.github/workflows/ci-reader-first-redesign.yml`.

## Latest verified CI state as of this checkpoint
- Latest commit before this checkpoint’s file additions: `b7b9c118e5aacfc7121df024de0d8df2585a1ed9` — “Accept Sorani Mushaf back label in Android 11 smoke tests”.
- Prior run **40381457774**:
  - Original source bundle download/hash: PASS.
  - Original source extraction: PASS.
  - Three-app source/required-asset coverage audit: PASS.
  - Core data coverage audit: PASS.
  - Kotlin-to-asset wiring audit: PASS.
  - Noor/Salawati prayer-source audit: PASS.
  - Gradle unit tests, Lint, Debug APK build: PASS.
  - APK signature/archive integrity: PASS.
  - Android 11 emulator smoke: FAILED in the *test’s label-matching logic* at the “open Mushaf” assertion.
- The captured XML artifact from that failed run confirms the Mushaf **did actually open**: it showed `مصحەفی پیرۆز`, `پەڕە 1 / 604` and verse chips `1:1` through `1:6`. The back button uses Sorani content description `گەڕانەوە`, which the prior smoke regex failed to recognize.
- The smoke regex has now been fixed to recognize the Sorani back label. This triggered new CI runs **40385539828** and **40385526386** on SHA `b7b9c118e5aacfc7121df024de0d8df2585a1ed9`. At the time of writing they were **in progress**. Check them first:
  - https://github.com/alanduzohiko/gptt-leets-go/actions/runs/40385539828
  - https://github.com/alanduzohiko/gptt-leets-go/actions/runs/40385526386
- Artifact from the prior run: `mashxall-reader-first-redesign-206` (235.9 MB); it contains the debug APK and test logs. Do not hand this APK to the user as final because the end-to-end smoke suite failed before the remaining tests completed.

## Current task / immediate next actions
1. Check the two CI runs above. If a run is still in progress, inspect its current jobs/logs. Prefer the newest completed run on the latest SHA.
2. If smoke passes the Mushaf stage, keep fixing any *real* subsequent failures; do not weaken assertions just to make CI green. Continue through verse actions, hadith detail, adhkar category/detail, Farmuda details, settings and Home prayer card.
3. If the verse sheet test fails, use the artifact XML to compare exact Sorani labels/accessible bounds with the expected regex. Update tests for all three locales where practical.
4. Download the newest artifact only after the workflow is green, and record APK SHA-256. Do not claim final readiness until the end-to-end smoke succeeds.
5. Inspect current code/audit coverage around the extra page/adhkar source files the user says they found. In the current model workspace, one file is visible at `/mnt/data/user-c0jF84t4Tdm7uRD9lAvDlWqY/ccc2d479de834ad7b6190ad2d198e1c8/tmp/style_mushaf_tajweed.webp` (a single tajweed-coloured sample page, not a full Mushaf dataset). If the new chat does not expose the user’s other files, inspect repository assets first and only request reattachment of the missing specific files; do not restart the project or ask the user to repeat the full requirements.
6. When emulator checks pass, still perform device validation with the user’s realme X2 Pro / Android 11 for real GPS permission behaviour and local prayer-time comparison. An emulator success does not prove GPS accuracy on the physical phone.
7. Continue integrating all relevant features/assets from Khairah, Tafsir Noor, and Salawati. Do not declare the project complete based only on the visible UI.

## Communication rules for the next chat
When the user says **“دوامة”**, interpret it as “resume the full Mashxall project from the checkpoint and continue work.” Start by checking the branch/PR and latest Actions run; do not ask them to paste the long request again. Give honest progress updates, do not claim unverified features work, and keep using the same repository/branch until checks pass.
