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
     * NOT 1.00 on purpose. At full power the wheels just spin and the robot
     * is not drivable.
     *
     * 0.75 was tried on the robot on 2026-11-13 and confirmed good by the
     * driver: "a lot less twitchy". Checked on blocks only, so if it feels
     * different on the floor this is the number to change. Raise it if the
     * drivers want more once they are used to it. Tune it in practice, not
     * at the event.
     */
    static final double DRIVE_FULL = 0.75;
    /**
     * Turning is capped harder than driving. Rotating a mecanum base scrubs
     * all four wheels sideways, so it loses grip and feels twitchy well
     * before straight-line driving does.
     *
     * 0.60 confirmed by the driver on 2026-11-13. Combined with DRIVE_FULL
     * that is 0.45 at full stick, which is where the twitchiness went.
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

    /**
     * Power used for a throw, before voltage compensation.
     *
     * DELIBERATELY LOW for first testing (2026-11-13). The launch stopper is
     * newly fitted and FIRE_STALL_AMPS has never been verified on this motor,
     * so a full-power shot could drive into the stopper for the remainder of
     * FIRE_MS before anything cut it. Start gentle, read the peak amps off
     * the screen, then raise this with dpad right until the throw is right.
     */
    static final double FIRE_POWER = 1.00;
    /**
     * Longest a throw may run. The current cutoff normally ends it sooner.
     *
     * MEASURED 2026-11-13, after the stopper and launcher were adjusted: a
     * full-power shot reaches the stopper at 171 ms ("hit stop at 171 ms,
     * peak 8.1 A"). 200 ms leaves a small margin over that without a long
     * dwell driving into the stopper if a tired battery slows the arm.
     *
     * The current cutoff is what normally ends the shot; this is the backstop.
     * If shots start reading "timed out" instead of "hit stop", the arm is no
     * longer reaching the stopper -- check the battery before changing this.
     */
    static final double FIRE_MS = 200;
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

    /**
     * Current that means "the arm hit the fire-end stop". Set 99 to disable.
     *
     * MEASURED 2026-11-13, after the launch stopper was fitted:
     *   0.50 power, arm never reaches the stopper -> peak 3.4-3.5 A
     *   0.70 power, arm DOES reach the stopper    -> peak 5.7 A
     *
     * Free-running at 0.70 scales to about 4.9 A, so the extra ~0.8 A is the
     * arm loading against the stopper. 5.3 sits between the two: high enough
     * not to trip on a clean throw, low enough to catch stopper contact.
     *
     * The old value of 7.0 was an unverified guess and was never reached, so
     * shots read "timed out" while the motor kept driving into the bracket
     * for the rest of FIRE_MS. That is what this fixes.
     *
     * REVISED 2026-11-13 after testing at full power: 5.3 was measured at
     * 0.70 power and turned out to trip DURING the throw at 1.00, cutting
     * shots short. A full-power shot with the cutoff disabled went clearly
     * further than the same shot with it on. Raised to 8.0, below the ~9.2 A
     * stall of this motor but above anything a clean full-power throw draws.
     *
     * The lesson: a current threshold measured at one power does not transfer
     * to another, because free-running current scales with power. If
     * FIRE_POWER changes, re-measure this.
     *
     * Re-measure if the power, the spring or the stopper position changes:
     * the threshold is specific to all three.
     */
    static final double FIRE_STALL_AMPS = 8.0;
    /**
     * Current that means "the arm is back home against the rest stop".
     *
     * History worth keeping: on the PREVIOUS build the arm rested against the
     * home stop under spring tension and drew 4.2 A at zero commanded power,
     * through BRAKE mode. This was raised to 5.5 to clear that.
     *
     * After the catapult was rebuilt with a launch stopper (2026-11-13) the
     * cocked arm reads 0.0 A -- it is held mechanically, not by the motor.
     * So the 4.2 A floor no longer applies and 5.5 may now be too HIGH to
     * detect the home stop on the return, in which case the return simply
     * runs the full RETURN_MS instead of stopping early. That is untidy but
     * not dangerous; the return runs at only 0.30 power.
     *
     * Lower this once a real return-into-home peak has been read off the
     * screen. It must stay below FIRE_STALL_AMPS.
     */
    static final double RETURN_STALL_AMPS = 5.5;
    /**
     * Current that aborts a manual jog, so a held dpad cannot stall the motor.
     * Also above the 4.2 A resting reading, for the same reason.
     */
    static final double JOG_STALL_AMPS = 6.5;

    // --- End-of-arc power taper -----------------------------------------
    /**
     * Fraction of FIRE_MS after which the throw starts easing off power.
     *
     * ADDED 2026-11-13. Firing at full power until the instant of contact
     * means the gearbox absorbs peak torque and peak speed in the same
     * moment, every single shot, and this robot has no fire-end hard stop to
     * take that load instead. Tapering costs a little release speed and buys
     * a large reduction in shock through the output stage and the stopper
     * bracket's fasteners.
     *
     * 0.75 of a 200 ms FIRE_MS starts the taper at 150 ms, just before the
     * 171 ms stop time measured on 2026-11-13. Set TAPER_START_FRAC to 1.0
     * to disable the taper entirely and go back to flat full power.
     *
     * This is open-loop and time-based because there is no encoder: it
     * assumes the arm takes roughly FIRE_MS to cross the arc. If the battery
     * sags or the stopper moves, the taper drifts relative to actual contact.
     * Re-check the logged stop time after changing FIRE_POWER or FIRE_MS.
     */
    static final double TAPER_START_FRAC = 0.75;
    /**
     * Power multiplier at the very end of the taper. 1.0 = no taper.
     *
     * Not lower than this: the arm still has to reach the stopper and trip
     * the current cutoff. Too deep a taper and shots read "timed out"
     * instead of "hit stop" because the arm arrives too softly.
     */
    static final double TAPER_END_SCALE = 0.55;
    /**
     * Ignore current readings for this long after a move starts (inrush spike).
     *
     * LOWERED from 80 to 25 on 2026-11-13. 80 ms was far more blind time than
     * the inrush actually needs, and it was hiding the event we most wanted to
     * measure: no "hit stop" can be reported before this many milliseconds, so
     * an arm that reaches the stopper early kept driving at full power until
     * the window expired. That is exactly the load the gearbox should not see.
     *
     * It also made the logged stop time unusable as a measurement. The
     * "hit stop at 171 ms" reading that the arm-length analysis was based on
     * could have been any contact from 80 ms onward, which is why no single
     * constant-acceleration fit matched the ball's measured flight.
     *
     * The inrush spike on a 5203 is largely over within 20 ms. 25 leaves a
     * small margin. If shots start cutting out at the very start of the throw,
     * raise this in 5 ms steps -- do not go back to 80.
     */
    static final double IGNORE_MS = 25;

    /**
     * Longest a single manual jog may run, in milliseconds.
     *
     * There is NO physical hard stop at the fire end of this robot (confirmed
     * 2026-11-13). Without one, a held dpad has nothing to stall against, so
     * the current cutoff may never trip and the arm can be driven past the end
     * of its useful travel into the linkage.
     *
     * This bounds every jog by time as well as current. Release and press
     * again to continue moving; that is deliberate, so the arm cannot be
     * walked to the end of its range by leaning on the dpad.
     *
     * Raise it once a fire-end hard stop is fitted, or lower it if 600 ms
     * still moves the arm too far in one press.
     */
    static final double JOG_MAX_MS = 600;

    /** Battery voltage the powers above are written for. */
    static final double NOMINAL_VOLTS = 12.5;
    /** Below this the voltage reading is treated as bogus and not used to scale. */
    static final double MIN_SANE_VOLTS = 6.0;

    // Tuning step sizes for the live controls.
    static final double POWER_STEP = 0.05;
    static final double TIME_STEP_MS = 25;
    /**
     * Gamepad 2 X toggles the fire cutoff off entirely, for experiments.
     * With it OFF the shot runs the full FIRE_MS no matter what the current
     * does, so the motor WILL drive into the stopper for any time left after
     * the arm arrives. Use it to find out what the mechanism can do, not for
     * a match. The screen shows CUTOFF OFF in capitals while it is disabled.
     */
    static final double CUTOFF_DISABLED_AMPS = 99.0;
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
    private boolean cutoffOn = true;
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
        boolean pDpadL = false, pDpadR = false, pLB = false, pRB = false, pX = false;
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
                if (gamepad2.x && !pX) cutoffOn = !cutoffOn;
                pX = gamepad2.x;
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
                        // Ease off power toward the end of the arc so the
                        // stopper and the gearbox do not take peak torque and
                        // peak speed at the same instant. Re-commanded every
                        // pass because setCat() also rescales for battery sag.
                        setCat(firePower * taperScale(timer.milliseconds()));
                        boolean hitStop = settled && amps > fireCutoff();
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
        boolean overCurrent = settled && amps > JOG_STALL_AMPS;
        boolean tooLong = timer.milliseconds() >= JOG_MAX_MS;
        if (overCurrent || tooLong) {
            // Either the arm is against something, or this press has run long
            // enough. Cut power but stay in JOG so it does not instantly
            // retry; the operator must release the dpad to try again.
            //
            // The time limit matters because this robot has no fire-end hard
            // stop, so a held dpad would otherwise have nothing to stop it.
            catapult.setPower(0);
        } else {
            setCat(power);
        }
    }

    /**
     * Power multiplier for where we are through the throw: 1.0 for the first
     * TAPER_START_FRAC of FIRE_MS, then ramping linearly down to
     * TAPER_END_SCALE at the end. Open-loop; see TAPER_START_FRAC.
     */
    private double taperScale(double elapsedMs) {
        double start = fireMs * TAPER_START_FRAC;
        if (elapsedMs <= start) return 1.0;
        if (fireMs <= start) return TAPER_END_SCALE;
        double through = (elapsedMs - start) / (fireMs - start);
        through = Math.max(0.0, Math.min(1.0, through));
        return 1.0 - through * (1.0 - TAPER_END_SCALE);
    }

    /** The cutoff actually in force, honouring the gamepad 2 X override. */
    private double fireCutoff() {
        return cutoffOn ? FIRE_STALL_AMPS : CUTOFF_DISABLED_AMPS;
    }

    private void showTelemetry(MecanumDrive drive, double amps, double speed) {
        double cutoff;
        switch (state) {
            case RETURNING: cutoff = RETURN_STALL_AMPS; break;
            case JOG:       cutoff = JOG_STALL_AMPS;    break;
            default:        cutoff = fireCutoff();      break;
        }
        telemetry.addData("Catapult", state);
        telemetry.addData("Fire power", "%.2f   (dpad L/R)", firePower);
        telemetry.addData("Fire time", "%.0f ms (bumpers)", fireMs);
        telemetry.addData("Taper", TAPER_START_FRAC >= 1.0
                ? "off"
                : String.format("from %.0f ms down to %.0f%% power",
                        fireMs * TAPER_START_FRAC, TAPER_END_SCALE * 100));
        telemetry.addData("Last shot", lastShot);
        telemetry.addData("Cutoff", cutoffOn ? "on (X toggles)" : "*** CUTOFF OFF ***");
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
