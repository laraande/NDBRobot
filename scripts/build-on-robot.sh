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

# A successful build that produced fewer classes than there are sources means
# the compiler did not see everything -- usually a file pushed a moment too
# late. Say so rather than letting it look fine.
CLASSES=$(adb -s "$HUB" shell 'ls /sdcard/FIRST/java/build/classes/org/firstinspires/ftc/teamcode/*.class' </dev/null | tr -d '\r' | grep -vc '\$' || true)
if [ "${CLASSES:-0}" -lt "${BEFORE:-0}" ]; then
  echo
  echo "$BEFORE source file(s) but only $CLASSES class(es) -- rebuilding once."
  curl -s -o /dev/null --max-time 20 "$BASE/java/build/start"
  curl -s --max-time 180 "$BASE/java/build/wait" >/dev/null || true
  LOG=$(adb -s "$HUB" shell 'cat /sdcard/FIRST/java/status/buildLog.txt' </dev/null | tr -d '\r')
  if [ -n "$LOG" ]; then
    echo
    echo "BUILD FAILED on retry:"
    echo "$LOG"
    exit 1
  fi
  CLASSES=$(adb -s "$HUB" shell 'ls /sdcard/FIRST/java/build/classes/org/firstinspires/ftc/teamcode/*.class' </dev/null | tr -d '\r' | grep -vc '\$' || true)
  if [ "${CLASSES:-0}" -lt "${BEFORE:-0}" ]; then
    echo "Still $CLASSES class(es) for $BEFORE source(s). Something else is wrong."
    exit 1
  fi
  echo "Retry produced all $CLASSES class(es)."
fi
echo
echo "Classes on the hub:"
adb -s "$HUB" shell 'ls /sdcard/FIRST/java/build/classes/org/firstinspires/ftc/teamcode/' </dev/null | tr -d '\r' | sed 's|^|  |'
echo
echo "If the Driver Station still shows an old error, restart the robot:"
echo "the Robot Controller can hold on to a previously built class."
