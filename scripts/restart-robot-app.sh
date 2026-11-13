#!/bin/bash
# Restart the Robot Controller app on the robot.
#
# Needed after every build. A new build writes new code, but the robot keeps
# running whatever it already loaded until the app restarts. Skipping this is
# the most common reason a change seems to do nothing.
#
# This does not reboot the hub. If a program changed what it inherits from,
# or an error will not clear, power cycle the robot instead.
source "$(dirname "$0")/common.sh"
require_hub

say "Restarting the Robot Controller..."
hub "am force-stop $RC_APP" >/dev/null
sleep 2
adb -s "$HUB" shell "monkey -p $RC_APP -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1

say "Waiting for the programs to load..."
for _ in $(seq 1 30); do
  sleep 2
  FOUND=$(adb -s "$HUB" logcat -d -t 400 2>/dev/null \
          | grep "OpmodeRegistration: registered" || true)
  if [ -n "$FOUND" ]; then
    say ""
    say "Programs the robot loaded:"
    echo "$FOUND" | sed 's/.*registered //' | sed 's/^/  /'
    say ""
    say "If yours is not listed, it is not on the robot."
    exit 0
  fi
done

die "Timed out. Check the Driver Hub -- it may still be reconnecting."
