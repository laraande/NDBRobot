package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.util.ElapsedTime;


public class ClawClose implements AutonStep {


    public ClawClose() { }

    @Override
    public String name() {
        return "claw step";
    }

    @Override
    public void start(Team7593OpMode opmode) {
    }

    private boolean done = false;
    public void loop(Team7593OpMode opmode) {
        if (done) return;
        if (done) return;
            opmode.robot.clawRight.setPosition(.85);
            opmode.robot.clawLeft.setPosition(.0);
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
