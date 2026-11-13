# Catapult calibration

A session plan for the team. Work through it in order, with two people:
one on the controls, one writing down what happens.

**Before you start you need the encoder cable fitted.** Without it the
position never changes and none of this works. See
[SESSION-2026-11-12.md](SESSION-2026-11-12.md) for the part.

Plan on **60-90 minutes**. Rushing this produces numbers you cannot
trust, which is worse than no numbers.

---

## What you are trying to achieve

Not maximum range. **Repeatability.**

A catapult that lands in the same spot every time at 80% of its range
scores more than one that occasionally reaches further. Everything below
is aimed at finding a setting you can rely on, then confirming it still
works when the battery is tired.

---

## Before the first shot

| Check | Why |
|---|---|
| Battery **fully charged**, note the voltage | Every number you record depends on it |
| Arm, hooks and bands **tight** | A loose arm scatters shots no software can fix |
| Clear space, nobody in front | First shots go further than you expect |
| Safety glasses | |
| Note the band/tubing setup | Range changes later may be the bands, not the code |

Write the starting voltage at the top of your sheet. If you cannot read
it from the Driver Station, note it anyway when you can.

---

## Step 1 — Find the limits (no firing)

Run **`0 BringUp`**, bumpers to **test 3**. Gamepad 2.

1. Tap **D-pad UP** to jog the arm up slowly. Do not hold it.
2. Keep going until the arm reaches its physical stop, or as far as you
   are willing to let it go.
3. Write down that tick count.
4. Tap **D-pad DOWN** to bring it back to its natural rest position.
5. Write down that tick count too.

```
Top of travel:  __________ ticks
Home (rest):    __________ ticks
```

Everything from here lives between those two numbers. If you command the
arm outside them it will fight a hard stop, which is how gears strip.

**Stop and fix first if:** the arm moves down when you press UP, the
ticks go down as the arm goes up, the ticks stay at 0 while the arm
moves, or anything grinds.

---

## Step 2 — Three release points, low power

Leave the power low for this whole step. You are finding the *shape* of
the throw, not the range.

Pick three release points spread across your travel. If your top of
travel was around 400 ticks, try 150 / 250 / 350.

For each one:

1. Jog the arm to that position.
2. Press **X** to save it as the release point.
3. Press **B** to home the arm.
4. Load a game piece.
5. Press **A** to fire.
6. Measure where it lands. Field tiles are a fine unit.
7. Repeat **three times** at the same setting.

```
release ticks | volts | shot 1 | shot 2 | shot 3 | spread | peak amps
    ______    |       |        |        |        |        |
    ______    |       |        |        |        |        |
    ______    |       |        |        |        |        |
```

**"Spread" is the most important column.** It is the difference between
the furthest and nearest of the three shots.

- Tight spread (under about half a tile): that release point is
  repeatable, keep it.
- Wide spread: something is loose, or the arm is not returning to the
  same home each time. **Fix that before tuning anything else.** No
  software setting compensates for a catapult that cannot repeat itself.

Also watch and write down:

- Does the arm **coast past** the release point? Roughly how far?
- Does it come back to the **same home** every time?
- Any noise that sounds like something hitting something.

---

## Step 3 — Raise power to reach the target

Now pick the release point with the **tightest spread** — not the
longest shot — and raise the fire power until the distance matches what
the game needs.

Raising power keeps the trajectory shape and changes the distance.
Moving the release point changes both at once, which is why you settle
the release point first.

```
release ticks | power | volts | shot 1 | shot 2 | shot 3 | spread
    ______    | 0.5   |       |        |        |        |
    ______    | 0.6   |       |        |        |        |
    ______    | 0.7   |       |        |        |        |
```

**Aim to land on a power around 0.75-0.85, not 1.0.** Leave headroom.
Step 5 explains why this matters more than it looks.

---

## Step 4 — Peak current, for jam detection

The screen shows `Peak this session` in amps. You should have been
writing it down; if not, fire three more at your chosen setting and read
it each time.

```
fire 1: ______ A    fire 2: ______ A    fire 3: ______ A
highest of the three: ______ A
```

This sets the stall threshold: a bit above the highest normal peak. If
your fires peak around 4.5 A, something near 6.0 A catches a real jam
without false alarms during normal shots.

The code currently has `99.0`, which catches nothing at all.

---

## Step 5 — The tired battery test

**Do not skip this one.** It is the step that decides matches.

A fresh battery is about 13.5 V. By the end of a match, under drivetrain
load, it can be 11.5 V or lower. Motor torque follows voltage, so the
same commanded power throws roughly **15% weaker** late in a match. That
is the difference between scoring and missing, and it happens exactly
when matches are decided.

1. Drive the robot hard for a minute or two to pull the battery down.
2. Note the voltage.
3. Fire three more shots at the **same settings as step 3**.

```
volts now: ______

shot 1: ______   shot 2: ______   shot 3: ______
distance lost vs step 3: ______
```

**What to do about it:**

- **Lost very little?** Good, nothing to do.
- **Lost noticeably?** Two options. Either tune the power so it scores at
  *low* voltage and accept overshooting slightly when fresh, or ask for
  voltage compensation in the code, which scales power by the measured
  battery voltage. The compensation needs headroom, which is why step 3
  says to aim for 0.75-0.85 rather than 1.0.

---

## What to hand back

```
Top of travel        ______ ticks
Home                 ______ ticks
Chosen release point ______ ticks
Chosen fire power    ______
Peak current         ______ A
Overshoot past release ______ ticks (if any)

Fresh battery: ______ V, distance ______
Tired battery: ______ V, distance ______
```

Those seven numbers set `DEFAULT_RELEASE_TICKS`, `FIRE_POWER`,
`STALL_CURRENT_A` and `OVERSHOOT_COMP`, and settle whether voltage
compensation is worth adding.

---

## Things that waste a session

**Chasing maximum range.** The furthest shot is usually the least
repeatable. Score consistently first.

**Tuning around a loose mechanism.** If the spread is wide, stop and
tighten things. Software cannot fix it.

**One shot per setting.** Three is the minimum that shows you whether a
result is real.

**Calibrating on a fresh battery only.** The robot spends most of a match
not fresh.

**Changing two things at once.** Change the release point or the power,
never both, or you will not know which did what.

---

## If something looks wrong

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md). The two most likely here:

- **Position stays at 0 while the arm moves** -- encoder cable not
  connected.
- **Motor whines and twitches but does not move** -- not enough power for
  a loaded arm, not a seized mechanism.
