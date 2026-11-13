# Driver Station op mode options

For the team to decide. Nothing here is built yet.

The driver asked whether we can have separate modes -- diagnostics,
practice, competition -- rather than one flat list. We can. This lays out
how, and the choices that need making.

---

## How the Driver Station organises things

The dropdown splits **Autonomous** from **TeleOp** automatically. Inside
each, op modes are grouped by a `group` label, and sorted alphabetically
by group and then by name.

That alphabetical sort is how ordering gets controlled. `0 BringUp` sits
at the top of its group because of the leading zero. Numbering the group
names puts the groups themselves in a chosen order.

Right now there are two, ungrouped in any deliberate way:

| Name | Group |
|---|---|
| `TeleOp2026` | Test |
| `0 BringUp` | Diagnostic |

---

## Question 1: how many groups, and what are they called?

### Option A -- three groups by purpose

| Group | Holds |
|---|---|
| `1 Match` | What gets run at a competition |
| `2 Practice` | Driver training, tuning, single-mechanism tests |
| `3 Diagnostic` | Hardware checks when something is wrong |

The driver opens the list at a competition and the match programs are at
the top. Everything else is present but out of the way.

### Option B -- two groups

`1 Match` and `2 Tools`, with practice and diagnostics together.

Simpler. Reasonable if there will only ever be two or three non-match
programs.

### Option C -- leave it flat

No groups. Fine while there are only two or three op modes total; gets
unwieldy past that. The archived seasons had 41 in a flat list, which is
how "which TeleOp is the right one?" becomes a question thirty seconds
before a match.

---

## Question 2: slow mode -- separate program, or a button?

The driver may want a reduced-speed mode for training or for lining up
precise shots.

### Option A -- a separate `Practice - Slow` op mode

Speed capped for the whole run. Nobody can accidentally go full speed.

Good for handing the controls to someone new. The cost is another entry
in the list, and the driver has to stop and re-select to change.

### Option B -- a button on the normal op mode

Hold a bumper or trigger and the robot moves at half speed; release for
full.

Usable situationally, mid-match, without re-selecting anything -- line up
a shot slowly, then drive away at speed. The cost is that a new driver
can still go full speed by letting go.

### Option C -- both

The separate op mode for training, the button for matches.

---

## Question 3: how many op modes in total?

Every entry is one more thing to select by accident under pressure.

A suggested shape, if option A is chosen for both questions above:

| Group | Op mode | What it is |
|---|---|---|
| `1 Match` | `TeleOp 2026` | The real one. Full power, drive plus catapult |
| `1 Match` | `Auto - Do Nothing` | Placeholder so the autonomous slot exists |
| `2 Practice` | `Practice - Slow` | Same code, speed capped |
| `2 Practice` | `Practice - Catapult Only` | Catapult without drive, for calibration |
| `3 Diagnostic` | `0 BringUp` | Tonight's four hardware tests |

Five entries. **Worth keeping the total under about six.**

`Practice - Catapult Only` is worth considering on its own merits: during
calibration, a robot that cannot drive is a robot that cannot roll away
while someone is loading it.

---

## Question 4: an autonomous placeholder?

There is no autonomous program this season yet.

Adding an empty `Auto - Do Nothing` now means the slot exists in the
right group, and whoever writes the real one does not have to think about
naming or grouping. It also gives the driver something safe to select if
autonomous is required but nothing is ready.

Cheap, and easy to skip if the team would rather not see an empty entry.

---

## What it would cost to build

Small. The groups are one annotation line per op mode. The practice
variants are thin subclasses that inherit the real code and change a
constant or two -- not copies, so a fix to the driving code applies to
all of them and they cannot drift apart.

Roughly half an hour, and none of it touches the catapult logic that is
still waiting on the encoder cable.

---

## What to decide

1. Three groups, two, or none?
2. Slow mode as a separate program, a button, or both?
3. Which op modes are worth having? Any here that are not?
4. Autonomous placeholder now, or wait?

Anything the driver wants that is not on this list is worth raising --
these are the options that came up, not the only ones possible.
