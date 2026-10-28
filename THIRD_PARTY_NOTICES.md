# Third-party notices

## Tajweed rule palette and markup mapping

The rule identifiers, description mapping, and base color palette for Mashxall's Tajweed text renderer are adapted from the MIT-licensed [spa5k/Tajweed (tajweed-ts)](https://github.com/spa5k/Tajweed) project. Mashxall has a native Kotlin/Jetpack Compose parser; the JavaScript package itself is not bundled.

Copyright (c) 2021 spa5k

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Color-coded Tajweed text source

Selected-surah tagged text is requested on demand from the AlQuranCloud
`quran-tajweed` edition at
`https://api.alquran.cloud/v1/surah/{surah_number}/quran-tajweed`.
Mashxall does not bundle this tagged Quran dataset. After a successful request,
the selected surah is cached in app-private storage so it can be used offline
on that device. The user can switch the feature off at any time.

This source connection does not imply a claim that every underlying text or
recording is covered by the MIT license above. The MIT notice applies only to
the Tajweed markup/rule mapping derived from spa5k/Tajweed. The original
604-page graphic Mushaf is unchanged by this text-colouring option.


## Alternate Quran text scripts (verse text only)

The QPC Hafs and Hafs Nastaliq verse-text datasets are requested from
[Qur'anpedia.net](https://quranpedia.net/) via the
[risan/quran-json project](https://github.com/risan/quran-json). The source
registry marks the selected snapshots as permitted under the
[Qur'anpedia.net Data License](https://api.quranpedia.net/dumps/LICENSE.md).
Snapshot revisions recorded by the source registry:

- QPC Hafs (Madinah script): `8528abfcb55d808562ae9675217c07890322178f`
- Hafs Nastaliq (Indo-Pak text): `a8b5918b0c6f636e61a0cb57fb3366667e45d712`

Mashxall downloads only the selected surah's text on demand and caches a
validated response in app-private storage. These are verse-text scripts, not
additional Mushaf page images; the original 604-page graphic edition is
unchanged. The project does not bundle Quranpedia's fonts or page images here.
