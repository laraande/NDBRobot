#!/bin/bash
# Replace the code on the Control Hub with the contents of teamcode/.
#   1. Backs up what is on the hub now into hub-snapshots/
#   2. Removes every .java file from the hub's teamcode folder
#   3. Copies teamcode/*.java onto the hub
# Then build from the OnBot Java page. Robot configurations are not touched.
set -e
cd "$(dirname "$0")/.."
LOCAL=teamcode
HUB=192.168.43.1:5555
HUB_SRC=/sdcard/FIRST/java/src
HUB_TEAMCODE=$HUB_SRC/org/firstinspires/ftc/teamcode

if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
  echo "Not connected. Run scripts/connect.sh first."; exit 1
fi

echo "Files that will be on the robot after this:"
ls "$LOCAL"/*.java | sed "s|$LOCAL/|  |"
echo
echo "Everything else in the robot's teamcode folder will be removed"
echo "(after a backup copy is saved on this laptop)."
read -r -p "Continue? [y/N] " answer
[ "$answer" = "y" ] || [ "$answer" = "Y" ] || { echo "Cancelled."; exit 0; }

BACKUP="hub-snapshots/before-deploy-$(date +%Y-%m-%d-%H%M%S)"
mkdir -p "$BACKUP"
adb -s "$HUB" pull -a "$HUB_SRC" "$BACKUP/src"
echo "Backup saved to $BACKUP"

adb -s "$HUB" shell "rm -f $HUB_TEAMCODE/*.java"
adb -s "$HUB" shell "rmdir '$HUB_SRC/TeleOp26-27' 2>/dev/null; true"
for f in "$LOCAL"/*.java; do
  adb -s "$HUB" push "$f" "$HUB_TEAMCODE/"
done

echo
echo "On the robot now:"
adb -s "$HUB" shell "ls -l $HUB_TEAMCODE"
echo
echo "Next: open http://192.168.43.1:8080, go to OnBot Java, refresh the page,"
echo "and press Build Everything (the wrench button)."
