package org.base_code.lights;

// FIX 1: The original imported GoBildaPrismDriver from TWO different packages
// (org.base_code.lights AND org.firstinspires.ftc.teamcode). Java can't tell which one you mean,
// so it fails to compile. Now everything comes from the SAME package as this file.
// If your Prism files (GoBildaPrismDriver, PrismAnimations, Color) are actually in
// org.firstinspires.ftc.teamcode, change the "package" line above to that instead.
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.base_code.lights.GoBildaPrismDriver.Artboard;
import org.base_code.lights.GoBildaPrismDriver.LayerHeight;
import org.base_code.lights.PrismAnimations.AnimationType;
import org.base_code.lights.PrismAnimations.PoliceLights;

import java.util.concurrent.TimeUnit;

/*
 * WHAT THIS FILE DOES
 * -------------------
 * This is a TeleOp that turns the Driver Station's telemetry screen into a menu for designing LED
 * animations on a goBILDA Prism. You walk through these steps with the gamepad:
 *   1. Pick a LAYER (0-9). Higher layers draw on top of lower ones.
 *   2. Pick the START/END LED for that layer.
 *   3. Pick an ANIMATION (solid, blink, rainbow, etc.).
 *   4. Tweak its colors, brightness, and speed/period (you see it live on the LEDs).
 *   5. Either add another layer on top, or save everything to an ARTBOARD (0-7).
 * An Artboard is saved on the Prism itself, so your real robot code can just say
 * "switch to artboard 3" without re-sending all the animation data.
 *
 * NOTE: The class name must match the file name, so this file must be named lights_base.java.
 */
@TeleOp(name="Prism Configurator", group="Linear OpMode")
//@Disabled
public class lights_base extends LinearOpMode {

    GoBildaPrismDriver prism; // the driver object that talks to the Prism over I2C

    // Which color of the current animation the joysticks are editing right now.
    public enum AnimationColor{
        PRIMARY_COLOR,
        SECONDARY_COLOR,
        TERTIARY_COLOR,
    }
    AnimationColor animationColor = AnimationColor.PRIMARY_COLOR;

    // Default style for the Police Lights animation (cycled with the right stick button).
    PoliceLights.PoliceLightsStyle policeLightsStyle = PoliceLights.PoliceLightsStyle.Style1;

    int startPoint = 0;    // first LED the animation covers
    int endPoint = 12;     // last LED the animation covers
    int brightness = 50;   // animation brightness (0-100)
    int period = 1000;     // time for one animation cycle, in milliseconds
    float speed = 0.5F;    // animation speed (0-1)

    int layerSelector = 0;     // menu cursor position for layers
    int animationSelector = 1; // menu cursor position for animations (1-12)
    int artboardSelector = 0;  // menu cursor position for artboards

    String hsbTelemetry; // text showing the current Hue/Saturation/Brightness (set by hsbViaJoystick)
    String hueTelemetry; // text showing start/end hue (set by hueViaJoystick)

    // Colors for the SingleFill animation (it uses three). Starts as red, white, blue.
    Color[] singleFillColors = {
            Color.RED, Color.WHITE, Color.BLUE
    };

    AnimationType selectedAnimation = AnimationType.SOLID; // the animation currently highlighted/chosen

    /*
     * The main state machine. Each state is one "page" of the menu on the Driver Station.
     */
    public enum ConfigState{
        WELCOME_SCREEN,
        SELECT_LAYER,
        SET_ENDPOINTS,
        SELECT_ANIMATION,
        CONFIGURE_ANIMATION,
        SET_BRIGHTNESS,
        SET_SPEED,
        FORK_IN_THE_ROAD,   // choose: save, go back, add another layer, or start over
        SAVE_TO_ARTBOARD,
        COMPLETE;
    }

    ConfigState configState = ConfigState.WELCOME_SCREEN;

    /*
     * Pairs each Prism layer with a remembered animation type so the menu can show
     * "LAYER_3 Animation: RAINBOW" after you've set it. The Prism itself doesn't need this;
     * it's only for the on-screen display.
     */
    public enum Layers{
        LAYER_0 (AnimationType.NONE,0,LayerHeight.LAYER_0),
        LAYER_1 (AnimationType.NONE,1,LayerHeight.LAYER_1),
        LAYER_2 (AnimationType.NONE,2,LayerHeight.LAYER_2),
        LAYER_3 (AnimationType.NONE,3,LayerHeight.LAYER_3),
        LAYER_4 (AnimationType.NONE,4,LayerHeight.LAYER_4),
        LAYER_5 (AnimationType.NONE,5,LayerHeight.LAYER_5),
        LAYER_6 (AnimationType.NONE,6,LayerHeight.LAYER_6),
        LAYER_7 (AnimationType.NONE,7,LayerHeight.LAYER_7),
        LAYER_8 (AnimationType.NONE,8,LayerHeight.LAYER_8),
        LAYER_9 (AnimationType.NONE,9,LayerHeight.LAYER_9);

        private AnimationType animationType;  // what's stored here (for display)
        private final int index;              // 0-9, used by the menu cursor
        private final LayerHeight layerHeight; // the real Prism layer constant

        Layers(AnimationType animationType, int index, LayerHeight layerHeight){
            this.animationType = animationType;
            this.index = index;
            this.layerHeight = layerHeight;
        }
    }

    /*
     * Which speed-related settings an animation supports.
     */
    public enum SpeedType{
        NO_SPEED,          // nothing to adjust (e.g. Solid)
        PERIOD_ONLY,       // only the period in ms
        SPEED_ONLY,        // only the 0-1 speed
        PERIOD_AND_SPEED,  // both
    }

    Layers selectedLayer = Layers.LAYER_0;
    Artboard selectedArtboard = Artboard.ARTBOARD_0;

