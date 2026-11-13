# Hardware guide

What plugs in where, and the mistakes that are easy to make.

Written after a session where a missing cable cost several hours,
because nothing recorded that it should have been there.

---

## The boards

| Board | Where | Holds |
|---|---|---|
| **Control Hub** | Mounted **vertically**, on edge | Four drive motors, the IMU |
| **Expansion Hub** | Lying flat, below it | The catapult motor |

Two boards because one hub has four motor ports and this robot has five
motors. They are linked by a cable and act as one system.

---

## Motor ports

| Hub | Port | Motor | Name in code |
|---|---|---|---|
| Control | 0 | back left wheel | `back left` |
| Control | 1 | back right wheel | `back right` |
| Control | 2 | front left wheel | `front left` |
| Control | 3 | front right wheel | `front right` |
| Expansion | 0 | catapult | `catapult` |

**The names have spaces in them.** `back left`, not `backLeft`. They must
match the configuration on the Driver Hub exactly or the code cannot find
the motor. This has already caused one failure.

The configuration lives on the Driver Hub (three dots -> Configure
Robot). A copy is kept in `config/2026-2027.xml` for reference, but
editing that copy changes nothing -- the robot reads its own.

---

## Every motor needs TWO cables

This is the thing nobody tells you, and it cost this team hours.

| Cable | Looks like | Does |
|---|---|---|
| **Power** | Two thick wires, red and black | Makes the motor turn |
| **Encoder** | Four thin wires, small white plug | Reports how far it has turned |

A motor with only power **works** -- it spins. It just cannot tell the
robot where it is.

### Why this hides so well

The drive wheels do not use their encoders. The code runs them in
`RUN_WITHOUT_ENCODER` mode, where you set a speed and that is all. So
all four drive motors could have no encoder cables at all and the robot
would drive perfectly.

The catapult is different. It needs to know where the arm is, to stop at
the right point and return home. Without an encoder it is blind.

**So an encoder fault can hide indefinitely until you build the first
mechanism that needs one.** That is exactly what happened here.

### What it looks like when the cable is missing

- The arm moves when you jog it
- The position on screen **stays at 0** and never changes
- The motor draws normal current
- Nothing errors

Confirmed on this robot: 668 log samples all reading exactly 0 while the
motor drew up to 4 A and the arm visibly moved.

### The right cable

goBILDA motors and REV hubs use **different connectors**, so you need an
adapter cable, not a straight one:

- **Motor end:** JST **XH** (goBILDA 5202 series)
- **Hub end:** JST **PH** (REV Control and Expansion Hub)

Search for "4-pin JST PH to JST XH encoder cable". About $3-4 from
ServoCity or goBILDA. Measure the run before ordering -- 150mm, 300mm
and 600mm all exist.

**Order spares.** They are cheap, fragile, and you will want encoders on
the drive motors eventually for autonomous.

---

## The IMU

Built into the Control Hub. Measures which way the robot is pointing.

Because it is a chip on a board, it measures rotation around **the
board's** axes, not the robot's. So the code has to be told how the hub
is mounted:

```java
LOGO_DIR = LEFT    // which way the REV logo faces
USB_DIR  = UP      // which way the USB ports face
```

These are **measured, not guessed** -- see `MecanumDrive.java` for the
numbers and how to re-check them.

**If the hub is ever remounted, these must be redone.** Run test 4 of
`0 BringUp`: zero the heading, turn the robot left, and the number should
go up. If it goes down, the logo direction is 180 degrees out.

Mounting orientation itself does not matter. Vertical is fine. Only
describing it wrongly matters.

---

## Power

The battery feeds the Control Hub, which feeds the Expansion Hub. There
is a switch in between.

**Battery voltage affects how the robot behaves.** A fresh battery is
about 13.5 V; by the end of a match under load it can be 11.5 V. Motor
torque follows voltage, so the same commanded power produces noticeably
less force late in a match.

This matters most for the catapult -- a shot tuned on a fresh battery
falls short on a tired one. See
[../CATAPULT-CALIBRATION.md](CATAPULT-CALIBRATION.md).

**Check voltage before trusting a test result.** A mechanism that "stopped
working" may just be on a flat battery.

---

## Before you blame the code

In the order that finds problems fastest:

1. **Is the battery charged?** Under 12 V, expect odd behaviour.
2. **Is it plugged in?** Both cables, both ends, including the encoder.
3. **Is the right configuration selected** on the Driver Hub?
4. **Do the names match** the table above, spaces and all?
5. **Did the code actually load?** Run `scripts/restart-robot-app.sh` and
   check your op mode is in the list it prints.

Only then start reading code.

---

## Recording changes

**When you change the wiring, write it down.** A commit saying "moved the
catapult to Expansion Hub port 1" costs thirty seconds and saves the next
person an evening.

This repo exists partly because the team that inherited this robot got no
documentation at all, and had to rediscover things like the missing
encoder cable the hard way.
