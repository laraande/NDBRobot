# Getting started

Everything you need to go from a laptop you have never used on this
project to changing the robot's code and saving your work.

Work through it in order. It takes about an hour the first time.

**Mac and Chromebook are both covered.** Pick your section where they
differ.

---

## 1. What you are working with

| Thing | What it is |
|---|---|
| **Control Hub** | The robot's computer. Runs the code. Makes its own Wi-Fi network. |
| **Expansion Hub** | A second board for more motors. The catapult is on this one. |
| **Driver Hub** | The tablet with the screen. Chooses and starts programs. |
| **OnBot Java** | A code editor that runs on the Control Hub, used in a browser. |
| **This repo** | The real copy of the code. The robot gets a copy pushed to it. |

The code lives in **two places**, and keeping them in step is most of the
work:

```
this repo  ---- deploy ---->  Control Hub  ----> robot moves
    ^                              |
    +--------- pull back ----------+
```

**Rule: the repo wins.** If you edit code in the OnBot Java web page,
copy it back here afterwards or the next deploy will overwrite it.

---

## 2. Install the tools

### On a Mac

Open **Terminal** (Cmd+Space, type "terminal").

Install Homebrew, a tool that installs other tools. Paste this and press
Enter:

```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

It asks for your password. Typing shows nothing -- that is normal.

Then install the two things you need:

```
brew install android-platform-tools gh
```

- `adb` talks to the Control Hub.
- `gh` talks to GitHub.

Check it worked:

```
adb version
gh --version
```

### On a Chromebook

You need **Linux** turned on. Settings -> Advanced -> Developers ->
Linux development environment -> Turn on. Give it 10 GB if asked. It
takes a few minutes.

A Terminal app appears. Open it and run:

```
sudo apt update
sudo apt install -y android-tools-adb git
```

For `gh`, follow the official Linux instructions:
https://github.com/cli/cli/blob/trunk/docs/install_linux.md

Check it worked:

```
adb version
gh --version
```

**Chromebook catch:** Linux on ChromeOS runs in a container with its own
network. Reaching the robot at `192.168.43.1` usually works, but if it
does not, see [Troubleshooting](TROUBLESHOOTING.md). A Mac is the
easier path if you have the choice.

---

## 3. Get the code

```
mkdir -p ~/github
cd ~/github
git clone <the repo URL>
cd NDBRobot
```

Ask whoever set up the repo for the URL. Once you have a GitHub account
and have been added to the project, you can copy it from the repo page's
green **Code** button.

Check you can compile without a robot:

```
scripts/check-code.sh
```

It should say "OK - everything compiles." This catches typos in seconds,
instead of finding them on the robot.

---

## 4. Connect to the robot

### Join the robot's Wi-Fi

1. Turn the Control Hub on. Wait for the Driver Hub to show **Robot
   Connected**.
2. On your laptop, join the Wi-Fi network named after the team --
   `7593-A-RC`.
3. The password is on the Driver Hub: three dots -> Program & Manage.

**While on the robot's Wi-Fi you have no internet.** That is expected.
The robot's network is not connected to anything else.

### Connect adb

```
scripts/connect.sh
```

You should see "Connected to the Control Hub."

### When you are done

```
adb disconnect
```

Then switch your Wi-Fi back. **Do this before leaving** -- a laptop still
pointed at a robot that has gone away behaves strangely.

---

## 5. Tethering: internet and robot at the same time

Normally the robot's Wi-Fi means no internet, so you cannot push to
GitHub while connected to the robot. Tethering from a phone gives you
both.

### Set it up

**iPhone:** Settings -> Personal Hotspot -> Allow Others to Join. Connect
the laptop to it by **USB cable**, not Wi-Fi -- the laptop's Wi-Fi needs
to stay on the robot.

**Android:** Settings -> Network -> Hotspot & tethering -> **USB
tethering**, with the cable plugged in.

Then join the robot's Wi-Fi as usual. The laptop now has two connections:
Wi-Fi to the robot, USB to the internet.

### The catch nobody warns you about

With both connections up, the laptop has **two default routes** and gets
confused about which to use. The symptom is specific and misleading:

- `ping 192.168.43.1` works
- `adb` works
- but `http://192.168.43.1:8080` fails instantly with **"No route to
  host"**

The OnBot Java web page becomes unreachable even though the robot is
clearly there.

**The fix** is to tunnel the web page through the adb connection, which
is unaffected:

```
adb -s 192.168.43.1:5555 forward tcp:8080 tcp:8080
```

Then use **`http://127.0.0.1:8080`** instead of the robot's address.

`scripts/build-on-robot.sh` does this for you, so if you build from the
command line you never hit it.

---

## 6. Change some code

The everyday loop:

```
1. Edit a file in teamcode/
2. scripts/check-code.sh          compiles on the laptop, seconds
3. scripts/connect.sh             if not already connected
4. scripts/build-on-robot.sh      pushes and builds on the robot
5. scripts/restart-robot-app.sh   so the robot loads the new code
6. Test it on the robot
7. Commit and push (see the GitHub guide)
```

**Step 5 matters more than it looks.** The robot keeps running the old
code until the Robot Controller restarts, even after a successful build.
Skipping it is the most common reason a change "does not work".

### Reading what the robot is doing

See [LOGGING.md](LOGGING.md). Short version: the diagnostic op mode
writes a CSV of everything it does, and `scripts/pull-logs.sh` copies it
to your laptop.

---

## 7. Do not trust "it worked"

The single most useful thing to learn from this project: **the robot
reports success while doing nothing.**

Real examples, all from one evening:

- A build said "successful" and produced an empty file with no code in
  it. The robot showed no programs at all.
- A rebuilt program was ignored because the old one was still loaded.
- A test ran for 153 seconds in the wrong mode because a button press
  never registered.

None of these announced themselves.

**So check.** After a deploy, confirm the programs actually registered:

```
scripts/restart-robot-app.sh
```

It prints what the robot loaded. If your op mode is not in that list, it
is not on the robot, whatever the build said.

[TROUBLESHOOTING.md](TROUBLESHOOTING.md) has the full list of failures
we have hit, what each looked like, and what fixed it. **Read it before
you spend an hour on something.**

---

## Where to go next

| Doc | For |
|---|---|
| [GITHUB-FOR-STUDENTS.md](GITHUB-FOR-STUDENTS.md) | Making an account, saving your work |
| [LOGGING.md](LOGGING.md) | Finding out what the robot actually did |
| [HARDWARE.md](HARDWARE.md) | Cables, ports, what plugs in where |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | When something is broken |
| [../CATAPULT-CALIBRATION.md](CATAPULT-CALIBRATION.md) | Tuning the catapult |
