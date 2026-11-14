package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/**
 * Competition TeleOp: driving + catapult, NO ENCODER NEEDED.
 *
 * Built for the 2026-11-14 event, when the catapult encoder cable is missing.
 * Nothing in here reads a motor position, so it works with power cables alone.
 *
 * The catapult runs OPEN LOOP: power for a set time, cut early if the motor
 * current spikes (which means the arm has reached a hard stop). Power is
 * scaled by battery voltage so shots stay similar as the battery drains.
 *
 * The whole catapult sequence is a state machine that advances one step per
 * loop. It never blocks, so the driver keeps full control of the robot during
 * a shot.
 *
 * GAMEPAD 1 (driver)
 *   left stick        drive / strafe
 *   right stick X     turn
 *   right trigger     HOLD for slow mode (precise lining up)
 *   back              toggle field-centric / robot-centric (starts robot-centric)
 *   start             re-zero the heading (point the robot downfield first)
 *
 * GAMEPAD 2 (operator)
 *   A                 fire (then settles and auto-returns home)
 *   B                 STOP the catapult immediately - overrides everything
 *   dpad up / down    jog the arm by hand
 *   dpad right/left   fire power  +/- 0.05   (tuning, shown on screen)
 *   right/left bumper fire time   +/- 25 ms  (tuning, shown on screen)
 *
 * Tuned values reset every time the op mode starts. When you find good ones,
 * write them into FIRE_POWER / FIRE_MS below and rebuild. See
 * docs/COMPETITION-TELEOP.md for the tuning procedure.
 */
@TeleOp(name = "1 Competition", group = "Competition")
public class CompetitionTeleOp extends LinearOpMode {

    // ===================== DRIVE =====================

    /**
     * Normal driving speed multiplier.
     *
     * NOT 1.00 on purpose. Tested on blocks 2026-11-13: at full power the
     * wheels just spin and the robot is not drivable. Lower this if the base
     * still breaks traction, raise it if the drivers want more once they are
     * used to it. Tune it in practice, not at the event.
     */
    static final double DRIVE_FULL = 0.75;
    /**
     * Turning is capped harder than driving. Rotating a mecanum base scrubs
     * all four wheels sideways, so it loses grip and feels twitchy well
     * before straight-line driving does.
     */
    static final double TURN_SCALE = 0.60;
    /** Slow-mode multiplier, used while gamepad 1's right trigger is held. */
    static final double DRIVE_SLOW = 0.35;
    /**
     * How far the right trigger must be pulled to count as "held".
     * Gamepad 1's LEFT BUMPER does the same thing: a definite click the
     * driver can find without looking, where the trigger is an analog
     * squeeze. Either one gives slow mode.
     */
    static final double SLOW_TRIGGER = 0.5;
    /** Stick movement below this is treated as zero (drift and thumb rest). */
    static final double STICK_DEADZONE = 0.08;
    /** Start in robot-centric. Gamepad 1 BACK toggles this during the match. */
    static final boolean START_FIELD_CENTRIC = false;

    // =================== CATAPULT (tune these) ===================
    // Everything below was chosen from the 2026-11-12 bring-up notes and is a
    // STARTING POINT. Tune on the robot with the live controls, then write the
    // values you settle on back into these constants.

    /** Positive power throws. Measured on the robot, do not flip blindly. */
    static final DcMotorSimple.Direction CATAPULT_DIR = DcMotorSimple.Direction.FORWARD;

    /** Power used for a throw, before voltage compensation. */
    static final double FIRE_POWER = 1.00;
    /** Longest a throw may run. The current cutoff normally ends it sooner. */
    static final double FIRE_MS = 300;
    /** Quiet pause after the throw, before the arm starts coming back. */
    static final double SETTLE_MS = 200;
    /** Gentle power for the return. Negative = back toward home. */
    static final double RETURN_POWER = -0.30;
    /** Longest the return may run before giving up and going idle. */
    static final double RETURN_MS = 1500;
    /** Manual jog power. 0.25 only whined at a loaded arm; 0.55 moves it. */
    static final double JOG_POWER = 0.55;
    /**
     * Small power held at idle to keep the arm seated at home. 0 = off.
     * If you set this, keep it tiny (e.g. -0.08). Anything larger cooks the
     * motor against the home stop for the whole match.
     */
    static final double REST_HOLD = 0.0;

    // --- Current limits -------------------------------------------------
    // The only feedback this robot has. Normal running was seen up to ~4 A;
    // the 19.2:1 motor stalls at ~9.2 A. Watch "peak A" on screen while
    // firing and set FIRE_STALL_AMPS between normal running and stall.

    /** Current that means "the arm hit the fire-end stop". Set 99 to disable. */
    static final double FIRE_STALL_AMPS = 7.0;
    /**
     * Current that means "the arm is back home against the rest stop".
     * MUST be lower than FIRE_STALL_AMPS: the return runs at a much lower
     * power, so it draws far less current even when fully stalled. If this is
     * set too high the arm never "arrives" and grinds against the home stop
     * for the whole of RETURN_MS.
     */
    static final double RETURN_STALL_AMPS = 3.0;
    /** Current that aborts a manual jog, so a held dpad cannot stall the motor. */
    static final double JOG_STALL_AMPS = 6.0;
    /** Ignore current readings for this long after a move starts (inrush spike). */
    static final double IGNORE_MS = 80;

