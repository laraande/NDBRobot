#!/bin/bash
# Restart the Robot Controller app on the Control Hub, from the laptop.
#
# Why this is needed: OnBot Java compiles a new jar, but the Robot Controller
# has already loaded the old classes into a running JVM. A rebuilt class does
# not replace one that is already loaded, so after a build the robot can still
# be running the previous code. This does what "Restart Robot" on the Driver
# Station menu does.
#
# It does NOT reboot the hub. If a class changed what it inherits from (for
# example a stray @TeleOp was removed from a helper class), even this may not
# be enough and the hub needs a full power cycle.
set -e
cd "$(dirname "$0")/.."
HUB=192.168.43.1:5555
PKG=com.qualcomm.ftcrobotcontroller

if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Run scripts/connect.sh first."; exit 1
fi

echo "Stopping the Robot Controller..."
adb -s "$HUB" shell "am force-stop $PKG"
sleep 2

echo "Starting it again..."
adb -s "$HUB" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1

echo "Waiting for op modes to register..."
for i in $(seq 1 30); do
  sleep 2
  REG=$(adb -s "$HUB" logcat -d -t 300 2>/dev/null | grep -c "OpmodeRegistration: registered" || true)
  if [ "${REG:-0}" -gt 0 ]; then
    echo
    echo "Registered op modes:"
    adb -s "$HUB" logcat -d -t 300 2>/dev/null \
      | grep "OpmodeRegistration: registered" \
      | sed 's/.*registered //' | sed 's/^/  /' | tail -10
    echo
    echo "The Driver Station may need a moment to reconnect."
    exit 0
  fi
done

echo "Timed out waiting for registration. Check the Driver Station."
exit 1
