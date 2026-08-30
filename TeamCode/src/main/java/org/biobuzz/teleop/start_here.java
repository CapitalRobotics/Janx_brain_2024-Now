package org.biobuzz.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.base_code.control.ControlMap;
import org.base_code.robot_core.DriveBase;
import org.base_code.robot_core.DriveConstants;

import java.util.Objects;

    @TeleOp(name = "Default Opmode", group = "TeleOp")
    public class start_here extends OpMode {
        private ControlMap controls;
        private MecanumDrive drive;
        private DriveConstants driveConstants;
        private RevIMU imu; // Assuming you have an IMU class for field-centric driving

        @Override
        public void init() {
            DriveBase driveBase = new DriveBase(hardwareMap);
            controls = new ControlMap(gamepad1, gamepad2);
            drive = driveBase.mecanum;
            imu = driveBase.imu;
            driveConstants = new DriveConstants();
        }

        @Override
        public void loop() {
            if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.ROBOT_CENTRIC)) {
                drive.driveRobotCentric(
                        controls.driver1.getLeftX() * driveConstants.forwardSpeed,
                        controls.driver1.getLeftY() * driveConstants.strafeSpeed,
                        controls.driver1.getRightX() * driveConstants.turnSpeed
                );
            } else if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
                drive.driveFieldCentric(
                        controls.driver1.getLeftY() * driveConstants.forwardSpeed,
                        controls.driver1.getLeftX() * driveConstants.strafeSpeed,
                        controls.driver1.getRightX() * driveConstants.turnSpeed,
                        imu.getRotation2d().getDegrees()  // IMU heading would go here
                );
            }
        }
    }

