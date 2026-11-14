# Troubleshooting the robot

Things that have actually gone wrong on this robot, what they looked like,
and what fixed them. Written 2026-11-12 after a long bring-up session.

Most of these share one trait: **the robot reported success while doing
nothing**. That is why they cost so much time.

---

## The Driver Station shows only "BlockModeArmTest"

**Looks like:** the Java op modes (`TeleOp2026`, `0 BringUp`) are missing
from the list. Only the Blocks program and the built-in utilities appear,
as if OnBot Java were not installed.

**Actually:** the build produced an **empty jar**. OnBot reported "build
successful" with an empty build log and wrote a 166-byte
`OnBotJava.jar` containing no classes. The Robot Controller loaded it
without complaint, so nothing registered and nothing errored.

**Check it:**

```
adb -s 192.168.43.1:5555 shell 'ls -la $(cat /sdcard/FIRST/java/status/currentOnBotJavaDir.txt)/'
```

A real jar with a few op modes is **tens of kilobytes** and sits beside a
`classes.dex`. A jar of a few hundred bytes is empty.

**Fix:** build again, then restart the Robot Controller. `scripts/build-on-robot.sh`
now checks the jar size and refuses to call an empty build a success.

**Root cause:** a build started while a source file was still being
written. See the next entry.

---

## "file does not contain class <Name>"

**Looks like:**

```
ERROR: cannot access org.firstinspires.ftc.teamcode.MecanumDrive
  bad source file: .../MecanumDrive.java
    file does not contain class org.firstinspires.ftc.teamcode.MecanumDrive
```

**Actually:** the build read the file mid-push. `adb push` is not atomic
from the build's point of view, so a build kicked off in the same breath
can see a half-written file.

**Fix:** `scripts/build-on-robot.sh` does the push itself and waits for
the hub's file listing to stop changing before building. Do not chain
`adb push && scripts/build-on-robot.sh` -- that runs the wait before the
push instead of after.

---

## Code changed but the robot behaves the same

**Looks like:** a constant was edited, the build succeeded, and the robot
still does the old thing.

**Actually:** OnBot compiles to a new jar, but the Robot Controller has
already loaded the old classes into a running JVM. A rebuilt class does
not replace one already in memory.

**Fix:** `scripts/restart-robot-app.sh`, or "Restart Robot" on the Driver
Station menu. A full power cycle is only needed when a class changed what
it inherits from.

---

## "'MecanumDrive' class doesn't inherit from the class 'OpMode'"

**Looks like:** a red error on the Driver Station naming a helper class.

**Actually:** the hub's copy of `MecanumDrive.java` had a stray
`@TeleOp(name = "TeleOp2026")` annotation on a class that is not an
OpMode, duplicating the name on `CatapultOpMode`. It compiles fine; the
clash is at registration.

**Fix:** the annotation is gone from this repo's copy. Deploy from here
rather than editing on the hub. If the error persists after a build, the
old class is still loaded -- see above.

---

## "Could not find hardware device: bl"

**Looks like:** a hardware-name error for a device this season's code
never asks for.

**Actually:** a stale class from the archived 2025-26 code, which used
short names like `bl`. This season's `MecanumDrive` asks for
`"back left"`, with a space.

**Fix:** restart the Robot Controller, or power cycle if that does not
clear it.

---

## adb will not connect after a reboot

**Looks like:** `failed to connect to '192.168.43.1:5555': Operation timed out`,
and `ping 192.168.43.1` gets no reply at all.

**Actually:** the laptop left the robot's Wi-Fi. When the hub reboots its
network disappears, and a laptop that is also tethered to a phone falls
back to the tether.

**Fix:** rejoin `7593-A-RC`, then `scripts/connect.sh`.

---

## HTTP to the hub fails while adb works

**Looks like:** `curl http://192.168.43.1:8080` returns
"No route to host" instantly, but `adb` and `ping` both work.

**Actually:** tethering to a phone while on the robot's Wi-Fi gives the
laptop two default routes. New TCP connections pick the tether interface
as their source and the robot cannot reply to it.

**Fix:** tunnel through adb instead of going direct:

```
adb -s 192.168.43.1:5555 forward tcp:8080 tcp:8080
```

then use `http://127.0.0.1:8080`. `scripts/build-on-robot.sh` does this
for you.

