#!/usr/bin/env bash
set -euo pipefail

: "${APK_PATH:?The successful build APK path must be supplied by the workflow}"
mkdir -p dist/runtime-test

# Bound every ADB call so a hung emulator command cannot occupy the runner for
# the workflow's full 45-minute timeout. The APK install gets a longer limit.
ADB_BIN="$(command -v adb)"
if [[ -z "$ADB_BIN" ]]; then
  echo "FAIL: adb executable was not found in PATH."
  exit 1
fi
MASHXALL_ADB_TIMEOUT_SECONDS="${MASHXALL_ADB_TIMEOUT_SECONDS:-180}"
adb() {
  timeout --foreground "${MASHXALL_ADB_TIMEOUT_SECONDS}s" "$ADB_BIN" "$@"
}

adb wait-for-device

# On slow Android 11 CI emulators, sys.boot_completed may become 1 before
# PackageManager's Binder service is registered. Confirm that package queries
# actually work before using adb install, and collect useful diagnostics if it
# never becomes ready.
package_ready=0
for attempt in $(seq 1 60); do
  if timeout --foreground 15s "$ADB_BIN" shell pm list packages >/dev/null 2>dist/runtime-test/package-manager-check.txt; then
    package_ready=1
    echo "PackageManager ready after $attempt check(s)." | tee dist/runtime-test/package-manager-ready.txt
    break
  fi
  sleep 5
done
if [[ "$package_ready" -ne 1 ]]; then
  echo "FAIL: PackageManager never became ready; app installation was not attempted." | tee dist/runtime-test/install.txt
  adb shell getprop > dist/runtime-test/getprop.txt || true
  adb shell service list > dist/runtime-test/services.txt 2>&1 || true
  timeout --foreground 20s "$ADB_BIN" logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
  exit 1
fi

adb logcat -c
install_success=0
for attempt in 1 2 3; do
  echo "Install attempt $attempt/3" | tee -a dist/runtime-test/install.txt
  if timeout --foreground 300s "$ADB_BIN" install --no-streaming "$APK_PATH" 2>&1 | tee -a dist/runtime-test/install.txt; then
    install_success=1
    break
  fi
  if grep -q "Can't find service: package" dist/runtime-test/install.txt; then
    echo "PackageManager transiently unavailable; waiting 15 seconds before retry." | tee -a dist/runtime-test/install.txt
    sleep 15
  else
    break
  fi
done
if [[ "$install_success" -ne 1 ]]; then
  adb shell getprop > dist/runtime-test/getprop.txt || true
  adb shell service list > dist/runtime-test/services.txt 2>&1 || true
  adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
  exit 1
fi

# A fresh emulator starts without location permission or a mock GPS fix. Grant both
# before first launch so the prayer screen is tested with coordinates rather than
# mistaking an empty location state for missing bundled data.
adb shell pm grant com.mashxall.unified android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.mashxall.unified android.permission.ACCESS_FINE_LOCATION
adb emu geo fix 44.3661 33.3152
sleep 3

check_screen() {
  local route="$1"
  local expected_title="$2"
  local file="dist/runtime-test/window-${route}.xml"

  echo "=== Runtime screen smoke: ${route} ==="
  adb shell am force-stop com.mashxall.unified
  # Android's am start parser on API 30 accepts Intent flags through -f,
  # not the unsupported long options --activity-new-task / --activity-clear-task.
  adb shell am start -S -f 0x10008000 \
    -n com.mashxall.unified/.MainActivity \
    --es com.mashxall.unified.OPEN_SCREEN "$route" |
    tee "dist/runtime-test/launch-${route}.txt"
  sleep 12

  local pid
  pid="$(adb shell pidof com.mashxall.unified | tr -d '\r')"
  if [[ -z "$pid" ]]; then
    echo "FAIL: app process died while opening route: $route"
    adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
    exit 1
  fi

  # Android 11 emulators can raise a system-process ANR dialog while the
  # large offline content bundle is being installed or first opened. Recover
  # from System UI, Settings, or app ANR prompts by tapping Android's Wait
  # button and re-inspecting the foreground window.
  local attempt
  for attempt in 1 2 3 4; do
    adb shell uiautomator dump /sdcard/mashxall-window.xml > "dist/runtime-test/uiautomator-${route}.txt" || true
    adb pull /sdcard/mashxall-window.xml "$file" >/dev/null || true
    if grep -Eqi "isn't responding|is not responding" "$file"; then
      echo "WARNING: Android ANR dialog covers route ${route}; tapping Wait and re-inspecting."
      local coords
      coords="$(python3 - "$file" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    print("")
    raise SystemExit(0)
for node in root.iter("node"):
    label = node.attrib.get("text", "").strip().lower()
    if label == "wait":
        bounds = node.attrib.get("bounds", "")
        m = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
print("")
PY
)"
      if [[ -n "$coords" ]]; then
        read -r tap_x tap_y <<< "$coords"
        adb shell input tap "$tap_x" "$tap_y" || true
      else
        adb shell input keyevent 4 || true
      fi
      sleep 8
      continue
    fi
    break
  done
  # Match the screen heading inside the main content area, not a same-named
  # item in the persistent bottom-navigation bar (which caused a false positive
  # for Quran when Prayer was still the active screen).
  if ! python3 - "$file" "$expected_title" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, pattern = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(path).getroot()
