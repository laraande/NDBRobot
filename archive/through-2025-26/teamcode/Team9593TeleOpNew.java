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

@TeleOp (name = "TeleOpNew") // this is the name you select on the Driver Station
public class Team9593TeleOpNew extends Team7593OpMode {


    @Override
    //code block to that will run at the VERY beginning of Teleop
    public ArrayList<AutonStep> createAutonSteps() {
        return null;
    }

    @Override
    public void init(){
        super.init();
        telemetry.addData("Say", "hi");
        telemetry.addData("Say", "hi2");
        telemetry.addData("Say", "hi3");
        telemetry.update();

        //stop the motor(s) and reset the motor encoders to 0
        //robot.linearSlide.setPower(0);
        //robot.linearSlideL.setPower(0);

    }

    int counter = 0;

    @Override
    public void loop(){
        counter++;
//        telemetry.addData("Say", counter);
//        telemetry.update();

        super.loop();

        double leftX, rightX, leftY, slideStick, runHoist, slidePowerUp, slidePowerDown, hoistPower;//declaration for the game sticks + power
        boolean firstLoop,launchPosition, hangPosition, runHoistUp, runHoistDown,powerLift, powerLift1, slowDrive, slowDrive2, clawOpen, clawClose, launchLock, launchUnlock, setEndGameMode, endGameMode; //declaration for the buttons/bumpers
        boolean clawLower, clawRaise;
        int linearSlidePosition,linearSlidePositionL;
        boolean extendSlide, retractSlide;
        double slidePower;
        
        if (counter < 2) {
            //robot.openRight.setPosition(.85);
            //robot.openLeft.setPosition(0.0);
        }
        
        WheelSpeeds speeds; //variable to hold speeds

        // get the inputs from the game pad controllers 
        leftX = gamepad1.left_stick_x; //must keep
        rightX = gamepad1.right_stick_x; //must keep
        leftY = gamepad1.left_stick_y;
        slowDrive = gamepad1.left_bumper;
        slowDrive2 = gamepad1.right_bumper;
        powerLift = gamepad2.right_bumper;
        powerLift1 = gamepad2.left_bumper;

        slideStick = gamepad2.right_stick_y;
        //runHoist = gamepad2.left_stick_y;
        runHoist = 0;
        extendSlide = gamepad2.x;
        retractSlide = gamepad2.y;
        runHoistUp = gamepad2.dpad_up;
        runHoistDown = gamepad2.dpad_down;
        clawOpen = gamepad2.x;
        clawClose = gamepad2.y;
        clawRaise = gamepad2.b;
        clawLower = gamepad2.a;
//        launchLock = gamepad1.a;
//        launchUnlock = gamepad1.b;
//        launchPosition = gamepad2.a;
        setEndGameMode = gamepad1.b;

//        telemetry.addData("Say", "clawOpen = " + clawOpen);
//        telemetry.update();
        
        
        //get the speeds by checking if either bumper button on gamepad 1 are being pressed
        if(slowDrive || slowDrive2){
            speeds = WheelSpeeds.mecanumDrive(leftX, leftY, rightX, true);
        }else{
            speeds = WheelSpeeds.mecanumDrive(leftX, leftY, rightX, false);
        }

        robot.powerTheWheels(speeds);
        
        //set the end game mode if gamepad b button b is hit - we didn't end up using this bit
        if (setEndGameMode) {
            endGameMode = true;
        } 

//        telemetry.addData("openRight Servo Position before", robot.openRight.getPosition());
//        telemetry.addData("openLeft Servo Position before", robot.openLeft.getPosition());
        
        
        if (clawOpen) {
            double currentRightPosition = robot.openRight.getPosition();
            //telemetry.addData("currentRightPosition", currentRightPosition);
            double newRightPosition = Math.max(currentRightPosition - .001, 0.6);
            //telemetry.addData("newRightPosition", newRightPosition);
            //telemetry.update();
            double currentLeftPosition = robot.openLeft.getPosition();
            double newLeftPosition = Math.min(currentLeftPosition + .001, 0.3);
            robot.openRight.setPosition(newRightPosition);  
            robot.openLeft.setPosition(newLeftPosition);   
        } else if (clawClose) {
            telemetry.addData("close position start", robot.openRight.getPosition());
            robot.openRight.setPosition(.95);  
            robot.openLeft.setPosition(0.01);   
            //telemetry.addData("close position end", robot.openRight.getPosition());
            //telemetry.update();
        }

/*        if (clawRaise) {
            robot.raiseRight.setPosition(0.6);     
            robot.raiseLeft.setPosition(.1);      
        } else if (clawLower) {
            robot.raiseRight.setPosition(0.2);     
            robot.raiseLeft.setPosition(0.6);        
        }
*/
        
        if (clawRaise) {
            robot.raiseRight.setPosition(0.6);     
            robot.raiseLeft.setPosition(.1);      
        } else if (clawLower) {
            robot.raiseRight.setPosition(0);     
            robot.raiseLeft.setPosition(1);        
        }
        //telemetry.addData("openRight Servo Position after", robot.openRight.getPosition());
        //telemetry.addData("openLeft Servo Position after", robot.openLeft.getPosition());

/* jb
        //claw - if the button for clawOpen or clawClose are pressed then activate the servos to move to the correct position
        if (clawOpen) {
            robot.clawRight.setPosition(.48);
            robot.clawLeft.setPosition(.42);
        } else if (clawClose) {
            robot.clawRight.setPosition(.85);
            robot.clawLeft.setPosition(0.0);
        }

        // airplane launcher locking pin servo control
        if (launchLock) {
            robot.launcher.setPosition(.85);
        } else if (launchUnlock) {
            robot.launcher.setPosition(.01);
        }

        // move the arm using the joystick    
        // activate more power using gamepad bumpers - doesn't seem to help
        if(powerLift || powerLift1){
            slidePowerUp = 0.9;
            slidePowerDown = 0.6;
            hoistPower = 0.9;
        } else {
            slidePowerUp = 0.5;
            slidePowerDown = 0.005;
            hoistPower = 0.3;
        }


        if (slideStick > 0) {
            // down
            robot.linearSlide.setPower(-slidePowerDown);
            //robot.linearSlideL.setPower(slidePower);
        }
        if (slideStick < 0) { 
            // up
            robot.linearSlide.setPower(slidePowerUp);
            //robot.linearSlideL.setPower(-slidePower);
        }
        if (slideStick >= -0.1 && slideStick <= 0.1) {
            robot.linearSlide.setPower(.2);
            //robot.linearSlideL.setPower(-.2);
            // need to do something here to hold it in place, so give it just enough power
            // to not fall down due to gravity.  

        }

        // we didn't end up using this following bit. 
        if (launchPosition ) {
            robot.linearSlide.setPower(.45);  //change this value to determine launch height
            //robot.linearSlideL.setPower(-.35);
            // this is enough power to hold off the ground

        }
// hoistMotor control (to tighten/loosen the lifting line)
        if (runHoist > 0 || runHoistUp) {
            robot.hoistMotor.setPower(hoistPower);
        }
        if (runHoist < 0 || runHoistDown) {
            robot.hoistMotor.setPower(-hoistPower);
        }
        if (runHoist >= -0.1 && runHoist <= 0.1 &&! runHoistUp &&! runHoistDown) {
            robot.hoistMotor.setPower(0);
        }


*/
        hoistPower = 0.40;
        if (runHoist > 0 || runHoistUp) {
            robot.armGear.setPower(hoistPower);
            robot.armGear2.setPower(-hoistPower);
        }
        if (runHoist < 0 || runHoistDown) {
            robot.armGear.setPower(-hoistPower);
            robot.armGear2.setPower(hoistPower);
        }
        if (runHoist >= -0.1 && runHoist <= 0.1 &&! runHoistUp &&! runHoistDown) {
            robot.armGear.setPower(0);
            robot.armGear2.setPower(0);
        }

/*
        slidePower = 1.0;
        if (extendSlide) {
            robot.linearSlide.setPower(slidePower);
        }
        if (retractSlide) {
            robot.linearSlide.setPower(-slidePower);
        }
        if (!extendSlide && !retractSlide) {
            robot.linearSlide.setPower(0);
        }
*/        
    }

}
