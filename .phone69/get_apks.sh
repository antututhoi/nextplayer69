#!/usr/bin/env bash
# apps.txt lines: name | kind | source     source = self (newest good build of this repo) or a download link
set -u
mkdir -p apks out69 samples
grep -v '^#' .phone69/apps.txt | while IFS='|' read -r name kind src; do
  name=$(echo "$name" | xargs); src=$(echo "$src" | xargs); [ -z "$name" ] && continue
  if [ -n "${APP_ONLY:-}" ] && [ "$name" != "$APP_ONLY" ]; then continue; fi
  if [ "$src" = "self" ]; then
    id=$(gh run list -R "$GITHUB_REPOSITORY" --workflow "$BUILD_WORKFLOW" --status success -L 1 --json databaseId -q '.[0].databaseId')
    rm -rf dl && gh run download "$id" -R "$GITHUB_REPOSITORY" -D dl && f=$(find dl -name '*.apk' | head -1)
    [ -n "${f:-}" ] && cp "$f" "apks/$name.apk" && echo "$name: build run $id $(basename "$f")" | tee -a out69/apps-found.txt
  else
    curl -sSfL -A "Mozilla/5.0" -o "apks/$name.apk" "$src" && echo "$name: $src" | tee -a out69/apps-found.txt || { rm -f "apks/$name.apk"; echo "$name: download failed" | tee -a out69/apps-found.txt; }
  fi
done
# sample files (made here, nothing downloaded)
sudo apt-get install -y -qq ffmpeg zip > /dev/null 2>&1
cd samples
ffmpeg -nostdin -loglevel error -f lavfi -i testsrc2=size=1280x720:rate=30 -f lavfi -i sine=frequency=440 -t 60 -c:v libx264 -pix_fmt yuv420p -c:a aac sample_h264.mp4
printf '1\n00:00:01,000 --> 00:00:20,000\nSubtitle line one\n\n2\n00:00:21,000 --> 00:00:50,000\nSubtitle line two\n' > sample_subs.srt
ffmpeg -nostdin -loglevel error -f lavfi -i testsrc=size=1920x1080:rate=24 -f lavfi -i sine=frequency=300 -f lavfi -i sine=frequency=600 -i sample_subs.srt -t 90 -map 0 -map 1 -map 2 -map 3 -c:v libx265 -c:a:0 aac -c:a:1 libopus -c:s srt -metadata:s:a:0 language=eng -metadata:s:a:1 language=jpn sample_subs.mkv
ffmpeg -nostdin -loglevel error -f lavfi -i testsrc=size=640x480 -frames:v 1 picture.png
ffmpeg -nostdin -loglevel error -f lavfi -i sine=frequency=500 -t 20 song.mp3
echo "hello from the 69 virtual phone" > note.txt
zip -q archive.zip note.txt picture.png
ls -la
