# Scripts

Each one does a single thing and says what happened. Run them from the
project folder:

```
scripts/connect.sh
```

## What they do

| Script | Does | Touches the robot? |
|---|---|---|
| `connect.sh` | Connects to the robot. **Run first, every session.** | no |
| `check-code.sh` | Compiles the code on the laptop. No robot needed. | no |
| `build-on-robot.sh` | Copies the code over and builds it. | writes |
| `restart-robot-app.sh` | Makes the robot load what was built. | restarts app |
| `pull-logs.sh` | Copies diagnostic logs to `logs/`. | reads only |
| `pull-from-robot.sh` | Copies the robot's code to `hub-snapshots/`. | reads only |
| `deploy-to-robot.sh` | Makes the robot match `teamcode/` exactly. **Deletes.** | asks first |

## The normal loop

```
scripts/check-code.sh         catch typos in seconds
scripts/connect.sh            once per session
scripts/build-on-robot.sh     copy over and build
scripts/restart-robot-app.sh  make the robot load it
```

**The last step is not optional.** A build writes new code, but the robot
keeps running what it already loaded until the app restarts. Skipping it
is the most common reason a change appears to do nothing.

`restart-robot-app.sh` prints the programs the robot loaded. If yours is
not in that list, it is not on the robot, whatever the build said.

## How they are put together

`common.sh` holds the shared settings and helpers. It is not run on its
own -- every other script starts with:

```bash
source "$(dirname "$0")/common.sh"
```

That gives them:

| | |
|---|---|
| `HUB`, `TEAMCODE`, `MATCHLOGS` ... | where things are, named once |
| `hub "command"` | run a command on the robot |
| `hub_push` / `hub_pull` | copy files to and from it |
| `require_hub` | stop with a clear message if it is not connected |
| `hub_web` | reach the robot's web page through adb |
| `say` / `warn` / `die` | print, print a warning, stop with a message |

So a script reads as a list of steps rather than a pile of `adb` commands.

### If you are changing one

**Keep the shape.** Steps in order, each with a comment saying why. If a
step exists because of something that went wrong, say so -- several of
them do, and that is the only record of it.

**Test `check-code.sh` first**, it needs no robot:

```
scripts/check-code.sh
```

**Check the syntax** of anything you edit before running it:

```
bash -n scripts/the-one-you-changed.sh
```

**Error messages should say what to do next**, not just what failed.
Compare "Not connected" with the message `require_hub` actually prints.
