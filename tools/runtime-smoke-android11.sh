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

# The app now asks for location on first use. Grant runtime permission and
# enable the emulator provider before automation, so the OS dialog does not
# cover the screen being tested. This does not replace the user's real-phone
# permission/GPS verification.
adb shell pm grant com.mashxall.unified android.permission.ACCESS_FINE_LOCATION || true
adb shell pm grant com.mashxall.unified android.permission.ACCESS_COARSE_LOCATION || true
adb shell settings put secure location_mode 3 || true
# Give the CI emulator a deterministic Kurdistan location before the app asks
# for its first fix. Emulator geo fix arguments are longitude, latitude.
adb emu geo fix 45.44 35.56 || true

check_screen() {
  local route="$1"
  local expected_title="$2"
  local file="dist/runtime-test/window-${route}.xml"

  echo "=== Runtime screen smoke: ${route} ==="
  adb shell am force-stop com.mashxall.unified
  # Android 11 can spend >12 seconds restoring this large app. An activity-start
  # timeout is not itself a failure if the requested route eventually renders.
  timeout --foreground 45s adb shell am start -S -W -f 0x10008000 \
    -n com.mashxall.unified/.MainActivity \
    --es com.mashxall.unified.OPEN_SCREEN "$route" \
    > "dist/runtime-test/launch-${route}.txt" 2>&1 || true
  sleep 8

  local pid
  pid="$(adb shell pidof com.mashxall.unified 2>/dev/null | tr -d '\r' || true)"
  if [[ -z "$pid" ]]; then
    echo "FAIL: app process died while opening route: $route"
    adb logcat -d -v threadtime > dist/runtime-test/logcat.txt || true
    exit 1
  fi

  local attempt
  local screen_ok=0
  for attempt in $(seq 1 10); do
    # Never reuse a UI hierarchy from the previous route. uiautomator can return
    # a null root during an ANR; an old XML previously misreported Quran as Hadith.
    rm -f "$file"
    adb shell rm -f /sdcard/mashxall-window.xml >/dev/null 2>&1 || true
    adb shell uiautomator dump /sdcard/mashxall-window.xml \
      > "dist/runtime-test/uiautomator-${route}-${attempt}.txt" 2>&1 || true
    if ! adb pull /sdcard/mashxall-window.xml "$file" >/dev/null 2>&1 || [[ ! -s "$file" ]]; then
      echo "WARN: UI hierarchy unavailable for ${route} (attempt ${attempt}/10); retrying."
      sleep 5
      continue
    fi

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
    raise SystemExit(0)
for node in root.iter("node"):
    if node.attrib.get("text", "").strip().lower() == "wait":
        match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
      if [[ -n "$coords" ]]; then
        read -r tap_x tap_y <<< "$coords"
        adb shell input tap "$tap_x" "$tap_y" || true
      else
        adb shell input keyevent 4 || true
      fi
      sleep 5
      continue
    fi

    if python3 - "$file" "$expected_title" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, pattern = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(path).getroot()
except Exception:
    raise SystemExit(1)
matcher = re.compile(pattern, re.I)
for node in root.iter("node"):
    text = node.attrib.get("text", "").strip()
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if not text or not match or not matcher.search(text):
        continue
    x1, y1, x2, y2 = map(int, match.groups())
    if y1 >= 220 and y2 <= 1584 and y2 > y1 and x2 > x1:
        print(f"PASS: content heading matched at y={y1}: {text}")
        raise SystemExit(0)
raise SystemExit(1)
PY
    then
      screen_ok=1
      echo "PASS: route ${route} rendered its own content screen (attempt ${attempt})."
      break
    fi

    echo "Waiting for the ${route} content screen to render (attempt ${attempt}/10)…"
    sleep 5
  done

  if [[ "$screen_ok" -ne 1 ]]; then
    echo "FAIL: expected content title not visible for route: $route"
    cat "$file" 2>/dev/null || true
    adb logcat -d -v threadtime > "dist/runtime-test/logcat-${route}-failure.txt" || true
    grep -E 'ANR in com\\.mashxall|FATAL EXCEPTION|AndroidRuntime|MashxallData|Content query failed|Input dispatching timed out' \
      "dist/runtime-test/logcat-${route}-failure.txt" | tail -n 100 || true
    exit 1
  fi
}
check_screen prayer 'کاتەکانی بانگ|Prayer times|مواقيت الصلاة'

