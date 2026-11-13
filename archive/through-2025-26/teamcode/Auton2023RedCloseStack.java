package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import java.util.ArrayList;

@Autonomous (name = "Auton-2023-Red-Close-Stack")
public class Auton2023RedCloseStack extends Team7593OpMode{


    @Override
    public void init(){
        super.init();
    }

    //@Override
    public ArrayList<AutonStep> createAutonSteps() {
        ArrayList<AutonStep> steps = new ArrayList<>();
        
        steps.add(new ClawClose());  //this closes the claw
        //need to lift the arm off the ground
        //steps.add(new ArmUp());  //this raises the arm
        steps.add(new DriveX(1.0,0.0)); //makes the robot sit for 1 second after grabbing pixle

        steps.add(new ArmUp(0.6,0.5)); //time and power for lifting arm

        
        //driveY (time and speed - where positive number is reverse)
        steps.add(new DriveX(2.3, -0.5)); //2.3 at -0.5 to the left
        //steps.add(new AngleRotate(6,0.2));
        steps.add(new DriveY(2.6, -0.6)); //2.7 AT -0.6 is 2 feet fwd to the board
        //steps.add(new AngleRotate(12,-0.2));
        
        steps.add(new ClawOpen()); //drops pixels on board
        steps.add(new DriveX(.2,.1)); //backs up slowly
        
        steps.add(new DriveX(1.5,0.0)); //stays still to let pixles fall
        steps.add(new DriveY(2.0, 0.2)); // moves to the right so pixles stay
        
        steps.add(new DriveY(.8, 0.3)); // drives backwards slowly
        steps.add(new DriveX(2.3,0.5)); // moves to the right 
        steps.add(new DriveY(3.0,-0.3)); // goes forward to park
        steps.add(new ArmUp(0.6,-0.5)); //puts arm down
        //steps.add(new AngleRotate(6,-0.2)); // rotates to the left
        //steps.add(new DriveY(0.8,-0.3)); // parks in the background

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
