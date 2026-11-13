package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;

@Autonomous (name = "OwheeliaDrive25")

public class OwheeliaDrive25 extends Team7593OpMode {

    /*double leftFrontPower;
    double rightFrontPower;
    double leftRearPower;
    double rightRearPower;
    */
    
    // todo: write your code here
    //@Override
    public String name() {
        return "OwheeliaDrive25";
    }

    //@Override
    public void start(Team7593OpMode opmode) {
    }

    private boolean done = false;
    public void loop(Team7593OpMode opmode){
        if (done) return;
        mecanumDrive(1.0,0,0);   
        sleep(1000);
        mecanumDrive(0,0,0);   
    }
    

    @Override
    public ArrayList<AutonStep> createAutonSteps() 
    {
        ArrayList<AutonStep> steps = new ArrayList<>();
        return steps;
    }   

    //private void mecanumDrive(double forward, double strafe, double rotate){

        // * the denominator is the largest motor power (absolute value) or 1
        // * This ensures all the powers maintain the same ratio,
        // * but only if at least one is out of the range [-1, 1]
         
         
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
    private void sleep(long sleeptime)
    {            
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e)
        {
            telemetry.addData("uh oh", e);
        }
    }
    
    
    
    
}