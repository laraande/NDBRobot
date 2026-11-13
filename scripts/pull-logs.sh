#!/bin/bash
# Copy the bring-up CSV logs off the Control Hub into logs/.
# Read-only: nothing on the robot changes or is deleted.
set -e
cd "$(dirname "$0")/.."
HUB=192.168.43.1:5555
HUB_LOGS=/sdcard/FIRST/matchlogs

if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Run scripts/connect.sh first."; exit 1
fi

mkdir -p logs

# List the CSVs on the hub first, so we can report what was found.
FOUND=$(adb -s "$HUB" shell "ls $HUB_LOGS/*.csv 2>/dev/null" </dev/null | tr -d '\r' | grep -c . || true)
if [ "$FOUND" = "0" ]; then
  echo "No .csv files in $HUB_LOGS."
  echo "Did the bring-up op mode run? Check the log name it showed on screen."
  exit 1
fi

echo "Found $FOUND log file(s) on the hub. Copying into logs/ ..."
adb -s "$HUB" shell "ls $HUB_LOGS/*.csv" </dev/null | tr -d '\r' | while IFS= read -r f; do
  name=$(basename "$f")
  adb -s "$HUB" pull -a "$f" "logs/$name" >/dev/null && echo "  logs/$name"
done

echo
echo "Newest log:"
ls -t logs/*.csv 2>/dev/null | head -1
echo
echo "Nothing was deleted from the robot. Run 'adb disconnect' before"
echo "switching Wi-Fi back."
