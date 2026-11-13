package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "Auton-2023-Red-Far")
public class Auton2023RedFar extends Team7593OpMode{


    @Override
    public void init(){
        super.init();
    }

    //@Override
    public ArrayList<AutonStep> createAutonSteps() {
        ArrayList<AutonStep> steps = new ArrayList<>();
        
        steps.add(new ClawClose());  //this closes the claw
        steps.add(new DriveX(0.8,0.0));
        
        //need to lift the arm off the ground
        //steps.add(new ArmUp());  //this raises the arm

        steps.add(new ArmUp(0.47,0.5)); //this raises the arm for 0.1 seconds

        
        //driveY (time and speed - where positive number is reverse)
        steps.add(new DriveX(2.4, -0.5));
        steps.add(new AngleRotate(360,0.3));
        steps.add(new DriveY(1.6, -0.6)); //2.1 AT -0.6 is 2 feet fwd
        steps.add(new AngleRotate(3,-0.3));
        //steps.add(new DriveX(time,0.0)); //talk to other team and stop if needed
        steps.add(new DriveY(2.0, -0.6));
        steps.add(new AngleRotate(5,-0.3));
        steps.add(new DriveY(2.0, -0.6));
        steps.add(new AngleRotate(5,-0.3));
        steps.add(new DriveY(0.9, -0.3));
        //steps.add(new AngleRotate(12,-0.2));
        //steps.add(new DriveY(2.2, -0.6)); //2.1 AT -0.6 is 2 feet fwd
        //steps.add(new AngleRotate(12,-0.2));
        //steps.add(new DriveY(2.4, -0.6)); //2.1 AT -0.6 is 2 feet fwd
        //steps.add(new AngleRotate(12,-0.2));
        //steps.add(new DriveY(2.4, -0.6)); //2.1 AT -0.6 is 2 feet fwd
        
        steps.add(new ClawOpen()); //drops pixels on board
        steps.add(new DriveX(.8,.1)); //backs up slowly
        
        steps.add(new DriveX(1.5,0.0)); //stays still to let pixles fall
        steps.add(new DriveY(0.7, -0.1)); // moves to the right so pixles stay
        
        steps.add(new DriveY(.8, 0.3)); // drives backwards slowly
        //NEXT LINE PARKS TO THE LEFT
        steps.add(new DriveX(1.8,-0.5)); // moves to the left 
        steps.add(new DriveY(1.2,-0.3)); // goes forward to park
    
        //------------------------------------------------
        //the following three steps will move it left 1 foot and realign it
        //driveX (time and speed - where positive number is right)
        //steps.add(new DriveX(2.8, -0.6)); //2.1 AT -0.6 is 1 feet left 
        // use AngleRotate to compensate for spin in DriveX
        // 10, -0.2 rotates 10 degrees left,  350, 0.2 rotates 10 degrees right
        //steps.add(new AngleRotate(356,0.2));
        //steps.add(new DriveY(0.8, -0.4)); 
        //------------------------------------------------

        //steps.add(new BlindaAutonStep());
        //steps.add(new DriveY(1.2, -0.2));
        //steps.add(new BlindaAutonStep());
        //steps.add(new ClawStep2());

        //park 
        //steps.add(new DriveY(1.2, 0.2));
        //steps.add(new AngleRotate(85, -0.2));
        //steps.add(new DriveY(6, 0.2));


        return steps;
    }


}
