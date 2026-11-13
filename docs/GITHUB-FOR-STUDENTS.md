# GitHub for students

How to make an account and save your work to the project.

**Yes, you can have an account.** GitHub requires you to be **13 or
older**. High school students are fine. If you are outside the US your
country may set a higher minimum -- GitHub's terms say to follow your
local law.

GitHub is free for what this project needs.

---

## Why bother

Git keeps every version of the code and who changed what. That means:

- **Nothing is ever lost.** Broke something at 11pm? Go back to this
  morning's version.
- **Two people can work without overwriting each other.**
- **You can see why a thing is the way it is.** "Why is this motor
  reversed?" has an answer with a date attached.

The robot's own code editor has none of this. Code edited only on the
robot has no history, and the next person to deploy overwrites it.

---

## 1. Make an account

1. Go to **github.com** and click **Sign up**.
2. Use an email you will still have after this school year. A personal
   address outlives a school one.
3. Pick a username you would not mind a future employer seeing. It is
   public, and it is on every change you make. First initial plus
   surname is a safe default.
4. Turn on **two-factor authentication** when asked. GitHub requires it
   for contributors. Use an authenticator app on your phone.

**Save your recovery codes somewhere you will find them again.** If you
lose your phone and your recovery codes, the account is gone for good,
and nobody can get it back for you.

5. Tell whoever runs the team repo your username so they can add you.

---

## 2. Tell git who you are

Once, on each computer you use:

```
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

Use the same email as your GitHub account, or your changes will not be
linked to you.

---

## 3. Sign in from the command line

```
gh auth login
```

Choose **GitHub.com**, then **HTTPS**, then **Login with a web browser**.
It shows a code, you paste it into the browser page it opens.

Check it worked:

```
gh auth status
```

---

## 4. The everyday loop

Four commands, in this order, every time.

### See what you changed

```
git status
```

Lists files you have edited. Start here when you are not sure where you
are.

### Stage the changes

```
git add teamcode/MecanumDrive.java
```

Or everything at once:

```
git add -A
```

### Commit with a message

```
git commit -m "Reverse the front right motor direction"
```

The message matters. Someone -- possibly you in March -- will read it
trying to work out why the robot behaves as it does.

**Good:** `Raise catapult jog power to 0.55, arm would not move at 0.25`
**Bad:** `fix`, `stuff`, `asdf`

Say *what changed and why*. The code already shows *how*.

### Push it to GitHub

```
git push
```

Now it is safe, and everyone else can see it.

---

## 5. Working with other people

### Before you start, get the latest

```
git pull
```

Do this **every time you sit down**. It brings in what others changed.
Skipping it is how conflicts happen.

### If git says there is a conflict

Two people changed the same lines. Git marks them like this:

```
<<<<<<< HEAD
    static final double FIRE_POWER = 0.5;
=======
    static final double FIRE_POWER = 0.8;
>>>>>>> main
```

Delete the markers, keep the line you want, save, then:

```
git add <the file>
git commit
```

**Ask someone if you are unsure.** Picking wrong here can silently undo
somebody's work.

### Branches, when you are ready

For anything big, work on a branch so `main` always holds code that
works:

```
git checkout -b my-change
... edit, commit ...
git push -u origin my-change
```

Then open a pull request on GitHub so someone can look before it joins
`main`.

For small fixes, committing straight to `main` is fine on a team this
size. Agree as a team which you are doing.

---

## 6. Things worth knowing

**Never commit passwords, Wi-Fi keys or API tokens.** Git remembers
everything -- deleting a password in a later commit does not remove it
from the history. If it happens, say so immediately so the password can
be changed.

**Commit small and often.** Ten small commits are easier to understand,
and easier to undo, than one enormous one.

**Do not commit things the project does not need.** Build output, editor
settings, big binary files. `.gitignore` already excludes the obvious
ones.

**If you break something,** it is recoverable. That is the entire point.
Ask before trying to fix it with commands you found online -- some of
them throw work away.

---

## Commands worth remembering

| Command | Does |
|---|---|
| `git status` | What have I changed? |
| `git pull` | Get everyone else's work |
| `git add -A` | Stage everything |
| `git commit -m "..."` | Save with a message |
| `git push` | Send to GitHub |
| `git log --oneline` | Recent history |
| `git diff` | Exactly what I changed, line by line |

---

## If you get stuck

`git status` almost always tells you where you are and often what to do
next. Read it before reaching for anything else.

Beyond that: ask. Everyone finds git confusing at first, and the people
who seem fluent mostly just broke things earlier and learned from it.
