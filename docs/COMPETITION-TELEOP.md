# Competition TeleOp

`teamcode/CompetitionTeleOp.java` — appears on the Driver Station as
**"1 Competition"**, group **Competition**.

This is the op mode to run at the event. It drives the robot and fires the
catapult, and it needs **no encoders at all**.

Written for the 2026-11-14 event (BIOBUZZ) while the catapult encoder cable
is still missing. Do not run `TeleOp2026` (`CatapultOpMode`) — it is
encoder-based and will sit firing until its timeout on this robot.

---

## Controls

### Gamepad 1 — driver

| Control | Does |
|---|---|
| **Left stick** | drive forward/back, strafe left/right |
| **Right stick X** | turn |
| **Right trigger** (hold) | slow mode, 35% speed, for lining up |
| **Left bumper** (hold) | slow mode as well -- same thing, easier to find |
| **Back** | toggle field-centric / robot-centric |
| **Start** | re-zero the heading |

### Speed limits

Full stick is **not** full motor power, deliberately.

| Setting | Value | What it limits |
|---|---|---|
| `DRIVE_FULL` | 0.75 | normal driving and strafing |
| `TURN_SCALE` | 0.60 | turning, on top of the above |
| `DRIVE_SLOW` | 0.35 | everything, while the right trigger is held |

Tested on blocks on 2026-11-13: at 1.00 the wheels simply spin and the robot
is not drivable. Turning is capped harder than driving because rotating a
mecanum base scrubs all four wheels sideways, so it loses grip first.

These values were tried on the robot and confirmed by the driver the same
evening ("a lot less twitchy"), **on blocks**. They have not yet been driven
on the floor, where the motors are loaded by the robot's weight and will feel
slower and more planted. Check them in the first practice session.

The stick is also **squared**, so half stick gives about a quarter of that
power. Fine control near centre, full available at the end of the travel.

These are the numbers most worth adjusting after the drivers have had a
practice session. Raise `DRIVE_FULL` if they want more speed; lower it if
the base still breaks traction. Change them in practice, not at the event.

Driving is **robot-centric by default** — stick forward moves the end of the
robot away from the catapult forward, whichever way the robot is facing. This
is what the drivers practised on and it is what the match should be driven in.

Field-centric is there because the IMU orientation was measured and verified
on 2026-11-12, so it is safe to use. If you turn it on, "forward" means
forward **from where the robot was pointing when the heading was last
zeroed** — either at op mode start or the last time START was pressed. If
field-centric ever feels rotated, point the robot downfield and press START.

### Gamepad 2 — operator

| Control | Does |
|---|---|
| **A** | fire, then settle, then auto-return home |
| **B** | **STOP the catapult now** — overrides everything |
| **Dpad up / down** | jog the arm by hand |
| **Dpad right / left** | fire power + / − 0.05 |
| **Right / left bumper** | fire time + / − 25 ms |

**B is the panic button.** It cuts catapult power immediately from any state,
and it wins over a held dpad and over a shot in progress. Teach the operator
this before anything else.

Firing is a **state machine**, not a blocking sequence: the driver keeps full
control of the robot the entire time a shot is happening.

A shot goes `IDLE → FIRING → SETTLE → RETURNING → IDLE`. Pressing A again
does nothing until the arm is back to `IDLE`. The current state is on screen.

---

## No fire-end hard stop

Confirmed 2026-11-13: this robot has a hard stop at **home** but **none at
the fire end**. That changes what the firing logic can rely on.

- `FIRE_STALL_AMPS` will effectively never trip on a throw, because there is
  nothing to stall against. **`FIRE_MS` is the control that ends every
  shot**, not a backstop. Expect `Last shot` to always read `timed out`.
- Distance is therefore tuned almost entirely with `FIRE_MS` and
  `FIRE_POWER`. Small time changes matter more than they would otherwise.
- Manual jogs are limited to `JOG_MAX_MS` (600 ms) per press, since a held
  dpad has nothing to stop it at that end. Release and press again to keep
  moving.
- The **return** is unaffected: the home stop exists, so
  `RETURN_STALL_AMPS` has something real to detect.

Once a fire-end stop is fitted, measure the peak amps on a jog into it, set
`FIRE_STALL_AMPS` between normal running and that peak, and shots will start
ending on geometry instead of on the clock -- which repeats far better.

---

## How the catapult works without an encoder

The motor position reads 0 forever, so nothing can be measured. Instead:

- **Fire** runs at `FIRE_POWER` for at most `FIRE_MS`, and cuts early if the
  motor current goes above `FIRE_STALL_AMPS` — which means the arm reached
  its hard stop at the fire end.
- **Settle** waits `SETTLE_MS` with the motor off.
- **Return** runs gently backward at `RETURN_POWER` until the current goes
  above `RETURN_STALL_AMPS` (home stop reached) or `RETURN_MS` runs out.

Current readings for the first `IGNORE_MS` (80 ms) of every move are ignored,
because a motor starting up draws a big inrush spike that looks exactly like
a stall.

**Power is voltage-compensated.** Commanded power is multiplied by
`12.5 / battery volts`, so a shot on a tired 11.5 V battery still throws
about as hard as one on a fresh 13.5 V battery. This is why a shot tuned in
practice should still land in the last match of the day.

---

## Tuning procedure

All three live controls change values **for this run only**. When you find
numbers that work, write them into the constants at the top of the file and
rebuild — otherwise they are gone the next time the op mode starts.

Do this with the **robot on blocks**, wheels off the ground, and nothing
loaded in the catapult until step 4.

