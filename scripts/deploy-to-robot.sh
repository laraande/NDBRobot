#!/bin/bash
# Replace the robot's code with the contents of teamcode/.
#
#   1. Saves a backup of what is on the robot now
#   2. Removes every .java file from the robot's teamcode folder
#   3. Copies teamcode/*.java across
#
# It asks before deleting anything. Robot configurations are not touched.
#
# For everyday work use scripts/build-on-robot.sh instead -- it copies the
# files across without removing anything. This script is for making the
# robot match this folder exactly.
source "$(dirname "$0")/common.sh"
require_hub

say "After this the robot will have exactly these files:"
ls teamcode/*.java | sed 's|teamcode/|  |'
say ""
say "Everything else in the robot's teamcode folder will be removed."
say "A backup is saved to this laptop first."
say ""
read -r -p "Continue? [y/N] " answer
case "$answer" in
  y|Y) ;;
  *) say "Cancelled. Nothing was changed."; exit 0 ;;
esac

BACKUP="hub-snapshots/before-deploy-$(date +%Y-%m-%d-%H%M%S)"
mkdir -p "$BACKUP"
hub_pull /sdcard/FIRST/java/src "$BACKUP/src"
say "Backup saved to $BACKUP"

hub "rm -f $TEAMCODE/*.java" >/dev/null
for f in teamcode/*.java; do
  hub_push "$f" "$TEAMCODE/"
done

say ""
say "On the robot now:"
hub "ls $TEAMCODE" | sed 's/^/  /'
say ""
say "Next: scripts/build-on-robot.sh --no-push"
say "Then: scripts/restart-robot-app.sh"
