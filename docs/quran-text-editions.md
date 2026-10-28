# Quran text-script editions in Mashxall

Mashxall continues to bundle its original Arabic Quran data and the original 604-page graphic Mushaf. The reader now also offers two additional **text scripts** for verse-by-verse reading. These are not replacement page-image Mushafs and do not change the original graphic pages.

## Available text scripts

- **Original Uthmani (bundled):** works offline immediately. It remains the fallback if an alternate script cannot be loaded.
- **QPC Hafs (Madinah script):** streamed by chapter from `https://quran-json.risanb.com/text/qpc-hafs/chapters/{chapter}.json`. Source snapshot revision recorded in the source registry: `8528abfcb55d808562ae9675217c07890322178f`.
- **Hafs Nastaliq (Indo-Pak text):** streamed by chapter from `https://quran-json.risanb.com/text/hafs-nastaliq/chapters/{chapter}.json`. Source snapshot revision recorded in the source registry: `a8b5918b0c6f636e61a0cb57fb3366667e45d712`.

The two alternate datasets are supplied by [Qur'anpedia.net](https://quranpedia.net/) through the [risan/quran-json data project](https://github.com/risan/quran-json). The source registry marks these snapshots as permitted under the [Qur'anpedia.net Data License](https://api.quranpedia.net/dumps/LICENSE.md). Attribution and the source snapshot revisions are kept here because the data license requires a link/source acknowledgement when a dataset is republished. The app downloads selected chapter text on demand; the audio files and fonts are not included in these text-script datasets.

## Validation, fallback and offline use

Before a chapter is rendered or cached, Mashxall requires the returned chapter ID to match the requested surah, every verse number to be present once in sequential Hafs order, and every verse text to be non-empty. Incomplete or wrongly numbered data is rejected so it cannot silently misalign the Quran text and translations. A successful chapter response is cached in app-private storage for later offline use. If the network request fails, the original bundled Uthmani text remains visible and can be used without internet.

Colored Tajweed markup is tied to the original Uthmani text and is disabled when an alternate script is selected. This prevents color spans from being applied to a different text sequence. Alternate scripts are available in the verse-text reader only. The full-page graphic reader remains the original 604-page image set.