# The heading can render while a large original timetable DB is still being
# copied/opened on the first run. Wait for prayer rows instead of treating the
# initial loading spinner as a missing Fajr label.
fajr_visible=0
for attempt in $(seq 1 30); do
  if grep -Eq 'بەیانی|الفجر|Fajr' dist/runtime-test/window-prayer.xml; then
    fajr_visible=1
    echo "PASS: Fajr prayer row rendered after poll $attempt."
    break
  fi
  if [[ "$attempt" -lt 30 ]]; then
    echo "Waiting for original prayer data to populate the screen ($attempt/30)…"
    sleep 5
    adb shell uiautomator dump /sdcard/mashxall-prayer-data.xml > "dist/runtime-test/uiautomator-prayer-data-$attempt.txt" 2>&1 || true
    adb pull /sdcard/mashxall-prayer-data.xml dist/runtime-test/window-prayer.xml >/dev/null 2>&1 || true
  fi
done
if [[ "$fajr_visible" -ne 1 ]]; then
  echo "FAIL: prayer screen did not render Fajr after waiting for original source data."
  adb logcat -d -v threadtime > dist/runtime-test/logcat-prayer-data-failure.txt || true
  grep -E 'MashxallData|Content query failed|prayertimes|salawati|Prayer|FATAL EXCEPTION|AndroidRuntime' dist/runtime-test/logcat-prayer-data-failure.txt | tail -n 120 || true
  cat dist/runtime-test/window-prayer.xml
  exit 1
fi

# The default clock must be 12-hour on the live prayer screen, not merely a label
# in Settings. Check localized AM/PM markers so 00:xx and 14:xx aren't shown as
# an unlabelled 24-hour clock.
if ! grep -Eq '[0-9]{1,2}:[0-9]{2} (AM|PM|صباحًا|مساءً|پ\.ن|د\.ن)' dist/runtime-test/window-prayer.xml; then
  echo "FAIL: prayer times did not show a localized 12-hour time marker by default."
  cat dist/runtime-test/window-prayer.xml
  exit 1
fi
echo "PASS: the prayer screen renders a localized 12-hour clock by default."

check_screen quran 'قورئانی پیرۆز|القرآن الكريم|The Holy Quran'

# The redesigned Quran entry is a surah list. Select Al-Fatiha, enter the
# real page-based Mushaf, then tap verse 1:1 to prove that verse actions and
# translations open inside the reader instead of a separate text-only screen.
surah_coords=""
for attempt in $(seq 1 20); do
  surah_coords="$(python3 - dist/runtime-test/window-quran.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
nodes = list(root.iter("node"))
patterns = re.compile(r"فاتح|الفاتحة|الفاتِحَة|al[- ]?fatiha|fatiha|fatih", re.I)
for node in nodes:
    if patterns.search(node.attrib.get("text", "").strip()):
        bounds = node.attrib.get("bounds", "")
        match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
            raise SystemExit(0)
# The first card's visible number is a fallback if the source uses a different
# localized spelling for Al-Fatiha.
for node in nodes:
    if node.attrib.get("text", "").strip() == "1":
        bounds = node.attrib.get("bounds", "")
        match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
            raise SystemExit(0)
PY
)"
  if [[ -n "$surah_coords" ]]; then
    echo "PASS: first-surah selection became available after poll $attempt."
    break
  fi
  if [[ "$attempt" -lt 20 ]]; then
    echo "Waiting for Quran source database and surah list ($attempt/20)…"
    sleep 3
    adb shell uiautomator dump /sdcard/mashxall-quran-list-wait.xml > "dist/runtime-test/uiautomator-quran-list-wait-$attempt.txt" 2>&1 || true
    adb pull /sdcard/mashxall-quran-list-wait.xml dist/runtime-test/window-quran.xml >/dev/null 2>&1 || true
  fi