    // One reusable object for each animation type the user can configure.
    PrismAnimations.Solid solid = new PrismAnimations.Solid();
    PrismAnimations.Solid endpointsAnimation = new PrismAnimations.Solid(); // preview while choosing endpoints
    PrismAnimations.Blink blink = new PrismAnimations.Blink();
    PrismAnimations.Pulse pulse = new PrismAnimations.Pulse();
    PrismAnimations.SineWave sineWave = new PrismAnimations.SineWave();
    PrismAnimations.DroidScan droidScan = new PrismAnimations.DroidScan();
    PrismAnimations.Rainbow rainbow = new PrismAnimations.Rainbow();
    PrismAnimations.Snakes snakes = new PrismAnimations.Snakes();
    PrismAnimations.Random random = new PrismAnimations.Random();
    PrismAnimations.Sparkle sparkle = new PrismAnimations.Sparkle();
    PrismAnimations.SingleFill singleFill = new PrismAnimations.SingleFill();
    PrismAnimations.RainbowSnakes rainbowSnakes = new PrismAnimations.RainbowSnakes();
    PoliceLights policeLights = new PoliceLights();

    @Override
    public void runOpMode() {
        // Find the Prism in the robot config. It must be named "prism" in the Robot Configuration.
        prism = hardwareMap.get(GoBildaPrismDriver.class,"prism");

        // Pre-start info screen (device stats).
        telemetry.addLine("Welcome to the Prism Configurator, enjoy these fun stats " +
                "and click the 'Play' button to continue");
        telemetry.addLine("");
        telemetry.addData("Device ID", prism.getDeviceID());
        telemetry.addLine(prism.getFirmwareVersionString());
        telemetry.addLine(prism.getHardwareVersionString());
        telemetry.addData("Power Cycle Count", prism.getPowerCycleCount());
        telemetry.addData("Run Time (Minutes)",prism.getRunTime(TimeUnit.MINUTES));
        telemetry.addData("Run Time (Hours)",prism.getRunTime(TimeUnit.HOURS));
        telemetry.update();

        waitForStart(); // wait for the driver to press PLAY
        resetRuntime();

        while (opModeIsActive()) {

            // Run the code for whichever menu page we're on. A = next, B = back (mostly).
            switch (configState){
                case WELCOME_SCREEN:
                    welcomeScreen();
                    if(gamepad1.aWasPressed()){
                        prism.clearAllAnimations(); // start with a blank slate
                        configState = ConfigState.SELECT_LAYER;
                    }
                    break;

                case SELECT_LAYER:
                    // D-pad moves the cursor up/down, clamped to layers 0-9.
                    if(gamepad1.dpadUpWasPressed()){
                        layerSelector -=1;
                    }
                    if(gamepad1.dpadDownWasPressed()){
                        layerSelector +=1;
                    }
                    layerSelector = Math.min(9, Math.max(0,layerSelector));
                    selectLayer(layerSelector); // draws menu + records selectedLayer
                    if(gamepad1.aWasPressed()){
                        configureEndpointsAnimation(true); // show a preview of the LED range
                        configState = ConfigState.SET_ENDPOINTS;
                    }
                    break;

                case SET_ENDPOINTS:
                    configureEndPoints();
                    if(gamepad1.aWasPressed()){
                        telemetry.clear();
                        selectAnimation(animationSelector);
                        configState = ConfigState.SELECT_ANIMATION;
                    }
                    if(gamepad1.bWasPressed()){
                        configState =  ConfigState.SELECT_LAYER;
                    }
                    break;

                case SELECT_ANIMATION:
                    if(gamepad1.dpadUpWasPressed()){
                        animationSelector -=1;
                        animationSelector = Math.min(12, Math.max(1,animationSelector));
                        selectAnimation(animationSelector);
                    }
                    if(gamepad1.dpadDownWasPressed()){
                        animationSelector +=1;
                        animationSelector = Math.min(12, Math.max(1,animationSelector));
                        selectAnimation(animationSelector);
                    }
                    if(gamepad1.aWasPressed()){
                        selectedLayer.animationType = selectedAnimation; // remember it for the layer list
                        configState = ConfigState.CONFIGURE_ANIMATION;
                    }
                    if(gamepad1.bWasPressed()){
                        configureEndpointsAnimation(true);
                        configState =  ConfigState.SET_ENDPOINTS;
                    }
                    break;

                case CONFIGURE_ANIMATION:
                    // Joysticks edit colors live; runs every loop so changes show instantly.
                    configureAnimation(true,false);
                    if(gamepad1.aWasPressed()){
                        configState = ConfigState.SET_BRIGHTNESS;
                    }
                    if(gamepad1.bWasPressed()){
                        telemetry.clear();
                        selectAnimation(animationSelector);
                        configState =  ConfigState.SELECT_ANIMATION;
                    }
                    break;

                case SET_BRIGHTNESS:
                    configureBrightness();
                    if(gamepad1.aWasPressed()){
                        configState = ConfigState.SET_SPEED;
                    }
                    if(gamepad1.bWasPressed()){
                        configState =  ConfigState.CONFIGURE_ANIMATION;
                    }
                    break;

                case SET_SPEED:
                    // configureSpeed() returns true if this animation has no speed setting,
                    // in which case we skip straight past this page.
                    if(configureSpeed()){
                        configState = ConfigState.FORK_IN_THE_ROAD;
                    }
                    if(gamepad1.aWasPressed()){
                        configState = ConfigState.FORK_IN_THE_ROAD;
                    }
                    if(gamepad1.bWasPressed()){
                        configState = ConfigState.SET_BRIGHTNESS;
                    }
                    break;

                case FORK_IN_THE_ROAD:
                    forkInTheRoad();
                    if(gamepad1.aWasPressed()){
                        telemetry.clear();
                        configState =  ConfigState.SAVE_TO_ARTBOARD;
                        selectArtboard(artboardSelector);
                    }
                    if(gamepad1.bWasPressed()){
                        // FIX 2: The original called configureSpeed() here, which also draws the
                        // speed menu and reads buttons as a side effect. Now we only ask whether
                        // the animation has a speed setting, with no side effects.
                        if(speedFromAnimation(selectedAnimation) == SpeedType.NO_SPEED){
                            configState = ConfigState.SET_BRIGHTNESS; // nothing to adjust, skip back further
                        } else {
                            configState = ConfigState.SET_SPEED;
                        }
                    }
                    if(gamepad1.yWasPressed()){
                        configState = ConfigState.SELECT_LAYER; // add another layer on top
                    }
                    if(gamepad1.xWasPressed()){
                        configState = ConfigState.WELCOME_SCREEN; // start over
                    }
                    break;

                case SAVE_TO_ARTBOARD:
                    if(gamepad1.dpadUpWasPressed()){
                        artboardSelector -=1;
                        artboardSelector = Math.min(7, Math.max(0,artboardSelector));
                        selectArtboard(artboardSelector);
                    }
                    if(gamepad1.dpadDownWasPressed()){
                        artboardSelector +=1;
                        artboardSelector = Math.min(7, Math.max(0,artboardSelector));
                        selectArtboard(artboardSelector);
                    }
                    if(gamepad1.aWasPressed()){
                        // Writes all current layers to flash on the Prism and makes it the
                        // artboard shown at power-on.
                        prism.saveCurrentAnimationsToArtboard(selectedArtboard);
                        prism.enableDefaultBootArtboard(true);
                        configState = ConfigState.COMPLETE;
                    }
                    if(gamepad1.bWasPressed()){
                        configState = ConfigState.FORK_IN_THE_ROAD;
                    }
                    break;

                case COMPLETE:
                    telemetry.addLine("Artboard saved!");
                    telemetry.addLine("");
                    telemetry.addLine("Press X to return to the beginning.");
                    if(gamepad1.xWasPressed()){
                        configState = ConfigState.WELCOME_SCREEN;
                    }
                    break;
            }
            telemetry.update();
            sleep(20); // small delay so we don't spam I2C/telemetry
        }
    }

