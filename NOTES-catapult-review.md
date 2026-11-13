# Catapult review and drive plan — Nov 12, 2026

Written before any change to `teamcode/`. Nothing on the robot has been touched.

## Which build is the better base: 907 (6:05 PM)

`history/onbot-builds/OnBot-build-00000907-2026-11-12+18.05.38.zip` is the
better base. The current file (build 909, identical to `teamcode/`) is a
rewrite that lost working behaviour.

What 907 has that the current file does not:

| | 907 (6:05 PM) | current (909) |
|---|---|---|
| Release point | `releasePos`, a live variable; **X saves** where the arm is | `RELEASE_POSITION = 300`, hardcoded |
| Stall detection | `STALL_CURRENT_A = 8.0`, below the ~9.2 A stall — **works** | `99.0` — never trips, so every stall check is dead |
| Stall reset | `resetStall(pos)` helper, called on every state entry | `stallTimer.reset()` only; `lastStallPos` goes stale |
| Overshoot | `OVERSHOOT_COMP` cuts power early | none |
| Fire power | `0.5` with a comment to raise it once proven | `1.0` straight away |
| Jog power | `0.30`, slow enough to place the release point | `1.00` |
| Travel limit | `releasePos + MAX_MARGIN`, follows calibration | `RELEASE_POSITION + 100`, fixed |
| Angle maths | correct: `pos / TICKS_PER_REV * 360` | wrong, see below |
| Doc comment | full first-run calibration procedure | removed |

What the current file has that 907 does not, and is worth keeping:

- `homeCatapult()` and the `USE_LIMIT_SWITCH` / `TouchSensor` path.
- `finishReturn()` with `HOME_HOLD_POWER`, for holding home against a
  rubber band.
- `isAtTarget()` comparing against the motor's own target position.

**Recommendation:** start from 907 and port those three things onto it.
That is a smaller, safer change than fixing 909, because the three defects
you listed are all *introduced by* 909 and all already correct in 907.

## The three issues you listed

### 1. `RELEASE_POSITION = 300` makes `RELEASE_ANGLE_DEG` dead

Confirmed. `RELEASE_ANGLE_DEG = 45.0` is read only by telemetry
(`"Release at (deg)"`), so the Driver Station reports a release angle that
has nothing to do with where the motor actually cuts power. 907 solved this
properly by not having an angle constant at all — you jog the arm to the
real release point and press X. Ticks are the honest unit here, because the
gear ratio is unknown.

**Recommendation:** keep the calibration flow. Delete `RELEASE_ANGLE_DEG`
rather than wiring it up.

### 2. `MOTOR_TICKS_PER_REV = 28` vs the "19.2:1 Yellow Jacket" comment

