package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Bring-up / diagnostic op mode. Shows on the Driver Station as "0 BringUp".
 *
 * This program does NOT try to be the competition op mode. It exists to answer
 * the questions that have to be answered before the real op mode can be
 * trusted, one at a time, with the answers written to a file:
 *
 *   Test 1  WHEELS      Which wheel is which, and which way does each spin?
 *   Test 2  DRIVE       Do the sticks produce the motion the driver expects?
 *   Test 3  CATAPULT    How many ticks is the real release point, and how
 *                       much current does the throw actually draw?
 *   Test 4  IMU         Does the heading increase when the robot turns left?
 *
 * Every test logs to /sdcard/FIRST/matchlogs/bringup-<date>.csv on the hub.
 * Retrieve it afterwards with scripts/pull-logs.sh.
 *
 * ---------------------------------------------------------------------------
 * CONTROLS (gamepad 1 unless noted)
 *
 *   LEFT / RIGHT BUMPER   previous / next test
 *   BACK                  mark the current moment in the log (press when
 *                         something looks wrong, so the row is easy to find)
 *
 *   Test 1 WHEELS: X = front left, Y = front right,
 *                  A = back left,  B = back right
 *                  Each spins ONE wheel forward at 30%. Nothing else moves.
 *
 *   Test 2 DRIVE:  left stick = translate, right stick X = turn
 *                  RIGHT TRIGGER held = full speed (otherwise 30%)
 *
 *   Test 3 CATAPULT (gamepad 2, so the catapult is never on the drive pad):
 *                  DPAD UP / DOWN = jog the arm slowly
 *                  X = save the arm's current position as the release point
 *                  A = fire at FIRE_POWER_TEST   B = return home
 *
 *   Test 4 IMU:    right stick X = turn in place. Read the heading.
 *                  OPTIONS = zero the heading
 * ---------------------------------------------------------------------------
 */
@TeleOp(name = "0 BringUp", group = "Diagnostic")
public class RobotBringUp extends LinearOpMode {

    // Deliberately tame. This program is for finding out what happens,
    // not for throwing hard or driving fast.
    static final double WHEEL_TEST_POWER = 0.30;
    static final double DRIVE_SLOW       = 0.30;
    static final double DRIVE_FULL       = 1.00;
    static final double JOG_POWER_TEST   = 0.25;
    static final double FIRE_POWER_TEST  = 0.50;
    static final double RETURN_POWER_TEST = 0.35;

    static final double LOG_INTERVAL_SEC = 0.05;   // 20 Hz
    static final int    RETURN_HOME_TICKS = 0;

    private DcMotorEx fl, fr, bl, br, catapult;
    private MecanumDrive drive;
    private boolean driveIsFieldCentric = false;

    private FileWriter log;
    private String logPath = "(not open)";
    private final ElapsedTime runTime  = new ElapsedTime();
    private final ElapsedTime logTimer = new ElapsedTime();

    private int test = 1;
    private static final int LAST_TEST = 4;

    // Test 3 state
    private int releaseTicks = -1;          // -1 = not calibrated yet
    private int peakTicks = 0;
    private double peakAmps = 0.0;
    private boolean firing = false;
    private final ElapsedTime fireTimer = new ElapsedTime();

    private int marks = 0;

