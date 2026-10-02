package org.base_code;

import static org.base_code.robot_core.DriveConstants.forwardSpeed;
import static org.base_code.robot_core.DriveConstants.strafeSpeed;
import static org.base_code.robot_core.DriveConstants.turnSpeed;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.base_code.control.ControlMap;
import org.base_code.robot_core.DriveBase;
import org.base_code.robot_core.DriveConstants;

import java.util.Objects;

@TeleOp(name = "Default Opmode", group = "TeleOp")
public class DefaultOpmode extends OpMode {
    private ControlMap controls;
    private MecanumDrive drive;
    //private RevIMU imu; // Assuming you have an IMU class for field-centric driving
    private RevIMU imu;

    @Override
    public void init() {
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls = new ControlMap(gamepad1, gamepad2);
        drive = driveBase.mecanum;
        //imu = driveBase.imu;

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
        } else if (Objects.equals(DriveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
            drive.driveFieldCentric(
                    controls.driver1.getLeftY() * forwardSpeed,
                    controls.driver1.getLeftX() * strafeSpeed,
                    controls.driver1.getRightX() * turnSpeed,
                    imu.getRotation2d().getDegrees()
            );
        }

    }
}