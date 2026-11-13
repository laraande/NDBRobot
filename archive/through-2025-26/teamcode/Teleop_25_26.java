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

@TeleOp (name = "Teleop_25_26")

public class Teleop_25_26 extends Team7593OpMode {
@Override
    //code block to that will run at the VERY beginning of Teleop
    public ArrayList<AutonStep> createAutonSteps() {
        return null;
    }

    @Override
    public void init(){
        super.init();
        /*robot.motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);
        robot.motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        robot.motorRearLeft.setDirection(DcMotor.Direction.FORWARD);
        robot.motorRearRight.setDirection(DcMotor.Direction.FORWARD);
        
        robot.motorFrontLeft.setZeroPowerBehavior(BRAKE);
        robot.motorFrontRight.setZeroPowerBehavior(BRAKE);
        robot.motorRearLeft.setZeroPowerBehavior(BRAKE);
        robot.motorRearRight.setZeroPowerBehavior(BRAKE);
        */
        
        
    }
    
    
    @Override
    public void loop(){
        super.loop();
        telemetry.addData("-gamepad1.left_stick_y", -gamepad1.left_stick_y);
        telemetry.addData("gamepad1.left_stick_x", gamepad1.left_stick_x);
        telemetry.addData("gamepad1.right_stick_x", gamepad1.right_stick_x);
        telemetry.addData("angle", robot.angle.getPosition());
        
        //loopWheels();
        mecanumDrive(-gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x);
        loopIntake();
        loopPush();
        loopLaunch();
        //loopFlip();
        loopPipeCleaner();
        //loopAngle();
        loopAutoLaunch();
        loopAutoLaunchFar();
    
        
    }
    /* This method runs robot driving */
    
     public void loopWheels(){

        double leftX, rightX, leftY;
        WheelSpeeds speeds; //variable to hold speeds
        boolean isSlow;
        
        leftX = gamepad1.left_stick_x; //must keep
        rightX = gamepad1.right_stick_x; //must keep
        leftY = gamepad1.left_stick_y;
        isSlow = gamepad1.left_bumper || gamepad1.right_bumper;
        speeds = WheelSpeeds.mecanumDrive(leftX, leftY, rightX, isSlow);
        robot.powerTheWheels(speeds);
    }
    
     /**
     * This method runs the continuous rotation servo for the intake.
     */
   
    public void loopIntake()
    {
        boolean takeIn = gamepad2.x;
        boolean takeOut = gamepad2.left_stick_button;
        double ballIntakeSpeed = 1.0;
        if (takeIn)
        {
            robot.intakeServoLeft.setPower(ballIntakeSpeed);
            robot.intakeServoRight.setPower(-ballIntakeSpeed);
        }
        else if (takeOut)
        {
            robot.intakeServoLeft.setPower(-ballIntakeSpeed);
            robot.intakeServoRight.setPower(ballIntakeSpeed);
            
        } else {
            robot.intakeServoLeft.setPower(0.0);
            robot.intakeServoRight.setPower(0.0);
        }
    }
    
        public void loopPush()
    {
        boolean push = gamepad2.b;
        boolean pushBack = gamepad2.right_bumper;
        double ballPushSpeed = 1.0;
        if (push)
        {
            robot.pushServoLeft.setPower(-ballPushSpeed);
            robot.pushServoRight.setPower(ballPushSpeed);
        } 
        else if (pushBack)
        {
            robot.pushServoLeft.setPower(ballPushSpeed);
            robot.pushServoRight.setPower(-ballPushSpeed);
        }
        else 
        {
            robot.pushServoLeft.setPower(0.0);
            robot.pushServoRight.setPower(0.0);
        }
        
    }
    
    public void loopLaunch()
    {
        boolean launch = gamepad2.a;
        double ballLaunchSpeed = 0.5;
        if (launch)
        {
            robot.launchMotor.setPower(ballLaunchSpeed);
        } else if (!launch) {
            robot.launchMotor.setPower(0.0);
            
        }
    }
    
