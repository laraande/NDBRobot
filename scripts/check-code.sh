#!/bin/bash
# Compile teamcode/ on the laptop. No robot needed.
#
# Catches typos, wrong names and missing imports in a couple of seconds, so
# the robot's build is not the first place an error shows up.
#
# It does NOT check hardware names, ports, or how the robot behaves. Only
# that the code compiles.
#
#   scripts/check-code.sh            check teamcode/
#   scripts/check-code.sh <folder>   check a different folder
source "$(dirname "$0")/common.sh"

LIB=tools/lib
TARGET="${1:-teamcode}"

if [ ! -f "$LIB/onbotjava-classes.jar" ]; then
  die "Missing $LIB/onbotjava-classes.jar.

These are the robot's own copies of the FTC libraries. Copy them from a
snapshot: hub-snapshots/<date>/FIRST/java/lib/"
fi

[ -d "$TARGET" ] || die "No such folder: $TARGET"

COUNT=$(ls "$TARGET"/*.java 2>/dev/null | wc -l | tr -d ' ')
[ "$COUNT" = "0" ] && die "No .java files in $TARGET/"

OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT

say "Checking $COUNT file(s) in $TARGET/ ..."

if javac -nowarn -proc:none -source 8 -target 8 \
     -bootclasspath "$LIB/android.jar" \
     -cp "$LIB/onbotjava-classes.jar:$LIB/android.jar:$LIB/androidx-rs.jar" \
     -d "$OUT" "$TARGET"/*.java 2>&1; then
  say "OK - everything compiles."
  say "(This does not check hardware names or robot behaviour.)"
else
  die "Errors above. Fix them before putting this on the robot."
fi