done
if [[ -z "$surah_coords" ]]; then
  echo "FAIL: the Quran screen did not show a selectable first surah."
  cat dist/runtime-test/window-quran.xml
  exit 1
fi
read -r surah_x surah_y <<< "$surah_coords"
adb shell input tap "$surah_x" "$surah_y"

mushaf_ready=0
for attempt in $(seq 1 20); do
  sleep 3
  adb shell uiautomator dump /sdcard/mashxall-mushaf-open.xml > "dist/runtime-test/uiautomator-mushaf-open-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-mushaf-open.xml dist/runtime-test/window-mushaf-fullscreen.xml >/dev/null 2>&1 || true
  if grep -Eq 'Mushaf|مصح|المصحف' dist/runtime-test/window-mushaf-fullscreen.xml &&
     grep -q '604' dist/runtime-test/window-mushaf-fullscreen.xml &&
     grep -Eq 'Exit|خروج|دەرچوون' dist/runtime-test/window-mushaf-fullscreen.xml; then
    mushaf_ready=1
    echo "PASS: selecting Al-Fatiha opened the real 604-page Mushaf (poll $attempt)."
    break
  fi
done
if [[ "$mushaf_ready" -ne 1 ]]; then
  echo "FAIL: choosing the first surah did not open the graphic Mushaf page."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  adb logcat -d -v threadtime > dist/runtime-test/logcat-mushaf-failure.txt || true
  grep -E 'MashxallMushaf|MashxallData|Content query failed|FATAL EXCEPTION|SQLite' dist/runtime-test/logcat-mushaf-failure.txt | tail -n 100 || true
  exit 1
fi
if grep -Eq 'The Holy Quran|القرآن الكريم|قورئانی پیرۆز' dist/runtime-test/window-mushaf-fullscreen.xml; then
  echo "FAIL: the app-level header remained visible in immersive Mushaf mode."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  exit 1
fi

# Verify Quran reader settings can search the bundled reciter library without
# leaving the page-based Mushaf. The reciter list is long, so scroll the settings
# list to its source-backed search box before returning to the page.
reader_settings_coords="$(python3 - dist/runtime-test/window-mushaf-fullscreen.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Settings|الإعدادات|ڕێکخستن", re.I)
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
if [[ -z "$reader_settings_coords" ]]; then
  echo "FAIL: Mushaf reader settings button was not tappable."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  exit 1
fi
read -r reader_settings_x reader_settings_y <<< "$reader_settings_coords"
adb shell input tap "$reader_settings_x" "$reader_settings_y"
sleep 2
for attempt in $(seq 1 5); do
  adb shell input swipe 540 1320 540 520 450 || true
  sleep 1
  adb shell uiautomator dump /sdcard/mashxall-reader-settings.xml > "dist/runtime-test/uiautomator-reader-settings-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-reader-settings.xml dist/runtime-test/window-reader-settings.xml >/dev/null 2>&1 || true
  if grep -Eq 'Search reciters by name|ابحث عن قارئ بالاسم|بە ناوی قورئان‌خوێن بگەڕێ' dist/runtime-test/window-reader-settings.xml; then
    echo "PASS: Quran settings expose a search box for the source reciter library."
    break
  fi
done
if ! grep -Eq 'Search reciters by name|ابحث عن قارئ بالاسم|بە ناوی قورئان‌خوێن بگەڕێ' dist/runtime-test/window-reader-settings.xml; then
  echo "FAIL: Quran reader settings did not expose the reciter-library search after scrolling."
  cat dist/runtime-test/window-reader-settings.xml
  exit 1
fi
adb shell input keyevent 4
sleep 2

verse_coords="$(python3 - dist/runtime-test/window-mushaf-fullscreen.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
for node in root.iter("node"):
    if node.attrib.get("text", "").strip() not in {"1:1", "١:١"}:
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$verse_coords" ]]; then
  echo "FAIL: Mushaf page has no accessible 1:1 verse chip for the in-reader action test."
  cat dist/runtime-test/window-mushaf-fullscreen.xml
  exit 1
