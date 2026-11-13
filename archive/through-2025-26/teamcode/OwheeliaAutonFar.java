package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;

@Autonomous (name = "OwheeliaAutonFar")
public class OwheeliaAutonFar extends Team7593OpMode
{

    @Override
    public void init(){
        super.init();
    }

    @Override
    public ArrayList<AutonStep> createAutonSteps() {
        ArrayList<AutonStep> steps = new ArrayList<>();
        steps.add(new OwheeliaAutonStepDrive25(2.0, 0, 0, 500));
        steps.add(new OwheeliaAutonStepDrive25(0, 0, 0, 0l));
        

        return steps;
    }



}