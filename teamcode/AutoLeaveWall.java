package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/**
 * Autonomous: wait, drive forward off the wall, stop.
 *
 * The whole program is four steps:
 *   1. Wait for the driver to press START (play).
 *   2. Wait 2 seconds.
 *   3. Drive forward for a few seconds.
 *   4. Stop.
 *
 * There are no drive encoders, so this is TIME based: the robot drives at a
 * set power for a set time. To go further, raise DRIVE_MS. To go faster,
 * raise DRIVE_POWER (but above about 0.5 the wheels start to spin).
 *
 * "Forward" is the end of the robot away from the catapult.
 *
 * When autonomous ends, "1 Competition" is selected automatically. Press
 * INIT, then START when TeleOp begins.
 */
@Autonomous(name = "1 Auto Leave Wall", group = "Competition",
        preselectTeleOp = "1 Competition")
public class AutoLeaveWall extends LinearOpMode {

    static final long   START_DELAY_MS = 2000;  // wait after START before moving
    static final double DRIVE_POWER    = 0.30;  // 0 = stopped, 1 = full speed
    static final long   DRIVE_MS       = 2000;  // how long to drive forward

    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, false);

        telemetry.addLine("Ready. Press START on the referee's go.");
        telemetry.update();

        // 1. Nothing moves until START is pressed.
        waitForStart();

        try {
            // 2. Wait.
            telemetry.addLine("Waiting...");
            telemetry.update();
            sleep(START_DELAY_MS);

            // 3. Drive forward (forward, strafe, turn, speed limit).
            telemetry.addLine("Driving forward");
            telemetry.update();
            drive.drive(DRIVE_POWER, 0, 0, 1.0);
            sleep(DRIVE_MS);
        } finally {
            // 4. Stop. The finally block runs even if the match is stopped
            //    part way through, so the wheels never keep spinning.
            drive.stop();
        }

        telemetry.addLine("Done.");
        telemetry.update();
    }
}
