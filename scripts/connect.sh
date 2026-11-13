#!/bin/bash
# Connect to the robot. Run this first, every session.
# The laptop must be on the robot's Wi-Fi.
source "$(dirname "$0")/common.sh"

adb disconnect >/dev/null 2>&1 || true
adb connect "$HUB"
sleep 1

if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  die "Could not connect.

  1. Is the laptop on the robot's Wi-Fi? (7593-A-RC)
  2. Is the Control Hub on?
  3. Still stuck: run 'adb kill-server' and try again."
fi

say "Connected to the Control Hub."
