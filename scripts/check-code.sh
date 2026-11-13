#!/bin/bash
# Compile-check teamcode/ on the laptop, without the robot.
#
# Catches typos, wrong method names, missing imports and type mistakes in a
# couple of seconds, so the OnBot Java "Build Everything" step is not the first
# time an error shows up. It does NOT catch wrong hardware names, wrong ports or
# anything about how the robot actually behaves -- only that the code compiles.
#
# Nothing is sent to the robot and nothing here is used by the robot. The jars in
# tools/lib/ are the Control Hub's own copies of the FTC SDK, pulled off it on
# 2026-11-12, so this works with no internet and no Android Studio.
set -e
cd "$(dirname "$0")/.."

LIB=tools/lib
if [ ! -f "$LIB/onbotjava-classes.jar" ]; then
  echo "Missing $LIB/onbotjava-classes.jar."
  echo "Copy it from a hub snapshot: hub-snapshots/<date>/FIRST/java/lib/"
  exit 1
fi

TARGET="${1:-teamcode}"
if [ ! -d "$TARGET" ]; then echo "No such folder: $TARGET"; exit 1; fi

COUNT=$(ls "$TARGET"/*.java 2>/dev/null | wc -l | tr -d ' ')
if [ "$COUNT" = "0" ]; then echo "No .java files in $TARGET"; exit 1; fi

OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT

echo "Checking $COUNT file(s) in $TARGET/ ..."
if javac -nowarn -proc:none -source 8 -target 8 \
     -bootclasspath "$LIB/android.jar" \
     -cp "$LIB/onbotjava-classes.jar:$LIB/android.jar:$LIB/androidx-rs.jar" \
     -d "$OUT" "$TARGET"/*.java 2>&1; then
  echo "OK - everything compiles."
  echo "(This does not check hardware names or robot behaviour.)"
else
  echo
  echo "Errors above. Fix them before deploying to the robot."
  exit 1
fi
