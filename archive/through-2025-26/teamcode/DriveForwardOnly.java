package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;

@Autonomous (name = "DriveForwardOnly")
public class DriveForwardOnly extends Team7593OpMode{


    @Override
    public void init(){
        super.init();
    }

    //@Override
    public ArrayList<AutonStep> createAutonSteps() {
        ArrayList<AutonStep> steps = new ArrayList<>();
        
        steps.add(new DriveY(2.7, -0.2));

        return steps;
    }


}