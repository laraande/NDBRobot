package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "Launch25") 

public class Launch25 implements AutonStep {


    public Launch25() { }

    @Override
    public String name() {
        return "Launch25";
    }

    @Override
    public void start(Team7593OpMode opmode) {
    }

    private boolean done = false;
    public void loop(Team7593OpMode opmode) {
        if (done) return;
            double anglePosition = 0.36;
            double launchPower = 0.50;
            opmode.robot.angle.setPosition(anglePosition);
            opmode.robot.launchMotor.setPower(launchPower);
            sleep(4000);
            opmode.robot.pushServoLeft.setPower(-1.0);
            opmode.robot.pushServoRight.setPower(1.0);
            opmode.robot.angle.setPosition(anglePosition);
            sleep(3000);
            opmode.robot.pipeCleaner.setPower(-0.5);
            opmode.robot.intakeServoLeft.setPower(1.0);
            opmode.robot.intakeServoRight.setPower(-1.0);
            sleep(7000);
            opmode.robot.launchMotor.setPower(0.0);
            opmode.robot.pushServoRight.setPower(0.0);
            opmode.robot.pushServoLeft.setPower(0.0);
            opmode.robot.pipeCleaner.setPower(0.0);
            opmode.robot.intakeServoLeft.setPower(0.0);
            opmode.robot.intakeServoRight.setPower(0.0);
            
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
        opmode.telemetry.addData("Say", "launch");
        opmode.telemetry.update();
    }
}
