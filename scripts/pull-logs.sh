#!/bin/bash
# Copy the diagnostic CSV logs off the robot into logs/.
# Read-only: nothing on the robot is changed or deleted.
source "$(dirname "$0")/common.sh"
require_hub

mkdir -p logs

FILES=$(hub "ls $MATCHLOGS/*.csv 2>/dev/null")
if [ -z "$FILES" ]; then
  die "No .csv logs on the robot.

Did the '0 BringUp' op mode run? It writes a log every time it does."
fi

say "Copying logs into logs/ ..."
echo "$FILES" | while read -r f; do
  [ -z "$f" ] && continue
  hub_pull "$f" "logs/$(basename "$f")"
  say "  logs/$(basename "$f")"
done

say ""
say "Newest: $(ls -t logs/*.csv 2>/dev/null | head -1)"
say "Nothing was deleted from the robot."
