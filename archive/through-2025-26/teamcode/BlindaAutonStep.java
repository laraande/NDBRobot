package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.util.ElapsedTime;


public class BlindaAutonStep implements AutonStep {


    private ElapsedTime time; //way to see the time
    private double linearSlide;

    public BlindaAutonStep() { }

    @Override
    public String name() {
        return "blinda step";
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

        opmode.robot.linearSlide.setPower(-1);

        return true;


        // if(time.time() > 4 ){
        // }
        // return false;
    }

    //@Override
    public void updateTelemetry(Team7593OpMode opmode) {
        opmode.telemetry.addLine();
        opmode.telemetry.addData("Say", "claw should open");
        opmode.telemetry.update();
    }
}