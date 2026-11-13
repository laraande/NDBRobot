# Bring-up test card

Print this or keep it on a phone. One person drives, one person writes.

Program on the Driver Station: **`0 BringUp`** (group: Diagnostic).
It sorts to the top of the list, above `TeleOp2026`.

Everything it records goes to a CSV on the hub. You do not have to write down
numbers you can read off the file later — but **do** write down anything the
robot *did*, because the file cannot see that.

- Bumpers (gamepad 1) change test.
- **BACK marks the log.** Press it the moment something looks wrong. It puts a
  findable row in the CSV so we can line up "the wheel juddered" with the data.

---

## Before you start

| | |
|---|---|
| Battery | Charged, and **keep it above 12 V**. Low battery looks exactly like a weak motor. |
| Robot | **On blocks, wheels off the ground**, for tests 1 and 2 first. |
| Catapult | Arm **down at home**, nothing loaded, no ball/piece in it. |
| Space | Clear in front of the catapult. First fire is at 50% power and may still throw. |
| Config | `2026-2027` active on the Driver Station. |

If the program stops immediately with "HARDWARE NAME NOT FOUND", the active
configuration is wrong. Read which name it could not find and check it against
Configure Robot.

---

## Test 1 — WHEELS (robot on blocks)

Finds out which wheel is which and which way each one spins. No direction
constants are applied here, so you are seeing the raw motor.

Press one button at a time. Each runs **one** wheel at 30%.

| Button | Should be | Write down |
|---|---|---|
| X | front left | which wheel actually moved, and FORWARD or BACKWARD |
| Y | front right | |
| A | back left | |
| B | back right | |

Fill this in — it is the single most useful thing from the whole session:

```
X ->  wheel: ____________   spins: FWD / BACK
Y ->  wheel: ____________   spins: FWD / BACK
A ->  wheel: ____________   spins: FWD / BACK
B ->  wheel: ____________   spins: FWD / BACK
```

"FORWARD" means: if all four did this, the robot would drive forward.

**Stop and say so if:** a button moves the wrong corner (config ports are
swapped), nothing moves (port or power), or two wheels move at once (two
devices share a name).

---

## Test 2 — DRIVE (still on blocks, then on the floor)

Uses the real drive code and the real direction constants. Right bumper to
get here.

Left stick = translate. Right stick X = turn. Right trigger = full speed.

On blocks first, then put it on the floor and do it again:

| Push | Robot should | Did it? |
|---|---|---|
| left stick **forward** | drive forward | |
| left stick **back** | drive backward | |
| left stick **right** | strafe right, no rotation | |
| left stick **left** | strafe left, no rotation | |
| right stick **right** | spin clockwise seen from above | |

**Write down the failure exactly.** These all mean different fixes:

- Forward drives backward -> one flag to flip
- Forward strafes sideways, or robot spins -> two wheels' directions swapped
- Strafe is mirrored but forward is fine -> strafe flag
- Drives fine but one wheel drags -> mechanical, or that wheel's direction
- Robot "crabs" slightly -> roller direction on one wheel is physically wrong

---

## Test 3 — CATAPULT (gamepad 2)

**This test produces the number the real op mode needs.** Clear the area.

1. Confirm the arm is at home. The screen shows `Position (ticks)` near 0.
2. **DPAD UP** on gamepad 2 jogs the arm up slowly. Tap it, don't hold it.
3. Jog until the arm is **where you want the throw to let go**.
4. Press **X**. The screen now shows `Release point` with a number.
   **Write that number down:** `release ticks = __________`
5. Press **B** to bring the arm home.
6. Press **A** to fire at 50%.
7. Read `Peak this session` and write both numbers:
   `peak ticks = ________   peak amps = ________ A`

Do steps 5-7 three times and write the peak amps each time. We need to know
the spread, not one number.

```
fire 1: peak ______ A    fire 2: peak ______ A    fire 3: peak ______ A
```

**Write down what the arm did**, which the log cannot tell us:
- Did it stop at the release point, or coast past it? How far past?
- Did it sound like it hit something? Did the robot lift or shift?
- Did the arm come back to the same place every time?

**Stop if:** the arm goes *down* when you press DPAD UP (motor direction),
ticks go down while the arm goes up (encoder direction), ticks stay at 0 while
it moves (encoder not plugged in), or it makes a grinding noise.

---

## Test 4 — IMU (on the floor)

Only needed before field-centric driving is turned on.

Press **OPTIONS** to zero the heading, then turn the robot in place with right
stick X.

| Turn | Heading should | Did it? |
|---|---|---|
| left / counter-clockwise | count **up** (0 -> 90) | |
| right / clockwise | count **down** (0 -> -90) | |
| full circle back to start | return near 0 | |

If left counts *down*, the hub's mounting constants in `MecanumDrive` are
wrong. Write down **how the Control Hub is physically mounted**: which way the
REV logo faces (up / down / forward / left...) and which way the USB port
faces. That is exactly what the fix needs.

---

## When you are done

1. Press **STOP** on the Driver Station. The log is flushed every row, so a
   battery pull loses almost nothing — but stop properly if you can.
2. Note the log filename from the screen: `bringup-__________________.csv`
3. Laptop on the robot's Wi-Fi, then:

   ```
   scripts/connect.sh
   scripts/pull-logs.sh
   adb disconnect
   ```

4. Switch Wi-Fi back and tell me the filename. I will read the CSV and the
   notes above together, and we fix what the two of them disagree about.

---

## What we are going to do with this

| From | We set |
|---|---|
| Test 1 table | `FL_DIR` / `FR_DIR` / `BL_DIR` / `BR_DIR` in `MecanumDrive` |
| Test 2 failures | `INVERT_DRIVE` / `INVERT_STRAFE`, or a direction swap |
| Test 3 release ticks | `DEFAULT_RELEASE_TICKS` in `CatapultOpMode` |
| Test 3 peak amps | `STALL_CURRENT_A`, set just under the real stall |
| Test 3 overshoot | `OVERSHOOT_COMP`, to cut power early |
| Test 4 | `LOGO_DIR` / `USB_DIR`, and whether field-centric can be used |