    @Override
    public void runOpMode() {

        // ----- Hardware. Named exactly as in config/2026-2027.xml. -----
        // If any of these names is wrong the op mode stops here with a clear
        // message, rather than failing halfway through a test.
        try {
            fl = hardwareMap.get(DcMotorEx.class, "front left");
            fr = hardwareMap.get(DcMotorEx.class, "front right");
            bl = hardwareMap.get(DcMotorEx.class, "back left");
            br = hardwareMap.get(DcMotorEx.class, "back right");
            catapult = hardwareMap.get(DcMotorEx.class, "catapult");
        } catch (IllegalArgumentException e) {
            telemetry.addLine("HARDWARE NAME NOT FOUND");
            telemetry.addLine(e.getMessage());
            telemetry.addLine("");
            telemetry.addLine("Check the active configuration on the Driver");
            telemetry.addLine("Station. Expected: 'front left', 'front right',");
            telemetry.addLine("'back left', 'back right', 'catapult', 'imu'.");
            telemetry.update();
            sleep(15000);
            return;
        }

        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        // Raw motors for test 1: no direction applied, so what we see is the
        // motor's own idea of "forward". That is the whole point of test 1.
        for (DcMotorEx m : new DcMotorEx[]{fl, fr, bl, br}) {
            m.setDirection(DcMotorSimple.Direction.FORWARD);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        catapult.setDirection(DcMotorSimple.Direction.FORWARD);
        catapult.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        catapult.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        catapult.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        openLog();

        telemetry.addLine("BRING-UP  --  put the robot on blocks for test 1");
        telemetry.addLine("");
        telemetry.addData("Log file", logPath);
        telemetry.addLine("Bumpers change test. BACK marks the log.");
        telemetry.addLine("Press START.");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) { closeLog(); return; }

        runTime.reset();
        logTimer.reset();

        boolean lbPrev = false, rbPrev = false, backPrev = false;
        boolean xPrev = false, aPrev = false, bPrev = false, optPrev = false;

        while (opModeIsActive()) {

            // ----- Test selection -----
            boolean lb = gamepad1.left_bumper, rb = gamepad1.right_bumper;
            if (rb && !rbPrev && test < LAST_TEST) { allStop(); test++; }
            if (lb && !lbPrev && test > 1)         { allStop(); test--; }
            rbPrev = rb; lbPrev = lb;

            boolean back = gamepad1.back;
            if (back && !backPrev) {
                marks++;
                writeRow("MARK", "operator marked this moment");
            }
            backPrev = back;

            switch (test) {
                case 1:
                    wheelTest();
                    break;
                case 2:
                    driveTest();
                    break;
                case 3: {
                    // Edge detect gamepad 2's buttons here, so one press is
                    // one action no matter how fast the loop runs.
                    boolean x = gamepad2.x, a = gamepad2.a, b = gamepad2.b;
                    if (x && !xPrev) saveReleasePoint();
                    if (a && !aPrev && !firing) startFireTest();
                    if (b && !bPrev) startReturnTest();
                    xPrev = x; aPrev = a; bPrev = b;
                    catapultTest();
                    break;
                }
                case 4: {
                    boolean opt = gamepad1.options;
                    if (opt && !optPrev) zeroHeading();
                    optPrev = opt;
                    imuTest();
                    break;
                }
                default:
                    break;
            }

            maybeLog();
            showTelemetry();
        }

        allStop();
        closeLog();
    }

    // ===================== Test 1: wheel identity =====================
    // One wheel at a time, at FORWARD direction, 30% power. The operator
    // writes down which physical wheel moved and whether it drove the robot
    // forward or backward. That table becomes the *_DIR constants.
    private void wheelTest() {
        double p = WHEEL_TEST_POWER;
        fl.setPower(gamepad1.x ? p : 0);
        fr.setPower(gamepad1.y ? p : 0);
        bl.setPower(gamepad1.a ? p : 0);
        br.setPower(gamepad1.b ? p : 0);
    }

    // ===================== Test 2: driver feel =====================
    // Uses MecanumDrive, so this tests the real mixing and the real
    // direction constants. If forward goes sideways, the *_DIR table from
    // test 1 is wrong.
    private void driveTest() {
        ensureDrive(false);
        double scale = gamepad1.right_trigger > 0.5 ? DRIVE_FULL : DRIVE_SLOW;
        drive.drive(-gamepad1.left_stick_y,
                     gamepad1.left_stick_x,
                     gamepad1.right_stick_x,
                     scale);
    }

