package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.hardware.lynx.LynxModule;
import org.firstinspires.ftc.robotcore.external.State;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;


@TeleOp(name = "TeleOp2026", group = "Test")


public class CatapultOpMode extends LinearOpMode{
    // MOTOR / MECHANISM SETTINGS (change these)
    static final double MOTOR_TICKS_PER_REV = 28.0;  // 19.2:1 Yellow Jacket
    static final double EXTERNAL_GEAR_RATIO = 1.0;    // motor turns per arm turn
    static final DcMotorSimple.Direction MOTOR_DIRECTION = DcMotorSimple.Direction.FORWARD;

    static final double RELEASE_ANGLE_DEG = 45.0;     // where the motor cuts power
    static final double MAX_ANGLE_DEG     = 200.0;    // hard software limit (above release)
    static final double MIN_ANGLE_DEG     = -10.0;    // limit when jogging downward

    static final boolean USE_LIMIT_SWITCH = false;    // true if a touch sensor is wired
    static final boolean USE_GAMEPAD_2    = true;     // false = use gamepad 1

    //TUNING CONSTANTS
    static final double FIRE_POWER        = 1.0;      // throw power (1.0 = max)
    static final double RETURN_POWER      = 0.35;     // gentle return
    static final double HOMING_POWER      = -0.20;    // creep toward limit switch
    static final double JOG_POWER         = 1.00;     // manual jog speed
    static final double HOME_HOLD_POWER   = 0.0;      // >0 holds home against a spring/rubber band

    static final int    POSITION_TOL      = 10;       // ticks, for RUN_TO_POSITION
    static final int    HOME_READY_TOL    = 40;       // ticks from home allowed to fire

    static final double FIRE_HOLD_SEC     = 0.20;     // pause after release
    static final double FIRE_TIMEOUT_SEC  = 2.0;      // abort throw if not released
    static final double RETURN_TIMEOUT_SEC = 5.0;     // abort return
    static final double HOMING_TIMEOUT_SEC = 4.0;     // abort homing

    static final double STALL_CURRENT_A   = 99.0;      // amps (19.2:1 stall is ~9.2 A)
    static final double STALL_TIME_SEC    = 0.35;     // time above that current = jam
   

    static final int HOME_POSITION = 0;
    static final double TICKS_PER_ARM_DEG = MOTOR_TICKS_PER_REV * EXTERNAL_GEAR_RATIO / 360.0;
    static final int RELEASE_POSITION = 300;
    static final int MAX_POSITION     = RELEASE_POSITION + 100;
    static final int MIN_POSITION     = (int) Math.round(MIN_ANGLE_DEG * TICKS_PER_ARM_DEG);

    private DcMotorEx catapult;
    private TouchSensor limit;
    private Gamepad pad;
   

    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime stallTimer = new ElapsedTime();
    private int lastStallPos = 0;

    private enum State { IDLE, FIRING, HOLD, RETURNING, MANUAL, FAULT }
    private State state = State.IDLE;
    private String faultMessage = "";

