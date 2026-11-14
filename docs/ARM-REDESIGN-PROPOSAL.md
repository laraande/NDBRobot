# Catapult arm redesign — analysis and proposal

Written Nov 13, 2026, from the Nov 12 floor measurements. Nothing on the
robot has been changed.

## Short answer

**Do not lengthen the arm.** The data does not support the length
hypothesis, and one of the two key measurements is an artifact of the
firing code rather than a property of the mechanism.

The mechanism is not torque-limited *or* speed-limited. It is
**energy-transfer limited**: the motor does roughly 3.3 J of work per shot
and the ball leaves with 0.6–0.9 J of it. Lengthening the arm does not
change that ratio — it slightly worsens it.

The three changes worth making, in order, are: fix the 171 ms measurement,
cut tip mass, and then add energy storage. A longer arm is the one change
that is predicted to do nothing.

---

## 1. The 171 ms number is not what it looks like

`CompetitionTeleOp.java` ignores current for the first `IGNORE_MS = 80`
milliseconds to skip the inrush spike:

```java
static final double IGNORE_MS = 80;   // line 190
boolean settled = timer.milliseconds() > IGNORE_MS;
boolean hitStop = settled && amps > fireCutoff();
```

So "hit stop at 171 ms" cannot mean anything earlier than 80 ms, and the
8.1 A peak is only sampled after 80 ms. Two readings are consistent with
that log line:

- The arm really did reach the stopper at ~171 ms.
- The arm reached the stopper **any time before 80 ms** and the code did
  not notice until the ignore window expired.

The second is physically likelier. A constant-acceleration quarter turn in
171 ms implies a release tip speed of 15.3 ft/s; the ball's measured flight
says 9.8–12.1 ft/s. A quarter turn in ~91 ms (171 minus the blind window)
implies 28.8 ft/s, which is far too fast. Neither matches, which is the
tell: **the arm is not accelerating smoothly through the whole arc**, so
no single constant-acceleration fit works and 171 ms should not be used as
a kinematic measurement at all.

This matters because "reaches the stopper while still accelerating" is the
main evidence for the arm being arc-limited rather than torque-limited,
and that evidence is unreliable.

## 2. Release velocity: the 45° assumption understates it

Back-calculating from range alone at 45° gives 9.8 ft/s. But the shot was
measured as 3 ft range with a 2 ft peak height, and those two numbers
together over-determine the trajectory:

| Quantity | Value |
|---|---|
| Vertical velocity (from 2 ft peak) | 11.3 ft/s |
| Horizontal velocity (from 3 ft range, 0.71 s flight) | 4.3 ft/s |
| **Release speed** | **12.1 ft/s** |
| **Release angle** | **69°, not 45°** |

The launch is much steeper than assumed. That is itself a finding: at 69°
the shot is throwing most of its energy straight up. Re-aiming to ~45°
would take the *same* 12.1 ft/s from 3 ft to about 4.6 ft — a 50% range
gain for zero mechanical change. This is the cheapest win available and it
is pure geometry, not energy.

Implied angular speed is 14.5 rad/s (139 rpm) at 10 in, against the
motor's 312 rpm free speed.

## 3. Where the energy actually goes

At 8.1 A the motor is producing about 2.09 N·m, so across a quarter turn
it does **3.28 J** of work. The ball leaves with **0.62–0.95 J**. That is
**19–29% efficiency** — and the missing 70-80% is not friction, it is
structural.

The reason is that the stopper releases the ball by **stopping the arm**.
The arm and tray carry rotational energy proportional to their own inertia,
and all of it is dumped into the stopper bracket rather than into the ball:

| Arm linear density | Ball's share of kinetic energy | Absorbed by arm + tray |
|---|---|---|
| 0.30 kg/m | 35% | 65% |
| 0.41 kg/m (goBILDA channel) | 32% | 68% |
| 0.55 kg/m | 30% | 70% |

A 45 g wiffle ball on the end of a ~105 g aluminium channel plus a ~60 g
tray will never get more than about a third of the swing energy. **This,
not arm length, is the binding constraint.**

## 4. Why a longer arm does not help

Modelling the swing with a linear motor curve (312 rpm free, 2.383 N·m
stall, scaled to 13.1 V) and goBILDA channel mass:

| Arm length | Quarter-turn time | Release ω | **Tip speed** | Ball's energy share |
|---|---|---|---|---|
| 10 in | 126 ms | 21.6 rad/s | **18.0 ft/s** | 32% |
| 12 in | 150 ms | 18.6 rad/s | **18.6 ft/s** | 31% |
| 15 in | 189 ms | 15.2 rad/s | **19.0 ft/s** | 29% |
| 18 in | 231 ms | 12.7 rad/s | **19.0 ft/s** | 27% |