    // ===================== Test 3: catapult =====================
    // The number this test exists to produce is releaseTicks. Jog the arm to
    // where the throw should let go, press X, and read it off the screen.
    private void catapultTest() {
        int pos = catapult.getCurrentPosition();
        double amps = catapult.getCurrent(CurrentUnit.AMPS);

        if (pos > peakTicks) peakTicks = pos;
        if (amps > peakAmps) peakAmps = amps;

        if (firing) {
            // Cut power at the saved release point, or at a 2 s timeout.
            if (releaseTicks > 0 && pos >= releaseTicks) {
                catapult.setPower(0);
                firing = false;
                writeRow("FIRE_END", "released at " + pos
                        + " ticks, peak " + String.format(Locale.US, "%.2f", peakAmps) + " A");
            } else if (fireTimer.seconds() > 2.0) {
                catapult.setPower(0);
                firing = false;
                writeRow("FIRE_TIMEOUT", "stopped at " + pos + " ticks");
            }
            return;
        }

        // Manual jog, slow.
        if (gamepad2.dpad_up) {
            catapult.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            catapult.setPower(JOG_POWER_TEST);
        } else if (gamepad2.dpad_down) {
            catapult.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            catapult.setPower(-JOG_POWER_TEST);
        } else if (catapult.getMode() == DcMotor.RunMode.RUN_WITHOUT_ENCODER) {
            catapult.setPower(0);
        }
    }

    /** Record where the arm is now as the release point. The whole reason
     *  test 3 exists is to produce this number. */
    private void saveReleasePoint() {
        releaseTicks = catapult.getCurrentPosition();
        writeRow("CALIBRATE", "release point saved at " + releaseTicks + " ticks");
    }

    private void startFireTest() {
        if (releaseTicks <= 0) {
            writeRow("FIRE_REFUSED", "no release point saved yet - press X first");
            return;
        }
        peakAmps = 0;
        catapult.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        catapult.setPower(FIRE_POWER_TEST);
        fireTimer.reset();
        firing = true;
        writeRow("FIRE_START", "target " + releaseTicks + " ticks at power "
                + FIRE_POWER_TEST);
    }

    private void startReturnTest() {
        firing = false;
        catapult.setTargetPosition(RETURN_HOME_TICKS);
        catapult.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        catapult.setPower(RETURN_POWER_TEST);
        writeRow("RETURN", "returning to " + RETURN_HOME_TICKS);
    }

    // ===================== Test 4: IMU =====================
    private void imuTest() {
        ensureDrive(true);
        drive.drive(0, 0, gamepad1.right_stick_x, DRIVE_SLOW);
    }

    private void zeroHeading() {
        ensureDrive(true);
        drive.resetHeading();
        writeRow("IMU_ZERO", "heading zeroed");
    }

    // ===================== Shared =====================

    /**
     * Build MecanumDrive on first use. Constructing it applies MecanumDrive's
     * own direction constants, which test 1 deliberately bypasses -- so it is
     * built only once a test actually needs it. Field-centric needs the IMU,
     * so if the mode asked for differs from what we built, rebuild it.
     */
    private void ensureDrive(boolean fieldCentric) {
        if (drive != null && driveIsFieldCentric == fieldCentric) return;
        if (drive != null) drive.stop();
        drive = new MecanumDrive(hardwareMap, fieldCentric);
        driveIsFieldCentric = fieldCentric;
    }

    private void allStop() {
        if (drive != null) drive.stop();
        for (DcMotorEx m : new DcMotorEx[]{fl, fr, bl, br}) m.setPower(0);
        catapult.setPower(0);
        firing = false;
    }

