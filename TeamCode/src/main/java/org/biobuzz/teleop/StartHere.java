package org.biobuzz.teleop;

import static org.base_code.robot_core.DriveConstants.forwardSpeed;
import static org.base_code.robot_core.DriveConstants.strafeSpeed;
import static org.base_code.robot_core.DriveConstants.turnSpeed;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.base_code.control.ControlMap;
import org.base_code.robot_core.DriveBase;
import org.base_code.robot_core.DriveConstants;

import java.util.Objects;


@TeleOp(name = "kick off")
public class StartHere extends OpMode {
    private ControlMap controls;
    private MecanumDrive drive;
    private RevIMU imu; // Assuming you have an IMU class for field-centric driving
    private DcMotorEx lodewheel;
    private double bob = 9000;
    private Servo claw;
    private static final double CLAW_CLOSED = 1;
    private static final double CLAW_OPEN = 0.1;
    @Override
    public void init() {
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls = new ControlMap(gamepad1, gamepad2);
        drive = driveBase.mecanum;
        //imu = driveBase.imu;
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    @Override
    public void loop() {
        // Drivetrain control
        if (Objects.equals(DriveConstants.robotDriveMode, DriveConstants.ROBOT_CENTRIC)) {
            drive.driveRobotCentric(
                    controls.driver1.getLeftX() * forwardSpeed,
                    controls.driver1.getLeftY() * -strafeSpeed,
                    controls.driver1.getRightX() * -turnSpeed
            );
//        } else if (Objects.equals(DriveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
//            drive.driveFieldCentric(
//                    controls.driver1.getLeftY() * forwardSpeed,
//                    controls.driver1.getLeftX() * strafeSpeed,
//                    controls.driver1.getRightX() * turnSpeed,
//                    imu.getRotation2d().getDegrees()
//            );
//        }

            if (gamepad2.left_trigger > 0.1) {
                claw.setPosition(CLAW_CLOSED);
                // Open claw: right trigger
            } else if (gamepad2.right_trigger > 0.1) {
                claw.setPosition(CLAW_OPEN);
            }

            if (gamepad2.left_bumper) {

                bob = bob + 1000;
            } else if (gamepad2.right_bumper) {
                bob = bob - 1000;
            }
            if (gamepad1.left_bumper) {
                forwardSpeed = 4;
                strafeSpeed = 4;
                turnSpeed = 4;
            } else if (gamepad1.right_bumper) {
                forwardSpeed = 0.5f;
                strafeSpeed = 0.5f;
                turnSpeed = 0.5f;
            }

            if (gamepad2.a) {
                lodewheel.setVelocity(bob); // ON
            } else if (gamepad2.b) {
                lodewheel.setVelocity(-bob); // ON
            } else {
                lodewheel.setVelocity(0); // OFF
            }
        }
    }
}
