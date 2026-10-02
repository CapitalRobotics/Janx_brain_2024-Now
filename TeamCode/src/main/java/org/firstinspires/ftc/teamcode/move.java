package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.pedroCommand.FollowPathCommand;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

@Disabled
public abstract class move extends CommandOpMode
{
    private Follower followerElement;
    private PathChain pathElement;

    //start and end points
    private Pose startPose = new Pose(0, 0, Math.toRadians(90));
    private Pose endPose = new Pose(48, 48, Math.toRadians(90));

    //gives context to the follower via hardwareMap, and makes the linear pathElement
    public void compilePathChain()
    {
        followerElement = Constants.createFollower(hardwareMap);
        pathElement = followerElement.pathBuilder()
                .addPath(new BezierLine(startPose, endPose))
                .setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading())
                .build();
    }

    @Override
    public void initialize()
    {
        //super.reset()
        followerElement.setStartingPose(startPose);
        compilePathChain();

        new FollowPathCommand(followerElement, pathElement).setGlobalMaxPower(0.5);

        super.run();
    }
}