    // ===================================================================================
    // MENU PAGES (each one just prints text to the Driver Station)
    // ===================================================================================

    /** Intro page explaining Animations, Layers, and Artboards. Also resets the layer list. */
    public void welcomeScreen(){
        telemetry.addLine("Welcome to the goBILDA Prism Configurator!");
        telemetry.addLine("Hold on tight - we were cooking when we made this.");
        telemetry.addLine("");
        telemetry.addLine("Core to understanding how to use this product is knowing these three terms:");
        telemetry.addLine("Animations: (Like RAINBOW or BLINK) - These have properties you can configure, " +
                "like their color. And they can have unique start and end points.");
        telemetry.addLine("");
        telemetry.addLine("Layers: There are 10 layers, each of which can store an animation. " +
                "These are hierarchical. So an Animation on layer 5 will cover an animation on layer 2 " +
                "if they overlap. You can use start and end points to have layers overlap to create new patterns! " +
                "Or show multiple animations at once on different LEDs.");
        telemetry.addLine("");
        telemetry.addLine("Artboards: An Artboard is a set of 10 layers which is stored on the Prism. " +
                "you can have up to 8 unique Artboards. Artboards are easy and computationally fast to switch between. " +
                "We recommend that you configure your Artboards for your robot once and then switch between them.");
        telemetry.addLine("");
        telemetry.addLine("Press A to continue");
        resetStoredAnimations();
    }

    /** Lists all 10 layers and an arrow next to the highlighted one. */
    public void selectLayer(int selector){
        telemetry.addLine("Select the Layer that you wish to save an Animation to.");
        telemetry.addLine("Use D-Pad up and D-Pad down to navigate through the layers.");
        telemetry.addLine("");
        telemetry.addData("LAYER_0 Animation", Layers.LAYER_0.animationType+layerCursor(Layers.LAYER_0,selector));
        telemetry.addData("LAYER_1 Animation", Layers.LAYER_1.animationType+layerCursor(Layers.LAYER_1,selector));
        telemetry.addData("LAYER_2 Animation", Layers.LAYER_2.animationType+layerCursor(Layers.LAYER_2,selector));
        telemetry.addData("LAYER_3 Animation", Layers.LAYER_3.animationType+layerCursor(Layers.LAYER_3,selector));
        telemetry.addData("LAYER_4 Animation", Layers.LAYER_4.animationType+layerCursor(Layers.LAYER_4,selector));
        telemetry.addData("LAYER_5 Animation", Layers.LAYER_5.animationType+layerCursor(Layers.LAYER_5,selector));
        telemetry.addData("LAYER_6 Animation", Layers.LAYER_6.animationType+layerCursor(Layers.LAYER_6,selector));
        telemetry.addData("LAYER_7 Animation", Layers.LAYER_7.animationType+layerCursor(Layers.LAYER_7,selector));
        telemetry.addData("LAYER_8 Animation", Layers.LAYER_8.animationType+layerCursor(Layers.LAYER_8,selector));
        telemetry.addData("LAYER_9 Animation", Layers.LAYER_9.animationType+layerCursor(Layers.LAYER_9,selector));
        telemetry.addLine("");
        telemetry.addLine("Press A to continue");
    }

    /** Returns "<--" if this layer is the highlighted one, and stores it as selectedLayer. */
    public String layerCursor(Layers layer, int selector){
        if(layer.index == selector){
            selectedLayer = layer;
            return "<--";
        }else return "";
    }

