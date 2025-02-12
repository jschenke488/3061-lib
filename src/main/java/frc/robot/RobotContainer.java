// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.commands.FollowPathWithEvents;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import frc.lib.team3061.RobotConfig;
import frc.lib.team3061.gyro.GyroIO;
import frc.lib.team3061.gyro.GyroIOPigeon2;
import frc.lib.team3061.pneumatics.Pneumatics;
import frc.lib.team3061.pneumatics.PneumaticsIO;
import frc.lib.team3061.pneumatics.PneumaticsIORev;
import frc.lib.team3061.swerve.SwerveModule;
import frc.lib.team3061.swerve.SwerveModuleIO;
import frc.lib.team3061.swerve.SwerveModuleIOSim;
import frc.lib.team3061.swerve.SwerveModuleIOTalonFX;
import frc.lib.team3061.vision.Vision;
import frc.lib.team3061.vision.VisionConstants;
import frc.lib.team3061.vision.VisionIO;
import frc.lib.team3061.vision.VisionIOPhotonVision;
import frc.lib.team3061.vision.VisionIOSim;
import frc.robot.Constants.Mode;
import frc.robot.commands.FeedForwardCharacterization;
import frc.robot.commands.FeedForwardCharacterization.FeedForwardCharacterizationData;
import frc.robot.commands.FollowPath;
import frc.robot.commands.TeleopSwerve;
import frc.robot.configs.DefaultRobotConfig;
import frc.robot.configs.MK4IRobotConfig;
import frc.robot.configs.SierraRobotConfig;
import frc.robot.operator_interface.OISelector;
import frc.robot.operator_interface.OperatorInterface;
import frc.robot.subsystems.drivetrain.Drivetrain;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  private OperatorInterface oi = new OperatorInterface() {};
  private RobotConfig config;
  private Drivetrain drivetrain;

  // use AdvantageKit's LoggedDashboardChooser instead of SendableChooser to ensure accurate logging
  private final LoggedDashboardChooser<Command> autoChooser =
      new LoggedDashboardChooser<>("Auto Routine");

  // RobotContainer singleton
  private static RobotContainer robotContainer = new RobotContainer();
  private final Map<String, Command> autoEventMap = new HashMap<>();

  /** Create the container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    /*
     * IMPORTANT: The RobotConfig subclass object *must* be created before any other objects
     * that use it directly or indirectly. If this isn't done, a null pointer exception will result.
     */

    // create real, simulated, or replay subsystems based on the mode and robot specified
    if (Constants.getMode() != Mode.REPLAY) {
      switch (Constants.getRobot()) {
        case ROBOT_DEFAULT:
        case ROBOT_2023_MK4I:
        case ROBOT_2022_SIERRA:
          {
            // create the specific RobotConfig subclass instance first
            if (Constants.getRobot() == Constants.RobotType.ROBOT_2023_MK4I) {
              config = new MK4IRobotConfig();
            } else if (Constants.getRobot() == Constants.RobotType.ROBOT_2022_SIERRA) {
              config = new SierraRobotConfig();
            } else {
              config = new DefaultRobotConfig();
            }

            GyroIO gyro = new GyroIOPigeon2(config.getGyroCANID());

            int[] driveMotorCANIDs = config.getSwerveDriveMotorCANIDs();
            int[] steerMotorCANDIDs = config.getSwerveSteerMotorCANIDs();
            int[] steerEncoderCANDIDs = config.getSwerveSteerEncoderCANIDs();
            double[] steerOffsets = config.getSwerveSteerOffsets();
            SwerveModule flModule =
                new SwerveModule(
                    new SwerveModuleIOTalonFX(
                        0,
                        driveMotorCANIDs[0],
                        steerMotorCANDIDs[0],
                        steerEncoderCANDIDs[0],
                        steerOffsets[0]),
                    0,
                    config.getRobotMaxVelocity());

            SwerveModule frModule =
                new SwerveModule(
                    new SwerveModuleIOTalonFX(
                        1,
                        driveMotorCANIDs[1],
                        steerMotorCANDIDs[1],
                        steerEncoderCANDIDs[1],
                        steerOffsets[1]),
                    1,
                    config.getRobotMaxVelocity());

            SwerveModule blModule =
                new SwerveModule(
                    new SwerveModuleIOTalonFX(
                        2,
                        driveMotorCANIDs[2],
                        steerMotorCANDIDs[2],
                        steerEncoderCANDIDs[2],
                        steerOffsets[2]),
                    2,
                    config.getRobotMaxVelocity());

            SwerveModule brModule =
                new SwerveModule(
                    new SwerveModuleIOTalonFX(
                        3,
                        driveMotorCANIDs[3],
                        steerMotorCANDIDs[3],
                        steerEncoderCANDIDs[3],
                        steerOffsets[3]),
                    3,
                    config.getRobotMaxVelocity());

            drivetrain = new Drivetrain(gyro, flModule, frModule, blModule, brModule);
            new Pneumatics(new PneumaticsIORev());
            new Vision(new VisionIOPhotonVision(config.getCameraName()));
            break;
          }
        case ROBOT_SIMBOT:
          {
            config = new MK4IRobotConfig();
            SwerveModule flModule =
                new SwerveModule(new SwerveModuleIOSim(), 0, config.getRobotMaxVelocity());

            SwerveModule frModule =
                new SwerveModule(new SwerveModuleIOSim(), 1, config.getRobotMaxVelocity());

            SwerveModule blModule =
                new SwerveModule(new SwerveModuleIOSim(), 2, config.getRobotMaxVelocity());

            SwerveModule brModule =
                new SwerveModule(new SwerveModuleIOSim(), 3, config.getRobotMaxVelocity());
            drivetrain = new Drivetrain(new GyroIO() {}, flModule, frModule, blModule, brModule);
            new Pneumatics(new PneumaticsIO() {});
            AprilTagFieldLayout layout;
            try {
              layout = new AprilTagFieldLayout(VisionConstants.APRILTAG_FIELD_LAYOUT_PATH);
            } catch (IOException e) {
              layout = new AprilTagFieldLayout(new ArrayList<>(), 16.4592, 8.2296);
            }
            new Vision(
                new VisionIOSim(
                    layout,
                    drivetrain::getPose,
                    RobotConfig.getInstance().getRobotToCameraTransform()));

            break;
          }
        default:
          break;
      }

    } else {
      SwerveModule flModule =
          new SwerveModule(new SwerveModuleIO() {}, 0, config.getRobotMaxVelocity());

      SwerveModule frModule =
          new SwerveModule(new SwerveModuleIO() {}, 1, config.getRobotMaxVelocity());

      SwerveModule blModule =
          new SwerveModule(new SwerveModuleIO() {}, 2, config.getRobotMaxVelocity());

      SwerveModule brModule =
          new SwerveModule(new SwerveModuleIO() {}, 3, config.getRobotMaxVelocity());
      drivetrain = new Drivetrain(new GyroIO() {}, flModule, frModule, blModule, brModule);
      new Pneumatics(new PneumaticsIO() {});
      new Vision(new VisionIO() {});
    }

    // disable all telemetry in the LiveWindow to reduce the processing during each iteration
    LiveWindow.disableAllTelemetry();

    updateOI();

    configureAutoCommands();
  }

  /**
   * This method scans for any changes to the connected joystick. If anything changed, it creates
   * new OI objects and binds all of the buttons to commands.
   */
  public void updateOI() {
    if (!OISelector.didJoysticksChange()) {
      return;
    }

    CommandScheduler.getInstance().getActiveButtonLoop().clear();
    oi = OISelector.findOperatorInterface();

    /*
     * Set up the default command for the drivetrain. The joysticks' values map to percentage of the
     * maximum velocities. The velocities may be specified from either the robot's frame of
     * reference or the field's frame of reference. In the robot's frame of reference, the positive
     * x direction is forward; the positive y direction, left; position rotation, CCW. In the field
     * frame of reference, the origin of the field to the lower left corner (i.e., the corner of the
     * field to the driver's right). Zero degrees is away from the driver and increases in the CCW
     * direction. This is why the left joystick's y axis specifies the velocity in the x direction
     * and the left joystick's x axis specifies the velocity in the y direction.
     */
    drivetrain.setDefaultCommand(
        new TeleopSwerve(drivetrain, oi::getTranslateX, oi::getTranslateY, oi::getRotate));

    configureButtonBindings();
  }

  /**
   * Factory method to create the singleton robot container object.
   *
   * @return the singleton robot container object
   */
  public static RobotContainer getInstance() {
    return robotContainer;
  }

  /** Use this method to define your button->command mappings. */
  private void configureButtonBindings() {
    // field-relative toggle
    oi.getFieldRelativeButton()
        .toggleOnTrue(
            Commands.either(
                Commands.runOnce(drivetrain::disableFieldRelative, drivetrain),
                Commands.runOnce(drivetrain::enableFieldRelative, drivetrain),
                drivetrain::getFieldRelative));

    // reset gyro to 0 degrees
    oi.getResetGyroButton().onTrue(Commands.runOnce(drivetrain::zeroGyroscope, drivetrain));

    // x-stance
    oi.getXStanceButton().onTrue(Commands.runOnce(drivetrain::enableXstance, drivetrain));
    oi.getXStanceButton().onFalse(Commands.runOnce(drivetrain::disableXstance, drivetrain));
  }

  /** Use this method to define your commands for autonomous mode. */
  private void configureAutoCommands() {
    autoEventMap.put("event1", Commands.print("passed marker 1"));
    autoEventMap.put("event2", Commands.print("passed marker 2"));

    // build auto path commands
    List<PathPlannerTrajectory> auto1Paths =
        PathPlanner.loadPathGroup(
            "testPaths1", config.getAutoMaxSpeed(), config.getAutoMaxAcceleration());
    Command autoTest =
        Commands.sequence(
            new FollowPathWithEvents(
                new FollowPath(auto1Paths.get(0), drivetrain, true),
                auto1Paths.get(0).getMarkers(),
                autoEventMap),
            Commands.runOnce(drivetrain::enableXstance, drivetrain),
            Commands.waitSeconds(5.0),
            Commands.runOnce(drivetrain::disableXstance, drivetrain),
            new FollowPathWithEvents(
                new FollowPath(auto1Paths.get(1), drivetrain, false),
                auto1Paths.get(1).getMarkers(),
                autoEventMap));

    // add commands to the auto chooser
    autoChooser.addDefaultOption("Do Nothing", new InstantCommand());

    // demonstration of PathPlanner path group with event markers
    autoChooser.addOption("Test Path", autoTest);

    // "auto" command for tuning the drive velocity PID
    autoChooser.addOption(
        "Drive Velocity Tuning",
        Commands.sequence(
            Commands.runOnce(drivetrain::disableFieldRelative, drivetrain),
            Commands.deadline(
                Commands.waitSeconds(5.0),
                Commands.run(() -> drivetrain.drive(1.5, 0.0, 0.0, false), drivetrain))));

    // "auto" command for characterizing the drivetrain
    autoChooser.addOption(
        "Drive Characterization",
        new FeedForwardCharacterization(
            drivetrain,
            true,
            new FeedForwardCharacterizationData("drive"),
            drivetrain::runCharacterizationVolts,
            drivetrain::getCharacterizationVelocity));

    Shuffleboard.getTab("MAIN").add(autoChooser.getSendableChooser());
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    return autoChooser.get();
  }
}
