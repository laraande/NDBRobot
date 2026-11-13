package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import static com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by NDHSB-Emma on 9/26/17.
 *
 * Our opmode that we built on ftc's opmode whoooooo it does crap yayyyyyy
 */
@TeleOp
public abstract class Team7593OpMode extends OpMode{
    private ArrayList<AutonStep> steps = null;
    private int currentStep = -1;
    protected Team7593Hardware robot = new Team7593Hardware();
    private HashMap<String, Object> sharedInfo = new HashMap<>();


    //final values
    public final double v0 = 13.0597; //avg voltage
    public final double voltageSlope = 3.22031; //slope of a graph plotted dist vs voltage
    public final double avgDist = 46.9611; //avg distance for the data
    public final double calibrationTime = 1.5; //control time used

    public abstract ArrayList<AutonStep> createAutonSteps();

    public boolean isInitialized = false;

    public enum Alliance{
        BLUE, RED
    }

    public void shareInfo(String name, Object value) {
        this.sharedInfo.put(name, value);
    }

    public Object getSharedInfo(String name) {
        return this.sharedInfo.get(name);
    }

    //probably will delete this later
    public void stopAuton(){
        steps = null;
        currentStep = -1;
    }
    
        @Override
    public void init() {
        this.robot.init(hardwareMap);
        steps = this.createAutonSteps();

        if(steps != null) {
            telemetry.log().add("INIT: Steps are there");
            //telemetry.log().add("current angle" + robot.getCurrentAngle());
            //telemetry.log().add("imu angle" + robot.imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle);
            //telemetry.log().add("init angle" + robot.initAngle);
            
        }
        else
            telemetry.log().add("INIT PROBLEM: Steps are NOT there");
        robot.motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);
        robot.motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        robot.motorRearLeft.setDirection(DcMotor.Direction.FORWARD);
        robot.motorRearRight.setDirection(DcMotor.Direction.FORWARD);
        
        robot.motorFrontLeft.setZeroPowerBehavior(BRAKE);
        robot.motorFrontRight.setZeroPowerBehavior(BRAKE);
        robot.motorRearLeft.setZeroPowerBehavior(BRAKE);
        robot.motorRearRight.setZeroPowerBehavior(BRAKE);

    }

    public void newDriverAssist(ArrayList<AutonStep> driverAssistSteps) {
        this.newDriverAssist(driverAssistSteps, false);
    }

    public void newDriverAssist(ArrayList<AutonStep> driverAssistSteps, boolean append){
        if (append) {
            if(steps != null) steps.addAll(driverAssistSteps);
            else {
                steps = driverAssistSteps;
                currentStep = -1;
            }
        } else {
            steps = driverAssistSteps;
            currentStep = -1;
        }
    }

    @Override
    public void loop() {

        if (!isInitialized) {
            isInitialized = true;
            robot.resetInitAngle();
        }

        //telemetry
        for (String name : sharedInfo.keySet()) {
            telemetry.addLine().addData(name, sharedInfo.get(name));
        }

        if (steps == null) {
            telemetry.addLine().addData("Step:", "No more Steps to run");
            return;
        }

        //start the steps
        if (currentStep == -1) {
            currentStep = 0;
            steps.get(currentStep).start(this);
        }


        steps.get(currentStep).loop(this);

        //telemetry
        telemetry.addLine().addData("Step:", steps.get(currentStep).name());
        steps.get(currentStep).updateTelemetry(this);

        //check if the steps are done
        if (steps.get(currentStep).isDone(this)) {
            telemetry.log().add("Step %s is done.", steps.get(currentStep).name());
            currentStep += 1;
            if (currentStep >= steps.size()) {
                steps = null;
                currentStep = -1;
            } else {
                steps.get(currentStep).start(this);
            }

        }
        


    }
     protected void robotSleep(long sleeptime)
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
    protected void mecanumDrive(double forward, double strafe, double rotate){


        /* the denominator is the largest motor power (absolute value) or 1
         * This ensures all the powers maintain the same ratio,
         * but only if at least one is out of the range [-1, 1]
         */
         
        boolean isSlow = gamepad1.left_bumper || gamepad1.right_bumper;
        
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
    
}

    


    