    /** Lists all 12 animations with an arrow next to the highlighted one. */
    public void selectAnimation(int animationSelector){
        telemetry.addLine("Select the Animation that you wish to use");
        telemetry.addLine("Use D-Pad up and D-Pad down to navigate through the Animations.");
        telemetry.addLine("");
        telemetry.addData("Solid",animationCursor(AnimationType.SOLID,animationSelector));
        telemetry.addData("Blink",animationCursor(AnimationType.BLINK,animationSelector));
        telemetry.addData("Pulse",animationCursor(AnimationType.PULSE,animationSelector));
        telemetry.addData("Sine Wave",animationCursor(AnimationType.SINE_WAVE,animationSelector));
        telemetry.addData("Droid Scan",animationCursor(AnimationType.DROID_SCAN,animationSelector));
        telemetry.addData("Rainbow",animationCursor(AnimationType.RAINBOW,animationSelector));
        telemetry.addData("Snakes",animationCursor(AnimationType.SNAKES,animationSelector));
        telemetry.addData("Random",animationCursor(AnimationType.RANDOM,animationSelector));
        telemetry.addData("Sparkle",animationCursor(AnimationType.SPARKLE,animationSelector));
        telemetry.addData("Single Fill",animationCursor(AnimationType.SINGLE_FILL,animationSelector));
        telemetry.addData("Rainbow Snakes",animationCursor(AnimationType.RAINBOW_SNAKES,animationSelector));
        telemetry.addData("Police Lights",animationCursor(AnimationType.POLICE_LIGHTS,animationSelector));
        telemetry.addLine("");
        telemetry.addLine("Press A to continue");
        telemetry.addLine("Press B to go back");
    }

    /**
     * If this is the highlighted animation: remember it, immediately send a live preview of it
     * to the LEDs, and return the arrow.
     */
    public String animationCursor(AnimationType animationType, int selector){
        if(animationType.AnimationTypeIndex == selector){
            selectedAnimation = animationType;
            configureAnimation(false,true); // true = insert it onto the Prism as a preview
            return "<--";
        }else return "";
    }

    /**
     * Start/end LED page.
     * D-pad left/right = start LED, bumpers = end LED. Start must always stay below end.
     */
    public void configureEndPoints(){
        if(gamepad1.dpadLeftWasPressed()){
            startPoint -= 1;
            startPoint = Math.min(Math.min(255,Math.max(0,startPoint)),endPoint-1);
            configureEndpointsAnimation(false);
        }
        if(gamepad1.dpadRightWasPressed()){
            startPoint += 1;
            startPoint = Math.min(Math.min(255,Math.max(0,startPoint)),endPoint-1);
            configureEndpointsAnimation(false);
        }
        if(gamepad1.leftBumperWasPressed()){
            endPoint -= 1;
            endPoint = Math.max(Math.min(255,Math.max(0,endPoint)),startPoint+1);
            configureEndpointsAnimation(false);
        }
        if(gamepad1.rightBumperWasPressed()){
            endPoint += 1;
            endPoint = Math.max(Math.min(255,Math.max(0,endPoint)),startPoint+1);
            configureEndpointsAnimation(false);
        }

        telemetry.addLine("Set the start and stop point for each LED");
        telemetry.addLine("");
        telemetry.addLine("Use the d-pad to set the start point, d-pad right moves it further from the Prism. " +
                "d-pad left moves it closer.");
        telemetry.addLine("Bumpers move the endpoint, The left bumper moves it closer to the Prism, right moves it further.");
        telemetry.addData("Start Point", startPoint);
        telemetry.addData("End Point", endPoint);
        telemetry.addLine("");
        telemetry.addLine("Press A to Continue");
        telemetry.addLine("Press B to go back");
    }

    /** Brightness page: d-pad up/down changes brightness by 10 (range 0-100). */
    public void configureBrightness(){
        if(gamepad1.dpadDownWasPressed()){
            brightness -= 10;
            brightness = Math.min(100,Math.max(0,brightness));
            configureAnimation(false,false);
        }
        if(gamepad1.dpadUpWasPressed()){
            brightness += 10;
            brightness = Math.min(100,Math.max(0,brightness));
            configureAnimation(false,false);
        }

        telemetry.addLine("Set the brightness for this animation");
        telemetry.addLine("");
        telemetry.addLine("Use the d-pad to adjust the brightness, up increases, down decreases.");
        telemetry.addLine("");
        telemetry.addData("Brightness", brightness);
        telemetry.addLine("");
        telemetry.addLine("Press A to Continue");
        telemetry.addLine("Press B to go back");
    }

