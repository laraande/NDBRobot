#!/bin/bash
# Connect adb to the Control Hub. The laptop must be on the robot's Wi-Fi.
HUB=192.168.43.1:5555
adb disconnect >/dev/null 2>&1
adb connect "$HUB"
sleep 1
if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Join the robot's Wi-Fi, then run this again."
  echo "If it still fails: adb kill-server, then run this again."
  exit 1
fi
echo "Connected to the Control Hub."
