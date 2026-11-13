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
