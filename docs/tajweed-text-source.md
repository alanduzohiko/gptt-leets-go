# Color-coded Tajweed in verse-text mode

Mashxall adds a native colour-coded Tajweed option for the standard verse-text
reader. It parses the tagged Arabic text into Compose text spans so only the
letters marked by a Tajweed rule receive that rule's colour. A dark-reader
palette improves contrast without changing the Qur'an text order.

## Data and offline behaviour

- Source: AlQuranCloud's `quran-tajweed` edition, requested one surah at a time.
- Network: the first use of a surah requires internet access.
- Cache: successfully parsed surahs are stored in the app's private files directory.
- Failure behaviour: if a complete, valid response is unavailable and no valid cache exists, the app leaves regular Qur'an text available and reports that Tajweed text could not be loaded.
- Scope: this colours the verse-text reader. It does not repaint the bundled 604-page graphic Mushaf; that page layout remains the original source image/vector page.
- Source code attribution: the rule identifiers and base colour mapping are adapted from the MIT-licensed [spa5k/Tajweed](https://github.com/spa5k/Tajweed). The exact notice is included in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

The Tajweed text is not bundled in the APK and is not presented as an offline feature until a surah has successfully been loaded and cached. The upstream text's own terms remain separate from the MIT license covering the parser palette/mapping.