    @Override
    public void runOpMode() {

        // ----- Hardware init -----
        catapult = hardwareMap.get(DcMotorEx.class, "catapult");
        if (USE_LIMIT_SWITCH) {
            limit = hardwareMap.get(TouchSensor.class, "catapultLimit");
        }
        pad = USE_GAMEPAD_2 ? gamepad2 : gamepad1;

        // Bulk reads: one hub read per loop instead of one per call. Makes position checks
        // (and so the release point) faster and more repeatable. AUTO clears the cache itself.
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        catapult.setDirection(MOTOR_DIRECTION);
        catapult.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        catapult.setTargetPositionTolerance(POSITION_TOL);

        telemetry.addLine("Put the arm at its HOME (down) position.");
        telemetry.addData("Release target (ticks)", RELEASE_POSITION);
        telemetry.addData("Max limit (ticks)", MAX_POSITION);
        telemetry.addLine("Press START to home.");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        homeCatapult();

        boolean aWasPressed = false;
        boolean bWasPressed = false;

        while (opModeIsActive()) {

            boolean a = pad.a;
            boolean b = pad.b;

            //Buttons (edge detected: one press = one action)
            if (a && !aWasPressed && state == State.IDLE) {
                if (isAtHome()) {
                    startFire();
                } else {
                    telemetry.addLine("Not at home - press B first");
                }
            }
            if (b && !bWasPressed) {
                startReturn();
            }
            aWasPressed = a;
            bWasPressed = b;

            // Manual jog
            if (state == State.IDLE || state == State.MANUAL) {
                int pos = catapult.getCurrentPosition();
                if (pad.dpad_up && pos < MAX_POSITION) {
                    manualJog(JOG_POWER);
                } else if (pad.dpad_down && pos > MIN_POSITION) {
                    manualJog(-JOG_POWER);
                } else if (state == State.MANUAL) {
                    stopMotor();
                    state = State.IDLE;
                }
            }

            // State machine
            int pos = catapult.getCurrentPosition();
            switch (state) {
                case FIRING:
                    if (pos >= RELEASE_POSITION) {
                        // Release: cut power and brake so the arm stops, piece flies
                        catapult.setPower(0);
                        timer.reset();
                        state = State.HOLD;
                    } else if (pos >= MAX_POSITION) {
                        fault("Travel limit reached");
                    } else if (timer.seconds() > FIRE_TIMEOUT_SEC) {
                        fault("Fire timeout - arm too slow?");
                    } else if (isStalled()) {
                        fault("Stall detected - arm jammed?");
                    }
                    break;

                case HOLD:
                    if (timer.seconds() >= FIRE_HOLD_SEC) {
                        startReturn();
                    }
                    break;

                case RETURNING:
                    if (isAtTarget()) {
                        finishReturn();
                    } else if (timer.seconds() > RETURN_TIMEOUT_SEC) {
                        fault("Return timeout");
                    } else if (isStalled()) {
                        fault("Stall detected during return");
                    }
                    break;

                case MANUAL:
                    if (isStalled()) {
                        fault("Stall detected while jogging");
                    }
                    break;

                default:
                    break;
            }

            // ----- Telemetry -----
            telemetry.addData("State", state);
            if (state == State.FAULT) {
                telemetry.addData("FAULT", faultMessage);
                telemetry.addLine("Press B to return home and clear");
            }
            telemetry.addData("Ready to fire", state == State.IDLE && isAtHome());
            telemetry.addData("Position (ticks)", pos);
            telemetry.addData("Arm angle (deg)", "%.1f", pos / TICKS_PER_ARM_DEG);
            telemetry.addData("Release at (deg)", RELEASE_ANGLE_DEG);
            telemetry.addData("Motor current (A)", "%.2f", catapult.getCurrent(CurrentUnit.AMPS));
            telemetry.addLine("A = fire | B = home/cancel | DPAD = jog");
            telemetry.update();
        }

        catapult.setPower(0);
    }

    // Helper methods

    private void startFire() {
        catapult.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        catapult.setPower(FIRE_POWER);
        timer.reset();
        stallTimer.reset();
        state = State.FIRING;
    }

    private void startReturn() {
        catapult.setTargetPosition(HOME_POSITION);
        catapult.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        catapult.setPower(RETURN_POWER);
        timer.reset();
        stallTimer.reset();
        state = State.RETURNING;
    }

    private void finishReturn() {
        if (HOME_HOLD_POWER > 0) {
            // stay in RUN_TO_POSITION and gently hold home
            catapult.setPower(HOME_HOLD_POWER);
        } else {
            stopMotor();
        }
        state = State.IDLE;
    }

    /** Manual jog without position control. */
    private void manualJog(double power) {
        catapult.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        catapult.setPower(power);
        if (state != State.MANUAL) stallTimer.reset();
        state = State.MANUAL;
    }

    /** Stop and brake. */
    private void stopMotor() {
        catapult.setPower(0);
        catapult.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }


    private void fault(String message) {
        stopMotor();
        faultMessage = message;
        state = State.FAULT;
    }

    private boolean isAtTarget() {
        return Math.abs(catapult.getTargetPosition() - catapult.getCurrentPosition()) <= POSITION_TOL;
    }

    private boolean isAtHome() {
        return Math.abs(catapult.getCurrentPosition() - HOME_POSITION) <= HOME_READY_TOL;
    }

    private boolean isStalled() {
    int pos = catapult.getCurrentPosition();
    double amps = catapult.getCurrent(CurrentUnit.AMPS);

    // Reset whenever the arm is actually moving or current is normal
    if (Math.abs(pos - lastStallPos) > 3 || amps < STALL_CURRENT_A) {
        lastStallPos = pos;
        stallTimer.reset();
        return false;
    }
    return stallTimer.seconds() > STALL_TIME_SEC;
    }

    private void homeCatapult() {
        if (USE_LIMIT_SWITCH) {
            catapult.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            timer.reset();
            catapult.setPower(HOMING_POWER);
            while (opModeIsActive() && !limit.isPressed()
                    && timer.seconds() < HOMING_TIMEOUT_SEC) {
                telemetry.addLine("Homing...");
                telemetry.update();
            }
            catapult.setPower(0);
        }
        catapult.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        catapult.setTargetPosition(HOME_POSITION);
        catapult.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        state = State.IDLE;
    }

}