except Exception as exc:
    print(f"FAIL: cannot parse UI hierarchy: {exc}")
    raise SystemExit(1)

matcher = re.compile(pattern, re.I)
matches = []
for node in root.iter("node"):
    text = node.attrib.get("text", "").strip()
    bounds = node.attrib.get("bounds", "")
    found = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if not text or not found or not matcher.search(text):
        continue
    x1, y1, x2, y2 = map(int, found.groups())
    # The app content starts below the brand/language header and above the bottom bar.
    if y1 >= 220 and y2 <= 1584 and y2 > y1 and x2 > x1:
        matches.append((y1, text))
if not matches:
    print(f"FAIL: no content-area heading matched {pattern!r}; bottom-nav labels are intentionally ignored.")
    raise SystemExit(1)
y, heading = min(matches)
print(f"PASS: content heading matched at y={y}: {heading}")
PY
  then
    echo "FAIL: expected content title not visible for route: $route"
    cat "$file"
    adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
    exit 1
  fi
  echo "PASS: route $route remained active; its actual content heading matched."
}

check_screen prayer 'کاتەکانی بانگ|Prayer times|مواقيت الصلاة'
if ! grep -Eq 'بەیانی|الفجر|Fajr' dist/runtime-test/window-prayer.xml; then
  echo "FAIL: Fajr label is missing from the rendered prayer screen."
  cat dist/runtime-test/window-prayer.xml
  exit 1
fi

check_screen quran 'قورئانی پیرۆز|القرآن الكريم|The Holy Quran'

# First launch must copy and SHA-256 verify the original Quran database into
# app-private storage. On a cold Android 11 emulator that can take longer than
# the normal screen-navigation delay. Poll the visible verse text instead of
# falsely declaring the data missing after a single 12-second screenshot.
quran_content_visible=0
for attempt in $(seq 1 20); do
  # Uthmani Mushaf text uses Quran-specific signs (for example ۡ rather
  # than the common ْ). Normalize Unicode combining marks before matching
  # so the smoke assertion verifies actual content instead of one spelling.
  if python3 - dist/runtime-test/window-quran.xml <<'PY'
import sys
import unicodedata
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(1)

def normalize_arabic(value):
    normalized = unicodedata.normalize("NFD", value).replace("\u0640", "")
    normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return normalized.replace("\u0671", "\u0627")

visible_text = " ".join(node.attrib.get("text", "") for node in root.iter("node"))
normalized = normalize_arabic(visible_text)
if "بسم الله الرحمن الرحيم" in normalized or "الحمد لله رب العالمين" in normalized:
    raise SystemExit(0)
raise SystemExit(1)
PY
  then
    quran_content_visible=1
    echo "PASS: readable Quran verse text appeared after cold-start poll $attempt (Uthmani diacritics normalized)."
    break
  fi
  if [[ "$attempt" -lt 20 ]]; then
    echo "Waiting for Quran database initialization ($attempt/20)…"
    sleep 3
    adb shell uiautomator dump /sdcard/mashxall-quran-content-wait.xml > "dist/runtime-test/uiautomator-quran-content-wait-$attempt.txt" 2>&1 || true
    adb pull /sdcard/mashxall-quran-content-wait.xml dist/runtime-test/window-quran.xml >/dev/null 2>&1 || true
  fi
done
if [[ "$quran_content_visible" -ne 1 ]]; then
  echo "FAIL: expected Quran verse text is still missing after up to 60 seconds of waiting."
  echo "Capturing Android logcat because the Quran route opened but readable verse content never appeared."
  timeout --foreground 20s "$ADB_BIN" logcat -d -v threadtime > dist/runtime-test/logcat-quran-data-failure.txt || true
  echo "=== Relevant Mashxall database errors ==="
  grep -E 'MashxallData|Content query failed|FATAL EXCEPTION|SQLite|Unable to open|quran\.db|no such column|no such table|database integrity mismatch' \
    dist/runtime-test/logcat-quran-data-failure.txt | tail -n 100 || true
  echo "=== APK content asset inventory (selected paths) ==="
  timeout --foreground 15s "$ADB_BIN" shell run-as com.mashxall.unified find files/content-databases -maxdepth 1 -type f -print 2>&1 | head -n 50 || true
  cat dist/runtime-test/window-quran.xml
  exit 1