fi
read -r verse_x verse_y <<< "$verse_coords"
adb shell input tap "$verse_x" "$verse_y"
sleep 7
adb shell uiautomator dump /sdcard/mashxall-quran-verse-actions.xml > dist/runtime-test/uiautomator-quran-verse-actions.txt 2>&1 || true
adb pull /sdcard/mashxall-quran-verse-actions.xml dist/runtime-test/window-quran.xml >/dev/null 2>&1 || true
if ! python3 - dist/runtime-test/window-quran.xml <<'PY'
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(1)
texts = [node.attrib.get("text", "").strip() for node in root.iter("node")]
def normalize(value):
    value = unicodedata.normalize("NFD", value).replace("\u0640", "")
    return "".join(ch for ch in value if unicodedata.category(ch) != "Mn").replace("\u0671", "\u0627")
whole = " ".join(texts)
normalized = normalize(whole)
has_ayah = "بسم الله الرحمن الرحيم" in normalized or "الحمد لله رب العالمين" in normalized
has_actions = any(re.search(r"Copy|نسخ|کۆپی", value, re.I) for value in texts) and any(
    re.search(r"Play|تشغيل|لێدان", value, re.I) for value in texts
)
if has_ayah and has_actions:
    raise SystemExit(0)
print("Visible text nodes did not include both the verse and its copy/play actions.")
print("\n".join(f"({len(value)}) {value[:160]}" for value in sorted(texts, key=len, reverse=True)[:15]))
raise SystemExit(1)
PY
then
  echo "FAIL: tapping verse 1:1 did not open the unified verse-action panel with verse text, copy and playback."
  cat dist/runtime-test/window-quran.xml
  exit 1
fi
echo "PASS: tapping verse 1:1 opens its verse text and contextual actions inside the Mushaf."

# Save this verse so the later Bookmarks test proves that the saved reference
# opens its exact Mushaf page and its own action panel.
save_verse_coords="$(python3 - dist/runtime-test/window-quran.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"^(Save|حفظ|پاشەکەوت)$", re.I)
for node in root.iter("node"):
    if not pattern.search(node.attrib.get("text", "").strip()):
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$save_verse_coords" ]]; then
  echo "FAIL: the verse action panel does not expose a Save/Bookmark action."
  cat dist/runtime-test/window-quran.xml
  exit 1
fi
read -r save_verse_x save_verse_y <<< "$save_verse_coords"
adb shell input tap "$save_verse_x" "$save_verse_y"
sleep 2
echo "PASS: saved verse 1:1 from inside the Mushaf action panel."

# Dismiss the verse sheet, then exit the Mushaf back to the surah list.
adb shell input keyevent 4 || true
sleep 3
adb shell uiautomator dump /sdcard/mashxall-mushaf-restored.xml > dist/runtime-test/uiautomator-mushaf-restored.txt 2>&1 || true
adb pull /sdcard/mashxall-mushaf-restored.xml dist/runtime-test/window-mushaf-sheet-dismissed.xml >/dev/null 2>&1 || true
exit_coords="$(python3 - dist/runtime-test/window-mushaf-sheet-dismissed.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
for node in root.iter("node"):
    if not re.search(r"Exit|خروج|دەرچوون", node.attrib.get("text", ""), re.I):
        continue
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", node.attrib.get("bounds", ""))
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$exit_coords" ]]; then
  echo "FAIL: fullscreen Mushaf exit control is not accessible after dismissing the verse sheet."
  cat dist/runtime-test/window-mushaf-sheet-dismissed.xml
  exit 1
fi
read -r exit_x exit_y <<< "$exit_coords"
adb shell input tap "$exit_x" "$exit_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-quran-restored.xml > dist/runtime-test/uiautomator-quran-restored.txt 2>&1 || true
adb pull /sdcard/mashxall-quran-restored.xml dist/runtime-test/window-quran-restored.xml >/dev/null 2>&1 || true
if ! grep -Eq 'قورئانی پیرۆز|القرآن الكريم|The Holy Quran' dist/runtime-test/window-quran-restored.xml; then
  echo "FAIL: exiting the Mushaf did not return to the surah list."
  cat dist/runtime-test/window-quran-restored.xml
  exit 1
fi
echo "PASS: the Mushaf exits cleanly back to the surah list and the app header."


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
echo "PASS: original Hadith article cards show a separate title and readable preview."