    /**
     * Speed/period page. What you can adjust depends on the animation (see speedFromAnimation).
     * Returns: true if this animation has no speed setting, so the caller should skip this page.
     */
    public boolean configureSpeed(){
        switch (speedFromAnimation(selectedAnimation)){
            case NO_SPEED: return true;

            case SPEED_ONLY:
                if(gamepad1.dpadUpWasPressed()){
                    speed += 0.1f;
                    speed = Math.min(1.0f, Math.max(0,speed));
                    configureAnimation(false,false);
                }
                if(gamepad1.dpadDownWasPressed()){
                    speed -= 0.1f;
                    speed = Math.min(1.0f, Math.max(0,speed));
                    configureAnimation(false,false);
                }
                telemetry.addLine("Set the speed for this animation from 0 to 1");
                telemetry.addLine("");
                telemetry.addLine("Use the d-pad to adjust the speed, up increases, down decreases.");
                telemetry.addLine("");
                telemetry.addData("Speed", speed);
                telemetry.addLine("");
                telemetry.addLine("Press A to Continue");
                telemetry.addLine("Press B to go back");
                break;

            case PERIOD_ONLY:
                adjustPeriodWithDpad();
                telemetry.addLine("Set the period for this animation in Milliseconds");
                telemetry.addLine("");
                telemetry.addLine("Use the d-pad to adjust the period, up increases, down decreases.");
                telemetry.addLine("");
                telemetry.addData("Period", period);
                telemetry.addLine("");
                telemetry.addLine("Press A to Continue");
                telemetry.addLine("Press B to go back");
                break;

            case PERIOD_AND_SPEED:
                adjustPeriodWithDpad(); // up/down = period
                if(gamepad1.dpadRightWasPressed()){
                    speed += 0.1f;
                    speed = Math.min(1.0f, Math.max(0,speed));
                    configureAnimation(false,false);
                }
                if(gamepad1.dpadLeftWasPressed()){
                    speed -= 0.1f;
                    speed = Math.min(1.0f, Math.max(0,speed));
                    configureAnimation(false,false);
                }
                telemetry.addLine("Set the period for this animation in Milliseconds");
                telemetry.addLine("");
                telemetry.addLine("Set the speed for this animation from 0-1");
                telemetry.addLine("");
                telemetry.addLine("Use the d-pad to adjust the period, up increases, down decreases.");
                telemetry.addLine("");
                telemetry.addData("Period", period);
                telemetry.addLine("");
                telemetry.addLine("Use the d-pad to adjust the speed, right increases, left decreases.");
                telemetry.addLine("");
                telemetry.addData("Speed", speed);
                telemetry.addLine("");
                telemetry.addLine("Press A to Continue");
                telemetry.addLine("Press B to go back");
                break;
        }
        return false;
    }

    /**
     * Shared period-adjust code (the original had this copy-pasted twice).
     * Steps of 100ms below ~500ms, 500ms above that.
     * FIX 3: minimum is now 100ms instead of 0, since a 0ms period would be invalid/divide-by-zero
     * for most animations.
     */
    private void adjustPeriodWithDpad(){
        if(gamepad1.dpadUpWasPressed()){
            if(period < 401){
                period += 100;
            } else period += 500;
            period = Math.min(300000, Math.max(100,period));
            configureAnimation(false,false);
        }
        if(gamepad1.dpadDownWasPressed()){
            if(period < 501){
                period -= 100;
            } else period -= 500;
            period = Math.min(300000, Math.max(100,period));
            configureAnimation(false,false);
        }
    }

    /** Tells us which speed controls each animation type supports. */
    public SpeedType speedFromAnimation(AnimationType animationType){
        if(animationType == AnimationType.BLINK || animationType == AnimationType.PULSE ||
                animationType == AnimationType.SPARKLE || animationType == AnimationType.POLICE_LIGHTS){
            return SpeedType.PERIOD_ONLY;
        }
        if (animationType == AnimationType.DROID_SCAN || animationType == AnimationType.RAINBOW ||
                animationType == AnimationType.SNAKES || animationType == AnimationType.RANDOM ||
                animationType == AnimationType.RAINBOW_SNAKES){
            return SpeedType.SPEED_ONLY;
        }
        if(animationType == AnimationType.SINE_WAVE || animationType == AnimationType.SINGLE_FILL){
            return SpeedType.PERIOD_AND_SPEED;
        }
        else return SpeedType.NO_SPEED;
    }

    /** "What next?" page: save (A), back (B), add another layer (Y), or start over (X). */
    public void forkInTheRoad(){
        telemetry.addLine("If you are done with your creation, press A to save it to an Artboard.");
        telemetry.addLine("");
        telemetry.addLine("To go back, press B.");
        telemetry.addLine("");
        telemetry.addLine("If you'd instead like to layer another animation on top of this " +
                "one, press Y.");
        telemetry.addLine("");
        telemetry.addLine("Press X to return to the start and clear currently set animations.");
    }

    /** Lists the 8 artboards with an arrow next to the highlighted one. */
    public void selectArtboard(int artboardSelector){
        telemetry.addLine("Select the Artboard that you wish to save to");
        telemetry.addLine("Use D-Pad up and D-Pad down to navigate through the Artboards.");
        telemetry.addLine("");
        telemetry.addData("Artboard 0",artboardCursor(Artboard.ARTBOARD_0,artboardSelector));
        telemetry.addData("Artboard 1",artboardCursor(Artboard.ARTBOARD_1,artboardSelector));
        telemetry.addData("Artboard 2",artboardCursor(Artboard.ARTBOARD_2,artboardSelector));
        telemetry.addData("Artboard 3",artboardCursor(Artboard.ARTBOARD_3,artboardSelector));
        telemetry.addData("Artboard 4",artboardCursor(Artboard.ARTBOARD_4,artboardSelector));
        telemetry.addData("Artboard 5",artboardCursor(Artboard.ARTBOARD_5,artboardSelector));
        telemetry.addData("Artboard 6",artboardCursor(Artboard.ARTBOARD_6,artboardSelector));
        telemetry.addData("Artboard 7",artboardCursor(Artboard.ARTBOARD_7,artboardSelector));
        telemetry.addLine("");
        telemetry.addLine("Press A to save");
        telemetry.addLine("Press B to go back");
    }

    /** Returns "<--" for the highlighted artboard and stores it as selectedArtboard. */
    public String artboardCursor(Artboard artboard, int selector){
        if(artboard.index == selector){
            selectedArtboard = artboard;
            return "<--";
        }else return "";
    }

    // ===================================================================================
    // ANIMATION SETUP
    // ===================================================================================

