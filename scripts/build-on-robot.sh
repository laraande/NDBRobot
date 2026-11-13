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

# OnBot packages the compiled classes into a jar and removes the intermediate
# .class files, so counting those is not a reliable check -- an earlier version
# of this script called a good build a failure for that reason. Check that a
# jar was produced after the build started instead.
JAR=$(adb -s "$HUB" shell 'ls -1 /sdcard/FIRST/java/build/jars/' </dev/null | tr -d '\r' | tail -1)
if [ -z "$JAR" ]; then
  echo
  echo "Build reported success but produced no jar. Something else is wrong."
  exit 1
fi

echo "Jar: $JAR"
echo
echo "If the Driver Station still shows an old error, restart the robot:"
echo "the Robot Controller can hold on to a previously built class."