The comment is wrong, not the number. 28 ticks/rev is the bare encoder
count; a 19.2:1 Yellow Jacket reports 537.7. 907's comment says
"bare 6000 rpm motor", which matches 28 and matches `STALL_CURRENT_A`'s
"~9.2 A" note (the 6000 rpm bare motor's stall current).

There is also a real maths bug in the current file, separate from the
comment. Line 54:

    TICKS_PER_ARM_DEG = MOTOR_TICKS_PER_REV * EXTERNAL_GEAR_RATIO / 360.0

is ticks per degree = 28/360 = 0.0778. Then line 190 does
`pos / TICKS_PER_ARM_DEG`, which divides *by* ticks-per-degree instead of
multiplying — so the displayed angle is off by a factor of 360²/28², about
165×. 907's version is correct:

    pos / (MOTOR_TICKS_PER_REV * EXTERNAL_GEAR_RATIO) * 360.0

`MIN_POSITION` is computed from the same bad constant: `-10 * 0.0778` rounds
to **0 ticks**, so jogging down below home is blocked entirely. 907 just
sets `MIN_POSITION = -30` directly.

**Recommendation:** fix the comment to "bare 6000 rpm motor", and take 907's
angle formula. Treat the angle as telemetry only until the real gear ratio
is measured.

### 3. `STALL_CURRENT_A = 99.0` disables stall detection

Confirmed, and it silently disables all four `isStalled()` checks (firing,
returning, jogging, plus the fault path). No goBILDA motor on a 12 V battery
draws 99 A; the hub's breaker trips well before that. 907 had `8.0`, just
under the ~9.2 A stall, which is the right shape.

`isStalled()` in the current file also has the stale-`lastStallPos` problem:
`startFire()` / `startReturn()` reset `stallTimer` but not `lastStallPos`, so
the first comparison after a state change is against a position from the
previous move. 907's `resetStall(pos)` sets both.

**Recommendation:** `STALL_CURRENT_A = 8.0`, and restore `resetStall()`.

## Plan for step 2: wiring MecanumDrive into CatapultOpMode

Not yet applied — for your review first.

1. **Base.** Restore 907's `CatapultOpMode` as the starting point, port in
   `homeCatapult()`, `finishReturn()` / `HOME_HOLD_POWER`, and `isAtTarget()`.
   Keep `@TeleOp(name = "TeleOp2026", group = "Test")` so the Driver Station
   entry does not change.

2. **Drive, gamepad 1.** In `runOpMode()`, before `waitForStart()`:

       drive = new MecanumDrive(hardwareMap, FIELD_CENTRIC);

   with `static final boolean FIELD_CENTRIC = false` — robot-centric first,
   since the IMU mounting orientation in `MecanumDrive` is also untested.
   Then each loop pass:

       drive.drive(-gamepad1.left_stick_y,   // + = forward (stick y is inverted)
                    gamepad1.left_stick_x,   // + = right
                    gamepad1.right_stick_x,  // + = clockwise
                    gamepad1.right_bumper ? SLOW_SCALE : FULL_SCALE);

   and `drive.stop()` after the loop, next to `catapult.setPower(0)`.

3. **Catapult stays on gamepad 2.** `USE_GAMEPAD_2` and `pad` are untouched,
   so A / B / X / D-pad keep working as they do now. No button is shared
   between the two gamepads, so there is no conflict to resolve.

4. **Making directions easy to flip** — this is the part worth getting right,
   since nothing is tested:
   - The four `*_DIR` constants are already at the top of `MecanumDrive`.
     I'll group them under a comment giving the flip procedure: drive
     forward, note which wheels spin backwards, flip only those.
   - Add `static final boolean INVERT_DRIVE = false` and
     `static final boolean INVERT_STRAFE = false` to `CatapultOpMode`,
     applied as multipliers at the call site. That lets you fix
     "forward goes backward" or "strafe is mirrored" from one place without
     reasoning about which of four motors to flip.
   - Add a `drive.resetHeading()` binding (gamepad 1 `options`), harmless
     now and needed the moment field-centric is switched on.

5. **Telemetry.** Add one line with the three stick values and the heading,
   so wrong directions are visible on the Driver Station without a test
   drive. Keep it below the catapult lines.

6. **Not in this step.** `MecanumDrive` is otherwise left alone — its mixing
   maths and `STRAFE_CORRECTION` are standard and fine. The `IMU` /
   `RevHubOrientationOnRobot` API it uses is the current one, not the
   deprecated `BNO055IMU`.

### Before deploying, also note

- The hub's own copy of `MecanumDrive.java` has a stray
  `@TeleOp(name = "TeleOp2026", group = "Test")` on a class that is not an
  OpMode, duplicating the name on `CatapultOpMode`. Checked by compiling
  both against SDK stubs: this **compiles fine**, so the hub build succeeds.
  The problem is at registration — the Driver Station gets two entries
  named "TeleOp2026", one of them a class that is not an OpMode. Your local
  edit already removed it. Still a good reason to deploy from this folder
  rather than edit on the hub.
- `scripts/deploy-to-robot.sh` does `rm -f $HUB_TEAMCODE/*.java`, which
  removes the 39 archived files from the robot. That is the intent, but it
  is worth saying out loud: after a deploy the robot will hold exactly two
  programs, and the old seasons' code will exist only in this repo.


## Verified by compiling

There is no FTC SDK on this laptop, so I wrote minimal stub classes for the
SDK types these files use and compiled against them. That checks syntax,
types and control flow — not SDK behaviour.

- `teamcode/RobotBringUp.java` — compiles clean with `-Xlint:all`, no warnings
- `teamcode/MecanumDrive.java` — compiles clean
- `teamcode/CatapultOpMode.java` (909) — compiles
- build 907's `CatapultOpMode.java` — compiles

So both candidate bases are buildable, and the choice between them is about
behaviour, not compilation.


## Measured on the robot, 2026-11-12

First bring-up run. `logs/bringup-2026-11-12-215603.csv`, 1718 samples.

### Wheel directions (test 1) — SETTLED

Each motor was run at FORWARD with no direction constant applied, and the
operator noted which wheel moved and which way it would drive the robot:

| Button | Wheel | Spun | So the constant must be |
|---|---|---|---|
| X | front left | forward | `FORWARD` |
| Y | front right | **backward** | `REVERSE` |
| A | back left | forward | `FORWARD` |
| B | back right | forward | `FORWARD` |

Every button moved the corner it was supposed to, so no ports are swapped in
`2026-2027.xml`.

Three of the four constants in `MecanumDrive` were wrong. The old values were
FL/BL `REVERSE` and FR/BR `FORWARD` — the usual mirrored-pair assumption. This
robot is not mirrored: only the front right is physically reversed. Driving
with the old values would have made the robot turn instead of going straight.

Now applied and built onto the hub (build at 22:07, clean log).

### Drive feel (test 2) — CONFIRMED 2026-11-12 22:22

`logs/bringup-2026-11-12-222217.csv`. 51 s in test 2, after the corrected
direction constants were built onto the hub.

The operator reported the wheels looked correct, and the log agrees. All
four mecanum patterns appear with the right signs:

| Motion | Wheel powers | Samples |
|---|---|---|
| forward | all four positive | 70 |
| backward | all four negative | 59 |
| strafe right | fl+ fr- bl- br+ | 59 |
| strafe left | fl- fr+ bl+ br- | 47 |

So the direction fix is verified in both the data and on the floor.

Not covered by this run: rotation (no turn samples, the right stick was
not used) and full speed (peak power was 0.21, the right trigger was not
held).

### Catapult (test 3) — BLOCKED, encoder reads nothing

`logs/bringup-2026-11-12-222217.csv`, 132 s in test 3. The operator
reported the arm "didn't seem to have enough power to move".

The log says something more specific:

- The motor drew **1-2 A for 12.25 s** across 245 samples, so it was
  energised and pushing.
- The encoder reported **0 ticks the entire time**. Not a small number:
  exactly zero, start to finish.
- Peak current 2.11 A, well under this motor's ~9 A stall, so it was not
  straining against a hard jam.

Current without motion means the motor is being driven but the Control
Hub cannot see the result. Most likely causes, in order:

1. **Encoder cable not connected** at the motor or at Expansion Hub
   port 0. The 4-pin JST is separate from the motor power leads and is
   easy to leave off.
2. **Mechanical bind**, or 25% jog power genuinely too low for this
   mechanism's gearing and spring.
3. Motor wired to a different port than the configuration says, though
   current is being drawn on the `catapult` device, which argues against
   this.

The Expansion Hub itself is fine: module 2 is detected and talking in the
Robot Controller log at 22:19:41.

**Nothing in the code can be tuned around this.** `DEFAULT_RELEASE_TICKS`,
`STALL_CURRENT_A` and `OVERSHOOT_COMP` all depend on encoder feedback, so
they stay unmeasured until the arm moves and the ticks change.

The distinguishing check for next session: press D-pad UP and watch the
arm. If it moves at all while the screen stays at 0 ticks, it is the
encoder cable. If it does not move, it is mechanical or power.

### Catapult encoder — CONFIRMED DEAD 2026-11-12 22:30

`logs/bringup-2026-11-12-223000.csv`, jog power raised to 0.55.

The arm **moved** this time, which the operator confirmed by eye, so the
earlier whine-and-twitch was simply too little power for a loaded arm.

The encoder is a separate fault and it is now proven:

- 668 samples in the file, **every one reads exactly 0 ticks**.
- Peak current 3.99 A, so the motor was working hard and really turning.
- `JOG_END` rows report "moved 0 ticks" for jogs where the arm visibly
  moved.

A value that never changes while the shaft turns is not drift or noise.
The encoder channel is not reporting at all.

**What to check on the robot**, in order:

1. The 4-pin JST encoder cable at the motor end. Separate from the two
   thick power leads and easy to leave unplugged.
2. The same cable at **Expansion Hub 2, motor port 0** -- the encoder
   socket beside the motor power socket, not the power socket itself.
3. The cable itself, if both ends are seated: swap it for a known-good
   one, or move the motor to another port and update the configuration.

Until ticks change, these stay unmeasurable:
`DEFAULT_RELEASE_TICKS`, `STALL_CURRENT_A`, `OVERSHOOT_COMP`.

The catapult cannot be run in any position-controlled mode without this.
`RUN_TO_POSITION` for the return home, the release point and the travel
limit all read the encoder, so the current `CatapultOpMode` would believe
the arm is permanently at 0 and never reach its release point.

### IMU orientation (test 4) — CONFIRMED 2026-11-12 23:18

`logs/bringup-2026-11-12-231838.csv`, 3645 samples in test 4.

The Control Hub is mounted vertically on edge, ports facing up, so the
old `LOGO_DIR = UP` / `USB_DIR = FORWARD` could not have been right.

Measured: zero the heading, turn left, read +58 degrees at a quarter
turn. The log shows three turns across the session:

| Turn | Heading | Direction |
|---|---|---|
| t=26-30 | 0 to -117 | right |
| t=60-65 | 0 to +150 | left |
| t=71-77 | 0 to +101 | left |

Left turns read positive, which is the SDK's convention, so
`LOGO_DIR = LEFT` and `USB_DIR = UP` are correct.

The IMU is also healthy in its own right: it tracks smoothly across the
full -180..+180 range, responds proportionally, and holds a steady value
when the robot is still, with no drift visible over the run.

Field-centric driving is therefore safe to enable whenever it is wanted.
It stays off by default until a driver asks for it.

### Still unmeasured

The run stayed in test 1, so these are still unknown and still guesses:

- `DEFAULT_RELEASE_TICKS` — needs test 3 (jog, press X)
- `STALL_CURRENT_A` — needs test 3's peak current over three fires
- `OVERSHOOT_COMP` — needs the operator to watch whether the arm coasts past
- Stick-to-motion check — needs test 2
- `LOGO_DIR` / `USB_DIR` and field-centric — needs test 4

Catapult ticks never left 0 and current never exceeded 0.01 A in this run.

## Build endpoint, for the record

OnBot Java's build is driven over HTTP:

- `GET /java/build/start` starts it (200). **Not** `start_build.json`, which
  404s; an earlier note here claimed that worked, and it did not — that first
  build was started by hand on the web page.
- `GET /java/build/wait` blocks until the build finishes.
- `/sdcard/FIRST/java/status/buildLog.txt` is empty on success and holds the
  compiler errors on failure.

`scripts/build-on-robot.sh` wraps this. It tunnels through adb because a
laptop tethered to a phone while on the robot's Wi-Fi has two default routes,
and direct HTTP to 192.168.43.1 fails with "no route to host" even though adb
and ping work.
