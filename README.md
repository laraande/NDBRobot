# NDB Robot

Code for the FTC robot, written in OnBot Java and run on a REV Control Hub.

## What is where

| Folder | Contents |
|---|---|
| `teamcode/` | The current season's code. This is exactly what goes on the robot. |
| `config/` | The robot's hardware configuration (`2026-2027.xml`), for reference. Older ones are in `config/archive/`. |
| `scripts/` | Connect to the robot, build code on it, pull logs back. See [scripts/README.md](scripts/README.md). |
| `docs/` | Guides: setup, hardware, logging, troubleshooting, calibration. |
| `logs/` | CSV logs pulled off the robot during testing. |
| `tools/lib/` | The Control Hub's own copies of the FTC SDK, used to check code on the laptop. |
| `archive/through-2025-26/` | All code from earlier seasons. Not loaded onto the robot. |
| `history/onbot-builds/` | The Control Hub's own backup of every build from Feb 5 to Nov 12, 2026. |
| `hub-snapshots/` | Raw copies pulled off the robot. Kept on this laptop only, not in git. |

## Current season (2026-27)

| File | What it does |
|---|---|
| `CatapultOpMode.java` | The driver-controlled program. Shows on the Driver Station as **TeleOp2026**. Runs the catapult from gamepad 2: A fires, B returns home, D-pad jogs. |
| `MecanumDrive.java` | Drive helper for the four wheels. Not yet used by `CatapultOpMode`. |

### Hardware the code expects

These names come from the `2026-2027` configuration on the robot.

| Name | Device | Port |
|---|---|---|
| `catapult` | goBILDA motor | Expansion Hub, motor 0 |
| `back left` | goBILDA motor | Control Hub, motor 0 |
| `back right` | goBILDA motor | Control Hub, motor 1 |
| `front left` | goBILDA motor | Control Hub, motor 2 |
| `front right` | goBILDA motor | Control Hub, motor 3 |
| `imu` | Built-in gyro | Control Hub |

The configuration itself is edited on the Driver Station (Configure Robot). The
copy in `config/` is a record, and the scripts do not change it.

## Checking code before deploying

Run this on the laptop, with or without the robot:

```
scripts/check-code.sh
```

It compiles `teamcode/` in a couple of seconds and reports typos, wrong method
names, missing imports and type mistakes, so OnBot Java's **Build Everything**
is not the first place an error turns up. Pass a folder to check a different
one, for example `scripts/check-code.sh archive/through-2025-26/teamcode`.

It does **not** check hardware names, ports, or how the robot behaves -- only
that the code compiles. Nothing is sent to the robot.

This needs no internet and no Android Studio: `tools/lib/` holds the Control
Hub's own SDK jars, pulled off the robot on 2026-11-12. If the SDK on the robot
is ever updated, copy the new jars from
`hub-snapshots/<date>/FIRST/java/lib/` over the ones in `tools/lib/`.

## Documentation

**New to this project? Start with
[docs/GETTING-STARTED.md](docs/GETTING-STARTED.md).** It covers Mac and
Chromebook setup, connecting to the robot, tethering, and the everyday
edit-build-test loop.

| Doc | For |
|---|---|
| [Getting started](docs/GETTING-STARTED.md) | Set up a laptop from scratch |
| [GitHub for students](docs/GITHUB-FOR-STUDENTS.md) | Make an account, save your work |
| [Hardware](docs/HARDWARE.md) | Cables, ports, what plugs in where |
| [Logging](docs/LOGGING.md) | Find out what the robot actually did |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | When something is broken |
| [Competition TeleOp](docs/COMPETITION-TELEOP.md) | Controls, tuning and first-test checklist for "1 Competition" |
| [Catapult calibration](docs/CATAPULT-CALIBRATION.md) | A field session plan for tuning it |
| [Bring-up card](docs/BRINGUP-CARD.md) | The operator's card for the diagnostic tests |
| [Driver Station options](docs/DRIVER-STATION-OPTIONS.md) | Op mode grouping, for the team to decide |
| [Catapult review notes](docs/NOTES-catapult-review.md) | What is measured, what is still a guess |
| [Session log, 12 Nov 2026](docs/SESSION-2026-11-12.md) | What the first bring-up session found |

### The one thing to know

**The robot reports success while doing nothing.** A build can "succeed"
and produce a file with no code in it. A rebuilt program can be ignored
because the old one is still loaded. After any deploy, check what
actually loaded:

```
scripts/restart-robot-app.sh
```

It prints the programs the robot registered. If yours is not listed, it
is not on the robot, whatever the build said.

## Working with the robot

One-time setup on a Mac: `brew install android-platform-tools`

1. Turn the robot on and join its Wi-Fi from the laptop. (The laptop has no
   internet while on the robot's Wi-Fi.)
2. Connect: `scripts/connect.sh`
3. Then either:
   - **Load this code onto the robot:** `scripts/deploy-to-robot.sh`, then open
     <http://192.168.43.1:8080>, go to OnBot Java, refresh, and press Build Everything.
   - **Copy the robot's code to the laptop:** `scripts/pull-from-robot.sh`
4. Before leaving the robot's Wi-Fi: `adb disconnect`

`deploy-to-robot.sh` replaces everything in the robot's `teamcode` folder with
the contents of `teamcode/` here. It saves a backup copy first and asks before
changing anything.

If code is edited in the OnBot Java web page, run `pull-from-robot.sh`
afterwards and copy the changed files into `teamcode/` so git stays the
source of truth.

## Earlier seasons

`archive/through-2025-26/teamcode/` holds the 39 files that were on the robot
before the cleanup. They build as one set (they share `Team7593OpMode` and
`Team7593Hardware`):

- **2025-26, flywheel launcher:** `Teleop_25_26`, `Launch25`, `OwheeliaAuton`,
  `OwheeliaAutonFar`, `OwheeliaAutonStepDrive25`, `OwheeliaDrive25`.
  Needs the `2526` / `Owheelia` configuration.
- **2024-25:** `AllenWalter*`, `SlideUp2025`, `ArmUp2025`, `IntakeOut2025`.
- **2023-24 and earlier:** `Auton2023*`, `Team7593TeleOp`, `Team9593TeleOpNew`,
  `Blinda*`, claw and arm steps.
- **Shared framework (from 2017):** `Team7593OpMode`, `Team7593Hardware`,
  `AutonStep`, `WheelSpeeds`, `DriveX`, `DriveY`, `AngleRotate`.

To run an old program again, copy that whole folder back into `teamcode/`,
deploy, and select the matching configuration on the Driver Station.
