#!/bin/bash
# Copy the Control Hub's code and configurations into hub-snapshots/<date-time>/.
# Read-only: nothing on the robot changes.
set -e
cd "$(dirname "$0")/.."
HUB=192.168.43.1:5555
if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Run scripts/connect.sh first."; exit 1
fi
DEST="hub-snapshots/$(date +%Y-%m-%d-%H%M%S)"
mkdir -p "$DEST/config"
adb -s "$HUB" pull -a /sdcard/FIRST/java/src "$DEST/src"
adb -s "$HUB" shell 'ls /sdcard/FIRST/' </dev/null | tr -d '\r' | grep '\.xml$' | while IFS= read -r f; do
  adb -s "$HUB" pull -a "/sdcard/FIRST/$f" "$DEST/config/" </dev/null || true
done
echo "Saved to $DEST"
