#!/bin/bash
# Copy the robot's code and configurations into hub-snapshots/.
# Read-only: nothing on the robot is changed.
source "$(dirname "$0")/common.sh"
require_hub

DEST="hub-snapshots/$(date +%Y-%m-%d-%H%M%S)"
mkdir -p "$DEST/config"

say "Copying the robot's code..."
hub_pull /sdcard/FIRST/java/src "$DEST/src"

say "Copying the robot's configurations..."
hub 'ls /sdcard/FIRST/' | grep '\.xml$' | while read -r f; do
  hub_pull "/sdcard/FIRST/$f" "$DEST/config/" || true
done

say "Saved to $DEST"
