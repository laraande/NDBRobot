package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.util.ElapsedTime;


public class BlindaAutonStep2 implements AutonStep {


    private ElapsedTime time; //way to see the time
    private double linearSlide;

    public BlindaAutonStep2() { }

    @Override
    public String name() {
        return "blinda step2";
    }

    @Override
    public void start(Team7593OpMode opmode) {
        time = new ElapsedTime();
    }

    @Override
    public void loop(Team7593OpMode opmode) {
        double linearSlidePower = linearSlide*.1;
        opmode.robot.linearSlide.setPower(linearSlidePower);
    }

//    @Override
//    public boolean isDone(Team7593OpMode opmode) {
//        return false;
//    }

    @Override
    public boolean isDone(Team7593OpMode opmode) {

        opmode.robot.linearSlide.setPower(0);
        return true;

    }

    //@Override
    public void updateTelemetry(Team7593OpMode opmode) {
        // opmode.telemetry.addLine();
        // opmode.telemetry.addData("Say", "claw should open");
        // opmode.telemetry.update();
    }
}