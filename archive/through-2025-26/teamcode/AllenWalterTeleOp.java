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

@TeleOp (name = "AllenWalterTeleOp")

public class AllenWalterTeleOp extends Team7593OpMode {
@Override
    //code block to that will run at the VERY beginning of Teleop
    public ArrayList<AutonStep> createAutonSteps() {
        return null;
    }

    @Override
    public void init(){
        super.init();
    }
    
    
    @Override
    public void loop(){
        super.loop();
        
        loopWheels();
        //loopLinearSlide();
        //loopArm();    
        //loopIntake();
        loopBallIntake();
    }
    
    public void loopWheels()
    {
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
    
    public void loopLinearSlide()
    {
        double extendSlide = gamepad2.left_stick_y;
        boolean holdPosition = gamepad2.left_bumper;
        //boolean liftSlide = gamepad2.right_bumper;
        double linearSlidePower = 0.6;

        telemetry.addData("extendSlide", extendSlide);

        if (extendSlide > 0.1)
        {
            robot.motorLinearSlideL.setPower(linearSlidePower);
            robot.motorLinearSlideR.setPower(-linearSlidePower);
        }
        else if (extendSlide < -0.1)    
        {
            robot.motorLinearSlideL.setPower(-linearSlidePower);
            robot.motorLinearSlideR.setPower(linearSlidePower);
        }
        else if (holdPosition)
        {
            robot.motorLinearSlideL.setPower(-0.35);
            robot.motorLinearSlideR.setPower(0.35);
        }
        /*else if (liftSlide)
        {
            robot.motorLinearSlideL.setPower(1);
            robot.motorLinearSlideR.setPower(-1);
        }*/
        else
        {
            robot.motorLinearSlideL.setPower(0);
            robot.motorLinearSlideR.setPower(0);
        }
    }
    
    public void loopArm()
    {
        double liftArm = gamepad2.right_stick_y;
        double liftArmPower = 0.5;
        
        if (liftArm > 0.1)
        {
            // lower the arm
            robot.armGear.setDirection(DcMotor.Direction.FORWARD);
            robot.armGear.setPower(0.6);
            /*sleep(200);
            robot.armGear.setDirection(DcMotor.Direction.REVERSE);
            robot.armGear.setPower(0.3);
            sleep(200);
            robot.armGear.setPower(0.2);
            sleep(200);
            robot.armGear.setPower(0.15);
            sleep(200);
            robot.armGear.setPower(0.15);
            sleep(200);
            telemetry.addData("liftArm", liftArm);*/
        }
        else if (liftArm < -0.1)
        {
            // raise the arm
            robot.armGear.setDirection(DcMotor.Direction.REVERSE);
            robot.armGear.setPower(1);
            telemetry.addData("liftArm", liftArm);
        }
        else
        {
            // no power to the arm
            robot.armGear.setPower(0);
            telemetry.addData("liftArm", liftArm);
        }
    }
    
    private void sleep(long sleeptime)
    {            
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e)
        {
            telemetry.addData("uh oh", e);
        }
    }
    /**
     * This method runs the continuous rotation servo for the intake.
     */
    public void loopIntake()
    {
        boolean isSweepingOut = gamepad2.x;
        boolean isSweepingIn = gamepad2.y;
        double sweepPower = 1.0;
        
        if (isSweepingIn)
        {
            // sweep the block into the intake
            robot.motorIntake.setDirection(DcMotor.Direction.FORWARD);
            robot.motorIntake.setPower(sweepPower);
        }
        else if (isSweepingOut)
        {
            // sweep the block out of the intake
            robot.motorIntake.setDirection(DcMotor.Direction.REVERSE);
            robot.motorIntake.setPower(0.3);
        }
        else
        {
            // no power to the arm
            robot.motorIntake.setPower(0);  
        }
        
    }
    
    public void loopBallIntake()
    {
        boolean pressDown = gamepad2.x;
        double ballIntakeSpeed = 1.0;
        if (pressDown)
        {
            robot.intakeServo.setPower(ballIntakeSpeed);    
        } else if (!pressDown) {
            robot.intakeServo.setPower(0.0);
        }
    }
        
}
