package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "AllenWalterAutonTest")

public class AllenWalterAutonTest extends Team7593OpMode {
    
    @Override
    public ArrayList<AutonStep> createAutonSteps() 
    {
        ArrayList<AutonStep> steps = new ArrayList<>();

//park 
        steps.add(new DriveX(8.0, 0.2));
         return steps;
    }  
}