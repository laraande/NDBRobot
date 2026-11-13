# Logging: finding out what the robot actually did

The robot moves faster than you can watch, and people remember what they
expected rather than what happened. Logs settle arguments.

---

## Three places information comes from

| Source | Shows | Use when |
|---|---|---|
| **Driver Hub screen** | Live telemetry | Watching a test as it runs |
| **CSV logs** | Every value, 20 times a second | Working out what actually happened |
| **Robot Controller log** | Crashes, what loaded | Something broke or did not start |

---

## 1. Telemetry on the screen

Any op mode can print to the Driver Hub:

```java
telemetry.addData("Position (ticks)", catapult.getCurrentPosition());
telemetry.addLine("A = fire | B = home");
telemetry.update();
```

`addData` for a label and value, `addLine` for plain text, and
**`update()` to actually send it** -- forgetting that is a common reason
"telemetry does not work".

Good for live checks. Useless afterwards: nothing is saved.

---

## 2. CSV logs

`0 BringUp` writes a file to the robot every time it runs:
`/sdcard/FIRST/matchlogs/bringup-<date>.csv`, one row every 50
milliseconds.

Columns: time, which test, event, note, the four wheel powers, catapult
ticks and amps, the saved release point, and the heading.

### Getting them off the robot

```
scripts/connect.sh
scripts/pull-logs.sh
```

They land in `logs/`. Nothing is deleted from the robot.

### Reading them

Open in Excel, Numbers or Google Sheets. Or from the command line:

```
head -1 logs/bringup-2026-11-12-231838.csv        # the column names
grep -v ',SAMPLE,' logs/bringup-*.csv             # just the events
```

Two kinds of row:

- **`SAMPLE`** -- a regular snapshot, 20 per second
- **everything else** -- something happened: a button, a state change, a
  fault

### Why it is worth the trouble

Real examples from this robot:

- "The catapult does not have enough power" -- the log showed the motor
  drawing 1-2 A for 12 seconds while the encoder never moved a tick.
  Two separate problems, and the log separated them.
- "Nothing happened" -- 153 seconds of log, all in the wrong test. The
  button to change test had never registered.
- "The wheels look right" -- confirmed, all four mecanum patterns with
  correct signs.

In each case the log said something more specific than the person did.
Not because anyone was careless -- the robot is just faster than
watching.

---

## 3. The Robot Controller log

Android's system log. Useful when something crashed or did not load.

```
adb -s 192.168.43.1:5555 logcat -d | tail -50
```

Worth filtering:

```
# what programs loaded?
adb -s 192.168.43.1:5555 logcat -d | grep "OpmodeRegistration: registered"

# anything crash?
adb -s 192.168.43.1:5555 logcat -d | grep -iE "exception|error|fatal"
```

**That first one is the single most useful command on this project.** If
your op mode is not in that list, it is not on the robot, whatever the
build said.

`scripts/restart-robot-app.sh` runs it for you after a restart.

---

## Adding logging to your own code

Mark the moment something interesting happens, rather than trying to
reconstruct it afterwards:

```java
writeRow("FIRE_START", "target " + releaseTicks + " ticks");
```

And log **both** what you commanded and what resulted:

```java
telemetry.addData("Commanded", power);
telemetry.addData("Actual position", motor.getCurrentPosition());
```

The gap between those two is where the bugs live. Current flowing with no
movement is how the encoder fault was found.

---

## Practical advice

**Press BACK during a test** when something looks wrong. It writes a
marked row so the moment is findable later.

**Pull the logs before changing anything.** They are easy to lose and
impossible to recreate.

**Commit interesting logs.** `logs/` is in the repo on purpose -- a log
showing a real failure is worth keeping alongside the fix.

**Write down what you saw, not just what was logged.** "The arm coasted
past and the robot hopped" is not in any CSV, and is often the thing that
explains the numbers.
