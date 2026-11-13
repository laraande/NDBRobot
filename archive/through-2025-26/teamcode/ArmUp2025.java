package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.hardware.DcMotor;


public class ArmUp2025 implements AutonStep {


    public ArmUp2025() { }

    @Override
    public String name() {
        return "arm up";
    }

    @Override
    public void start(Team7593OpMode opmode) {
    }

    private boolean done = false;
    public void loop(Team7593OpMode opmode) {
        if (done) return;
            opmode.robot.armGear.setDirection(DcMotor.Direction.REVERSE);
            opmode.robot.armGear.setPower(0.9);
            sleep(2000);
            opmode.robot.armGear.setPower(0);
    }
    
        private void sleep(long sleeptime)
    {            
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e)
        {
          
        }
    }

    @Override
    public boolean isDone(Team7593OpMode opmode) {
        boolean result = done;
        done = true;
        return result;
    }

    //@Override
    public void updateTelemetry(Team7593OpMode opmode) {
        opmode.telemetry.addLine();
        opmode.telemetry.addData("Say", "claw should open");
        opmode.telemetry.update();
    }
}