1. **Watch the amps.** The screen shows `Motor A` as
   `current (cutoff X) peak Y`. Everything below depends on reading this.

2. **Find the real stall current.** Jog the arm with dpad up until it reaches
   the fire-end hard stop and the jog cuts out. Note the **peak** figure.
   That is roughly what a full-power stall looks like. Set `FIRE_STALL_AMPS`
   part way between normal running (~4 A per the bring-up notes) and that
   peak — `7.0` is the starting guess.

3. **Check the return threshold.** Jog the arm down to the home stop and note
   the peak again. The return runs at only 0.30 power, so this number is much
   smaller than the fire stall. `RETURN_STALL_AMPS` must sit **below** it, and
   above normal return running current. Starting guess `3.0`.
   *If the arm grinds at home for a second and a half after every shot, this
   value is too high.*

4. **Tune power and time.** Load the catapult. Fire with A. After each shot
   the screen shows `Last shot` as either:
   - `hit stop at NNN ms, peak N.N A` — the arm reached the fire stop. Good.
     This is the shot ending on geometry, not on a guess at time.
   - `timed out at NNN ms, peak N.N A` — `FIRE_MS` ran out first. Add time
     with the right bumper until it reads *hit stop*, or add power with
     dpad right.

   Adjust for distance with dpad left/right (power) and the bumpers (time),
   one change at a time, and fire again.

5. **Write the good values in.** Edit `FIRE_POWER`, `FIRE_MS`,
   `FIRE_STALL_AMPS` and `RETURN_STALL_AMPS` in
   `teamcode/CompetitionTeleOp.java`, then:

   ```bash
   scripts/check-code.sh
   ```

   ```bash
   scripts/build-on-robot.sh && scripts/restart-robot-app.sh
   ```

   **Check "1 Competition" appears in the list that prints.** This robot has
   reported a successful build while compiling nothing — see
   `docs/TROUBLESHOOTING.md`.

---

## First test checklist — robot on blocks

Wheels off the ground. Battery above 12 V, or results mean nothing.

- [ ] **Battery checked.** `Battery` on screen reads over 12 V.
- [ ] **Op mode registered.** "1 Competition" is in the Driver Station list.
- [ ] **Wheels forward.** Left stick forward — all four wheels drive the
      robot forward. If one spins the wrong way, **stop**: the directions in
      `MecanumDrive` were measured on 2026-11-12 and something has been
      rewired. Do not edit them by guesswork, re-run `0 BringUp` test 1.
- [ ] **Strafe.** Left stick right — wheels show the mecanum strafe pattern
      (front left and back right one way, front right and back left the
      other).
- [ ] **Turn.** Right stick right — left wheels forward, right wheels
      backward. (Rotation was never tested on 2026-11-12; this is the first
      real check of it.)
- [ ] **Slow mode.** Hold right trigger — `Drive` reads `SLOW` and the
      wheels visibly slow down.
- [ ] **Catapult direction, low power.** Dpad **down** first, briefly. The
      arm should move **toward home**, not toward the throw. If it throws,
      stop and flip `CATAPULT_DIR`.
- [ ] **Catapult direction up.** Dpad up briefly — arm moves toward the
      throw end. Release before it reaches the stop.
- [ ] **Jog cutout works.** Hold dpad up into the fire-end stop. Power should
      cut and the motor go quiet instead of grinding. Note the peak amps.
- [ ] **`FIRE_STALL_AMPS` set** from that peak (step 2 above).
- [ ] **`RETURN_STALL_AMPS` set** from the home-stop peak (step 3 above).
- [ ] **Dry fire.** Press A with nothing loaded. The state should walk
      `FIRING → SETTLE → RETURNING → IDLE` on screen and end with the motor
      silent. The arm should not grind at either end.
- [ ] **B works.** Press A, then press B mid-shot. Power must cut instantly
      and the state go to `IDLE`.
- [ ] **B beats a held dpad.** Hold dpad up and press B at the same time.
      The motor must stop and stay stopped while B is held.
- [ ] **Stop is clean.** Press the Driver Station stop during a shot. All
      motors must go silent immediately — rule R10 / G403 / G404.
- [ ] **Loaded shots.** Load, fire, tune power and time (step 4).
- [ ] **Good values written into the constants** and rebuilt (step 5).

---

## If something goes wrong at the event

| Symptom | Likely cause |
|---|---|
| Arm grinds at home after every shot | `RETURN_STALL_AMPS` too high |
| Every shot says "timed out" | `FIRE_MS` too short, or `FIRE_STALL_AMPS` too high |
| Shots cut off early and fall short | `FIRE_STALL_AMPS` too low — the inrush or normal running current is tripping it |
| Shots fall short late in the day | battery, not code. Check volts on screen |
| Arm will not move at all | `JOG_POWER` — 0.25 was measured as too weak for a loaded arm; it is set to 0.55 |
| Robot turns instead of driving straight | drive directions; re-run `0 BringUp` test 1, do not guess |
| Driving feels rotated | field-centric got toggled on. Press BACK to turn it off |
| Wheels spin / robot feels uncontrollable | lower `DRIVE_FULL`, and `TURN_SCALE` if it is the turning |
| Robot feels sluggish | raise `DRIVE_FULL`; check the right trigger or left bumper is not stuck in slow mode |

Set `FIRE_STALL_AMPS` to `99` to disable the current cutoff entirely and run
on time alone. That is the fallback if current readings turn out to be
unreliable at the event.