Tip speed is **flat to within 6% from 10 to 18 inches**. Your second
instinct was the right one: inertia growing as L² cancels the velocity
gain from L almost exactly, because arm mass dominates the 45 g ball. The
length-vs-range exponent on this mechanism is approximately **zero**, not 2.

Going from 10 in to 15 in is predicted to change range by under 10%, while
raising stopper impact energy by about 2.5x and pushing the arm outside the
18 in starting cube when cocked. That is a strictly bad trade.

(Note the model predicts 18 ft/s where you measure 12.1 ft/s. The gap is
the energy-transfer loss in §3 — further confirmation that the loss is
real and large, and that it is where the opportunity is.)

## 5. What to do instead

### Immediate, free: re-aim to 45°

The shot is leaving at 69°. Moving the stopper to release nearer 45° should
take 3 ft to ~4.6 ft with no new parts. Do this first and re-measure before
anything else, because it changes the baseline every later test is judged
against.

### Next: cut tip mass, keep length at 10 in

Every gram at the tip is a gram competing with a 45 g ball for the same
energy. Target the tray and the outer third of the arm.

- **Tray:** replace any aluminium tray with a 3D-printed PLA or
  polycarbonate cup, ~15 g instead of ~60 g. Largest single win available.
  A wiffle ball needs almost no retention — a shallow cradle is enough.
- **Arm:** keep goBILDA channel for the inner 4 in at the pivot where
  bending moment is highest, and switch the outer 6 in to a lighter
  section. A 1/2 in carbon fibre tube (hand-cut, no machining) bonded or
  bolted into the channel saves roughly 40 g at the worst possible place.
- **Lighten in the right place:** mass at the tip costs 9x what the same
  mass costs at 1/3 radius, because inertia goes as r². Drill or slot only
  the outer half, and leave the pivot end alone.

Predicted effect: ball share from ~32% to ~50%, release speed up roughly
25%, range up roughly 55%. Combined with re-aiming, ~6-7 ft — which is the
range you wanted from a 15 in arm, without the arm.

### Then, and only then: energy storage

You said you wanted to exhaust the simple arm change first; this analysis
says the simple arm change is the *lighter* arm, not the longer one. After
that, surgical tubing is the real answer, because it decouples throw energy
from motor torque and lets the motor wind up slowly against the stopper
instead of slamming it. But the mass work above makes the tubing more
effective too, so it is not wasted effort either way.

## 6. Structural concerns

At 8.1 A the arm currently delivers an impact the bracket was never sized
for, and it does it every shot. What fails first, in order:

1. **The stopper bracket's fasteners.** Single-shear M4 bolts in aluminium
   channel will elongate their holes long before anything breaks. Check for
   hole ovalling after every 20-30 shots. Fix: back the bracket with a
   second plate so the bolts are in double shear, and add a hard rubber or
   Sorbothane pad on the contact face to spread the impulse over
   milliseconds instead of microseconds.
2. **The gearbox output stage.** This is the expensive failure. A 19.2:1
   5203 has a plastic-and-metal planetary stack that is not rated for
   repeated reverse shock loads. Every stopper hit back-drives it.
3. **The pivot shaft and its bearings.** Impact loads a shaft in bending.
   Use a bearing on *both* sides of the arm, not a single cantilevered
   bearing — a cantilevered pivot concentrates the whole moment at one
   point.

**Protecting the gearbox is the important one**, because the current code
makes it worse. Right now the fire cutoff can only fire after
`IGNORE_MS = 80`, so an early stopper hit runs the motor into the stop at
full power for the remainder of the window. Two code changes help, and both
are smaller than any mechanical change:

- Lower `IGNORE_MS` to ~25-30 ms. The inrush spike on a 5203 is largely
  over in 20 ms; 80 ms is far more blind time than needed, and it is
  currently hiding the entire event you are trying to measure.
- Taper power near the end of the arc instead of cutting at 100%. Firing at
  full power to the last instant means the gearbox absorbs peak torque *and*
  peak speed at the same moment.

Do not lengthen the arm before these are in place. A 15 in arm raises
impact energy at the bracket by ~2.5x for no range gain, which is the worst
possible combination.

## 7. The spare motors: you are right, with one caveat

Your read is correct on both.

- **50.9:1 (117 rpm):** right to reject. At 117 rpm free speed, a quarter
  turn under load would take ~400 ms and tip speed would be *below* what
  you get now. More torque does not help when you are only converting 30%
  of the torque you already have.
- **1:1 (6000 rpm):** right to reject, for the reason you gave. It cannot
  accelerate the arm's inertia in a quarter turn, and a bare-output 5203
  taking direct stopper impact would not survive long.

