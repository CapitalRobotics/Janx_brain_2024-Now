package org.base_code.robot_core;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.motors.Motor;

public class DriveBase
{
    private final HardwareMap hardwareMap;
    private Motor frontLeft, frontRight, backLeft, backRight;

    public final MecanumDrive mecanum;
    //public final IMU imu;

    public DriveBase(HardwareMap hardwareMap)
    {
        this.hardwareMap = hardwareMap;
        this.mecanum = initDriveBase();

    }

    private MecanumDrive initDriveBase()
    {
        frontLeft  = new Motor(hardwareMap, DriveConstants.frontLeftMotorName);
        frontRight = new Motor(hardwareMap, DriveConstants.frontRightMotorName);
        backLeft   = new Motor(hardwareMap, DriveConstants.backLeftMotorName);
        backRight  = new Motor(hardwareMap, DriveConstants.backRightMotorName);

        for (Motor m : new Motor[]{frontLeft, frontRight, backLeft, backRight})
        {
            m.setRunMode(Motor.RunMode.RawPower);
            m.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        }

        // Invert whichever side is needed for your robot, e.g.:
        // frontRight.setInverted(true);
        // backRight.setInverted(true);

        return new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
    }

//    private IMU initIMU()
//    {
//        IMU imu = hardwareMap.get(IMU.class, "imu");
//
//        // Change these to match how your Control Hub is mounted
//        IMU.Parameters params = new IMU.Parameters(
//                new RevHubOrientationOnRobot(
//                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
//                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD));
//
//        imu.initialize(params);
//        return imu;
//    }

    /** Heading in degrees, handy for mecanum.driveFieldCentric(...) */
//    public double getHeadingDegrees()
//    {
//        return imu.getRobotYawPitchRollAngles().getYaw(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES);
//    }
}