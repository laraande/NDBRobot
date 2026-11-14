package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Autonomous: drive straight off the wall, then stop and wait.
 *
 * Written 2026-11-14 at the BIOBUZZ event. The robot has no drive encoders,
 * so this is TIME based: drive at a set power for a set time. Distance will
 * vary a little with battery charge and floor, so set the time comfortably
 * longer than the distance you need.
 *
 * It keeps the robot driving straight with the IMU (the hub mounting was
 * measured on 2026-11-12, see MecanumDrive). The catapult is never touched.
 *
 * HOW TO RUN IT IN A MATCH
 *   1. Place the robot against the wall in its starting spot.
 *   2. Driver Station: choose "1 Auto Leave Wall", press INIT.
 *   3. While INIT is showing you may adjust with gamepad 1 (screen shows it):
 *        dpad up / down     drive time  +/- 100 ms
 *        dpad right / left  start delay +/- 500 ms (default 1 s)
 *        Y                  flip direction (forward / backward)
 *      Then hands off.
 *   4. On the referee's "go", press START (the play button).
 *   5. When autonomous ends, "1 Competition" is already selected for you.
 *      Press INIT, then START on the TeleOp cue.
 *
 * Adjustments made during INIT are forgotten when the op mode ends. Once you
 * find good numbers, write them into the constants below and rebuild.
 *
 * "Forward" is the end of the robot away from the catapult.
 */
@Autonomous(name = "1 Auto Leave Wall", group = "Competition",
        preselectTeleOp = "1 Competition")
public class AutoLeaveWall extends LinearOpMode {

    // ===================== SETTINGS =====================

    /** Drive power. Kept low: at high power the wheels spin (seen 2026-11-13). */
    static final double DRIVE_POWER = 0.35;

    /** How long to drive. A GUESS until timed on the practice field. */
    static final double DRIVE_MS = 1500;

    /**
     * Wait this long after START before moving. 0 = go immediately.
     *
     * Defaults to 1 s so the robot never jumps the instant play is pressed:
     * hands are clear of the field, and an alliance partner leaving the same
     * wall gets a head start. Adjust in INIT with dpad right/left.
     */
    static final double START_DELAY_MS = 1000;

    /** +1 drives forward (away from the catapult end), -1 drives backward. */
    static final int DIRECTION = 1;

    /** Time taken to build up to full drive power, so the wheels grip. */
    static final double RAMP_MS = 250;

    /**
     * Heading hold: how hard to steer back toward the starting heading, per
     * degree of drift. Set to 0 to turn heading hold off completely (try
     * that if the robot ever wiggles or curves the wrong way).
     */
    static final double HEADING_KP = 0.02;

    /** Most turning heading hold may add, so it can never spin the robot. */
    static final double MAX_TURN = 0.20;

    /** Limits for the INIT adjustments. Autonomous is 30 s in total. */
    static final double MAX_DRIVE_MS = 5000;
    static final double MAX_DELAY_MS = 20000;

    // ====================================================

    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, false);

        double driveMs = DRIVE_MS;
        double delayMs = START_DELAY_MS;
        int direction = DIRECTION;

        // Edge detection so one press = one step.
        boolean lastUp = false, lastDown = false, lastLeft = false, lastRight = false, lastY = false;

        // ---------- INIT: robot must not move ----------
        while (opModeInInit()) {
            boolean up = gamepad1.dpad_up, down = gamepad1.dpad_down;
            boolean left = gamepad1.dpad_left, right = gamepad1.dpad_right;
            boolean y = gamepad1.y;

            if (up && !lastUp) driveMs = Math.min(MAX_DRIVE_MS, driveMs + 100);
            if (down && !lastDown) driveMs = Math.max(0, driveMs - 100);
            if (right && !lastRight) delayMs = Math.min(MAX_DELAY_MS, delayMs + 500);
            if (left && !lastLeft) delayMs = Math.max(0, delayMs - 500);
            if (y && !lastY) direction = -direction;

            lastUp = up; lastDown = down; lastLeft = left; lastRight = right; lastY = y;

            telemetry.addLine("READY - press START on the referee's go");
            telemetry.addData("Direction", direction > 0 ? "FORWARD (away from catapult)" : "BACKWARD (catapult end first)");
            telemetry.addData("Drive time", "%.0f ms   (dpad up/down)", driveMs);
            telemetry.addData("Start delay", "%.0f ms   (dpad right/left)", delayMs);
            telemetry.addData("Power", "%.2f", DRIVE_POWER);
            telemetry.addData("Heading", "%.1f deg", drive.getHeadingDeg());
            telemetry.update();
        }

        if (isStopRequested()) return;

        // Zero the heading now, not at INIT, in case the robot was nudged
        // while being lined up.
        drive.resetHeading();
        ElapsedTime timer = new ElapsedTime();

        try {
            // ---------- Optional wait ----------
            while (opModeIsActive() && timer.milliseconds() < delayMs) {
                drive.stop();
                telemetry.addData("Waiting", "%.1f s", (delayMs - timer.milliseconds()) / 1000.0);
                telemetry.update();
            }

            // ---------- Drive off the wall ----------
            timer.reset();
            while (opModeIsActive() && timer.milliseconds() < driveMs) {
                double t = timer.milliseconds();
                double ramp = RAMP_MS > 0 ? Math.min(1.0, t / RAMP_MS) : 1.0;

                // Heading is + when the robot has turned left; turn is + for
                // clockwise. So a + heading needs a + (clockwise) correction.
                double heading = drive.getHeadingDeg();
                double turn = clamp(HEADING_KP * heading, -MAX_TURN, MAX_TURN);

                drive.drive(direction * DRIVE_POWER * ramp, 0, turn, 1.0);

                telemetry.addData("Driving", "%.0f / %.0f ms", t, driveMs);
                telemetry.addData("Heading", "%.1f deg", heading);
                telemetry.update();
            }
            drive.stop();

            // ---------- Done: sit still until the period ends ----------
            while (opModeIsActive()) {
                drive.stop();
                telemetry.addLine("Done. Waiting for TeleOp.");
                telemetry.addData("Final heading", "%.1f deg", drive.getHeadingDeg());
                telemetry.update();
            }
        } finally {
            // Nothing may stay powered once the op mode ends.
            drive.stop();
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
