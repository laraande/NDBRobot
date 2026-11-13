package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
public class MecanumDrive {

    // ===== SETTINGS =====
    // Measured on the robot with RobotBringUp test 1 on 2026-11-12. Each motor
    // was run at FORWARD with no direction applied, and the wheel that moved
    // was noted along with which way it would have driven the robot:
    //
    //     front left   spun forward   -> FORWARD
    //     front right  spun BACKWARD  -> REVERSE
    //     back left    spun forward   -> FORWARD
    //     back right   spun forward   -> FORWARD
    //
    // Only the front right is physically reversed. To re-measure after any
    // rewiring, run "0 BringUp" test 1 again: a wheel that drives the robot
    // backward needs REVERSE here, one that drives it forward needs FORWARD.
    static final DcMotorSimple.Direction FL_DIR = DcMotorSimple.Direction.FORWARD;
    static final DcMotorSimple.Direction BL_DIR = DcMotorSimple.Direction.FORWARD;
    static final DcMotorSimple.Direction FR_DIR = DcMotorSimple.Direction.REVERSE;
    static final DcMotorSimple.Direction BR_DIR = DcMotorSimple.Direction.FORWARD;

    // How the Control Hub is mounted on the robot (field-centric only).
    //
    // Measured on the robot 2026-11-12 with "0 BringUp" test 4, not guessed.
    //
    // The Control Hub stands VERTICALLY on edge above the Expansion Hub, with
    // its USB/HDMI port edge facing straight up and its logo face in a
    // vertical plane. The front of the robot is the end away from the
    // catapult.
    //
    // Test 4 result: zeroing the heading and turning the robot left gave
    // +58 degrees at a quarter turn. The log (logs/bringup-2026-11-12-231838.csv)
    // shows three turns, two left and one right, reading +150, +101 and -117,
    // tracking smoothly across the full -180..+180 range and holding steady
    // when stationary. Left turns read positive, which is what the SDK
    // expects, so this pair is correct.
    //
    // To re-check after remounting the hub: run test 4, zero with START, turn
    // left. Positive means these are right. Negative means the logo is 180
    // degrees out (swap LEFT and RIGHT). A heading that barely moves means the
    // logo points along a different axis entirely.
    static final RevHubOrientationOnRobot.LogoFacingDirection LOGO_DIR =
            RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
    static final RevHubOrientationOnRobot.UsbFacingDirection USB_DIR =
            RevHubOrientationOnRobot.UsbFacingDirection.UP;

    static final double STRAFE_CORRECTION = 1.1; // strafing is less efficient than driving

    private final DcMotorEx fl, fr, bl, br;
    private IMU imu;
    private final boolean fieldCentric;

    public MecanumDrive(HardwareMap hw, boolean fieldCentric) {
        this.fieldCentric = fieldCentric;

        fl = hw.get(DcMotorEx.class, "front left");
        fr = hw.get(DcMotorEx.class, "front right");
        bl = hw.get(DcMotorEx.class, "back left");
        br = hw.get(DcMotorEx.class, "back right");

        fl.setDirection(FL_DIR);
        bl.setDirection(BL_DIR);
        fr.setDirection(FR_DIR);
        br.setDirection(BR_DIR);

        for (DcMotorEx m : new DcMotorEx[]{fl, fr, bl, br}) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        if (fieldCentric) {
            imu = hw.get(IMU.class, "imu");
            imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(LOGO_DIR, USB_DIR)));
            imu.resetYaw();
        }
    }

    /** Make the robot's current facing "forward" for field-centric driving. */
    public void resetHeading() {
        if (fieldCentric) imu.resetYaw();
    }

    public double getHeadingDeg() {
        return fieldCentric ? imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES) : 0.0;
    }

    /**
     * @param forward  -1..1 (+ = forward)
     * @param strafe   -1..1 (+ = right)
     * @param turn     -1..1 (+ = clockwise)
     * @param scale    0..1 overall speed limit (slow mode)
     */
    public void drive(double forward, double strafe, double turn, double scale) {
        if (fieldCentric) {
            double h = -imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            double rotStrafe  = strafe * Math.cos(h) - forward * Math.sin(h);
            double rotForward = strafe * Math.sin(h) + forward * Math.cos(h);
            strafe = rotStrafe;
            forward = rotForward;
        }

        strafe *= STRAFE_CORRECTION;

        double denom = Math.max(Math.abs(forward) + Math.abs(strafe) + Math.abs(turn), 1.0);

        fl.setPower(scale * (forward + strafe + turn) / denom);
        bl.setPower(scale * (forward - strafe + turn) / denom);
        fr.setPower(scale * (forward - strafe - turn) / denom);
        br.setPower(scale * (forward + strafe - turn) / denom);
    }

    public void stop() {
        drive(0, 0, 0, 0);
    }
}
