#!/usr/bin/env bash
# Runs inside the virtual phone step: sample files, then each app: install, walk, crash log.
set -u
adb shell settings put global hide_error_dialogs 0 < /dev/null
for t in 1 2 3 4 5 6 7 8; do
  adb shell mkdir -p /sdcard/Movies /sdcard/Download/sub < /dev/null
  adb push samples/sample_h264.mp4 /sdcard/Movies/ < /dev/null > /dev/null 2>&1
  adb shell ls /sdcard/Movies/sample_h264.mp4 < /dev/null 2>/dev/null | grep -q sample && break
  sleep 15
done
for f in samples/*; do
  case "$f" in
    *.mp4|*.mkv|*.srt) adb push "$f" /sdcard/Movies/ < /dev/null > /dev/null 2>&1 ;;
    *) adb push "$f" /sdcard/Download/ < /dev/null > /dev/null 2>&1 ;;
  esac
done
adb push samples/note.txt /sdcard/Download/sub/ < /dev/null > /dev/null 2>&1
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/sample_h264.mp4 < /dev/null > /dev/null 2>&1
adb shell "content call --method scan_volume --uri content://media --arg external_primary" < /dev/null > /dev/null 2>&1
AAPT=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt
for apk in apks/*.apk; do
  name=$(basename "$apk" .apk)
  kind=$(grep "^$name|" .phone69/apps.txt | head -1 | cut -d'|' -f2 | xargs); kind=${kind:-other}
  pkg=$("$AAPT" dump badging "$apk" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  out="$PWD/out69/$name"; mkdir -p "$out"
  "$AAPT" dump badging "$apk" 2>/dev/null | grep -E "^package|sdkVersion|native-code|application-label:" > "$out/apk-info.txt"
  echo "== $name ($pkg, $kind)"
  adb install -r -g "$apk" > "$out/install.txt" 2>&1 || { cat "$out/install.txt"; continue; }
  adb shell appops set "$pkg" MANAGE_EXTERNAL_STORAGE allow < /dev/null
  adb shell appops set "$pkg" SYSTEM_ALERT_WINDOW allow < /dev/null
  adb logcat -c
  adb shell "screenrecord --size 540x1200 --bit-rate 600000 --time-limit 170 /sdcard/first_$name.mp4" < /dev/null > /dev/null 2>&1 &
  BUDGET_MIN="${BUDGET_MIN:-70}" python3 -I .phone69/crawl.py "$out/crawl" "$name" "$kind" "$pkg" < /dev/null > "$out/crawl_stdout.txt" 2>&1
  adb logcat -d -t 20000 > "$out/logcat.txt" 2>&1
  grep -n -A25 "FATAL EXCEPTION\|ANR in $pkg" "$out/logcat.txt" > "$out/crashes.txt" 2>/dev/null || echo "no crashes" > "$out/crashes.txt"
  grep -c "FATAL EXCEPTION" "$out/logcat.txt" | sed "s/^/$name crashes: /"
  adb pull "/sdcard/first_$name.mp4" "$out/" < /dev/null > /dev/null 2>&1
  adb shell am force-stop "$pkg" < /dev/null
done
exit 0
