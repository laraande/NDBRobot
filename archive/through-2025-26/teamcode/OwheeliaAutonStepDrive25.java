package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "OwheeliaAutonStepDrive25") 

public class OwheeliaAutonStepDrive25 implements AutonStep {

private double forward = 0;
private double strafe = 0;
private double rotate = 0;
private long sleep = 0;

    public OwheeliaAutonStepDrive25() { }
    public OwheeliaAutonStepDrive25(double forward, double strafe, double rotate, long sleep) { 
        this.forward = forward;
        this.strafe = strafe;
        this.rotate = rotate;
        this.sleep = sleep;
   
    }
    /*public OwheeliaAutonStepDrive25(double forward, double strafe, double rotate) { 
        super(forward, strafe, rotate, 0l);
        
    } */


    @Override
    public String name() {
        return "OwheeliaAutonStepDrive25";
    }

    @Override
    public void start(Team7593OpMode opmode) {
       /*double forward = 2.0;
       double strafe = 0;
       double rotate = 0;
       */
        opmode.mecanumDrive(forward, strafe, rotate);
        opmode.robotSleep(sleep);
        opmode.mecanumDrive(0,0,0);
    }

    private boolean done = false;
    public void loop(Team7593OpMode opmode) {
        if (done) return;
    }
    @Override
    public void updateTelemetry(Team7593OpMode opmode) {
        opmode.telemetry.addLine();
        opmode.telemetry.addData("Say", "launch");
        opmode.telemetry.update();
    }
    @Override
    public boolean isDone(Team7593OpMode opmode) {
        boolean result = done;
        done = true;
        return result;
    }
    
}