    public void loopPipeCleaner()
    {
        boolean intake = gamepad2.y;
        double intakeSpeed = -0.5;
        if (intake)
        {
            robot.pipeCleaner.setPower(intakeSpeed);
        } else if (!intake) {
            robot.pipeCleaner.setPower(0.0);
            
        }
    }

    public void loopFlip()
    {
        boolean flip = gamepad2.y;
        if (flip)
        {
            robot.flipServo.setPosition(0);
        }
        boolean flipDown = gamepad2.left_bumper;
        if (flipDown)
        {
            robot.flipServo.setPosition(1.0);
            
        }
    }

    public void loopAutoLaunch()
    {
        boolean autoLaunch = gamepad2.right_stick_button;
        if (autoLaunch)
        {
            robot.angle.setPosition(0.36);
            robot.launchMotor.setPower(0.55);
            sleep(4000);
            robot.pushServoLeft.setPower(-1.0);
            robot.pushServoRight.setPower(1.0);
            sleep(2000);
            robot.angle.setPosition(0.36);
            robot.pipeCleaner.setPower(-0.5);            
            robot.intakeServoLeft.setPower(1.0);
            robot.intakeServoRight.setPower(-1.0);
            sleep(4000);
            robot.launchMotor.setPower(0.0);
            robot.pushServoRight.setPower(0.0);
            robot.pushServoLeft.setPower(0.0);
            robot.pipeCleaner.setPower(0.0);
        }
    }
    
    public void loopAutoLaunchFar()
        {
           boolean autoLaunchFar =  gamepad1.left_stick_button;
           if (autoLaunchFar) {
            double anglePosition = 0.44;
            robot.angle.setPosition(anglePosition);
            robot.launchMotor.setPower(0.65);
            sleep(4000);
            robot.pushServoLeft.setPower(-1.0);
            robot.pushServoRight.setPower(1.0);
            sleep(2000);
            robot.angle.setPosition(anglePosition);
            robot.pipeCleaner.setPower(-0.5);            
            robot.intakeServoLeft.setPower(1.0);
            robot.intakeServoRight.setPower(-1.0);
            sleep(4000);
            robot.launchMotor.setPower(0.0);
            robot.pushServoRight.setPower(0.0);
            robot.pushServoLeft.setPower(0.0);
            robot.pipeCleaner.setPower(0.0);
           }
        }
    
    public void loopAngle()
    {
        boolean angle = gamepad1.dpad_up;
            robot.angle.setPosition(0.36);
    }
    
    private void sleep(long sleeptime)
    {            
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e)
        {
          
        }
    }
    double leftFrontPower;
    double rightFrontPower;
    double leftRearPower;
    double rightRearPower;
    
    
    //private void mecanumDrive(double forward, double strafe, double rotate){

        /* the denominator is the largest motor power (absolute value) or 1
         * This ensures all the powers maintain the same ratio,
         * but only if at least one is out of the range [-1, 1]
         */
         
        /*boolean isSlow = gamepad1.left_bumper || gamepad1.right_bumper;
        
        double denominator = Math.max(Math.abs(forward) + Math.abs(strafe) + Math.abs(rotate), 1);
        double speedMultiplier = 1.0;
        leftFrontPower = (forward + strafe + rotate) / denominator;
        rightFrontPower = (forward - strafe - rotate) / denominator;
        leftRearPower = (forward - strafe + rotate) / denominator;
        rightRearPower = (forward + strafe - rotate) / denominator;
        
        if (isSlow){
            speedMultiplier = 0.5;
        }

        robot.motorFrontLeft.setPower(leftFrontPower * speedMultiplier);
        robot.motorFrontRight.setPower(rightFrontPower * speedMultiplier);
        robot.motorRearLeft.setPower(leftRearPower * speedMultiplier);
        robot.motorRearRight.setPower(rightRearPower * speedMultiplier);

    }


  */  
}