article_detail_coords="$(python3 - dist/runtime-test/window-hadith-articles.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"سیفەتەکانی شکۆ و گەورەیی|صفات الجلال والعظمة|Attributes of Majesty and Greatness", re.I)
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
if [[ -z "$article_detail_coords" ]]; then
  echo "FAIL: source Hadith article title could not be tapped to open its details."
  cat dist/runtime-test/window-hadith-articles.xml
  exit 1
fi
read -r article_detail_x article_detail_y <<< "$article_detail_coords"
adb shell input tap "$article_detail_x" "$article_detail_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-hadith-article-detail.xml > dist/runtime-test/uiautomator-hadith-article-detail.txt 2>&1 || true
adb pull /sdcard/mashxall-hadith-article-detail.xml dist/runtime-test/window-hadith-article-detail.xml >/dev/null 2>&1 || true
# Verify the full article body in the detail sheet before scrolling: long source
# narrations naturally place the copy control below the first viewport.
if ! grep -Eq 'سیفەتەکانی شکۆ و گەورەیی|صفات الجلال والعظمة|Attributes of Majesty and Greatness' dist/runtime-test/window-hadith-article-detail.xml; then
  echo "FAIL: tapping an article preview did not open the dedicated detail sheet."
  cat dist/runtime-test/window-hadith-article-detail.xml
  exit 1
fi
if ! python3 - dist/runtime-test/window-hadith-article-detail.xml <<'PY'
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(1)
texts = [node.attrib.get("text", "").strip() for node in root.iter("node")]
if max((len(text) for text in texts), default=0) < 120:
    raise SystemExit(1)
PY
then
  echo "FAIL: the selected Hadith article detail did not render readable body text."
  cat dist/runtime-test/window-hadith-article-detail.xml
  exit 1
fi
hadith_copy_visible=0
for attempt in $(seq 1 8); do
  if grep -Eq 'Copy article|نسخ المقالة|وتار کۆپی بکە' dist/runtime-test/window-hadith-article-detail.xml; then
    hadith_copy_visible=1
    break
  fi
  adb shell input swipe 540 1500 540 420 400 || true
  sleep 1
  rm -f dist/runtime-test/window-hadith-article-detail.xml
  adb shell rm -f /sdcard/mashxall-hadith-article-detail.xml >/dev/null 2>&1 || true
  adb shell uiautomator dump /sdcard/mashxall-hadith-article-detail.xml > "dist/runtime-test/uiautomator-hadith-article-scroll-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-hadith-article-detail.xml dist/runtime-test/window-hadith-article-detail.xml >/dev/null 2>&1 || true
done
if [[ "$hadith_copy_visible" -ne 1 ]]; then
  echo "FAIL: the Hadith article detail opened but its copy action did not become visible after scrolling."
  cat dist/runtime-test/window-hadith-article-detail.xml
  exit 1
fi
echo "PASS: the Hadith article detail opens with readable body text and exposes its copy action after scrolling."

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

# A supplication should be a compact titled card. Open one entry and verify its
# own detail sheet contains dedicated actions rather than dumping all source
# languages into the same long list row.
farmuda_detail_coords="$(python3 - dist/runtime-test/window-farmuda.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Read full text|قراءة النص كاملًا|دەقەکە بە تەواوی بخوێنەوە", re.I)
for node in root.iter("node"):
    if not pattern.search(node.attrib.get("text", "")):
        continue
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", node.attrib.get("bounds", ""))
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
PY
)"
if [[ -z "$farmuda_detail_coords" ]]; then
  echo "FAIL: Farmuda list did not expose a separate read-full-text action."
  cat dist/runtime-test/window-farmuda.xml
  exit 1
fi
read -r farmuda_x farmuda_y <<< "$farmuda_detail_coords"
adb shell input tap "$farmuda_x" "$farmuda_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-farmuda-detail.xml > dist/runtime-test/uiautomator-farmuda-detail.txt 2>&1 || true
adb pull /sdcard/mashxall-farmuda-detail.xml dist/runtime-test/window-farmuda-detail.xml >/dev/null 2>&1 || true
if ! grep -Eq 'SORANI KURDISH|الكردية السورانية|کوردی سۆرانی|ARABIC|النص العربي|دەقی عەرەبی' dist/runtime-test/window-farmuda-detail.xml; then
  echo "FAIL: opening a Farmuda entry did not expose its source-language detail content."
  cat dist/runtime-test/window-farmuda-detail.xml
  exit 1
