#!/usr/bin/env bash
set -euo pipefail

: "${APK_PATH:?The successful build APK path must be supplied by the workflow}"
mkdir -p dist/runtime-test

adb wait-for-device
adb logcat -c
adb install --no-streaming "$APK_PATH" | tee dist/runtime-test/install.txt

check_screen() {
  local route="$1"
  local expected_title="$2"
  local file="dist/runtime-test/window-${route}.xml"

  echo "=== Runtime screen smoke: ${route} ==="
  adb shell am force-stop com.mashxall.unified
  adb shell am start -n com.mashxall.unified/.MainActivity --es com.mashxall.unified.OPEN_SCREEN "$route" |
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
  if ! grep -Eq "$expected_title" "$file"; then
    echo "FAIL: expected title not visible for route: $route"
    cat "$file"
    adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
    exit 1
  fi
  echo "PASS: route $route remained active; title matched."
}

check_screen prayer 'کاتەکانی بانگ|Prayer times|مواقيت الصلاة'
if ! grep -Eq 'بەیانی|الفجر|Fajr' dist/runtime-test/window-prayer.xml; then
  echo "FAIL: Fajr label is missing from the rendered prayer screen."
  cat dist/runtime-test/window-prayer.xml
  exit 1
fi

check_screen quran 'قورئانی پیرۆز|القرآن الكريم|The Holy Quran'
if ! grep -Eq 'بِسْم|بسم|ٱلْحَمْد|الحمد' dist/runtime-test/window-quran.xml; then
  echo "FAIL: expected Quran verse text is missing from the rendered Quran screen."
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
if ! grep -Eq '8202|٨٬٢٠٢|٨٢٠٢' dist/runtime-test/window-hadith.xml; then
  echo "FAIL: expected source Hadith count was not visible."
  cat dist/runtime-test/window-hadith.xml
  exit 1
fi

check_screen books 'کتێبخانە|المكتبة|Book library'
if ! grep -q '94' dist/runtime-test/window-books.xml; then
  echo "FAIL: expected 94-record source book catalogue was not visible."
  cat dist/runtime-test/window-books.xml
  exit 1
fi

check_screen word_by_word 'شیکردنەوەی وشە بە وشە|تفسير كلمة بكلمة|Word-by-word analysis'
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
