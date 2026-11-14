# First test sheet — "1 Competition"

Written 2026-11-13, the night before the BIOBUZZ event. Work through this in
order and write the numbers in as you go.

**Setup for everything below:**

- Robot **on blocks**, all four wheels off the ground
- Battery **above 12 V** (check the `Battery` line; it was 13.14 V tonight)
- **Nothing loaded** in the catapult until section D
- A hand near the Driver Station **stop button** at all times
- "1 Competition" selected, **INIT**, then **▶**

**If the code gets rebuilt at any point, you must stop and re-INIT + ▶.**
A rebuild does not change what is already running.

---

## What is already verified

Done on 2026-11-13, no need to repeat unless something changes:

- [x] All five motors found (no "could not find hardware device")
- [x] Forward and reverse, all four wheels correct
- [x] Strafe both directions, correct mecanum pattern
- [x] Turn both directions
- [x] Slow mode on right trigger and left bumper
- [x] Speed caps confirmed by the driver (0.75 drive / 0.60 turn)
- [x] Catapult resting current measured: **4.2 A** on the stop, **0.0 A** off it

---

## A. Catapult direction and the jog cutout

The arm sits at the home stop under spring tension, so dpad down only
presses it harder into the stop. Direction is confirmed by the fact that
down did **not** throw it.

### A1. Jog up to the fire stop

**Hold gamepad 2 dpad UP** until the arm reaches the fire-end hard stop, then
keep holding for about one more second.

Watch the `Motor A` line: `current (cutoff 6.5) peak N.N`

| Check | Expected | Result |
|---|---|---|
| Arm direction | moves toward the **throw** end | |
| Current while travelling | climbs above 0 | ______ A |
| At the hard stop | motor goes **quiet**, stops pulling | yes / no |
| `peak` after release | the highest value seen | ______ A |

**Stop immediately and report if:** the motor keeps grinding and never goes
quiet, or it cuts out before the arm moves at all.

**Write the peak down.** It sets `FIRE_STALL_AMPS`.

### A2. Jog back down

**Hold dpad DOWN** until the arm returns to the home stop, then release.

| Check | Expected | Result |
|---|---|---|
| Arm direction | moves toward **home** | |
| Reaches home and cuts out or is released | motor quiet | yes / no |
| `peak` | ______ A | |

---

## B. The emergency stop

This is the control the operator must trust. **B is the panic button.**

### B1. B during a jog

**Hold dpad UP, and while still holding it, press B.**

| Check | Expected | Result |
|---|---|---|
| Motor | stops **instantly** | yes / no |
| Stays stopped while B held | yes, even with dpad still held | yes / no |
| `Catapult` line | `IDLE` | |

This specific case was broken in the first version of the file: B set the
state to idle and the code then re-applied jog power in the same loop pass.
**If the motor does not stop here, that is a real bug and we stop testing.**

### B2. B during a shot

Covered in C2 below, once firing is verified.

---

## C. Dry fire, nothing loaded

### C1. One full cycle

Make sure the arm is at **home** and the state is `IDLE`. **Press A once.**

Watch the `Catapult` line walk through the states:

| Stage | Expected | Seen? |
|---|---|---|
| 1 | `FIRING` | |
| 2 | `SETTLE` (motor off, 200 ms) | |
| 3 | `RETURNING` | |
| 4 | `IDLE`, motor silent | |

Then read the `Last shot` line:

```
hit stop at NNN ms, peak N.N A      <- good, ended on the hard stop
timed out at NNN ms, peak N.N A     <- FIRE_MS ran out first
```

| Field | Result |
|---|---|
| `hit stop` or `timed out`? | |
| Time | ______ ms |
| Peak | ______ A |

**Then watch the return specifically.** The arm should come back to home and
**stop**. If it grinds against the home stop for a second and a half,
`RETURN_STALL_AMPS` is too high and needs lowering. It was raised to 5.5
tonight because the first value, 3.0, was below the 4.2 A resting current.

| Check | Expected | Result |
|---|---|---|
| Return reaches home | yes | |
| Grinding at home after arriving | **no** | yes / no |
| Time from fire to idle | roughly under 2 s | ______ |

### C2. B during a shot

**Press A, then press B while it is still firing or returning.**

| Check | Expected | Result |
|---|---|---|
| Motor | cuts **instantly** | yes / no |
| `Catapult` | `IDLE` | |

After this the arm may be stranded mid-travel. **Jog it back to home with
dpad down** before the next shot — with no encoder the code cannot know where
the arm is, so firing from a half-way position is on the operator.

### C3. Repeat C1 three times

Consistency matters more than any single shot.

| Shot | Result line | Peak A |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |

If the three disagree wildly, say so before loading anything.

---

## D. Loaded shots and tuning

Only once A, B and C all pass.

### D1. First loaded shot

Load the catapult. Arm at home, state `IDLE`. **Press A.**

| Field | Result |
|---|---|
| `Last shot` | |
| Peak | ______ A |
| Did it throw properly? | |

### D2. Tune

One change at a time, then fire again.

| Want | Control |
|---|---|
| More distance | dpad **right** (power +0.05) |
| Less distance | dpad **left** (power −0.05) |
| Longer push | **right bumper** (+25 ms) |
| Shorter push | **left bumper** (−25 ms) |

Aim for `Last shot` reading **`hit stop`** rather than `timed out`. Hitting
the stop means the throw ended on the mechanism's own geometry, which repeats
far better than a time guess.

Record what you settle on:

| Constant | Value found |
|---|---|
| `FIRE_POWER` | |
| `FIRE_MS` | |
| `FIRE_STALL_AMPS` (from the A1 peak) | |
| `RETURN_STALL_AMPS` | |

### D3. Write them in

**These are lost when the op mode stops.** Tell me the values and I will put
them in the constants, rebuild, reload and commit.

---

## E. Rule checks before the event

| Check | How | Result |
|---|---|---|
| Stop kills everything | press stop mid-shot; all motors silent at once | |
| No motion after stop | nothing creeps or hums | |
| Arm not left stalled | after any shot, motor is quiet at home | |

R10 / G403 / G404: no powered motion once the op mode ends.

---

## Current values as of 2026-11-13

| Constant | Value | Status |
|---|---|---|
| `DRIVE_FULL` | 0.75 | driver-confirmed, on blocks |
| `TURN_SCALE` | 0.60 | driver-confirmed, on blocks |
| `DRIVE_SLOW` | 0.35 | confirmed |
| `FIRE_POWER` | 1.00 | **untested** |
| `FIRE_MS` | 300 | **untested** |
| `SETTLE_MS` | 200 | untested, low risk |
| `RETURN_POWER` | −0.30 | **untested** |
| `RETURN_MS` | 1500 | **untested** |
| `JOG_POWER` | 0.55 | measured 11-12 |
| `REST_HOLD` | 0.0 | off |
| `FIRE_STALL_AMPS` | 7.0 | **guess — A1 sets this** |
| `RETURN_STALL_AMPS` | 5.5 | measured above the 4.2 A resting draw |
| `JOG_STALL_AMPS` | 6.5 | same |
| `IGNORE_MS` | 80 | untested |

---

## If a deploy is needed

`scripts/build-on-robot.sh` **does not work on this robot** — it uses
`adb push`, which the build service reads as empty files, so it produces a
166-byte jar and no op modes. Use the save endpoint instead; see
[TROUBLESHOOTING.md](TROUBLESHOOTING.md). A real jar is about 24 kB.