    /** Battery voltage the powers above are written for. */
    static final double NOMINAL_VOLTS = 12.5;
    /** Below this the voltage reading is treated as bogus and not used to scale. */
    static final double MIN_SANE_VOLTS = 6.0;

    // Tuning step sizes for the live controls.
    static final double POWER_STEP = 0.05;
    static final double TIME_STEP_MS = 25;
    static final double MIN_FIRE_POWER = 0.10;
    static final double MIN_FIRE_MS = 25;

    /** Telemetry refresh. The default 250 ms is too slow to read peak amps. */
    static final int TELEMETRY_MS = 50;

    // =====================================================================

    private enum Cat { IDLE, FIRING, SETTLE, RETURNING, JOG }

    private DcMotorEx catapult;
    private VoltageSensor battery;
    private Cat state = Cat.IDLE;

    /** Times the current catapult state. Reset on every state change. */
    private final ElapsedTime timer = new ElapsedTime();

    private double firePower = FIRE_POWER;
    private double fireMs = FIRE_MS;
    private double peakAmps = 0;
    private String lastShot = "none yet";

    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap, START_FIELD_CENTRIC);

        catapult = hardwareMap.get(DcMotorEx.class, "catapult");
        catapult.setDirection(CATAPULT_DIR);
        // No encoder cable, so RUN_WITHOUT_ENCODER is the only usable mode.
        catapult.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        catapult.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        battery = hardwareMap.voltageSensor.iterator().next();

        // One bulk read per loop instead of a separate call per sensor. Keeps
        // the loop fast enough that the current cutoff reacts in time.
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        telemetry.setMsTransmissionInterval(TELEMETRY_MS);
        telemetry.addLine("Ready. Arm should be resting at home.");
        telemetry.addData("Battery", "%.2f V", battery.getVoltage());
        telemetry.addLine("GP1 back = field-centric toggle, start = re-zero heading");
        telemetry.update();

        waitForStart();

        // Previous-frame button states, for rising-edge detection.
        boolean pA = false;
        boolean pDpadL = false, pDpadR = false, pLB = false, pRB = false;
        boolean pBack = false, pStart = false;

        // Everything from here to the finally block must stay non-blocking.
        // The only loop is this one, and it is bounded by opModeIsActive().
        try {
            while (opModeIsActive()) {

                // ---------- DRIVE (gamepad 1) ----------
                if (gamepad1.back && !pBack) {
                    drive.setFieldCentric(!drive.isFieldCentric());
                }
                if (gamepad1.start && !pStart) {
                    drive.resetHeading();
                }
                pBack = gamepad1.back;
                pStart = gamepad1.start;

                // Slow mode: right trigger OR left bumper, whichever the driver
                // reaches for. Both are hold-to-engage, not a toggle, so
                // letting go always returns to normal speed.
                boolean slowRequested =
                        gamepad1.right_trigger > SLOW_TRIGGER || gamepad1.left_bumper;
                double speed = slowRequested ? DRIVE_SLOW : DRIVE_FULL;
                // Turn is scaled again, on top of the overall speed limit.
                drive.drive(shape(-gamepad1.left_stick_y),
                            shape(gamepad1.left_stick_x),
                            shape(gamepad1.right_stick_x) * TURN_SCALE,
                            speed);

                // ---------- CATAPULT LIVE TUNING (gamepad 2) ----------
                if (gamepad2.dpad_right && !pDpadR) {
                    firePower = Math.min(1.0, firePower + POWER_STEP);
                }
                if (gamepad2.dpad_left && !pDpadL) {
                    firePower = Math.max(MIN_FIRE_POWER, firePower - POWER_STEP);
                }
                if (gamepad2.right_bumper && !pRB) {
                    fireMs += TIME_STEP_MS;
                }
                if (gamepad2.left_bumper && !pLB) {
                    fireMs = Math.max(MIN_FIRE_MS, fireMs - TIME_STEP_MS);
                }
                pDpadR = gamepad2.dpad_right;
                pDpadL = gamepad2.dpad_left;
                pRB = gamepad2.right_bumper;
                pLB = gamepad2.left_bumper;

                // ---------- CATAPULT STATE MACHINE ----------
                double amps = catapult.getCurrent(CurrentUnit.AMPS);
                boolean settled = timer.milliseconds() > IGNORE_MS;

                if (gamepad2.b) {
                    // Emergency stop. Unconditional, and it returns to the top
                    // of the loop so no other branch can re-power the motor in
                    // the same pass. Releasing B leaves the arm wherever it is.
                    catapult.setPower(0);
                    state = Cat.IDLE;
                    pA = gamepad2.a;
                    showTelemetry(drive, amps, speed);
                    continue;
                }

                // A fires, but only from a standstill.
                if (gamepad2.a && !pA && state == Cat.IDLE) {
                    peakAmps = 0;
                    setCat(firePower);
                    timer.reset();
                    state = Cat.FIRING;
                    settled = false;
                }
                pA = gamepad2.a;

                switch (state) {
                    case IDLE:
                    case JOG:
                        if (gamepad2.dpad_up) {
                            startOrContinueJog(JOG_POWER, amps);
                        } else if (gamepad2.dpad_down) {
                            startOrContinueJog(-JOG_POWER, amps);
                        } else if (state == Cat.JOG) {
                            // Jog released: back to rest.
                            setCat(REST_HOLD);
                            state = Cat.IDLE;
                        }
                        break;

                    case FIRING:
                        if (settled) {
                            peakAmps = Math.max(peakAmps, amps);
                        }
                        boolean hitStop = settled && amps > FIRE_STALL_AMPS;
                        if (hitStop || timer.milliseconds() >= fireMs) {
                            lastShot = String.format("%s at %.0f ms, peak %.1f A",
                                    hitStop ? "hit stop" : "timed out",
                                    timer.milliseconds(), peakAmps);
                            catapult.setPower(0);
                            timer.reset();
                            state = Cat.SETTLE;
                        }
                        break;

                    case SETTLE:
                        if (timer.milliseconds() >= SETTLE_MS) {
                            setCat(RETURN_POWER);
                            timer.reset();
                            state = Cat.RETURNING;
                        }
                        break;

                    case RETURNING:
                        // RETURN_STALL_AMPS is deliberately lower than the fire
                        // cutoff: the return runs at ~0.30 power, so hitting the
                        // home stop draws much less current than a full-power
                        // stall. Too high a threshold here means the arm never
                        // registers as home and grinds for the whole timeout.
                        boolean home = settled && amps > RETURN_STALL_AMPS;
                        if (home || timer.milliseconds() >= RETURN_MS) {
                            setCat(REST_HOLD);
                            state = Cat.IDLE;
                        }
                        break;
                }

                showTelemetry(drive, amps, speed);
            }
        } finally {
            // R10 / G403 / G404: nothing may be powered once the op mode ends,
            // including when it ends because of an exception or a stop press.
            drive.stop();
            catapult.setPower(0);
        }
    }

    /**
     * Drive the arm by hand, with a current cutoff so that holding the dpad
     * against a hard stop cannot sit there stalling the motor.
     */
    private void startOrContinueJog(double power, double amps) {
        if (state != Cat.JOG) {
            timer.reset();
            state = Cat.JOG;
            peakAmps = 0;
        }
        boolean settled = timer.milliseconds() > IGNORE_MS;
        if (settled) {
            peakAmps = Math.max(peakAmps, amps);
        }
        if (settled && amps > JOG_STALL_AMPS) {
            // Against a stop. Cut power but stay in JOG so it does not instantly
            // retry; the operator must release the dpad to try again.
            catapult.setPower(0);
        } else {
            setCat(power);
        }
    }

    private void showTelemetry(MecanumDrive drive, double amps, double speed) {
        double cutoff;
        switch (state) {
            case RETURNING: cutoff = RETURN_STALL_AMPS; break;
            case JOG:       cutoff = JOG_STALL_AMPS;    break;
            default:        cutoff = FIRE_STALL_AMPS;   break;
        }
        telemetry.addData("Catapult", state);
        telemetry.addData("Fire power", "%.2f   (dpad L/R)", firePower);
        telemetry.addData("Fire time", "%.0f ms (bumpers)", fireMs);
        telemetry.addData("Last shot", lastShot);
        telemetry.addData("Motor A", "%.1f  (cutoff %.1f)  peak %.1f", amps, cutoff, peakAmps);
        telemetry.addData("Battery", "%.2f V", battery.getVoltage());
        telemetry.addData("Drive", "%s (%.2f), %s",
                speed < DRIVE_FULL ? "SLOW" : "normal", speed,
                drive.isFieldCentric() ? "field-centric" : "robot-centric");
        telemetry.addData("Heading", "%.0f deg (GP1 start to re-zero)", drive.getHeadingDeg());
        telemetry.update();
    }

    /**
     * Set catapult power, scaled so a tired battery still throws about as hard
     * as a fresh one. A 1.00 command on an 11.5 V battery becomes 1.00 after
     * clamping, so compensation only really bites below full power.
     */
    private void setCat(double power) {
        double v = battery.getVoltage();
        double scaled = (v > MIN_SANE_VOLTS) ? power * NOMINAL_VOLTS / v : power;
        catapult.setPower(Math.max(-1.0, Math.min(1.0, scaled)));
    }

    /**
     * Small deadzone, then squared keeping the sign: fine control near centre,
     * full speed at full stick.
     */
    private double shape(double v) {
        if (Math.abs(v) < STICK_DEADZONE) return 0;
        return v * Math.abs(v);
    }
}