---

## The catapult motor whines and twitches but does not move

**Actually:** not enough power for a loaded arm. At 0.25 the motor drew
1-2 A and only twitched. At 0.55 it moves.

A whine with a twitch means the motor is pushing against a real load. A
motor with a disconnected encoder but no load spins smoothly and quietly
-- different sound, different problem.

---

## The catapult moves but the position stays at 0 ticks

**Actually:** the encoder cable is missing. Confirmed 2026-11-12: 668 log
samples all read exactly 0 while the motor drew up to 4 A and the arm
visibly moved.

**Fix:** a 4-pin JST PH (hub end) to JST XH (motor end) encoder cable.
goBILDA 5202 motors use XH; the REV hub uses PH.

Until then the catapult cannot use `RUN_TO_POSITION`, a release point, or
a travel limit -- they all read the encoder.

---

## build-on-robot.sh keeps saying "BUILD PRODUCED AN EMPTY JAR"

Seen 2026-11-13, three times in a row, so the script's own advice ("just run
it again") did not help.

**Symptoms.** The build reports success in about **one second**, writes a
**zero-byte** `buildLog.txt`, and leaves `build/classes/` empty. The jar is
166 bytes. No op modes register. A real build of four files takes **ten to
fifteen seconds** and makes a jar of tens of kilobytes, so the timing alone
tells the two apart.

**Not the cause** (all checked and ruled out that night):

- the source files -- `md5sum` on the hub matched the laptop exactly
- the SDK jars in `/sdcard/FIRST/java/lib/` -- intact, `unzip -t` clean,
  md5 equal to `tools/lib/`
- a stale Robot Controller -- force-stopping and relaunching changed nothing
- disk space -- 12 GB free

The giveaway is in the build's own source backup. Pull the newest zip from
`/sdcard/FIRST/java/srcBackups/` and list it:

```
unzip -l OnBot-build-<newest>.zip
```

A good build's backup shows real file sizes. The failing ones showed **every
`.java` file at 0 bytes**, while `ls` on the same files reported correct
sizes. The build service was reading the freshly pushed files as empty, so
javac compiled nothing and had nothing to report.

**The actual cause, found 2026-11-13:** `adb push`. Files pushed that way
land on disk correctly -- `ls`, `md5sum` and even the hub's own HTTP read
endpoint all return the right bytes -- but the build service's file reader
sees them as empty. Rebuilding, restarting the app and re-pushing all fail
the same way, because the push is the problem.

**Fix: write the files through the hub's own save endpoint instead of
pushing them.** For each file:

```
adb -s 192.168.43.1:5555 forward tcp:8080 tcp:8080
```

```
curl -s -X POST "http://127.0.0.1:8080/java/file/save" --data-urlencode "f=/src/org/firstinspires/ftc/teamcode/CompetitionTeleOp.java" --data-urlencode "data@teamcode/CompetitionTeleOp.java"
```

Then build. The source backup in `srcBackups/` will show real file sizes and
the jar will be tens of kilobytes. Pasting the file into the OnBot Java web
page editor and pressing Build Everything does the same thing by hand.

**Earlier partial fix** (works sometimes, by timing luck only -- do not rely
on it): trigger the build through the web API after pushing:

```
adb -s 192.168.43.1:5555 forward tcp:8080 tcp:8080
```

```
curl -s "http://127.0.0.1:8080/java/build/start"
```

Then wait for `"completed":true` and check the size, which should be tens of
thousands of bytes, not 166:

```
curl -s "http://127.0.0.1:8080/java/build/status"
```

That produced a 24,695-byte jar and all three op modes registered. Pressing
**Build Everything** on the OnBot Java web page at `http://192.168.43.1:8080`
does the same thing from a browser.

**If it happens at an event**, go straight to the web page. Do not spend
time re-running the script.

---

## General advice

**Do not trust "build successful" on its own.** Check that op modes
actually registered:

```
adb -s 192.168.43.1:5555 logcat -d | grep "OpmodeRegistration: registered"
```

`0 BringUp`, `TeleOp2026` and `1 Competition` should all appear.
`scripts/restart-robot-app.sh` prints this list for you.

A jar size under about 2000 bytes means nothing was compiled, whatever the
build said. Check it before believing a deploy.