fi

# Exercise the 604-page Mushaf's immersive display, rather than only proving
# that its verse-list screen opens.
mushaf_coords="$(python3 - dist/runtime-test/window-quran.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    if not re.search(r"Mushaf|مصح", text, re.I):
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$mushaf_coords" ]]; then
  echo "FAIL: Mushaf view toggle is not accessible in the Quran screen."
  cat dist/runtime-test/window-quran.xml
  exit 1
fi
read -r mushaf_x mushaf_y <<< "$mushaf_coords"
adb shell input tap "$mushaf_x" "$mushaf_y"
sleep 15
adb shell uiautomator dump /sdcard/mashxall-mushaf-fullscreen.xml > dist/runtime-test/uiautomator-mushaf-fullscreen.txt || true
adb pull /sdcard/mashxall-mushaf-fullscreen.xml dist/runtime-test/window-mushaf-fullscreen.xml >/dev/null || true
if ! grep -Eq 'Mushaf|مصح' dist/runtime-test/window-mushaf-fullscreen.xml ||
   ! grep -Eq 'Exit|خروج|دەرچوون' dist/runtime-test/window-mushaf-fullscreen.xml ||
   ! grep -q '604' dist/runtime-test/window-mushaf-fullscreen.xml; then
  echo "FAIL: immersive Mushaf viewer controls or 604-page metadata are missing."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
  exit 1
fi
if grep -Eq 'The Holy Quran|القرآن الكريم|قورئانی پیرۆز' dist/runtime-test/window-mushaf-fullscreen.xml; then
  echo "FAIL: app Quran header is still visible; immersive mode did not hide app chrome."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  exit 1
fi
echo "PASS: immersive full-screen Mushaf controls and page range are visible."

exit_coords="$(python3 - dist/runtime-test/window-mushaf-fullscreen.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    if not re.search(r"Exit|خروج|دەرچوون", text, re.I):
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$exit_coords" ]]; then
  echo "FAIL: fullscreen Mushaf exit control is not accessible."
  exit 1
fi
read -r exit_x exit_y <<< "$exit_coords"
adb shell input tap "$exit_x" "$exit_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-quran-restored.xml > dist/runtime-test/uiautomator-quran-restored.txt || true
adb pull /sdcard/mashxall-quran-restored.xml dist/runtime-test/window-quran-restored.xml >/dev/null || true
if ! grep -Eq 'قورئانی پیرۆز|القرآن الكريم|The Holy Quran' dist/runtime-test/window-quran-restored.xml; then
  echo "FAIL: exiting immersive Mushaf did not restore the Quran screen."
  cat dist/runtime-test/window-quran-restored.xml
  exit 1
fi
echo "PASS: exiting immersive Mushaf restores the Quran screen and app chrome."

check_screen hadith 'فەرموودەکان|الأحاديث|Hadith'

# The Hadith database is the largest text DB apart from Quran; on a cold
# Android 11 start the immutable asset must be copied and SHA-256 verified
# before its first SELECT. Do not confuse the initial "0 loaded" state with
# missing records: wait until the real source count is rendered, then capture
# DB errors if it never arrives.
hadith_content_visible=0
for attempt in $(seq 1 20); do
  if grep -Eq '8202|٨٬٢٠٢|٨٢٠٢' dist/runtime-test/window-hadith.xml; then
    hadith_content_visible=1
    echo "PASS: source Hadith count became visible after cold-start poll $attempt."
    break
  fi
  if [[ "$attempt" -lt 20 ]]; then
    echo "Waiting for first Hadith database open/query ($attempt/20)…"
    sleep 3
    adb shell uiautomator dump /sdcard/mashxall-hadith-content-wait.xml > "dist/runtime-test/uiautomator-hadith-content-wait-$attempt.txt" 2>&1 || true
    adb pull /sdcard/mashxall-hadith-content-wait.xml dist/runtime-test/window-hadith.xml >/dev/null 2>&1 || true
  fi