fi
farmuda_actions_visible=0
for attempt in $(seq 1 8); do
  if grep -Eq 'Favourite|مفضلة|دڵخواز' dist/runtime-test/window-farmuda-detail.xml &&
     grep -Eq 'Copy|نسخ|کۆپی' dist/runtime-test/window-farmuda-detail.xml; then
    farmuda_actions_visible=1
    break
  fi
  adb shell input swipe 540 1500 540 420 400 || true
  sleep 1
  rm -f dist/runtime-test/window-farmuda-detail.xml
  adb shell rm -f /sdcard/mashxall-farmuda-detail.xml >/dev/null 2>&1 || true
  adb shell uiautomator dump /sdcard/mashxall-farmuda-detail.xml > "dist/runtime-test/uiautomator-farmuda-scroll-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-farmuda-detail.xml dist/runtime-test/window-farmuda-detail.xml >/dev/null 2>&1 || true
done
if [[ "$farmuda_actions_visible" -ne 1 ]]; then
  echo "FAIL: Farmuda detail did not expose favourite and copy controls after scrolling."
  cat dist/runtime-test/window-farmuda-detail.xml
  exit 1
fi
echo "PASS: Farmuda entry source text renders and its favourite/copy actions are accessible after scrolling."
check_screen tafsir 'تەفسیرو وەرگێڕان|التفسير والترجمة|Tafsir & translation'
check_screen azkar 'زیکر و دوعاکان|الأذكار والأدعية|Adhkar & duas'

# The redesigned Adhkar route opens topic cards first. Open a real source category,
# then open an individual remembrance and assert that its dedicated details sheet
# shows the full Arabic text rather than stuffing every dhikr into the category list.
azkar_category_coords=""
for attempt in $(seq 1 8); do
  azkar_category_coords="$(python3 - dist/runtime-test/window-azkar.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"entries.*tap to open|ذكرًا.*اضغط للفتح|زیکر.*بۆ کردنەوە دەست لێبدە", re.I)
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
  if [[ -n "$azkar_category_coords" ]]; then break; fi
  sleep 3
  adb shell uiautomator dump /sdcard/mashxall-azkar-category.xml > "dist/runtime-test/uiautomator-azkar-category-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-azkar-category.xml dist/runtime-test/window-azkar.xml >/dev/null 2>&1 || true
done
if [[ -z "$azkar_category_coords" ]]; then
  echo "FAIL: source Adhkar categories did not render as separate selectable cards."
  cat dist/runtime-test/window-azkar.xml
  exit 1
fi
read -r azkar_category_x azkar_category_y <<< "$azkar_category_coords"
adb shell input tap "$azkar_category_x" "$azkar_category_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-azkar-items.xml > dist/runtime-test/uiautomator-azkar-items.txt 2>&1 || true
adb pull /sdcard/mashxall-azkar-items.xml dist/runtime-test/window-azkar-items.xml >/dev/null 2>&1 || true

azkar_item_coords="$(python3 - dist/runtime-test/window-azkar-items.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Open remembrance|فتح الذكر|زیکرەکە بکەرەوە", re.I)
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
if [[ -z "$azkar_item_coords" ]]; then
  echo "FAIL: Adhkar category opened but individual remembrance cards were not available."
  cat dist/runtime-test/window-azkar-items.xml
  exit 1
fi
read -r azkar_item_x azkar_item_y <<< "$azkar_item_coords"
adb shell input tap "$azkar_item_x" "$azkar_item_y"
sleep 5
adb shell uiautomator dump /sdcard/mashxall-azkar-detail.xml > dist/runtime-test/uiautomator-azkar-detail.txt 2>&1 || true
adb pull /sdcard/mashxall-azkar-detail.xml dist/runtime-test/window-azkar.xml >/dev/null 2>&1 || true
if ! grep -Eq 'Repetitions|عدد التكرارات|ژمارەی دووبارەکردنەوە' dist/runtime-test/window-azkar.xml; then
  echo "FAIL: tapping an Adhkar item did not open its dedicated detail and repetition counter."
  cat dist/runtime-test/window-azkar.xml
  exit 1
