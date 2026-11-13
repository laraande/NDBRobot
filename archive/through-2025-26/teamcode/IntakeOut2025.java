package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;


public class IntakeOut2025 implements AutonStep {
 
  public IntakeOut2025() { }
  
  @Override
  public String name() {
    return "intake control";
  }
  
  @Override
  public void start(Team7593OpMode opmode) {
  }
  
  @Override
  public void loop(Team7593OpMode opmode) {
    
    // X button: Run intake inward
    if (opmode.gamepad1.x) {
      opmode.robot.motorIntake.setDirection(
        DcMotor.Direction.FORWARD
      );
      opmode.robot.motorIntake.setPower(0.25);
    }
    
    // Y button: Run intake outward
    else if (opmode.gamepad1.y) {
      opmode.robot.motorIntake.setDirection(
        DcMotor.Direction.REVERSE
      );
      opmode.robot.motorIntake.setPower(0.25);
    }
    
    // No button pressed: Stop intake
    else {
      opmode.robot.motorIntake.setPower(0);
    }
  }
  
  @Override
  public boolean isDone(Team7593OpMode opmode) {
    return false;
  }
  
  @Override
  public void updateTelemetry(Team7593OpMode opmode) {
    opmode.telemetry.addData("Intake", 
                             opmode.gamepad1.x ? "IN" :
                               opmode.gamepad1.y ? "OUT" : "STOPPED"
    );
    opmode.telemetry.update();
  }

}
