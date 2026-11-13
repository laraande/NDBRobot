# Shared settings and helpers for the robot scripts.
# Not run on its own. Each script starts with:
#
#     source "$(dirname "$0")/common.sh"
#
# Everything below exists so the individual scripts can read like a list of
# steps instead of a pile of adb incantations.

set -e
cd "$(dirname "$0")/.."

# ----- Where things are -----
HUB=192.168.43.1:5555          # the Control Hub, over Wi-Fi
PORT=8080                      # the OnBot Java web server
TEAMCODE=/sdcard/FIRST/java/src/org/firstinspires/ftc/teamcode
JAVA_STATUS=/sdcard/FIRST/java/status
MATCHLOGS=/sdcard/FIRST/matchlogs
RC_APP=com.qualcomm.ftcrobotcontroller

# ----- Talking to the robot -----

# Run a command on the robot and print the result.
#
# The </dev/null stops adb from swallowing this script's own input, and the
# tr removes the carriage returns Android adds to every line, which otherwise
# break string comparisons in confusing ways.
hub() {
  adb -s "$HUB" shell "$@" </dev/null | tr -d '\r'
}

# Copy a file to the robot.
hub_push() {
  adb -s "$HUB" push "$1" "$2" >/dev/null
}

# Copy a file from the robot.
hub_pull() {
  adb -s "$HUB" pull -a "$1" "$2" >/dev/null
}

# Stop with a clear message if the robot is not reachable.
require_hub() {
  if [ "$(adb -s "$HUB" get-state 2>/dev/null)" != "device" ]; then
    echo "Not connected to the robot."
    echo
    echo "  1. Is the laptop on the robot's Wi-Fi?"
    echo "  2. Is the Control Hub on, and the Driver Hub showing Robot Connected?"
    echo "  3. Run scripts/connect.sh"
    exit 1
  fi
}

# Reach the hub's web server through the adb connection.
#
# Needed because a laptop tethered to a phone while on the robot's Wi-Fi has
# two default routes, and plain HTTP to 192.168.43.1 fails with "no route to
# host" even though ping and adb both work. adb's connection is unaffected,
# so we tunnel through it and talk to 127.0.0.1 instead.
hub_web() {
  adb -s "$HUB" forward "tcp:$PORT" "tcp:$PORT" >/dev/null
  WEB="http://127.0.0.1:$PORT"
}

# ----- Output -----
say()  { echo "$*"; }
warn() { echo; echo "$*"; }
die()  { echo; echo "$*"; exit 1; }