done
if [[ "$hadith_content_visible" -ne 1 ]]; then
  echo "FAIL: 8,202 source Hadith records were not shown after up to 60 seconds."
  timeout --foreground 20s "$ADB_BIN" logcat -d -v threadtime > dist/runtime-test/logcat-hadith-data-failure.txt || true
  echo "=== Relevant Mashxall Hadith/database errors ==="
  grep -E 'MashxallData|Content query failed|FATAL EXCEPTION|SQLite|Unable to open|db_hadith\.db|no such column|no such table|database integrity mismatch' \
    dist/runtime-test/logcat-hadith-data-failure.txt | tail -n 120 || true
  echo "=== Cached content database files ==="
  timeout --foreground 15s "$ADB_BIN" shell run-as com.mashxall.unified find files/content-databases -maxdepth 1 -type f -print 2>&1 | head -n 80 || true
  cat dist/runtime-test/window-hadith.xml
  exit 1
fi

article_coords="$(python3 - dist/runtime-test/window-hadith.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Articles\s*\(11\)|المقالات\s*\(11\)|وتارەکان\s*\(11\)", re.I)
for node in root.iter("node"):
    if not pattern.search(node.attrib.get("text", "")):
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$article_coords" ]]; then
  echo "FAIL: the 11-article tab is missing from the Hadith screen."
  cat dist/runtime-test/window-hadith.xml
  exit 1
fi
read -r article_x article_y <<< "$article_coords"
adb shell input tap "$article_x" "$article_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-hadith-articles.xml > dist/runtime-test/uiautomator-hadith-articles.txt || true
adb pull /sdcard/mashxall-hadith-articles.xml dist/runtime-test/window-hadith-articles.xml >/dev/null || true
if ! grep -Eq 'سیفەتەکانی شکۆ و گەورەیی|صفات الجلال والعظمة|Attributes of Majesty and Greatness' dist/runtime-test/window-hadith-articles.xml; then
  echo "FAIL: the source Hadith articles did not render their localized title."
  cat dist/runtime-test/window-hadith-articles.xml
  adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
  exit 1
fi
if ! python3 - dist/runtime-test/window-hadith-articles.xml <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
texts = [node.attrib.get("text", "").strip() for node in root.iter("node")]
if max((len(text) for text in texts), default=0) < 120:
    raise SystemExit(1)
PY
then
  echo "FAIL: the source Hadith article title appeared but its body text did not render."
  cat dist/runtime-test/window-hadith-articles.xml
  exit 1
fi
echo "PASS: original Hadith articles and body text render in the second Hadith tab."

check_screen books 'کتێبخانە|المكتبة|Book library'
if ! grep -q '94' dist/runtime-test/window-books.xml; then
  echo "FAIL: expected 94-record source book catalogue was not visible."
  cat dist/runtime-test/window-books.xml
  exit 1
fi

check_screen word_by_word 'شیکردنەوەی وشە بە وشە|تفسير كلمة بكلمة|Word-by-word analysis'

# The word-analysis screen starts with a default surah/ayah pair (1:1), but
# must explicitly load it. Tap the visible arrow just as a user would; this
# also verifies pressing the button works when the inputs have not changed.
word_analysis_coords="$(python3 - dist/runtime-test/window-word_by_word.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
for node in root.iter("node"):
    if node.attrib.get("text", "").strip() not in {"→", "←"}:
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$word_analysis_coords" ]]; then
  echo "FAIL: word-analysis load button was not visible."
  cat dist/runtime-test/window-word_by_word.xml
  exit 1
fi
read -r word_analysis_x word_analysis_y <<< "$word_analysis_coords"
adb shell input tap "$word_analysis_x" "$word_analysis_y"
word_analysis_content_visible=0
for attempt in $(seq 1 30); do
  sleep 3
  adb shell uiautomator dump /sdcard/mashxall-word-analysis-loaded.xml > "dist/runtime-test/uiautomator-word-analysis-loaded-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-word-analysis-loaded.xml dist/runtime-test/window-word_by_word.xml >/dev/null 2>&1 || true
  if python3 - dist/runtime-test/window-word_by_word.xml <<'PY'
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(1)
texts = [node.attrib.get("text", "").strip() for node in root.iter("node")]
# A rendered 1:1 verse is shown only inside the results list, so this confirms
# the button triggered an actual content query rather than merely opening the screen.
if any(len(text) >= 35 for text in texts):
    raise SystemExit(0)
raise SystemExit(1)
PY
  then
    word_analysis_content_visible=1
    echo "PASS: default word-analysis selection 1:1 loaded actual results (poll $attempt)."
    break
  fi
