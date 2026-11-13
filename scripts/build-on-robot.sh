#!/bin/bash
# Push teamcode/ to the robot and build it.
#
# Does what "Build Everything" on the OnBot Java web page does, but from the
# laptop, and checks the result properly. Run scripts/restart-robot-app.sh
# afterwards so the robot actually loads what was built.
#
#   scripts/build-on-robot.sh             push, then build
#   scripts/build-on-robot.sh --no-push   build what is already there
source "$(dirname "$0")/common.sh"
require_hub

# --- 1. Copy the code over ---------------------------------------------
# The push happens inside this script on purpose. The build must not start
# until the files have fully landed, and doing both here is the only way to
# guarantee the order.
if [ "${1:-}" != "--no-push" ]; then
  for f in teamcode/*.java; do
    hub_push "$f" "$TEAMCODE/"
  done
  say "Pushed $(ls teamcode/*.java | wc -l | tr -d ' ') file(s)."
fi

# --- 2. Wait for the files to settle -----------------------------------
# A build started too soon reads a half-written file. It then either fails
# with "does not contain class" or, worse, succeeds and compiles nothing.
# Waiting for the robot's view of the folder to stop changing avoids both.
wait_for_files_to_settle() {
  local previous="" current=""
  for _ in $(seq 1 10); do
    current=$(hub "ls -l $TEAMCODE/")
    [ "$current" = "$previous" ] && return 0
    previous="$current"
    sleep 1
  done
}
wait_for_files_to_settle

# --- 3. Start the build ------------------------------------------------
hub_web
if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$WEB/java/build/start")" != "200" ]; then
  die "Could not start the build.

Is the Robot Controller running on the hub? Try scripts/restart-robot-app.sh."
fi

say "Building..."
curl -s --max-time 180 "$WEB/java/build/wait" >/dev/null || true

# --- 4. Did it compile? ------------------------------------------------
ERRORS=$(hub "cat $JAVA_STATUS/buildLog.txt")
if [ -n "$ERRORS" ]; then
  warn "BUILD FAILED:"
  echo "$ERRORS"
  exit 1
fi

say "Build successful ($(hub "head -1 $JAVA_STATUS/buildSuccessful.txt"))."

# --- 5. Did it actually produce anything? ------------------------------
# The nastiest failure on this hub: a build reports success, writes an empty
# build log, and produces a jar with no code in it. The robot then shows no
# Java programs at all, as if nothing was ever installed.
#
# Checking that a jar exists is not enough, because the empty one exists.
# Size is what tells them apart: a real jar is tens of kilobytes.
JAR="$(hub "cat $JAVA_STATUS/currentOnBotJavaDir.txt")/OnBotJava.jar"
SIZE=$(hub "wc -c < '$JAR' 2>/dev/null" | tr -d ' ')

[ -z "$SIZE" ] && die "Build reported success but produced no jar at all."

if [ "$SIZE" -lt 2000 ]; then
  die "BUILD PRODUCED AN EMPTY JAR ($SIZE bytes).

It said it succeeded but compiled nothing. The robot will show no Java
programs, as if nothing was installed.

This usually means a file was still being written when the build read it.
Just run this script again."
fi

say "Jar looks good ($SIZE bytes)."
say ""
say "Now run scripts/restart-robot-app.sh so the robot loads it."
