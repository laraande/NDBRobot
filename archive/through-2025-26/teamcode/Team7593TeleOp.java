package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.util.ArrayList;

@TeleOp (name = "TeleOp")
public class Team7593TeleOp extends Team7593OpMode {

    double position1 = robot.HOME;
    double position2 = robot.MAX;
    final double SPEED = 0.02; //sets rate to move servo


    Orientation angles; //to use the imu (mostly for telemetry)

    //double extensionPosition = robot.EXTENSION_HOME;
    final double EXTENSION_SPEED = 0.08; //sets rate to move servo

    @Override
    //code block to that will run at the VERY beginning of Teleop
    public ArrayList<AutonStep> createAutonSteps() {
        return null;
    }

    @Override
    public void init(){
        super.init();
        telemetry.addData("Say", "hi");
        telemetry.update();

        //stop the motor(s) and reset the motor encoders to 0
        robot.linearSlide.setPower(0);
        robot.linearSlideL.setPower(0);

        // robot.linearSlide.setDirection(DcMotor.Direction.REVERSE);
        robot.linearSlide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.linearSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        // robot.linearSlideL.setDirection(DcMotor.Direction.REVERSE);
        robot.linearSlideL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.linearSlideL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        //get the starting encoder value of tilt (this is so we don't assume the starting econder value is 0)
        // mike-obsolete  oldEncoderVal = robot.linearSlide.getCurrentPosition();

        // telemetry.addData("Say", "HELLO FROM THE OTHER SIDE");
        // telemetry.addData("Say", "running");

        //add 1/14
        telemetry.addLine();
        telemetry.update();
    }

    int counter = 0;

    @Override
    public void loop(){
        counter++;
        // telemetry.addData("Count", "%d", counter);
        // telemetry.update();

        //use sarmer's loop so that auton steps can run
        super.loop();
        //return;

        //get the current encoder value of tilt
        // mike currEncoderVal = robot.linearSlide.getCurrentPosition();
        //return;

        //double maxSlidePosition = 500;
        double leftX, rightX, leftY, linearSlide, runHoist; //declaration for the game sticks + power
        int linearSlidePosition, linearSlidePositionL, liftScale;
        boolean setLaunchAngle,powerLift, powerLift1, slowDrive, slowDrive2, clawOpen, clawClose, launchLock, launchUnlock; //declaration for the buttons/bumpers
        WheelSpeeds speeds; //variable to hold speeds

        // get teh 
        leftX = gamepad1.left_stick_x; //must keep
        rightX = gamepad1.right_stick_x; //must keep
        leftY = gamepad1.left_stick_y;
        slowDrive = gamepad1.left_bumper;
        slowDrive2 = gamepad1.right_bumper;
        powerLift = gamepad2.right_bumper;
        powerLift1 = gamepad2.left_bumper;

        linearSlide = gamepad2.right_stick_y;
        runHoist = gamepad2.left_stick_y;
        clawOpen = gamepad2.x;
        clawClose = gamepad2.y;
        launchLock = gamepad1.a;
        launchUnlock = gamepad1.b;

        
        //get the speeds
        if(slowDrive || slowDrive2){
            speeds = WheelSpeeds.mecanumDrive(leftX, leftY, rightX, true);
        }else{
            speeds = WheelSpeeds.mecanumDrive(leftX, leftY, rightX, false);
        }

        robot.powerTheWheels(speeds);

        // activate more power using gamepad bumpers - doesn't seem to help
        if(powerLift || powerLift1){
            robot.linearSlide.setPower(0.9);
            robot.linearSlideL.setPower(0.9);
            liftScale = 5;
        } else {
            robot.linearSlide.setPower(0.9);
            robot.linearSlideL.setPower(0.9);
            liftScale = 10;
        }


        //claw
        /*if (clawOpen) {
            robot.clawRight.setPosition(.48);
            robot.clawLeft.setPosition(.42);
        } else if (clawClose) {
            robot.clawRight.setPosition(.85);
            robot.clawLeft.setPosition(0.0);
        }*/

        // airplane launcher locking pin servo contorl
        if (launchLock) {
            robot.launcher.setPosition(.85);
        } else if (launchUnlock) {
            robot.launcher.setPosition(.01);
        }

        // hoistMotor control (to tighten/loosen the lifting line)
        robot.hoistMotor.setPower(runHoist);

        linearSlidePosition = robot.linearSlide.getCurrentPosition();
        linearSlidePositionL = robot.linearSlideL.getCurrentPosition();

    
        // move the arm using the joystick    
        if (linearSlide > 0) {
            linearSlidePosition -= 1 * liftScale;
            linearSlidePositionL += 1 * liftScale;
        }
        if (linearSlide < 0) {
            linearSlidePosition += 1 * liftScale;
            linearSlidePositionL -= 1 * liftScale;
        }

        robot.linearSlide.setTargetPosition(linearSlidePosition);
        robot.linearSlideL.setTargetPosition(linearSlidePositionL);
        robot.linearSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        robot.linearSlideL.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        //robot.waitForNextHardwareCycle();
        //robot.linearSlide.setPower(0);
        //robot.linearSlideL.setPower(0);

        telemetry.addData(
              "Say", "linearSlide is at %d target %d stick at %f",
              robot.linearSlide.getCurrentPosition(),
              linearSlidePosition,
              linearSlide
         );
        telemetry.addData(
              "Say", "linearSlideL is at %d target %d stick at %f",
              robot.linearSlideL.getCurrentPosition(),
              linearSlidePositionL,
              linearSlide
         );

        /*
        WHOA TELEMETRY
        */

        telemetry.update();
        

    }

}
