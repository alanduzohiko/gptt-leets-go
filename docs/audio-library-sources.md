# Audio library sources and offline behaviour

## Quran reciter catalog

Mashxall's added full-surah and recitation-edition metadata is derived from [risan/quran-json: data/audio/mp3quran.json](https://github.com/risan/quran-json/blob/main/data/audio/mp3quran.json). The repository's source registry explicitly records the MP3Quran metadata license as **unknown** because no redistribution license could be identified. The app therefore does not claim CC BY-SA or another license for this catalog. It preserves source attribution and links to MP3Quran-hosted recordings rather than bundling audio files. Please verify current terms with MP3Quran before commercial redistribution.

The upstream catalog contains 288 recitation entries with different chapter coverage. Mashxall retains each entry's declared surah count, and playback/download is disabled when the chosen edition does not include the selected surah. The direct audio files remain hosted by MP3Quran.net; the metadata license is not asserted to be a license for the recordings themselves.

Known Arabic display names include أحمد النفيس, ناصر القطامي, ياسر الدوسري, and إسلام صبحي. Other entries retain the source's romanized name rather than guessing a translation.

## Adhan and mosque audio catalog

The online adhan index is derived from the public [Kiwifu/adhan-mp3 collection](https://github.com/Kiwifu/adhan-mp3), whose README describes 224 recordings (the current repository tree contains 231 MP3 files). The catalog index records the original file name, display name, file size, category and direct source URL. The total upstream MP3 payload is approximately 599.0 MB; Mashxall does **not** bundle that entire collection in the APK.

The repository description states that the collection is free for Islamic applications and personal use, but the current repository tree has no formal license file and individual recording rights have not been independently confirmed. Mashxall therefore links to the original host, and transfers an individual recording to the app's private storage only after the user taps Download. This source note is not a blanket claim that every recording has the same reuse license.

## Offline storage and network

Downloads are saved in the app's private files directory; they are not written to public shared storage. Downloading requires an internet connection, while a successfully saved track can be played again without internet. A failed or incomplete transfer is not marked as saved.
