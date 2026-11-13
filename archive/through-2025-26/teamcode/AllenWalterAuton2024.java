package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "AllenWalterAuton2024") 
public class AllenWalterAuton2024 extends Team7593OpMode 
{ 


    @Override
    public ArrayList<AutonStep> createAutonSteps() 
    {
        ArrayList<AutonStep> steps = new ArrayList<>();

//park 
        //steps.add(new DriveY(1.2, -0.2));
        //steps.add(new AngleRotate(85, -0.2));
        //steps.add(new DriveY(7.0, 0.2));
 //forward
        steps.add(new DriveY(1.2, -0.2));
//left
        steps.add(new DriveX(8.0, -0.2));
//turn
        steps.add(new AngleRotate(-45, 0.2));
//left more
        steps.add(new DriveX(0.7, -0.2));
//ls up
        steps.add(new SlideUp2025());
//back
        steps.add(new DriveY (4.3, 0.2));
//arm up
        steps.add(new ArmUp2025());
//eject
        steps.add(new IntakeOut2025());
        
return steps;
    }   
    
}
//}