done
if [[ "$word_analysis_content_visible" -ne 1 ]]; then
  echo "FAIL: tapping the word-analysis load button did not render verse/word results."
  adb logcat -d -v threadtime > dist/runtime-test/logcat-word-analysis-failure.txt || true
  grep -E 'MashxallData|Content query failed|FATAL EXCEPTION|db_word_by_word|quran_corpus|no such column|no such table' dist/runtime-test/logcat-word-analysis-failure.txt | tail -n 100 || true
  cat dist/runtime-test/window-word_by_word.xml
  exit 1
fi
check_screen farmuda 'فەرموودە و دوعاکان|المأثورات والأدعية|Supplications & sayings'
check_screen tafsir 'تەفسیرو وەرگێڕان|التفسير والترجمة|Tafsir & translation'
check_screen azkar 'زیکر و دوعاکان|الأذكار والأدعية|Adhkar & duas'
check_screen names 'ناوە پیرۆزەکان|أسماء الله الحسنى|Names of Allah'
check_screen tasbih 'تەسبیح|المسبحة|Digital tasbih'
check_screen audio 'دەنگ و تلاوەت|التلاوات والصوتيات|Recitations & audio'
check_screen lexicon 'فەرهەنگی وشەکان|معجم الكلمات|Word lexicon'
check_screen qibla 'ئاڕاستەی قیبلە|اتجاه القبلة|Qibla direction'
check_screen bookmarks 'نیشانکراوەکان|العلامات المرجعية|Bookmarks'
check_screen backup 'پاشەکەوتکردن و گەڕاندنەوە|النسخ الاحتياطي والاستعادة|Backup & restore'
check_screen home 'دڵت ڕووناک بکە|أضئ قلبك بالمعرفة|Let knowledge light your heart'
check_screen library 'کتێبخانەی ئیسلامی|المكتبة الإسلامية|Islamic library'
check_screen worship 'زیکر و دوعاکان|الأذكار والأدعية|Adhkar & duas'
check_screen settings 'ڕێکخستن|الإعدادات|Settings'

assert_visible_body_text() {
  local route="$1"
  local min_chars="${2:-35}"
  local file="dist/runtime-test/window-${route}.xml"
  echo "=== Content body smoke: ${route} (minimum ${min_chars} characters) ==="
  if ! python3 - "$file" "$min_chars" <<'PY'
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

path = Path(sys.argv[1])
minimum = int(sys.argv[2])
try:
    root = ET.parse(path).getroot()
except Exception as exc:
    print(f"FAIL: cannot parse UI hierarchy {path}: {exc}")
    raise SystemExit(1)
texts = [node.attrib.get("text", "").strip() for node in root.iter()]
longest = max((len(text) for text in texts), default=0)
candidates = [text for text in texts if len(text) >= minimum]
if not candidates:
    print(f"FAIL: no visible content paragraph with at least {minimum} characters; longest text was {longest}.")
    for text in sorted(texts, key=len, reverse=True)[:12]:
        print(f"  ({len(text)}) {text[:180]}")
    raise SystemExit(1)
print(f"PASS: {len(candidates)} visible content text nodes; longest is {longest} characters.")
for text in sorted(candidates, key=len, reverse=True)[:2]:
    print(f"  ({len(text)}) {text[:180]}")
PY
  then
    adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
    exit 1
  fi
}

# A screen title alone is not enough; require content records to render too.
assert_visible_body_text quran 35
assert_visible_body_text tafsir 35
assert_visible_body_text hadith 35
assert_visible_body_text azkar 35
assert_visible_body_text word_by_word 35
assert_visible_body_text farmuda 35

adb logcat -d -v threadtime > dist/runtime-test/logcat.txt
if grep -E 'FATAL EXCEPTION|NoClassDefFoundError|UnsatisfiedLinkError|Fatal signal' dist/runtime-test/logcat.txt; then
  echo "FAIL: Android reported a fatal runtime error while exercising the screens."
  exit 1
fi

cat > dist/runtime-test/RESULT.txt <<'EOF'
PASS
APK installed on Android 11 (API 30).
Prayer, Quran, Hadith, 94-book catalogue, word-analysis, Farmuda, tafsir, adhkar, names, tasbih, audio, lexicon, Qibla, bookmarks, and backup screens were opened directly.
UI automation verified the prayer-time title and Fajr label, Quran text, source Hadith count, and catalogue count. It also required long visible content text in Quran, tafsir, Hadith, adhkar, word-analysis and Farmuda screens.
No fatal process exception was found in the captured logcat.
This CI emulator run does not replace checking GPS sensors, compass accuracy, adhan playback, and local alarm permissions on the user's realme phone.
EOF
cat dist/runtime-test/RESULT.txt