    private void showTelemetry() {
        telemetry.addData("TEST", "%d of %d   %s", test, LAST_TEST, testName());
        telemetry.addLine("bumpers = change test | BACK = mark log");
        telemetry.addLine("");

        switch (test) {
            case 1:
                telemetry.addLine("One wheel at a time, 30%, no direction applied.");
                telemetry.addLine("X=front left  Y=front right  A=back left  B=back right");
                telemetry.addLine("Write down: which wheel moved, and did it push");
                telemetry.addLine("the robot FORWARD or BACKWARD?");
                telemetry.addData("fl / fr", "%.2f / %.2f", fl.getPower(), fr.getPower());
                telemetry.addData("bl / br", "%.2f / %.2f", bl.getPower(), br.getPower());
                break;
            case 2:
                telemetry.addLine("Left stick translates, right stick X turns.");
                telemetry.addLine("Right trigger = full speed.");
                telemetry.addLine("Check: stick forward -> robot forward?");
                telemetry.addLine("       stick right   -> robot strafes right?");
                telemetry.addData("sticks", "fwd %.2f  strafe %.2f  turn %.2f",
                        -gamepad1.left_stick_y, gamepad1.left_stick_x,
                        gamepad1.right_stick_x);
                break;
            case 3:
                telemetry.addLine("GAMEPAD 2. DPAD jogs, X saves release, A fires, B homes.");
                telemetry.addData("Position (ticks)", catapult.getCurrentPosition());
                telemetry.addData("Release point", releaseTicks < 0
                        ? "NOT SAVED - jog up, then press X" : releaseTicks);
                telemetry.addData("Current (A)", "%.2f",
                        catapult.getCurrent(CurrentUnit.AMPS));
                telemetry.addData("Peak this session", "%d ticks, %.2f A",
                        peakTicks, peakAmps);
                telemetry.addData("Firing", firing);
                break;
            case 4:
                telemetry.addLine("Right stick X turns in place. OPTIONS zeroes.");
                telemetry.addLine("Check: turning LEFT should make heading go UP.");
                telemetry.addData("Heading (deg)", "%.1f",
                        drive == null ? 0.0 : drive.getHeadingDeg());
                break;
            default: break;
        }

        telemetry.addLine("");
        telemetry.addData("Marks", marks);
        telemetry.addData("Log", logPath);
        telemetry.update();
    }

    private String testName() {
        switch (test) {
            case 1: return "WHEELS";
            case 2: return "DRIVE";
            case 3: return "CATAPULT";
            case 4: return "IMU";
            default: return "?";
        }
    }

    // ===================== Logging =====================
    // One CSV per run, under the folder the Driver Station already uses for
    // match logs, so it survives a rebuild and is easy to pull.
    private void openLog() {
        try {
            File dir = new File(AppUtil.FIRST_FOLDER, "matchlogs");
            if (!dir.exists() && !dir.mkdirs()) {
                logPath = "(could not create matchlogs)";
                return;
            }
            String stamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US)
                    .format(new Date());
            File f = new File(dir, "bringup-" + stamp + ".csv");
            log = new FileWriter(f);
            log.write("time_s,test,event,note,fl,fr,bl,br,"
                    + "cat_ticks,cat_amps,release_ticks,heading_deg\n");
            log.flush();
            logPath = f.getName();
        } catch (IOException e) {
            log = null;
            logPath = "(log failed: " + e.getMessage() + ")";
            RobotLog.ee("BringUp", e, "could not open log");
        }
    }

    private void maybeLog() {
        if (logTimer.seconds() < LOG_INTERVAL_SEC) return;
        logTimer.reset();
        writeRow("SAMPLE", "");
    }

    private void writeRow(String event, String note) {
        if (log == null) return;
        try {
            log.write(String.format(Locale.US,
                    "%.3f,%d,%s,%s,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%d,%.1f\n",
                    runTime.seconds(), test, event, note.replace(',', ';'),
                    fl.getPower(), fr.getPower(), bl.getPower(), br.getPower(),
                    catapult.getCurrentPosition(),
                    catapult.getCurrent(CurrentUnit.AMPS),
                    releaseTicks,
                    drive == null ? 0.0 : drive.getHeadingDeg()));
            // Flushed every row: a bring-up run usually ends with the stop
            // button or a battery pull, and an unflushed buffer is a lost run.
            log.flush();
        } catch (IOException e) {
            RobotLog.ee("BringUp", e, "log write failed");
        }
    }

    private void closeLog() {
        if (log == null) return;
        try { log.close(); } catch (IOException ignored) { }
        log = null;
    }
}