fi
echo "PASS: Adhkar categories open a list, and an individual remembrance opens its own detail sheet."
check_screen names 'ناوە پیرۆزەکان|أسماء الله الحسنى|Names of Allah'
check_screen tasbih 'تەسبیح|المسبحة|Digital tasbih'
check_screen audio 'دەنگ و تلاوەت|التلاوات والصوتيات|Recitations & audio'
check_screen lexicon 'فەرهەنگی وشەکان|معجم الكلمات|Word lexicon'
check_screen qibla 'ئاڕاستەی قیبلە|اتجاه القبلة|Qibla direction'
check_screen bookmarks 'نیشانکراوەکان|العلامات المرجعية|Bookmarks'

# Opening a bookmark must resume its exact Quran page and verse in the graphic
# Mushaf, not merely reset the user to the surah list.
bookmark_open_coords="$(python3 - dist/runtime-test/window-bookmarks.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Open at this verse|فتح عند هذه الآية|لەسەر ئەم ئایەتە بکەرەوە", re.I)
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
if [[ -z "$bookmark_open_coords" ]]; then
  echo "FAIL: the saved verse does not expose its open-at-verse action."
  cat dist/runtime-test/window-bookmarks.xml
  exit 1
fi
read -r bookmark_open_x bookmark_open_y <<< "$bookmark_open_coords"
adb shell input tap "$bookmark_open_x" "$bookmark_open_y"
bookmark_restore_ok=0
for attempt in $(seq 1 20); do
  sleep 3
  adb shell uiautomator dump /sdcard/mashxall-bookmark-resume.xml > "dist/runtime-test/uiautomator-bookmark-resume-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-bookmark-resume.xml dist/runtime-test/window-bookmark-resume.xml >/dev/null 2>&1 || true
  if grep -Eq 'Mushaf|مصح|المصحف' dist/runtime-test/window-bookmark-resume.xml &&
     grep -q '604' dist/runtime-test/window-bookmark-resume.xml &&
     grep -Eq 'Copy|نسخ|کۆپی' dist/runtime-test/window-bookmark-resume.xml; then
    bookmark_restore_ok=1
    echo "PASS: opening a saved bookmark restored its graphic Mushaf page and verse-action panel."
    break
  fi
done
if [[ "$bookmark_restore_ok" -ne 1 ]]; then
  echo "FAIL: opening a bookmark did not resume the exact saved verse in the Mushaf."
  cat dist/runtime-test/window-bookmark-resume.xml
  adb logcat -d -v threadtime > dist/runtime-test/logcat-bookmark-resume-failure.txt || true
  grep -E 'MashxallData|FATAL EXCEPTION|SQLite|Content query failed' dist/runtime-test/logcat-bookmark-resume-failure.txt | tail -n 80 || true
  exit 1
fi

check_screen backup 'پاشەکەوتکردن و گەڕاندنەوە|النسخ الاحتياطي والاستعادة|Backup & restore'
check_screen home 'کاتەکانی نوێژ|مواقيت الصلاة|Prayer times'

# Verify the new home hierarchy: live prayer times first, a resume-last-Mushaf
# action, and quick access beneath it. Tapping "Continue reading" must re-open
# the saved page-based Mushaf, not the old text-only verse list.
if ! grep -Eq 'بەردەوام بە لە خوێندنەوە|متابعة القراءة|Continue reading' dist/runtime-test/window-home.xml; then
  echo "FAIL: home does not show the continue-reading shortcut."
  cat dist/runtime-test/window-home.xml
  exit 1
fi
if ! grep -Eq 'دەستگەیشتنی خێرا|وصول سريع|Quick access' dist/runtime-test/window-home.xml; then
  echo "FAIL: quick access was removed from the home screen."
  cat dist/runtime-test/window-home.xml
  exit 1