    /**
     * Routes to the right configureXxx() for the currently selected animation.
     * @param showTelemetry   true = also print that animation's instructions
     * @param isBeingInserted true = send it to the Prism as a NEW animation on the layer;
     *                        false = just update the one already there
     */
    public void configureAnimation(boolean showTelemetry, boolean isBeingInserted){
        switch(selectedAnimation){
            case SOLID:
                configureSolid(showTelemetry, isBeingInserted);
                break;
            case BLINK:
                configureBlink(showTelemetry, isBeingInserted);
                break;
            case PULSE:
                configurePulse(showTelemetry, isBeingInserted);
                break;
            case SINE_WAVE:
                configureSineWave(showTelemetry, isBeingInserted);
                break;
            case DROID_SCAN:
                configureDroidScan(showTelemetry, isBeingInserted);
                break;
            case RAINBOW:
                configureRainbow(showTelemetry, isBeingInserted);
                break;
            case SNAKES:
                configureSnakes(showTelemetry, isBeingInserted);
                break;
            case RANDOM:
                configureRandom(showTelemetry, isBeingInserted);
                break;
            case SPARKLE:
                configureSparkle(showTelemetry, isBeingInserted);
                break;
            case SINGLE_FILL:
                configureSingleFill(showTelemetry, isBeingInserted);
                break;
            case RAINBOW_SNAKES:
                configureRainbowSnakes(showTelemetry, isBeingInserted);
                break;
            case POLICE_LIGHTS:
                configurePoliceLights(showTelemetry, isBeingInserted);
                break;
        }
    }

