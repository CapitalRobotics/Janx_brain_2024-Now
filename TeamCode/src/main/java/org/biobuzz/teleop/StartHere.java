package org.biobuzz.teleop;

import static org.base_code.robot_core.DriveConstants.forwardSpeed;
import static org.base_code.robot_core.DriveConstants.strafeSpeed;
import static org.base_code.robot_core.DriveConstants.turnSpeed;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;


import org.base_code.control.ControlMap;
import org.base_code.robot_core.DriveBase;
import org.base_code.robot_core.DriveConstants;

import java.util.Objects;


@TeleOp(name = "this 1")
public class StartHere extends OpMode {
    private ControlMap controls;
    private MecanumDrive drive;
    private DcMotorEx lodewheel;
    private double bob = 9000;
    private Servo claw;
    private static final double CLAW_CLOSED = 1;
    private static final double CLAW_OPEN = 0.5;
    private DcMotorEx flywheel_s;
    private static final int TARGET_FLYWHEEL_s = 9999999; // target ticks/sec
    private static final int RAMP_RATE_s = 1500;
    private double currentFlywheelVelocity_s = 0;
    private DcMotorEx flywheel_b;
    private static final int TARGET_FLYWHEEL_b = 9999999;
    private static final int RAMP_RATE_b = 900;
    private double currentFlywheelVelocity_b = 0;




    @Override
    public void init() {
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls = new ControlMap(gamepad1, gamepad2);
        drive = driveBase.mecanum;

        flywheel_s = hardwareMap.get(DcMotorEx.class, "flywheel_s");
        flywheel_b = hardwareMap.get(DcMotorEx.class, "flywheel_b");
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");
        flywheel_s.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel_b.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheel_s.setDirection(DcMotorEx.Direction.REVERSE);
        flywheel_b.setDirection(DcMotorEx.Direction.FORWARD);
        claw = hardwareMap.get(Servo.class, "claw");
        claw.setPosition(CLAW_CLOSED);
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

            if (gamepad1.left_bumper) {
                forwardSpeed = 4;
                strafeSpeed = 4;
                turnSpeed = 4;
            } else if (gamepad1.right_bumper) {
                forwardSpeed = 0.5f;
                strafeSpeed = 0.5f;
                turnSpeed = 0.5f;
            }

            if (gamepad2.left_trigger > 0.1) {
                claw.setPosition(CLAW_CLOSED);
                // Open claw: right trigger
            } else if (gamepad2.right_trigger > 0.1) {
                claw.setPosition(CLAW_OPEN);
            }

            if (gamepad2.dpad_left) {
                bob += 1000;
            } else if (gamepad2.dpad_right) {
                bob -= 1000;
            }
            if (gamepad2.a) {
                lodewheel.setVelocity(bob); // ON
            } else if (gamepad2.b) {
                lodewheel.setVelocity(-bob); // ON
            } else {
                lodewheel.setVelocity(0); // OFF
            }


            }
            if (gamepad2.right_bumper) {
                // Ramp up launch motors up to target speed
                currentFlywheelVelocity_s = Math.min(
                        currentFlywheelVelocity_s + RAMP_RATE_s,
                        TARGET_FLYWHEEL_s
                );

                if (currentFlywheelVelocity_s > TARGET_FLYWHEEL_s) {
                    currentFlywheelVelocity_s = TARGET_FLYWHEEL_s;
                }

            } else {
                // Ramp launch motors back down smoothly to 0
                currentFlywheelVelocity_s = Math.max(
                        currentFlywheelVelocity_s - RAMP_RATE_s,
                        0
                );
            }
            if (currentFlywheelVelocity_s > 7000) {
                currentFlywheelVelocity_s = 7000;
            }
            if (gamepad2.left_bumper) {
                // Ramp up launch motors up to target speed
                currentFlywheelVelocity_b = Math.min(
                        currentFlywheelVelocity_b + RAMP_RATE_b,
                        TARGET_FLYWHEEL_b
                );

                if (currentFlywheelVelocity_b > TARGET_FLYWHEEL_b) {
                    currentFlywheelVelocity_b = TARGET_FLYWHEEL_b;
                }

            } else {
                // Ramp launch motors back down smoothly to 0
                currentFlywheelVelocity_b = Math.max(
                        currentFlywheelVelocity_b - RAMP_RATE_b,
                        0
                );
            }
            if (currentFlywheelVelocity_b > 1150) {
                currentFlywheelVelocity_b = 1150;
            }
            flywheel_s.setVelocity(currentFlywheelVelocity_s);
            flywheel_b.setVelocity(currentFlywheelVelocity_b);
            telemetry.addData("Status", "Running");
            telemetry.addData("Flywheel Target", TARGET_FLYWHEEL_s);
            telemetry.addData("Flywheel Commanded", currentFlywheelVelocity_s);
            telemetry.addData("Flywheel Target", TARGET_FLYWHEEL_b);
            telemetry.addData("Flywheel Commanded", currentFlywheelVelocity_b);
            telemetry.addData("Small Flywheel1 Actual", flywheel_s.getVelocity());
            telemetry.addData("Big Flywheel Actual", flywheel_b.getVelocity());
        }
    }