fi
resume_coords="$(python3 - dist/runtime-test/window-home.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"بەردەوام بە لە خوێندنەوە|متابعة القراءة|Continue reading", re.I)
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
if [[ -z "$resume_coords" ]]; then
  echo "FAIL: Continue reading shortcut has no tappable UI bounds."
  cat dist/runtime-test/window-home.xml
  exit 1
fi
read -r resume_x resume_y <<< "$resume_coords"
adb shell input tap "$resume_x" "$resume_y"
sleep 7
adb shell uiautomator dump /sdcard/mashxall-home-resume.xml > dist/runtime-test/uiautomator-home-resume.txt 2>&1 || true
adb pull /sdcard/mashxall-home-resume.xml dist/runtime-test/window-home-resume.xml >/dev/null 2>&1 || true
if ! grep -Eq 'Mushaf|مصح|المصحف' dist/runtime-test/window-home-resume.xml ||
   ! grep -q '604' dist/runtime-test/window-home-resume.xml; then
  echo "FAIL: Continue reading did not resume the saved 604-page graphic Mushaf."
  cat dist/runtime-test/window-home-resume.xml
  exit 1
fi
echo "PASS: home prayer-times card, continue-reading resume, and quick access are visible and usable."

check_screen library 'کتێبخانەی کتێب|مكتبة الكتب|Book library'
check_screen worship 'پەرستش و زیکر|العبادات والأذكار|Worship'
check_screen settings 'ڕێکخستن|الإعدادات|Settings'

# The global Settings entry must open the real Quran reader settings dialog,
# rather than sending the user to an unrelated generic library page.
reader_settings_open_coords=""
for attempt in $(seq 1 8); do
  rm -f dist/runtime-test/window-settings.xml
  adb shell uiautomator dump /sdcard/mashxall-settings.xml > "dist/runtime-test/uiautomator-settings-scroll-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-settings.xml dist/runtime-test/window-settings.xml >/dev/null 2>&1 || true
  reader_settings_open_coords="$(python3 - dist/runtime-test/window-settings.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)
pattern = re.compile(r"Quran reader settings|إعدادات قارئ القرآن|ڕێکخستنەکانی خوێندنەوەی قورئان", re.I)
for node in root.iter("node"):
    if not pattern.search(node.attrib.get("text", "")):
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        if y1 >= 200 and y2 <= 1584 and x2 > x1 and y2 > y1:
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
            raise SystemExit(0)
PY
)"
  if [[ -n "$reader_settings_open_coords" ]]; then break; fi
  if [[ "$attempt" -lt 8 ]]; then
    adb shell input swipe 540 1370 540 510 450 || true
    sleep 1
  fi
done
if [[ -z "$reader_settings_open_coords" ]]; then
  echo "FAIL: global Settings does not contain a Quran reader settings shortcut."
  cat dist/runtime-test/window-settings.xml
  exit 1
fi
read -r quran_settings_x quran_settings_y <<< "$reader_settings_open_coords"
adb shell input tap "$quran_settings_x" "$quran_settings_y"
quran_settings_opened=0
for attempt in $(seq 1 8); do
  sleep 2
  adb shell uiautomator dump /sdcard/mashxall-settings-quran.xml > "dist/runtime-test/uiautomator-settings-quran-$attempt.txt" 2>&1 || true
  adb pull /sdcard/mashxall-settings-quran.xml dist/runtime-test/window-settings-quran.xml >/dev/null 2>&1 || true
  if grep -Eq 'DEFAULT TRANSLATION|الترجمة الافتراضية|وەرگێڕانی بنەڕەت' dist/runtime-test/window-settings-quran.xml &&
     grep -Eq 'DEFAULT TAFSIR|التفسير الافتراضي|تەفسیرى بنەڕەت' dist/runtime-test/window-settings-quran.xml; then
    quran_settings_opened=1
    echo "PASS: global Settings opens the in-reader translation and tafsir choices."
    break
  fi
done
if [[ "$quran_settings_opened" -ne 1 ]]; then
  echo "FAIL: global Quran reader settings did not show translation and tafsir choices."
  cat dist/runtime-test/window-settings-quran.xml
  exit 1
fi

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