    /** Shows a plain Solid animation over the chosen LED range so you can see the endpoints. */
    public void configureEndpointsAnimation(boolean isBeingInserted){
        endpointsAnimation.setStartIndex(startPoint);
        endpointsAnimation.setStopIndex(endPoint);
        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, endpointsAnimation);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }
    }

    /*
     * Each configureXxx() below follows the same pattern:
     *   1. Read the joysticks to change the animation's color(s).
     *   2. Apply the shared settings (start/end LED, brightness, period, speed).
     *   3. Send it to the Prism (insert if new, update if it already exists).
     *   4. Optionally print instructions.
     */

    public void configureSolid(boolean showTelemetry, boolean isBeingInserted){
        solid.setPrimaryColor(hsbViaJoystick(solid.getPrimaryColor()));
        solid.setStartIndex(startPoint);
        solid.setStopIndex(endPoint);
        solid.setBrightness(brightness);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, solid);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Solid");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureBlink(boolean showTelemetry, boolean isBeingInserted){
        // Edit whichever color is currently selected (Y toggles).
        switch (animationColor){
            case PRIMARY_COLOR:
                blink.setPrimaryColor(hsbViaJoystick(blink.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                blink.setSecondaryColor(hsbViaJoystick(blink.getSecondaryColor()));
                break;
        }
        toggleThroughColors(gamepad1.yWasPressed(),false);

        blink.setStartIndex(startPoint);
        blink.setStopIndex(endPoint);
        blink.setBrightness(brightness);
        blink.setPeriod(period);
        blink.setPrimaryColorPeriod(period/2); // primary color on for half the cycle

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, blink);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Blink");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary and secondary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configurePulse(boolean showTelemetry, boolean isBeingInserted){
        switch (animationColor){
            case PRIMARY_COLOR:
                pulse.setPrimaryColor(hsbViaJoystick(pulse.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                pulse.setSecondaryColor(hsbViaJoystick(pulse.getSecondaryColor()));
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),false);

        pulse.setStartIndex(startPoint);
        pulse.setStopIndex(endPoint);
        pulse.setBrightness(brightness);
        pulse.setPeriod(period);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, pulse);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Pulse");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary and secondary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureSineWave(boolean showTelemetry, boolean isBeingInserted){
        switch (animationColor){
            case PRIMARY_COLOR:
                sineWave.setPrimaryColor(hsbViaJoystick(sineWave.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                sineWave.setSecondaryColor(hsbViaJoystick(sineWave.getSecondaryColor()));
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),false);

        sineWave.setStartIndex(startPoint);
        sineWave.setStopIndex(endPoint);
        sineWave.setBrightness(brightness);
        sineWave.setPeriod(period);
        sineWave.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, sineWave);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Sine Wave");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary and secondary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureDroidScan(boolean showTelemetry, boolean isBeingInserted){
        switch (animationColor){
            case PRIMARY_COLOR:
                droidScan.setPrimaryColor(hsbViaJoystick(droidScan.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                droidScan.setSecondaryColor(hsbViaJoystick(droidScan.getSecondaryColor()));
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),false);

        droidScan.setStartIndex(startPoint);
        droidScan.setStopIndex(endPoint);
        droidScan.setBrightness(brightness);
        droidScan.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, droidScan);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Droid Scan");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary and secondary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureRainbow(boolean showTelemetry, boolean isBeingInserted){
        // Rainbow uses a hue RANGE instead of colors: left stick = start hue, right stick = end hue.
        float[] hues = hueViaJoystick(rainbow.getStartHue(),rainbow.getStopHue());
        rainbow.setStartHue(hues[0]);
        rainbow.setStopHue(hues[1]);

        rainbow.setStartIndex(startPoint);
        rainbow.setStopIndex(endPoint);
        rainbow.setBrightness(brightness);
        rainbow.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, rainbow);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }
        if(showTelemetry){
            telemetry.addLine("Selected Animation: Rainbow");
            telemetry.addLine("Move the joysticks to configure the range of the rainbow");
            telemetry.addLine("Left Joystick X (side-to-side) changes the start hue");
            telemetry.addLine("Right Joystick x (side-to-side) changes the end hue");
            telemetry.addLine("");
            telemetry.addLine(hueTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureSnakes(boolean showTelemetry, boolean isBeingInserted){
        snakes.setColors(hsbViaJoystick(snakes.getColors()[0]));

        snakes.setStartIndex(startPoint);
        snakes.setStopIndex(endPoint);
        snakes.setBrightness(brightness);
        snakes.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, snakes);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry) {
            telemetry.addLine("Selected Animation: Snakes");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureRandom(boolean showTelemetry, boolean isBeingInserted){
        // Random flashes random colors picked from a hue range.
        float[] hues = hueViaJoystick(random.getStartHue(), random.getStopHue());
        random.setStartHue(hues[0]);
        random.setStopHue(hues[1]);

        random.setStartIndex(startPoint);
        random.setStopIndex(endPoint);
        random.setBrightness(brightness);
        random.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, random);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Random");
            telemetry.addLine("Move the joysticks to configure the range of the random colors");
            telemetry.addLine("Left Joystick X (side-to-side) changes the start hue");
            telemetry.addLine("Right Joystick x (side-to-side) changes the end hue");
            telemetry.addLine("");
            telemetry.addLine(hueTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureSparkle(boolean showTelemetry, boolean isBeingInserted){
        switch (animationColor){
            case PRIMARY_COLOR:
                sparkle.setPrimaryColor(hsbViaJoystick(sparkle.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                sparkle.setSecondaryColor(hsbViaJoystick(sparkle.getSecondaryColor()));
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),false);

        sparkle.setStartIndex(startPoint);
        sparkle.setStopIndex(endPoint);
        sparkle.setBrightness(brightness);
        sparkle.setPeriod(period);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, sparkle);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Sparkle");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary and secondary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureSingleFill(boolean showTelemetry, boolean isBeingInserted){
        // FIX 4: The original read the current color from singleFill.getColors(), which on the
        // first pass is the animation's default (not our red/white/blue), and could make the
        // untouched colors jump. We now read from our own singleFillColors array instead.
        switch (animationColor){
            case PRIMARY_COLOR:
                singleFillColors[0] = hsbViaJoystick(singleFillColors[0]);
                break;
            case SECONDARY_COLOR:
                singleFillColors[1] = hsbViaJoystick(singleFillColors[1]);
                break;
            case TERTIARY_COLOR:
                singleFillColors[2] = hsbViaJoystick(singleFillColors[2]);
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),true); // true = this animation has 3 colors

        singleFill.setColors(singleFillColors);

        singleFill.setStartIndex(startPoint);
        singleFill.setStopIndex(endPoint);
        singleFill.setBrightness(brightness);
        singleFill.setSpeed(speed);
        singleFill.setPeriod(period);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, singleFill);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Single Fill");
            telemetry.addLine("");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary, secondary, and tertiary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configureRainbowSnakes(boolean showTelemetry, boolean isBeingInserted){
        float[] hues = hueViaJoystick(rainbowSnakes.getStartHue(),rainbowSnakes.getStopHue());
        rainbowSnakes.setStartHue(hues[0]);
        rainbowSnakes.setStopHue(hues[1]);

        rainbowSnakes.setStartIndex(startPoint);
        rainbowSnakes.setStopIndex(endPoint);
        rainbowSnakes.setBrightness(brightness);
        rainbowSnakes.setSpeed(speed);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, rainbowSnakes);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Rainbow Snakes");
            telemetry.addLine("Move the joysticks to configure the range of the rainbow");
            telemetry.addLine("Left Joystick X (side-to-side) changes the start hue");
            telemetry.addLine("Right Joystick x (side-to-side) changes the end hue");
            telemetry.addLine("");
            telemetry.addLine(hueTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    public void configurePoliceLights(boolean showTelemetry, boolean isBeingInserted){
        // Police lights has three colors.
        switch (animationColor){
            case PRIMARY_COLOR:
                policeLights.setPrimaryColor(hsbViaJoystick(policeLights.getPrimaryColor()));
                break;
            case SECONDARY_COLOR:
                policeLights.setSecondaryColor(hsbViaJoystick(policeLights.getSecondaryColor()));
                break;
            case TERTIARY_COLOR:
                policeLights.setTertiaryColor(hsbViaJoystick(policeLights.getTertiaryColor()));
                break;
        }

        toggleThroughColors(gamepad1.yWasPressed(),true);

        // Clicking the right stick cycles Style1 -> Style2 -> Style3 -> Style1.
        switch (policeLightsStyle){
            case Style1:
                if(gamepad1.rightStickButtonWasPressed()){
                    policeLightsStyle = PoliceLights.PoliceLightsStyle.Style2;
                }
                break;
            case Style2:
                if(gamepad1.rightStickButtonWasPressed()){
                    policeLightsStyle = PoliceLights.PoliceLightsStyle.Style3;
                }
                break;
            case Style3:
                if(gamepad1.rightStickButtonWasPressed()){
                    policeLightsStyle = PoliceLights.PoliceLightsStyle.Style1;
                }
                break;
        }

        policeLights.setStartIndex(startPoint);
        policeLights.setStopIndex(endPoint);
        policeLights.setBrightness(brightness);
        policeLights.setPoliceLightsStyle(policeLightsStyle);
        policeLights.setPeriod(period);

        if(isBeingInserted){
            prism.insertAndUpdateAnimation(selectedLayer.layerHeight, policeLights);
        } else {
            prism.updateAnimationFromIndex(selectedLayer.layerHeight);
        }

        if(showTelemetry){
            telemetry.addLine("Selected Animation: Police Lights");
            telemetry.addLine("");
            telemetry.addLine("Toggle between styles by clicking the right joystick");
            telemetry.addData("Police Lights Style: ",policeLightsStyle);
            telemetry.addLine("");
            showHsbTelemetry();
            telemetry.addLine(hsbTelemetry);
            telemetry.addLine("");
            telemetry.addLine("Click the Y button to switch between setting the primary, secondary, and tertiary color");
            telemetry.addLine(animationColor.toString());
            telemetry.addLine("");
            telemetry.addLine("Press A to Continue");
            telemetry.addLine("Press B to go back");
        }
    }

    /** Clears the on-screen layer list (does not touch the Prism itself). */
    public void resetStoredAnimations(){
        Layers.LAYER_0.animationType = AnimationType.NONE;
        Layers.LAYER_1.animationType = AnimationType.NONE;
        Layers.LAYER_2.animationType = AnimationType.NONE;
        Layers.LAYER_3.animationType = AnimationType.NONE;
        Layers.LAYER_4.animationType = AnimationType.NONE;
        Layers.LAYER_5.animationType = AnimationType.NONE;
        Layers.LAYER_6.animationType = AnimationType.NONE;
        Layers.LAYER_7.animationType = AnimationType.NONE;
        Layers.LAYER_8.animationType = AnimationType.NONE;
        Layers.LAYER_9.animationType = AnimationType.NONE;
    }

    // ===================================================================================
    // COLOR / JOYSTICK HELPERS
    // ===================================================================================

    /*
     * Colors are edited as Hue / Saturation / Brightness (HSB) instead of Red/Green/Blue because
     * it's far more intuitive with joysticks:
     *   Left stick X  -> hue (which color)
     *   Right stick X -> saturation (how vivid vs. washed out)
     *   Right stick Y -> brightness
     * The result is converted back to RGB for the Prism.
     */
    public Color hsbViaJoystick(Color previousColor){
        final float HUE_JOYSTICK_SCALAR = 5;
        final float SATURATION_JOYSTICK_SCALAR = 0.05F;
        final float BRIGHTNESS_JOYSTICK_SCALAR = 0.05f;

        // Convert the existing RGB color to HSV so we can nudge it.
        float[] hsb = new float[3];
        android.graphics.Color.RGBToHSV(previousColor.red,previousColor.green,previousColor.blue,hsb);

        // Nudge each value by how far the stick is pushed, then clamp to a valid range.
        // FIX 5: hue is capped at 359.99 (Android treats hue as 0-360 exclusive; exactly 360
        // is out of range). Brightness is capped at 0.98 as in the original (see note below).
        hsb[0] = Math.max(Math.min(hsb[0] +(gamepad1.left_stick_x*HUE_JOYSTICK_SCALAR), 359.99f),0);
        hsb[1] = Math.max(Math.min(hsb[1] +(gamepad1.right_stick_x*SATURATION_JOYSTICK_SCALAR), 1),0);
        hsb[2] = Math.max(Math.min(hsb[2] +(-gamepad1.right_stick_y*BRIGHTNESS_JOYSTICK_SCALAR), 0.98f),0);

        // Android packs the converted RGB into one int; we unpack it below.
        int colorInt = android.graphics.Color.HSVToColor(hsb);
        Color color = new Color(0,0,0);

        // Brightness is capped at 0.98 internally, but we show it to the user as 1.0 so it
        // doesn't look like they can't reach full brightness.
        // (The original comment said this limit was on saturation; it's actually on brightness.)
        if(hsb[2] > 0.97){
            hsb[2] = 1.0f;
        }
        hsbTelemetry = String.format("Hue/Saturation/Brightness: %4.2f %4.2f %4.2f", hsb[0], hsb[1], hsb[2]);

        color.red = android.graphics.Color.red(colorInt);
        color.green = android.graphics.Color.green(colorInt);
        color.blue = android.graphics.Color.blue(colorInt);
        return color;
    }

    /**
     * For animations that use a hue range (Rainbow, Random, Rainbow Snakes).
     * Left stick X moves the start hue, right stick X moves the end hue. Returns {start, end}.
     */
    public float[] hueViaJoystick(float previousStartHue, float previousEndHue){
        final float HUE_JOYSTICK_SCALAR = 5;
        float[] hues = new float[2];

        hues[0] = Math.max(Math.min(previousStartHue +(gamepad1.left_stick_x*HUE_JOYSTICK_SCALAR), 360),0);
        hues[1] = Math.max(Math.min(previousEndHue +(gamepad1.right_stick_x*HUE_JOYSTICK_SCALAR),360),0);

        hueTelemetry = String.format("Start hue/end hue: %4.2f %4.2f", hues[0], hues[1]);

        return hues;
    }

    /** Prints the joystick instructions for color editing. */
    public void showHsbTelemetry(){
        telemetry.addLine("Move the joysticks to configure the color of the LEDs");
        telemetry.addLine("Left Joystick X (side-to-side) Changes the Hue of a color");
        telemetry.addLine("Right Joystick X (side-to-side) changes the Saturation");
        telemetry.addLine("Right Joystick Y (up-and-down) changes the Brightness");
        telemetry.addLine("");
    }

    /**
     * When Y is pressed, advance which color is being edited:
     * primary -> secondary -> (tertiary, if the animation has 3 colors) -> primary.
     */
    public void toggleThroughColors(boolean button, boolean thirdColor){
        if(button){
            switch (animationColor){
                case PRIMARY_COLOR:
                    animationColor = AnimationColor.SECONDARY_COLOR;
                    break;
                case SECONDARY_COLOR:
                    if(thirdColor){
                        animationColor = AnimationColor.TERTIARY_COLOR;
                    } else{
                        animationColor = AnimationColor.PRIMARY_COLOR;
                    }
                    break;
                case TERTIARY_COLOR:
                    animationColor = AnimationColor.PRIMARY_COLOR;
                    break;
            }
        }
    }
}