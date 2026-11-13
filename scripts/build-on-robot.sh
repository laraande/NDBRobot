#!/bin/bash
# Build the code that is on the Control Hub, from the laptop.
#
# Does what "Build Everything" on the OnBot Java page does, and prints the
# result. Use it after scripts/deploy-to-robot.sh so a compile error shows up
# here instead of only on the robot's web page.
#
# The hub's web server is reached through the adb connection rather than
# directly, because a laptop that is tethered to a phone while on the robot's
# Wi-Fi has two default routes and HTTP to 192.168.43.1 fails with "no route
# to host". adb's own connection is unaffected, so we tunnel through it.
set -e
cd "$(dirname "$0")/.."
HUB=192.168.43.1:5555
PORT=8080

if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Run scripts/connect.sh first."; exit 1
fi

# Push teamcode/ first unless --no-push was given. Doing the push here rather
# than in a separate command means the settle-wait below always runs after it;
# chaining "adb push && build-on-robot.sh" outside the script let the build
# read a half-written file and fail with "does not contain class".
if [ "${1:-}" != "--no-push" ]; then
  for f in teamcode/*.java; do
    adb -s "$HUB" push "$f" /sdcard/FIRST/java/src/org/firstinspires/ftc/teamcode/ >/dev/null
  done
  echo "Pushed $(ls teamcode/*.java | wc -l | tr -d ' ') file(s)."
fi

adb -s "$HUB" forward "tcp:$PORT" "tcp:$PORT" >/dev/null

# A build started too soon after an adb push can read the source directory
# before the new file lands, then report success having compiled without it.
# One second was not enough in practice, so wait for the hub's own view of the
# sources to stop changing before starting.
BEFORE=$(adb -s "$HUB" shell 'ls /sdcard/FIRST/java/src/org/firstinspires/ftc/teamcode/*.java' </dev/null | tr -d '\r' | wc -l | tr -d ' ')
settle_prev=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  settle_now=$(adb -s "$HUB" shell 'ls -l /sdcard/FIRST/java/src/org/firstinspires/ftc/teamcode/' </dev/null | tr -d '\r' | md5 2>/dev/null || true)
  if [ -n "$settle_prev" ] && [ "$settle_now" = "$settle_prev" ]; then break; fi
  settle_prev="$settle_now"
  sleep 1
done

BASE="http://127.0.0.1:$PORT"
code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$BASE/java/build/start")
if [ "$code" != "200" ]; then
  echo "Could not start the build (HTTP $code)."
  echo "Is the Robot Controller app running on the hub?"
  exit 1
fi

echo "Building..."
curl -s --max-time 180 "$BASE/java/build/wait" >/dev/null || true

LOG=$(adb -s "$HUB" shell 'cat /sdcard/FIRST/java/status/buildLog.txt' </dev/null | tr -d '\r')
WHEN=$(adb -s "$HUB" shell 'head -1 /sdcard/FIRST/java/status/buildSuccessful.txt' </dev/null | tr -d '\r')

if [ -n "$LOG" ]; then
  echo
  echo "BUILD FAILED:"
  echo "$LOG"
  exit 1
fi

echo "Build successful ($WHEN)."

# Verify the jar actually contains something.
#
# An empty build is the nastiest failure mode on this hub: OnBot can report
# "build successful" with an empty buildLog and still write a 166-byte jar
# holding no classes. The Robot Controller then loads it without complaint and
# the Driver Station shows only the Blocks op modes, as if the Java had never
# been installed. That happened at 22:59 on 2026-11-12 after a build read a
# half-written source file, and it cost a reboot and a lot of confusion.
#
# Neither of the two obvious checks works: counting .class files fails because
# OnBot removes them after packaging, and checking that a jar exists fails
# because the empty jar exists. Size is what distinguishes them -- a real jar
# with a few op modes is tens of kilobytes.
JAR_DIR=$(adb -s "$HUB" shell 'cat /sdcard/FIRST/java/status/currentOnBotJavaDir.txt' </dev/null | tr -d '\r')
JAR_SIZE=$(adb -s "$HUB" shell "wc -c < '$JAR_DIR/OnBotJava.jar' 2>/dev/null" </dev/null | tr -d '\r ' )

if [ -z "$JAR_SIZE" ]; then
  echo
  echo "Build reported success but no jar was produced. Something else is wrong."
  exit 1
fi

if [ "$JAR_SIZE" -lt 2000 ]; then
  echo
  echo "BUILD PRODUCED AN EMPTY JAR ($JAR_SIZE bytes)."
  echo
  echo "The build said it succeeded, but compiled nothing. The robot will show"
  echo "only the Blocks op modes, with no Java ones, as if nothing was installed."
  echo "This usually means the build read a source file while it was still being"
  echo "written. Run this script again."
  exit 1
fi

echo "Jar: $JAR_SIZE bytes, looks good."
echo
echo "If the Driver Station still shows an old error, restart the robot:"
echo "the Robot Controller can hold on to a previously built class."