The caveat: the current 19.2:1 is also not obviously optimal — it is just
the best of the three. Once tip mass comes down, a ~13:1 or 15:1 would be
better matched. That is a purchase, not something in the parts bin, so it
is not a recommendation for this week.

One correction to the hardware notes while we are here:
`docs/NOTES-catapult-review.md` establishes that `MOTOR_TICKS_PER_REV = 28`
and the ~9.2 A stall figure both belong to the **bare 6000 rpm motor**, not
the 19.2:1. If the motor actually on the robot is the 19.2:1, its stall
current is lower than 9.2 A and `FIRE_STALL_AMPS = 8.0` may be sitting
above the real stall. Worth confirming which motor is physically installed
before the next test session — it changes the meaning of every current
threshold in the file.

## 8. The cheap validation test

This measures the length exponent **without building anything**, by
shortening the arm you have rather than lengthening it. Shortening is free,
reversible, and tests the same curve.

Move the tray inward to three radii — 10 in (baseline), 7.5 in, and 5 in —
by bolting it into existing channel holes. Then:

1. Fix fire power at 1.00 and `FIRE_MS` at 250 so nothing times out early.
2. Set `IGNORE_MS` to 25 first, or the timing data is meaningless.
3. Five shots at each radius. Record range, peak height, peak amps, and the
   reported stop time.
4. Mark the landing point with tape — a single-shot measurement has more
   scatter than the effect you are looking for.

**How to read it:** plot log(range) against log(radius) and take the slope.

| Slope | Meaning | Action |
|---|---|---|
| ~2 | Speed-limited; length helps as you hoped | Build the 15 in arm |
| ~0-0.5 | Torque/inertia-limited, as predicted here | Cut tip mass instead |
| negative | Transfer-limited and worsening with length | Cut mass, then add tubing |

My prediction is a slope near 0.3, which would say a 15 in arm buys about
12% range for 2.5x the impact energy.

The second half of the test is worth more than the first: at each radius,
also record peak height and range separately so you can compute the
release *angle* as in §2. If the angle drifts between radii, the stopper
geometry is coupling to arm length and that needs fixing before any length
conclusion is trustworthy.

Total cost: zero parts, about 45 minutes, and it either confirms the
analysis or tells you I am wrong in a way you can act on.

---

## ADDENDUM — the motor was misidentified (2026-11-13, later)

Section 7 flagged that the motor should be confirmed before trusting any
current threshold. It has now been read off the label, and it is **not**
the 19.2:1 that this document and the rest of the repo assumed.

**Fitted motor: goBILDA 5203 Yellow Jacket, SKU 5203-2402-0019,
5.2:1 ratio, 1150 rpm free @ 12 V.**

This changes the central conclusion, so take the numbers above with care.

### What it changes

The measured release speed of ~113-139 rpm was read against an assumed
312 rpm free speed, giving an apparent efficiency of 36-45%. Against the
real 1150 rpm free speed it is closer to **10%**.

That is not a small correction. It says the arm is nowhere near the
motor's speed limit; it is **heavily torque-limited**. The motor has
speed it cannot use because 5.2:1 is too little reduction to accelerate
the arm's inertia through a quarter turn.

### What that means for a longer arm

**A longer arm is probably the wrong move.** Lengthening raises inertia
with the square of length, pushing a mechanism that is already
torque-starved further into the regime where range gains approach zero.
The r-squared projection in section 3 assumed far more speed headroom
than actually exists.

### The better move

**Change the gear ratio, not the arm length.** Trade the speed that is
not being used for the torque that is missing. This is a parts-bin swap
rather than fabrication, and it is testable in an evening.

- **50.9:1 (117 rpm):** rejected earlier for being too slow, and that
  reasoning was sound on its own terms -- 117 rpm free is barely above
  the ~113 rpm the arm already reaches, so it would likely be slower
  under load. Still probably not the answer, but much closer than the
  earlier dismissal implied.
- **A ratio in between, roughly 13:1 to 19.2:1**, is the shape of the
  right answer: several times the torque, still 2-3x the tip speed
  currently achieved. That is a purchase.

### Also affected

Every current threshold in `CompetitionTeleOp.java` was reasoned against
a ~9.2 A stall that belongs to the **bare 6000 rpm motor**. A 5.2:1
stalls lower. `FIRE_STALL_AMPS = 8.0` may therefore sit at or above this
motor's true stall, meaning it would not protect against a genuine jam.

It does reliably trip on stopper contact (measured: "hit stop at 171 ms,
peak 8.1 A"), so the normal case is covered. The stall current of this
specific motor has never been measured.

### The validation test still stands

The shorten-the-arm experiment in section 8 is unaffected and is now
*more* worth running, because it measures the real length exponent
directly rather than relying on a model whose inputs were wrong.
