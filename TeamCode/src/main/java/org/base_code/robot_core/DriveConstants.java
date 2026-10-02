package org.base_code.robot_core;
import com.acmerobotics.dashboard.config.Config;

@Config
public class DriveConstants {
    public static String ROBOT_CENTRIC = "robot_centric";
    public static String FIELD_CENTRIC = "field_centric";

    public static float strafeSpeed = 0.5f;
    public static float forwardSpeed = 0.5f;
    public static float turnSpeed = 0.5f;

    public static String frontLeftMotorName = "fl";
    public static String frontRightMotorName = "fr";
    public static String backLeftMotorName = "bl";
    public static String backRightMotorName = "br";

    public static String robotDriveMode = ROBOT_CENTRIC;
}
