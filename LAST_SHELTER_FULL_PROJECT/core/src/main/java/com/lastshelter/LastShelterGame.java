package com.lastshelter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.lastshelter.bunkerevents.DailyBunkerEvent;
import com.lastshelter.commands.AnalyseLogsCommand;
import com.lastshelter.commands.BunkerCommand;
import com.lastshelter.commands.ExploreCommand;
import com.lastshelter.commands.RepairCommand;
import com.lastshelter.commands.RestCommand;
import com.lastshelter.core.BunkerCore;
import com.lastshelter.events.GameEvent;
import com.lastshelter.events.GameEventType;
import com.lastshelter.utility.CheckpointSystem;
import com.lastshelter.utility.GameSnapshot;
import com.lastshelter.utility.SaveSystem;
import com.lastshelter.utility.SettingsSystem;
import com.lastshelter.world.Door;
import com.lastshelter.world.DoorState;

public class LastShelterGame extends ApplicationAdapter {
    private static final float ROOM_HEIGHT = 4f;
    private static final float WALL_THICKNESS = 0.35f;
    private static final float DOOR_WIDTH = 3f;
    private static final float EYE_HEIGHT = 1.65f;
    private static final float PLAYER_RADIUS = 0.35f;
    private static final float WALK_SPEED = 3.2f;
    private static final float SPRINT_SPEED = 5.2f;
    private static final float MOUSE_SENSITIVITY = 0.16f;
    private static final String KEYPAD_CODE = "427";

    private final Vector3 move = new Vector3();
    private final Vector3 flatForward = new Vector3();
    private final Vector3 right = new Vector3();
    private final Array<ModelInstance> bunker = new Array<>();
    private final Array<Door> doors = new Array<>();
    private final Array<Spinner> spinners = new Array<>();
    private final Array<Blinker> blinkers = new Array<>();
    private final Array<EffectParticle> particles = new Array<>();
    private final Array<LightPulse> lightPulses = new Array<>();
    private final Array<WalkArea> walkableAreas = new Array<>();
    private final Array<Blocker> blockers = new Array<>();
    private final Array<Interactable> interactables = new Array<>();
    private final Array<Task> tasks = new Array<>();
    private final Array<DailyPlan> dailyPlans = new Array<>();
    private final Array<BunkerRoom> rooms = new Array<>();
    private final BunkerCore bunkerCore = BunkerCore.getInstance();

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model roomModel;
    private Environment environment;
    private SpriteBatch hudBatch;
    private BitmapFont hudFont;
    private float yaw = 180f;
    private float pitch;
    private String interactionMessage = "";
    private float interactionMessageTime;
    private String aiMessage = "";
    private float aiMessageTime;
    private float resourceMonitorTime;
    private MiniGame activeMiniGame = MiniGame.NONE;
    private Interactable activeInteractable;
    private float timingMarker;
    private float timingDirection = 1f;
    private int wireStep;
    private String keypadInput = "";
    private int day = 1;
    private int health = 100;
    private int stress = 18;
    private int foodSupply = 75;
    private int powerLevel = 78;
    private int aiTrust = 45;
    private int morality = 60;
    private int daysWithoutFood;
    private String gameOverReason = "";
    private boolean endingReached;
    private PointLight flashlight;
    private float effectTime;
    private float headBobTime;
    private boolean playerWalking;
    private SaveSystem saveSystem;
    private CheckpointSystem checkpointSystem;
    private SettingsSystem settingsSystem;
    private UtilityScreen utilityScreen = UtilityScreen.LOADING;
    private float loadingTime;
    private float autosaveTime;
    private boolean fullscreen;
    private float shakeTime;
    private float shakeDuration;
    private float shakeStrength;

    @Override
    public void create() {
        camera = new PerspectiveCamera(70f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, EYE_HEIGHT, 2.5f);
        camera.near = 0.1f;
        camera.far = 60f;
        updateCameraDirection();

        modelBatch = new ModelBatch();
        hudBatch = new SpriteBatch();
        hudFont = new BitmapFont();
        hudFont.setColor(Color.WHITE);
        saveSystem = new SaveSystem();
        checkpointSystem = new CheckpointSystem();
        settingsSystem = new SettingsSystem();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.22f, 0.22f, 0.25f, 1f));
        environment.add(new DirectionalLight().set(0.85f, 0.82f, 0.72f, -0.4f, -1f, -0.25f));
        flashlight = new PointLight().set(0.55f, 0.58f, 0.5f, 0f, EYE_HEIGHT, 0f, 8f);
        environment.add(flashlight);

        buildBunker();
        buildDailyPlans();
        bunkerCore.getEventBus().subscribe(event -> {
            if (event.getType() == GameEventType.STATE_CHANGED) {
                announceAi(bunkerCore.getAiStrategy().warning(event.getMessage()));
                if (bunkerCore.getState().name().equals("CRITICAL")) {
                    startShake(0.45f, 0.05f);
                }
            }
        });
        startDay();
        checkpointSystem.mark(createSnapshot());
        Gdx.input.setCursorCatched(true);
    }

    private void buildBunker() {
        ModelBuilder builder = new ModelBuilder();
        roomModel = builder.createBox(
            1f,
            1f,
            1f,
            new com.badlogic.gdx.graphics.g3d.Material(),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        addRoom("Main Hall", 0f, 0f, 14f, 12f, true, true, true, true,
            new Color(0.28f, 0.3f, 0.32f, 1f), new Color(1f, 0.85f, 0.58f, 1f));
        addRoom("Generator Room", -15f, 0f, 10f, 10f, false, true, false, false,
            new Color(0.32f, 0.29f, 0.22f, 1f), new Color(1f, 0.62f, 0.22f, 1f));
        addRoom("Water Sector", 15f, 0f, 10f, 10f, true, false, false, false,
            new Color(0.18f, 0.28f, 0.32f, 1f), new Color(0.28f, 0.78f, 1f, 1f));
        addRoom("AI Core Room", 0f, -15f, 10f, 10f, false, false, false, true,
            new Color(0.22f, 0.2f, 0.31f, 1f), new Color(0.82f, 0.42f, 1f, 1f));
        addRoom("Storage Room", 0f, 15f, 10f, 10f, false, false, true, true,
            new Color(0.29f, 0.27f, 0.22f, 1f), new Color(0.55f, 1f, 0.48f, 1f));
        addRoom("Exit Sector", 0f, 31f, 10f, 10f, false, false, true, true,
            new Color(0.25f, 0.27f, 0.29f, 1f), new Color(1f, 0.22f, 0.16f, 1f));

        addHorizontalCorridor(-8.5f, 0f, 4f, DOOR_WIDTH);
        addHorizontalCorridor(8.5f, 0f, 4f, DOOR_WIDTH);
        addVerticalCorridor(0f, -8.5f, DOOR_WIDTH, 4f, true);
        addVerticalCorridor(0f, 8.5f, DOOR_WIDTH, 4f, true);
        addVerticalCorridor(0f, 23f, DOOR_WIDTH, 6f, true);

        addRoomProps();
        addAiFixtures();
        addAtmosphereFixtures();
        addExitSectorSecurity();
    }

    private void addBox(float width, float height, float depth, float x, float y, float z, Color color) {
        addBoxInstance(width, height, depth, x, y, z, color);
    }

    private ModelInstance addBoxInstance(float width, float height, float depth, float x, float y, float z,
        Color color) {
        ModelInstance instance = new ModelInstance(roomModel);
        instance.transform.setToTranslation(x, y, z).scale(width, height, depth);
        instance.materials.first().set(ColorAttribute.createDiffuse(color));
        bunker.add(instance);
        return instance;
    }

    private void addRoom(String name, float x, float z, float width, float depth, boolean westDoor,
        boolean eastDoor, boolean northDoor, boolean southDoor, Color wallColor, Color lightColor) {
        Color floorColor = wallColor.cpy().mul(0.78f, 0.78f, 0.78f, 1f);
        Color ceilingColor = wallColor.cpy().mul(0.45f, 0.45f, 0.45f, 1f);
        addBox(width, WALL_THICKNESS, depth, x, -WALL_THICKNESS / 2f, z, floorColor);
        addBox(width, WALL_THICKNESS, depth, x, ROOM_HEIGHT + WALL_THICKNESS / 2f, z, ceilingColor);
        addWallX(x - width / 2f, z, depth, westDoor, wallColor);
        addWallX(x + width / 2f, z, depth, eastDoor, wallColor);
        addWallZ(x, z - depth / 2f, width, northDoor, wallColor);
        addWallZ(x, z + depth / 2f, width, southDoor, wallColor);
        addBox(1.25f, 0.12f, 1.25f, x, ROOM_HEIGHT - 0.18f, z, lightColor);

        walkableAreas.add(new WalkArea(x, z, width - WALL_THICKNESS - PLAYER_RADIUS * 2f,
            depth - WALL_THICKNESS - PLAYER_RADIUS * 2f));
        rooms.add(new BunkerRoom(name, x, z, width, depth));
        PointLight roomLight = new PointLight().set(lightColor.r, lightColor.g, lightColor.b, x, 3.35f, z, 16f);
        environment.add(roomLight);
        lightPulses.add(new LightPulse(roomLight, lightColor, name.equals("Generator Room")
            || name.equals("Bunker Corridor")));
    }

    private void addWallX(float x, float z, float depth, boolean hasDoor, Color wallColor) {
        if (!hasDoor) {
            addBox(WALL_THICKNESS, ROOM_HEIGHT, depth, x, ROOM_HEIGHT / 2f, z, wallColor);
            return;
        }

        float segmentDepth = (depth - DOOR_WIDTH) / 2f;
        addBox(WALL_THICKNESS, ROOM_HEIGHT, segmentDepth, x, ROOM_HEIGHT / 2f,
            z - DOOR_WIDTH / 2f - segmentDepth / 2f, wallColor);
        addBox(WALL_THICKNESS, ROOM_HEIGHT, segmentDepth, x, ROOM_HEIGHT / 2f,
            z + DOOR_WIDTH / 2f + segmentDepth / 2f, wallColor);
    }

    private void addWallZ(float x, float z, float width, boolean hasDoor, Color wallColor) {
        if (!hasDoor) {
            addBox(width, ROOM_HEIGHT, WALL_THICKNESS, x, ROOM_HEIGHT / 2f, z, wallColor);
            return;
        }

        float segmentWidth = (width - DOOR_WIDTH) / 2f;
        addBox(segmentWidth, ROOM_HEIGHT, WALL_THICKNESS,
            x - DOOR_WIDTH / 2f - segmentWidth / 2f, ROOM_HEIGHT / 2f, z, wallColor);
        addBox(segmentWidth, ROOM_HEIGHT, WALL_THICKNESS,
            x + DOOR_WIDTH / 2f + segmentWidth / 2f, ROOM_HEIGHT / 2f, z, wallColor);
    }

    private void addHorizontalCorridor(float x, float z, float width, float depth) {
        Color wallColor = new Color(0.22f, 0.23f, 0.24f, 1f);
        addBox(width, WALL_THICKNESS, depth, x, -WALL_THICKNESS / 2f, z, wallColor);
        addBox(width, WALL_THICKNESS, depth, x, ROOM_HEIGHT + WALL_THICKNESS / 2f, z, wallColor.cpy().mul(0.5f));
        addBox(width, ROOM_HEIGHT, WALL_THICKNESS, x, ROOM_HEIGHT / 2f, z - depth / 2f, wallColor);
        addBox(width, ROOM_HEIGHT, WALL_THICKNESS, x, ROOM_HEIGHT / 2f, z + depth / 2f, wallColor);
        walkableAreas.add(new WalkArea(x, z, width + PLAYER_RADIUS * 2f, depth - PLAYER_RADIUS));
    }

    private void addVerticalCorridor(float x, float z, float width, float depth, boolean openEnds) {
        Color wallColor = new Color(0.22f, 0.23f, 0.24f, 1f);
        addBox(width, WALL_THICKNESS, depth, x, -WALL_THICKNESS / 2f, z, wallColor);
        addBox(width, WALL_THICKNESS, depth, x, ROOM_HEIGHT + WALL_THICKNESS / 2f, z, wallColor.cpy().mul(0.5f));
        addBox(WALL_THICKNESS, ROOM_HEIGHT, depth, x - width / 2f, ROOM_HEIGHT / 2f, z, wallColor);
        addBox(WALL_THICKNESS, ROOM_HEIGHT, depth, x + width / 2f, ROOM_HEIGHT / 2f, z, wallColor);
        if (!openEnds) {
            addBox(width, ROOM_HEIGHT, WALL_THICKNESS, x, ROOM_HEIGHT / 2f, z + depth / 2f, wallColor);
        }
        walkableAreas.add(new WalkArea(x, z, width - PLAYER_RADIUS, depth + PLAYER_RADIUS * 2f));
    }

    private void addRoomProps() {
        addMainHallDesign();
        addGeneratorRoomDesign();
        addWaterSectorDesign();
        addAiCoreDesign();
        addStorageDesign();
    }

    private void addMainHallDesign() {
        Color metal = new Color(0.36f, 0.39f, 0.41f, 1f);
        Color darkMetal = new Color(0.19f, 0.21f, 0.23f, 1f);
        Color sign = new Color(0.84f, 0.2f, 0.14f, 1f);
        addBox(11.6f, 0.14f, 0.18f, 0f, 0.18f, -4.95f, darkMetal);
        addBox(11.6f, 0.14f, 0.18f, 0f, 0.18f, 4.95f, darkMetal);
        blinkers.add(new Blinker(addBoxInstance(1.4f, 0.42f, 0.08f, -3.9f, 2.25f, -5.72f, sign),
            sign, 1.8f));
        blinkers.add(new Blinker(addBoxInstance(1.4f, 0.42f, 0.08f, 3.9f, 2.25f, -5.72f, sign),
            sign, 2.2f));
        addCamera(-5.4f, 4.5f, darkMetal);
        addProp(2.8f, 0.55f, 1.1f, -2.3f, 0.28f, 1.8f, new Color(0.38f, 0.39f, 0.4f, 1f));
        addTaskInteractable("Communication Panel", "Restore communication panel", "Communication restored",
            "Restore communication panel", 2.3f, -1.5f, new Color(0.95f, 0.78f, 0.35f, 1f),
            MiniGame.NONE);
    }

    private void addGeneratorRoomDesign() {
        Color generator = new Color(0.46f, 0.34f, 0.16f, 1f);
        Color cable = new Color(0.08f, 0.08f, 0.09f, 1f);
        Color panel = new Color(0.74f, 0.56f, 0.16f, 1f);
        addProp(3.2f, 1.8f, 2.3f, -15f, 0.9f, 0f, generator);
        addProp(2.1f, 0.34f, 1.25f, -15f, 1.98f, 0f, new Color(0.58f, 0.42f, 0.18f, 1f));
        addProp(0.18f, 0.1f, 4.4f, -12.9f, 0.08f, 0.45f, cable);
        addProp(0.18f, 0.1f, 4f, -17.2f, 0.08f, -0.45f, cable);
        addProp(1.15f, 1.8f, 0.24f, -19.25f, 1.05f, -1.9f, panel);
        addProp(1.15f, 1.8f, 0.24f, -19.25f, 1.05f, 1.9f, panel);
        addProp(0.3f, 2.1f, 0.3f, -18.3f, 1.05f, -2.2f, new Color(0.62f, 0.45f, 0.16f, 1f));
        addTaskInteractable("Generator Panel", "Repair generator", "Generator repaired", "Repair generator",
            -13f, -2.1f, new Color(1f, 0.48f, 0.18f, 1f), MiniGame.GENERATOR_TIMING);
    }

    private void addWaterSectorDesign() {
        Color pipe = new Color(0.1f, 0.39f, 0.47f, 1f);
        Color valve = new Color(0.14f, 0.72f, 0.84f, 1f);
        addProp(0.34f, 3.2f, 0.34f, 18.6f, 1.6f, -2.9f, pipe);
        addProp(4.7f, 0.34f, 0.34f, 16.4f, 2.9f, -2.9f, pipe);
        addProp(0.34f, 2.6f, 0.34f, 11.7f, 1.3f, 2.8f, pipe);
        addProp(4.9f, 0.34f, 0.34f, 14.1f, 2.45f, 2.8f, pipe);
        addValve(12.7f, 2.1f, valve);
        addValve(17.4f, -1.5f, valve);
        addProp(1.6f, 1.35f, 2.6f, 16.8f, 0.68f, 1.4f, new Color(0.1f, 0.36f, 0.43f, 1f));
        addProp(3.4f, 0.25f, 0.5f, 13.8f, 0.13f, -2.8f, new Color(0.2f, 0.54f, 0.6f, 1f));
        addTaskInteractable("Broken Pipe", "Fix broken pipe", "Broken pipe fixed", "Fix broken pipe",
            13f, -1.9f, new Color(0.3f, 0.9f, 1f, 1f), MiniGame.WIRE_RECONNECT);
    }

    private void addValve(float x, float z, Color color) {
        addProp(0.18f, 0.18f, 0.9f, x, 1.45f, z, color);
        addProp(0.9f, 0.18f, 0.18f, x, 1.45f, z, color);
    }

    private void addAiCoreDesign() {
        Color tower = new Color(0.16f, 0.16f, 0.24f, 1f);
        Color monitor = new Color(0.32f, 0.26f, 0.78f, 1f);
        addServerTower(-2.9f, -17.4f, tower);
        addServerTower(2.9f, -17.4f, tower);
        addServerTower(-2.9f, -12.9f, tower);
        addServerTower(2.9f, -12.9f, tower);
        addProp(2.2f, 2.2f, 2.2f, 0f, 1.1f, -16.6f, new Color(0.28f, 0.16f, 0.48f, 1f));
        addProp(3.2f, 0.22f, 0.65f, -2.4f, 1.55f, -13.8f, new Color(0.45f, 0.28f, 0.68f, 1f));
        blinkers.add(new Blinker(addBoxInstance(1.2f, 0.72f, 0.1f, -3.7f, 2.05f, -15.1f, monitor),
            monitor, 4.1f));
        blinkers.add(new Blinker(addBoxInstance(1.2f, 0.72f, 0.1f, 3.7f, 2.05f, -15.1f, monitor),
            monitor, 4.6f));
        addTaskInteractable("AI Terminal", "Reboot AI terminal", "AI terminal rebooted", "Reboot AI terminal",
            2.3f, -13.1f, new Color(0.92f, 0.48f, 1f, 1f), MiniGame.KEYPAD_TERMINAL);
    }

    private void addServerTower(float x, float z, Color color) {
        addProp(1.05f, 2.75f, 0.9f, x, 1.38f, z, color);
        addProp(0.75f, 0.08f, 0.1f, x, 2.05f, z + 0.46f, new Color(0.26f, 0.7f, 0.86f, 1f));
    }

    private void addStorageDesign() {
        Color shelf = new Color(0.28f, 0.29f, 0.25f, 1f);
        Color crate = new Color(0.44f, 0.34f, 0.19f, 1f);
        Color food = new Color(0.72f, 0.52f, 0.23f, 1f);
        Color fuel = new Color(0.18f, 0.52f, 0.35f, 1f);
        addShelf(-3.7f, 14.8f, shelf);
        addShelf(3.7f, 16.1f, shelf);
        addProp(1.25f, 1.4f, 1.25f, -2.6f, 0.7f, 16.6f, crate);
        addProp(1.25f, 1.9f, 1.25f, 2.2f, 0.95f, 17.1f, crate);
        addProp(0.8f, 0.55f, 0.65f, -3.7f, 0.55f, 14.8f, food);
        addProp(0.8f, 0.55f, 0.65f, -3.7f, 1.55f, 14.8f, food);
        addProp(0.55f, 1.1f, 0.55f, 3.7f, 0.72f, 16.1f, fuel);
        addProp(0.55f, 1.1f, 0.55f, 3.7f, 1.85f, 16.1f, fuel);
        addTaskInteractable("Storage Crate", "Collect food supply", "Food supply collected",
            "Collect food supply", 1.9f, 12.8f, new Color(0.62f, 1f, 0.42f, 1f), MiniGame.NONE);
    }

    private void addShelf(float x, float z, Color color) {
        addProp(1.8f, 0.12f, 0.82f, x, 0.42f, z, color);
        addProp(1.8f, 0.12f, 0.82f, x, 1.25f, z, color);
        addProp(1.8f, 0.12f, 0.82f, x, 2.08f, z, color);
        addProp(0.12f, 2.2f, 0.12f, x - 0.78f, 1.1f, z, color);
        addProp(0.12f, 2.2f, 0.12f, x + 0.78f, 1.1f, z, color);
    }

    private void addAiFixtures() {
        Color screen = new Color(0.22f, 0.7f, 0.82f, 1f);
        Color cameraShell = new Color(0.12f, 0.13f, 0.15f, 1f);
        Color speaker = new Color(0.28f, 0.29f, 0.31f, 1f);
        addCamera(5.4f, -4.5f, cameraShell);
        addCamera(-18.6f, -3.7f, cameraShell);
        addCamera(18.7f, 3.6f, cameraShell);
        addCamera(-3.8f, -18.7f, cameraShell);
        addScreen(0.1f, -18.7f, screen);
        addSpeaker(-5.8f, 4.8f, speaker);
        addSpeaker(4.1f, 18.8f, speaker);
    }

    private void addCamera(float x, float z, Color color) {
        addBox(0.45f, 0.32f, 0.45f, x, 3.35f, z, color);
        addBox(0.08f, 0.42f, 0.08f, x, 3.62f, z, color);
    }

    private void addScreen(float x, float z, Color color) {
        blinkers.add(new Blinker(addBoxInstance(1.5f, 0.9f, 0.12f, x, 1.95f, z, color), color, 3.4f));
    }

    private void addSpeaker(float x, float z, Color color) {
        addBox(0.65f, 0.65f, 0.16f, x, 2.65f, z, color);
    }

    private void addAtmosphereFixtures() {
        Color door = new Color(0.24f, 0.27f, 0.29f, 1f);
        addDoor(-7f, 0f, true, door, true, DoorState.CLOSED);
        addDoor(7f, 0f, true, door, true, DoorState.CLOSED);
        addDoor(0f, -6f, false, door, true, DoorState.CLOSED);
        addDoor(0f, 6f, false, door, true, DoorState.CLOSED);

        addFan(-16.2f, -1.4f);
        addFan(14.9f, 1.7f);
        addFan(0f, -15f);
        addGeneratorParts();
        addHallwayWarnings();
        addRoomParticles();
    }

    private Door addDoor(float x, float z, boolean verticalSplit, Color color, boolean automatic, DoorState state) {
        if (verticalSplit) {
            ModelInstance low = addBoxInstance(0.18f, 2.8f, 1.35f, x, 1.4f, z - 0.72f, color);
            ModelInstance high = addBoxInstance(0.18f, 2.8f, 1.35f, x, 1.4f, z + 0.72f, color);
            Door door = new Door(low, high, x, z, true, 0.22f, DOOR_WIDTH, automatic, state);
            doors.add(door);
            addDoorPassage(x, z, verticalSplit, state);
            return door;
        } else {
            ModelInstance left = addBoxInstance(1.35f, 2.8f, 0.18f, x - 0.72f, 1.4f, z, color);
            ModelInstance rightPanel = addBoxInstance(1.35f, 2.8f, 0.18f, x + 0.72f, 1.4f, z, color);
            Door door = new Door(left, rightPanel, x, z, false, DOOR_WIDTH, 0.22f, automatic, state);
            doors.add(door);
            addDoorPassage(x, z, verticalSplit, state);
            return door;
        }
    }

    private void addDoorPassage(float x, float z, boolean verticalSplit, DoorState state) {
        if (state == DoorState.LOCKED) {
            return;
        }
        float passageDepth = DOOR_WIDTH + PLAYER_RADIUS * 2f;
        float passageThickness = WALL_THICKNESS + PLAYER_RADIUS * 3f;
        if (verticalSplit) {
            walkableAreas.add(new WalkArea(x, z, passageThickness, passageDepth));
        } else {
            walkableAreas.add(new WalkArea(x, z, passageDepth, passageThickness));
        }
    }

    private void addFan(float x, float z) {
        Color fanColor = new Color(0.19f, 0.2f, 0.22f, 1f);
        addBox(0.12f, 0.38f, 0.12f, x, 3.58f, z, fanColor);
        ModelInstance blade = addBoxInstance(2.1f, 0.08f, 0.16f, x, 3.34f, z, fanColor);
        ModelInstance crossBlade = addBoxInstance(0.16f, 0.08f, 2.1f, x, 3.34f, z, fanColor);
        spinners.add(new Spinner(blade, x, 3.34f, z, 2.1f, 0.08f, 0.16f, 180f));
        spinners.add(new Spinner(crossBlade, x, 3.34f, z, 0.16f, 0.08f, 2.1f, 180f));
    }

    private void addGeneratorParts() {
        Color part = new Color(0.58f, 0.42f, 0.18f, 1f);
        ModelInstance rotor = addBoxInstance(1.25f, 0.2f, 0.45f, -15f, 1.95f, 0f, part);
        spinners.add(new Spinner(rotor, -15f, 1.95f, 0f, 1.25f, 0.2f, 0.45f, 120f));
    }

    private void addHallwayWarnings() {
        Color sign = new Color(0.82f, 0.16f, 0.12f, 1f);
        blinkers.add(new Blinker(addBoxInstance(1.25f, 0.42f, 0.08f, -8.5f, 2.55f, -1.33f, sign),
            sign, 2.1f));
        blinkers.add(new Blinker(addBoxInstance(0.08f, 0.42f, 1.25f, 1.33f, 2.55f, 8.5f, sign),
            sign, 2.7f));
        addAlarmLight(-8.5f, 0f);
        addAlarmLight(0f, 8.5f);
    }

    private void addAlarmLight(float x, float z) {
        Color red = new Color(1f, 0.15f, 0.1f, 1f);
        blinkers.add(new Blinker(addBoxInstance(0.38f, 0.18f, 0.38f, x, 3.72f, z, red), red, 4.2f));
    }

    private void addRoomParticles() {
        addParticleSet(EffectKind.SPARK, -17.7f, 2.1f, -1.5f, 4, new Color(1f, 0.72f, 0.18f, 1f));
        addParticleSet(EffectKind.STEAM, -14.3f, 0.25f, 2.7f, 4, new Color(0.66f, 0.7f, 0.7f, 1f));
        addParticleSet(EffectKind.SMOKE, -16.8f, 2.25f, 1.4f, 3, new Color(0.28f, 0.29f, 0.3f, 1f));
        addParticleSet(EffectKind.WATER, 13.8f, 2.35f, -2.8f, 5, new Color(0.22f, 0.75f, 1f, 1f));
    }

    private void addParticleSet(EffectKind kind, float x, float y, float z, int count, Color color) {
        for (int i = 0; i < count; i++) {
            float size = kind == EffectKind.SPARK || kind == EffectKind.WATER ? 0.08f : 0.16f;
            ModelInstance instance = addBoxInstance(size, size, size, x, y, z, color);
            particles.add(new EffectParticle(instance, kind, x, y, z, size, i / (float)count));
        }
    }

    private void addExitSectorSecurity() {
        Color exitDoor = new Color(0.36f, 0.08f, 0.07f, 1f);
        Color scanner = new Color(0.13f, 0.58f, 0.68f, 1f);
        addDoor(0f, 36f, false, exitDoor, false, DoorState.LOCKED);
        addBox(4.2f, 0.26f, 0.32f, 0f, 3.15f, 35.72f, new Color(0.18f, 0.19f, 0.2f, 1f));
        addBox(0.34f, 2.8f, 0.34f, -2.15f, 1.4f, 34.3f, scanner);
        addBox(0.34f, 2.8f, 0.34f, 2.15f, 1.4f, 34.3f, scanner);
        addAlarmLight(-1.7f, 34.9f);
        addAlarmLight(1.7f, 34.9f);
        interactables.add(new Interactable("Main Exit", "Check exit lockdown", "Emergency lockdown active",
            0f, 34.25f, null, MiniGame.NONE, true));
    }

    private void addProp(float width, float height, float depth, float x, float y, float z, Color color) {
        addBox(width, height, depth, x, y, z, color);
    }

    private void addTaskInteractable(String name, String prompt, String actionMessage, String taskName, float x,
        float z, Color color, MiniGame miniGame) {
        Task task = new Task(taskName);
        tasks.add(task);
        addBox(0.7f, 1.15f, 0.7f, x, 0.58f, z, color);
        interactables.add(new Interactable(name, prompt, actionMessage, x, z, task, miniGame, false));
    }

    private void buildDailyPlans() {
        addDay("Repair generator", "Generator output unstable", Problem.GENERATOR, -2, 7, -6, -14, 2, 1);
        addDay("Fix broken pipe", "Pipe leak in Water Sector", Problem.PIPE, -3, 8, -7, -9, 1, 2);
        addDay("Collect food supply", "Rations running low", Problem.RATIONS, 1, 5, -10, -5, 0, 1);
        addDay("Reboot AI terminal", "AI routing errors", Problem.AI_GLITCH, -1, 9, -6, -7, -4, 0);
        addDay("Restore communication panel", "Signal blackout", Problem.COMMS, -1, 6, -7, -6, 2, -1);
        addDay("Repair generator", "Generator cooling alarm", Problem.GENERATOR, -4, 11, -7, -16, 1, 0);
        addDay("Fix broken pipe", "Water pressure collapse", Problem.PIPE, -4, 10, -8, -8, 0, 1);
        addDay("Collect food supply", "Storage inventory gap", Problem.RATIONS, 0, 7, -12, -6, 0, 0);
        addDay("Reboot AI terminal", "AI trust audit", Problem.AI_GLITCH, -2, 10, -7, -7, -5, -2);
        addDay("Restore communication panel", "Final distress signal", Problem.COMMS, -2, 8, -8, -8, 3, 2);
    }

    private void addDay(String objective, String bunkerProblem, Problem problem, int healthChange, int stressChange,
        int foodChange, int powerChange, int trustChange, int moralityChange) {
        dailyPlans.add(new DailyPlan(objective, bunkerProblem, problem, healthChange, stressChange, foodChange,
            powerChange, trustChange, moralityChange));
    }

    private void startDay() {
        for (Task task : tasks) {
            task.complete = false;
        }
        interactionMessage = "Day " + day + ": " + currentDay().mainObjective;
        interactionMessageTime = 3f;
        announceAi(objectiveSuggestion());
        bunkerCore.getEventBus().publish(new GameEvent(GameEventType.DAY_STARTED, "Day " + day));
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        handleFullscreenToggle();
        if (utilityScreen == UtilityScreen.LOADING) {
            updateLoading(delta);
            drawLoadingScreen();
            return;
        }
        if (utilityScreen == UtilityScreen.PAUSE || utilityScreen == UtilityScreen.SETTINGS) {
            handleUtilityMenu();
            drawWorld();
            drawHud();
            drawUtilityMenu();
            return;
        }
        aiMessageTime = Math.max(0f, aiMessageTime - delta);
        if (isRunFinished()) {
            handleFinishedRun();
        } else if (activeMiniGame == MiniGame.NONE) {
            handleLook();
            handleMovement(delta);
            handleInteraction(delta);
            handleDayAdvance();
            monitorResources(delta);
        } else {
            updateMiniGame(delta);
        }
        updateAtmosphere(delta);
        updateBunkerState();
        autosave(delta);
        drawWorld();
        drawHud();
        if (isRunFinished()) {
            drawRunFinishedScreen();
        } else if (activeMiniGame != MiniGame.NONE) {
            drawMiniGame();
        }
    }

    private void drawWorld() {
        Gdx.gl.glClearColor(0.035f, 0.04f, 0.045f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(bunker, environment);
        modelBatch.end();
    }

    private void handleLook() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            pauseGame();
            return;
        }
        if (!Gdx.input.isCursorCatched()) {
            return;
        }

        yaw -= Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
        pitch = MathUtils.clamp(pitch - Gdx.input.getDeltaY() * MOUSE_SENSITIVITY, -88f, 88f);
        updateCameraDirection();
    }

    private void updateCameraDirection() {
        camera.direction.set(
            MathUtils.sinDeg(yaw) * MathUtils.cosDeg(pitch),
            MathUtils.sinDeg(pitch),
            MathUtils.cosDeg(yaw) * MathUtils.cosDeg(pitch)
        ).nor();
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private void handleMovement(float delta) {
        flatForward.set(camera.direction.x, 0f, camera.direction.z).nor();
        right.set(flatForward).crs(Vector3.Y).nor();
        move.setZero();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            move.add(flatForward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            move.sub(flatForward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            move.add(right);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            move.sub(right);
        }

        if (!move.isZero()) {
            playerWalking = true;
            float speed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) ? SPRINT_SPEED : WALK_SPEED;
            move.nor().scl(speed * delta);
            movePlayer(move.x, move.z);
            camera.update();
        } else {
            playerWalking = false;
        }
    }

    private void movePlayer(float xAmount, float zAmount) {
        float nextX = camera.position.x + xAmount;
        if (canStandAt(nextX, camera.position.z)) {
            camera.position.x = nextX;
        }

        float nextZ = camera.position.z + zAmount;
        if (canStandAt(camera.position.x, nextZ)) {
            camera.position.z = nextZ;
        }
        camera.position.y = EYE_HEIGHT;
    }

    private boolean canStandAt(float x, float z) {
        boolean insideBunker = false;
        for (WalkArea area : walkableAreas) {
            if (area.contains(x, z)) {
                insideBunker = true;
                break;
            }
        }
        if (!insideBunker) {
            return false;
        }

        for (Blocker blocker : blockers) {
            if (blocker.contains(x, z)) {
                return false;
            }
        }
        for (Door door : doors) {
            if (door.blocks(x, z, PLAYER_RADIUS)) {
                return false;
            }
        }
        return true;
    }

    private void handleInteraction(float delta) {
        interactionMessageTime = Math.max(0f, interactionMessageTime - delta);
        Interactable nearest = nearestInteractable();
        if (nearest != null && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (nearest.opensOuterDoor) {
                announceAi("Sector locked for safety.");
                interactionMessage = nearest.actionMessage;
                interactionMessageTime = 2.25f;
                return;
            }
            if (nearest.task != null && !nearest.task.complete) {
                if (nearest.miniGame != MiniGame.NONE) {
                    startMiniGame(nearest);
                    return;
                }
                completeTask(nearest);
            } else if (nearest.task != null) {
                interactionMessage = nearest.actionMessage;
            } else {
                interactionMessage = nearest.actionMessage;
            }
            interactionMessageTime = 2.25f;
        }
    }

    private void handleDayAdvance() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            return;
        }
        if (!isTaskComplete(currentDay().mainObjective)) {
            interactionMessage = "Finish main objective before ending day";
            interactionMessageTime = 2.25f;
            announceAi(objectiveSuggestion());
            return;
        }
        new RestCommand().execute(bunkerCore.getEventBus());
        resolveDay();
    }

    private void resolveDay() {
        DailyPlan plan = currentDay();
        if (plan.problem == Problem.GENERATOR && !isTaskComplete("Repair generator")) {
            announceAi("Generator fault unresolved.");
            startShake(0.7f, 0.08f);
            gameOver("Generator problem ignored");
            return;
        }
        if (plan.problem == Problem.PIPE && !isTaskComplete("Fix broken pipe")) {
            announceAi("Water pressure unstable.");
            startShake(0.6f, 0.06f);
            gameOver("Pipe problem ignored");
            return;
        }
        new DailyBunkerEvent("Day " + day + " condition check", true, () -> applyDayChanges(plan))
            .trigger(bunkerCore.getEventBus());
    }

    private void applyDayChanges(DailyPlan plan) {
        health += plan.healthChange;
        stress += plan.stressChange;
        foodSupply += plan.foodChange;
        powerLevel += plan.powerChange;
        aiTrust += plan.trustChange;
        morality += plan.moralityChange;
        applyTaskResourceHelp();
        clampResources();

        if (foodSupply == 0) {
            daysWithoutFood++;
        } else {
            daysWithoutFood = 0;
        }
        if (!checkSurvivalConditions()) {
            return;
        }
        if (day == 10) {
            endingReached = true;
            Gdx.input.setCursorCatched(false);
            saveGame("Autosaved ending");
            return;
        }
        day++;
        startDay();
        saveCheckpoint("Checkpoint: Day " + day);
    }

    private void applyTaskResourceHelp() {
        if (isTaskComplete("Repair generator")) {
            powerLevel += 15;
        }
        if (isTaskComplete("Fix broken pipe")) {
            health += 3;
            stress -= 3;
        }
        if (isTaskComplete("Collect food supply")) {
            foodSupply += 18;
        }
        if (isTaskComplete("Reboot AI terminal")) {
            aiTrust += 8;
            stress -= 2;
        }
        if (isTaskComplete("Restore communication panel")) {
            morality += 5;
            stress -= 2;
        }
    }

    private void clampResources() {
        health = MathUtils.clamp(health, 0, 100);
        stress = MathUtils.clamp(stress, 0, 100);
        foodSupply = MathUtils.clamp(foodSupply, 0, 100);
        powerLevel = MathUtils.clamp(powerLevel, 0, 100);
        aiTrust = MathUtils.clamp(aiTrust, 0, 100);
        morality = MathUtils.clamp(morality, 0, 100);
    }

    private boolean checkSurvivalConditions() {
        if (powerLevel == 0) {
            announceAi("Power level critical.");
            startShake(0.8f, 0.09f);
            gameOver("Power reached zero");
            return false;
        }
        if (stress == 100) {
            announceAi("Stress threshold exceeded.");
            gameOver("Stress reached maximum");
            return false;
        }
        if (daysWithoutFood >= 2) {
            announceAi("Food reserve exhausted.");
            gameOver("Food supply stayed empty too long");
            return false;
        }
        return true;
    }

    private void gameOver(String reason) {
        gameOverReason = reason;
        bunkerCore.getEventBus().publish(new GameEvent(GameEventType.GAME_OVER, reason));
        closeMiniGame();
        Gdx.input.setCursorCatched(false);
    }

    private boolean isRunFinished() {
        return endingReached || !gameOverReason.isEmpty();
    }

    private void handleFinishedRun() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    private void updateLoading(float delta) {
        loadingTime += delta;
        if (loadingTime >= 0.8f) {
            utilityScreen = UtilityScreen.NONE;
            announceAi(saveSystem.hasSave() ? "Save data available from pause menu." : objectiveSuggestion());
        }
    }

    private void drawLoadingScreen() {
        Gdx.gl.glClearColor(0.02f, 0.025f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        hudBatch.begin();
        hudFont.draw(hudBatch, "LAST SHELTER", Gdx.graphics.getWidth() / 2f - 68f,
            Gdx.graphics.getHeight() / 2f + 24f);
        hudFont.draw(hudBatch, "Loading bunker systems...", Gdx.graphics.getWidth() / 2f - 104f,
            Gdx.graphics.getHeight() / 2f - 12f);
        hudBatch.end();
    }

    private void pauseGame() {
        utilityScreen = UtilityScreen.PAUSE;
        playerWalking = false;
        Gdx.input.setCursorCatched(false);
    }

    private void resumeGame() {
        utilityScreen = UtilityScreen.NONE;
        Gdx.input.setCursorCatched(true);
    }

    private void handleUtilityMenu() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (utilityScreen == UtilityScreen.SETTINGS) {
                utilityScreen = UtilityScreen.PAUSE;
            } else {
                resumeGame();
            }
            return;
        }
        if (utilityScreen == UtilityScreen.PAUSE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                saveGame("Game saved");
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
                loadGame();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                loadCheckpoint();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
                utilityScreen = UtilityScreen.SETTINGS;
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                settingsSystem.changeMasterVolume(-0.1f);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                settingsSystem.changeMasterVolume(0.1f);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                settingsSystem.changeEffectsVolume(-0.1f);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                settingsSystem.changeEffectsVolume(0.1f);
            }
        }
    }

    private void drawUtilityMenu() {
        float x = Gdx.graphics.getWidth() / 2f - 190f;
        float y = Gdx.graphics.getHeight() / 2f + 118f;
        hudBatch.begin();
        if (utilityScreen == UtilityScreen.PAUSE) {
            hudFont.draw(hudBatch, "PAUSED", x, y);
            hudFont.draw(hudBatch, "ESC  Resume", x, y - 34f);
            hudFont.draw(hudBatch, "S  Save   L  Load   C  Checkpoint", x, y - 62f);
            hudFont.draw(hudBatch, "O  Settings   F11  Fullscreen", x, y - 90f);
        } else {
            hudFont.draw(hudBatch, "SETTINGS", x, y);
            hudFont.draw(hudBatch, "Master volume: " + percent(settingsSystem.getMasterVolume())
                + "   LEFT / RIGHT", x, y - 34f);
            hudFont.draw(hudBatch, "Effects volume: " + percent(settingsSystem.getEffectsVolume())
                + "   DOWN / UP", x, y - 62f);
            hudFont.draw(hudBatch, "F11  Fullscreen   ESC  Back", x, y - 90f);
        }
        hudBatch.end();
    }

    private String percent(float value) {
        return Math.round(value * 100f) + "%";
    }

    private void handleFullscreenToggle() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            return;
        }
        if (fullscreen) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        fullscreen = !fullscreen;
    }

    private void autosave(float delta) {
        if (activeMiniGame != MiniGame.NONE || isRunFinished()) {
            return;
        }
        autosaveTime += delta;
        if (autosaveTime >= 45f) {
            saveGame("Autosaved");
        }
    }

    private void saveCheckpoint(String message) {
        checkpointSystem.mark(createSnapshot());
        saveGame(message);
    }

    private void saveGame(String message) {
        saveSystem.save(createSnapshot());
        autosaveTime = 0f;
        interactionMessage = message;
        interactionMessageTime = 2.25f;
    }

    private void loadGame() {
        GameSnapshot snapshot = saveSystem.load();
        if (snapshot == null) {
            interactionMessage = "No save data";
            interactionMessageTime = 2.25f;
            return;
        }
        applySnapshot(snapshot);
        checkpointSystem.mark(createSnapshot());
        interactionMessage = "Save loaded";
        interactionMessageTime = 2.25f;
    }

    private void loadCheckpoint() {
        if (!checkpointSystem.hasCheckpoint()) {
            interactionMessage = "No checkpoint";
            interactionMessageTime = 2.25f;
            return;
        }
        applySnapshot(checkpointSystem.restore());
        interactionMessage = "Checkpoint restored";
        interactionMessageTime = 2.25f;
    }

    private GameSnapshot createSnapshot() {
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.day = day;
        snapshot.health = health;
        snapshot.stress = stress;
        snapshot.foodSupply = foodSupply;
        snapshot.powerLevel = powerLevel;
        snapshot.aiTrust = aiTrust;
        snapshot.morality = morality;
        snapshot.daysWithoutFood = daysWithoutFood;
        snapshot.playerX = camera.position.x;
        snapshot.playerZ = camera.position.z;
        snapshot.yaw = yaw;
        snapshot.pitch = pitch;
        snapshot.completedTasks = completedTaskNames();
        return snapshot;
    }

    private void applySnapshot(GameSnapshot snapshot) {
        day = MathUtils.clamp(snapshot.day, 1, 10);
        health = snapshot.health;
        stress = snapshot.stress;
        foodSupply = snapshot.foodSupply;
        powerLevel = snapshot.powerLevel;
        aiTrust = snapshot.aiTrust;
        morality = snapshot.morality;
        daysWithoutFood = snapshot.daysWithoutFood;
        yaw = snapshot.yaw;
        pitch = snapshot.pitch;
        camera.position.set(snapshot.playerX, EYE_HEIGHT, snapshot.playerZ);
        clampResources();
        gameOverReason = "";
        endingReached = false;
        startDay();
        restoreCompletedTasks(snapshot.completedTasks);
        updateCameraDirection();
        updateBunkerState();
    }

    private String completedTaskNames() {
        StringBuilder names = new StringBuilder();
        for (Task task : tasks) {
            if (!task.complete) {
                continue;
            }
            if (names.length() > 0) {
                names.append("|");
            }
            names.append(task.name);
        }
        return names.toString();
    }

    private void restoreCompletedTasks(String completedTasks) {
        if (completedTasks == null || completedTasks.isEmpty()) {
            return;
        }
        String[] names = completedTasks.split("\\|");
        for (String name : names) {
            for (Task task : tasks) {
                if (task.name.equals(name)) {
                    task.complete = true;
                }
            }
        }
    }

    private DailyPlan currentDay() {
        return dailyPlans.get(day - 1);
    }

    private boolean isTaskComplete(String taskName) {
        for (Task task : tasks) {
            if (task.name.equals(taskName)) {
                return task.complete;
            }
        }
        return false;
    }

    private void startMiniGame(Interactable interactable) {
        activeInteractable = interactable;
        activeMiniGame = interactable.miniGame;
        timingMarker = 0f;
        timingDirection = 1f;
        wireStep = 0;
        keypadInput = "";
        Gdx.input.setCursorCatched(false);
    }

    private void updateMiniGame(float delta) {
        if (activeMiniGame == MiniGame.GENERATOR_TIMING) {
            timingMarker += timingDirection * delta * 0.85f;
            if (timingMarker <= 0f || timingMarker >= 1f) {
                timingMarker = MathUtils.clamp(timingMarker, 0f, 1f);
                timingDirection *= -1f;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (timingMarker >= 0.42f && timingMarker <= 0.58f) {
                    winMiniGame();
                } else {
                    failMiniGame("Generator timing missed");
                }
            }
        } else if (activeMiniGame == MiniGame.WIRE_RECONNECT) {
            handleWireKey(1, Input.Keys.NUM_1, Input.Keys.NUMPAD_1);
            handleWireKey(2, Input.Keys.NUM_2, Input.Keys.NUMPAD_2);
            handleWireKey(3, Input.Keys.NUM_3, Input.Keys.NUMPAD_3);
        } else if (activeMiniGame == MiniGame.KEYPAD_TERMINAL) {
            handleKeypadDigit("4", Input.Keys.NUM_4, Input.Keys.NUMPAD_4);
            handleKeypadDigit("2", Input.Keys.NUM_2, Input.Keys.NUMPAD_2);
            handleKeypadDigit("7", Input.Keys.NUM_7, Input.Keys.NUMPAD_7);
            failOnWrongKeypadDigit("0", Input.Keys.NUM_0, Input.Keys.NUMPAD_0);
            failOnWrongKeypadDigit("1", Input.Keys.NUM_1, Input.Keys.NUMPAD_1);
            failOnWrongKeypadDigit("3", Input.Keys.NUM_3, Input.Keys.NUMPAD_3);
            failOnWrongKeypadDigit("5", Input.Keys.NUM_5, Input.Keys.NUMPAD_5);
            failOnWrongKeypadDigit("6", Input.Keys.NUM_6, Input.Keys.NUMPAD_6);
            failOnWrongKeypadDigit("8", Input.Keys.NUM_8, Input.Keys.NUMPAD_8);
            failOnWrongKeypadDigit("9", Input.Keys.NUM_9, Input.Keys.NUMPAD_9);
        }
    }

    private void handleWireKey(int expectedStep, int numberKey, int numpadKey) {
        if (!Gdx.input.isKeyJustPressed(numberKey) && !Gdx.input.isKeyJustPressed(numpadKey)) {
            return;
        }
        if (wireStep + 1 != expectedStep) {
            failMiniGame("Wrong wire connection");
            return;
        }
        wireStep++;
        if (wireStep == 3) {
            winMiniGame();
        }
    }

    private void handleKeypadDigit(String digit, int numberKey, int numpadKey) {
        if (activeMiniGame != MiniGame.KEYPAD_TERMINAL
            || !Gdx.input.isKeyJustPressed(numberKey) && !Gdx.input.isKeyJustPressed(numpadKey)) {
            return;
        }
        if (KEYPAD_CODE.charAt(keypadInput.length()) != digit.charAt(0)) {
            failMiniGame("Keypad code rejected");
            return;
        }
        keypadInput += digit;
        if (keypadInput.equals(KEYPAD_CODE)) {
            winMiniGame();
        }
    }

    private void failOnWrongKeypadDigit(String digit, int numberKey, int numpadKey) {
        if (activeMiniGame == MiniGame.KEYPAD_TERMINAL
            && (Gdx.input.isKeyJustPressed(numberKey) || Gdx.input.isKeyJustPressed(numpadKey))) {
            failMiniGame("Keypad code rejected");
        }
    }

    private void winMiniGame() {
        completeTask(activeInteractable);
        closeMiniGame();
    }

    private void failMiniGame(String message) {
        interactionMessage = "Mini-game failed: " + message;
        interactionMessageTime = 2.25f;
        announceAi("Action error logged. Repeat procedure.");
        startShake(0.35f, 0.035f);
        closeMiniGame();
    }

    private void completeTask(Interactable interactable) {
        interactable.task.complete = true;
        commandFor(interactable.task.name).execute(bunkerCore.getEventBus());
        bunkerCore.getEventBus().publish(new GameEvent(GameEventType.TASK_COMPLETED, interactable.task.name));
        interactionMessage = "Task complete: " + interactable.actionMessage;
        interactionMessageTime = 2.25f;
        announceAi(actionComment(interactable.task.name));
        saveCheckpoint("Checkpoint saved");
    }

    private BunkerCommand commandFor(String taskName) {
        if (taskName.equals("Repair generator") || taskName.equals("Fix broken pipe")) {
            return new RepairCommand(taskName);
        }
        if (taskName.equals("Reboot AI terminal")) {
            return new AnalyseLogsCommand();
        }
        return new ExploreCommand(taskName);
    }

    private void closeMiniGame() {
        activeMiniGame = MiniGame.NONE;
        activeInteractable = null;
        Gdx.input.setCursorCatched(true);
    }

    private Interactable nearestInteractable() {
        for (Interactable interactable : interactables) {
            float xDistance = camera.position.x - interactable.x;
            float zDistance = camera.position.z - interactable.z;
            if (xDistance * xDistance + zDistance * zDistance < 3.2f) {
                return interactable;
            }
        }
        return null;
    }

    private void drawHud() {
        Interactable nearest = nearestInteractable();
        hudBatch.begin();
        hudFont.draw(hudBatch, "LAST SHELTER", 24f, Gdx.graphics.getHeight() - 24f);
        hudFont.draw(hudBatch, currentRoomName(), 24f, Gdx.graphics.getHeight() - 48f);
        drawObjectiveTracker();
        if (nearest != null && activeMiniGame == MiniGame.NONE && !isRunFinished()) {
            hudFont.draw(hudBatch, "E  " + nearest.prompt, 24f, 44f);
        }
        hudFont.draw(hudBatch, "N  End day after main objective", 24f, 20f);
        if (interactionMessageTime > 0f) {
            hudFont.draw(hudBatch, interactionMessage, 24f, 68f);
        }
        drawAiMonitor();
        hudFont.draw(hudBatch, "+", Gdx.graphics.getWidth() / 2f - 4f, Gdx.graphics.getHeight() / 2f + 6f);
        hudBatch.end();
    }

    private void drawAiMonitor() {
        float y = Gdx.graphics.getHeight() - 84f;
        hudFont.draw(hudBatch, "BUNKER AI MONITOR", 24f, y);
        hudFont.draw(hudBatch, aiMessageTime > 0f ? aiMessage : objectiveSuggestion(), 24f, y - 22f);
    }

    private void drawMiniGame() {
        float x = Gdx.graphics.getWidth() / 2f - 220f;
        float y = Gdx.graphics.getHeight() / 2f + 110f;
        hudBatch.begin();
        if (activeMiniGame == MiniGame.GENERATOR_TIMING) {
            int markerIndex = MathUtils.clamp((int)(timingMarker * 20f), 0, 20);
            String timingLine = "---------------------";
            timingLine = timingLine.substring(0, markerIndex) + "|" + timingLine.substring(markerIndex + 1);
            hudFont.draw(hudBatch, "GENERATOR TIMING REPAIR", x, y);
            hudFont.draw(hudBatch, "Press SPACE when | is near [====].", x, y - 28f);
            hudFont.draw(hudBatch, timingLine, x, y - 62f);
            hudFont.draw(hudBatch, "--------[====]-------", x, y - 86f);
        } else if (activeMiniGame == MiniGame.WIRE_RECONNECT) {
            hudFont.draw(hudBatch, "WIRE RECONNECT", x, y);
            hudFont.draw(hudBatch, "Connect wires in order: 1  2  3", x, y - 28f);
            hudFont.draw(hudBatch, "Connected: " + wireStep + " / 3", x, y - 62f);
            hudFont.draw(hudBatch, "Wrong order fails.", x, y - 86f);
        } else if (activeMiniGame == MiniGame.KEYPAD_TERMINAL) {
            hudFont.draw(hudBatch, "KEYPAD TERMINAL", x, y);
            hudFont.draw(hudBatch, "Enter reboot code: 427", x, y - 28f);
            hudFont.draw(hudBatch, "Input: " + keypadInput, x, y - 62f);
            hudFont.draw(hudBatch, "Wrong digit fails.", x, y - 86f);
        }
        hudBatch.end();
    }

    private void drawRunFinishedScreen() {
        float x = Gdx.graphics.getWidth() / 2f - 210f;
        float y = Gdx.graphics.getHeight() / 2f + 70f;
        hudBatch.begin();
        if (endingReached) {
            hudFont.draw(hudBatch, "LAST SHELTER - 10 DAYS SURVIVED", x, y);
            hudFont.draw(hudBatch, "The bunker holds through the final day.", x, y - 34f);
        } else {
            hudFont.draw(hudBatch, "GAME OVER", x, y);
            hudFont.draw(hudBatch, gameOverReason, x, y - 34f);
        }
        hudFont.draw(hudBatch, "Press ESC to close.", x, y - 78f);
        hudBatch.end();
    }

    private String currentRoomName() {
        for (BunkerRoom room : rooms) {
            if (room.contains(camera.position.x, camera.position.z)) {
                return room.name;
            }
        }
        return "Bunker Corridor";
    }

    private void drawObjectiveTracker() {
        float x = Gdx.graphics.getWidth() - 280f;
        float y = Gdx.graphics.getHeight() - 24f;
        hudFont.draw(hudBatch, "DAY " + day + " / 10", x, y);
        hudFont.draw(hudBatch, "MAIN OBJECTIVE", x, y - 22f);
        hudFont.draw(hudBatch, taskMark(currentDay().mainObjective) + currentDay().mainObjective, x, y - 44f);
        hudFont.draw(hudBatch, "BUNKER PROBLEM", x, y - 76f);
        hudFont.draw(hudBatch, currentDay().bunkerProblem, x, y - 98f);
        drawResources(x, y - 132f);
    }

    private String taskMark(String taskName) {
        return isTaskComplete(taskName) ? "[x] " : "[ ] ";
    }

    private void drawResources(float x, float y) {
        hudFont.draw(hudBatch, "Health: " + health, x, y);
        hudFont.draw(hudBatch, "Stress: " + stress, x, y - 20f);
        hudFont.draw(hudBatch, "Food Supply: " + foodSupply, x, y - 40f);
        hudFont.draw(hudBatch, "Power Level: " + powerLevel, x, y - 60f);
        hudFont.draw(hudBatch, "AI Trust: " + aiTrust, x, y - 80f);
        hudFont.draw(hudBatch, "Morality: " + morality, x, y - 100f);
    }

    private void updateAtmosphere(float delta) {
        effectTime += delta;
        updateDoors(delta);
        updateSpinners();
        updateBlinkers();
        updateParticles();
        updateLightPulses();
        updateCameraMotion(delta);
    }

    private void updateDoors(float delta) {
        for (Door door : doors) {
            door.update(camera.position.x, camera.position.z, delta);
        }
    }

    private void updateSpinners() {
        for (Spinner spinner : spinners) {
            spinner.update(effectTime);
        }
    }

    private void updateBlinkers() {
        boolean emergency = isEmergency();
        for (Blinker blinker : blinkers) {
            float pulse = 0.55f + 0.45f * MathUtils.sin(effectTime * blinker.speed);
            if (emergency) {
                pulse = 0.25f + 0.75f * Math.abs(MathUtils.sin(effectTime * blinker.speed * 1.8f));
            }
            blinker.tint(pulse);
        }
    }

    private void updateParticles() {
        for (EffectParticle particle : particles) {
            particle.update(effectTime);
        }
    }

    private void updateLightPulses() {
        boolean emergency = isEmergency();
        for (LightPulse pulse : lightPulses) {
            float flicker = pulse.flickers ? 0.72f + 0.28f * Math.abs(MathUtils.sin(effectTime * 7.7f))
                : 0.92f + 0.08f * MathUtils.sin(effectTime * 1.7f);
            if (emergency) {
                flicker *= 0.72f + 0.28f * Math.abs(MathUtils.sin(effectTime * 11f));
            }
            pulse.light.set(pulse.color.r * flicker, pulse.color.g * flicker, pulse.color.b * flicker,
                pulse.light.position.x, pulse.light.position.y, pulse.light.position.z, 16f * flicker);
        }
    }

    private void updateCameraMotion(float delta) {
        if (playerWalking && activeMiniGame == MiniGame.NONE && !isRunFinished()) {
            headBobTime += delta * 8.5f;
        }
        float bob = playerWalking ? MathUtils.sin(headBobTime) * 0.035f : 0f;
        float shake = shakeOffset(delta, 23f);
        camera.position.y = EYE_HEIGHT + bob + shake;
        updateCameraDirection();
        if (shakeTime > 0f) {
            camera.direction.rotate(Vector3.Y, shakeOffset(0f, 29f) * 1.8f);
            camera.update();
        }

        flatForward.set(camera.direction.x, 0f, camera.direction.z).nor();
        right.set(flatForward).crs(Vector3.Y).nor();
        float sway = MathUtils.sin(effectTime * 2.7f + headBobTime * 0.25f) * 0.16f;
        flashlight.set(0.55f, 0.58f, 0.5f,
            camera.position.x + flatForward.x * 0.7f + right.x * sway,
            camera.position.y - 0.12f + MathUtils.cos(effectTime * 2.1f) * 0.03f,
            camera.position.z + flatForward.z * 0.7f + right.z * sway, 8f);
    }

    private boolean isEmergency() {
        return bunkerCore.getState().isEmergency() || currentDay().problem == Problem.GENERATOR
            && !isTaskComplete("Repair generator") || currentDay().problem == Problem.PIPE
            && !isTaskComplete("Fix broken pipe");
    }

    private void startShake(float duration, float strength) {
        shakeDuration = duration;
        shakeTime = duration;
        shakeStrength = strength;
    }

    private float shakeOffset(float delta, float speed) {
        if (shakeTime <= 0f || shakeDuration <= 0f) {
            return 0f;
        }
        shakeTime = Math.max(0f, shakeTime - delta);
        float fade = shakeTime / shakeDuration;
        return MathUtils.sin(effectTime * speed) * shakeStrength * fade * fade;
    }

    private void updateBunkerState() {
        boolean pipeIsolation = currentDay().problem == Problem.PIPE && !isTaskComplete("Fix broken pipe");
        boolean shutdown = isRunFinished() && powerLevel == 0;
        bunkerCore.updateState(powerLevel, stress, pipeIsolation, shutdown);
    }

    private void monitorResources(float delta) {
        resourceMonitorTime -= delta;
        if (resourceMonitorTime > 0f || aiMessageTime > 0f) {
            return;
        }
        resourceMonitorTime = 5f;
        if (powerLevel <= 45) {
            announceAi("Power consumption is increasing.");
        } else if (currentDay().problem == Problem.PIPE && !isTaskComplete("Fix broken pipe")) {
            announceAi("Water pressure unstable.");
        } else if (foodSupply <= 28) {
            announceAi("Food reserve below preferred margin.");
        } else if (stress >= 70) {
            announceAi("Stress load rising. Maintain procedure.");
        }
    }

    private void announceAi(String message) {
        aiMessage = message;
        aiMessageTime = 4f;
    }

    private String objectiveSuggestion() {
        return bunkerCore.getAiStrategy().objectiveSuggestion(currentDay().mainObjective);
    }

    private String actionComment(String taskName) {
        return bunkerCore.getAiStrategy().actionComment(taskName);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        roomModel.dispose();
        modelBatch.dispose();
        hudBatch.dispose();
        hudFont.dispose();
    }

    private static class WalkArea {
        private final float minX;
        private final float maxX;
        private final float minZ;
        private final float maxZ;

        private WalkArea(float x, float z, float width, float depth) {
            minX = x - width / 2f;
            maxX = x + width / 2f;
            minZ = z - depth / 2f;
            maxZ = z + depth / 2f;
        }

        protected boolean contains(float x, float z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private static class Blocker extends WalkArea {
        private Blocker(float x, float z, float width, float depth) {
            super(x, z, width + PLAYER_RADIUS * 2f, depth + PLAYER_RADIUS * 2f);
        }
    }

    private static class Interactable {
        private final String name;
        private final String prompt;
        private final String actionMessage;
        private final float x;
        private final float z;
        private final Task task;
        private final MiniGame miniGame;
        private final boolean opensOuterDoor;

        private Interactable(String name, String prompt, String actionMessage, float x, float z,
            Task task, MiniGame miniGame, boolean opensOuterDoor) {
            this.name = name;
            this.prompt = prompt;
            this.actionMessage = actionMessage;
            this.x = x;
            this.z = z;
            this.task = task;
            this.miniGame = miniGame;
            this.opensOuterDoor = opensOuterDoor;
        }
    }

    private static class Task {
        private final String name;
        private boolean complete;

        private Task(String name) {
            this.name = name;
        }
    }

    private static class DailyPlan {
        private final String mainObjective;
        private final String bunkerProblem;
        private final Problem problem;
        private final int healthChange;
        private final int stressChange;
        private final int foodChange;
        private final int powerChange;
        private final int trustChange;
        private final int moralityChange;

        private DailyPlan(String mainObjective, String bunkerProblem, Problem problem, int healthChange,
            int stressChange, int foodChange, int powerChange, int trustChange, int moralityChange) {
            this.mainObjective = mainObjective;
            this.bunkerProblem = bunkerProblem;
            this.problem = problem;
            this.healthChange = healthChange;
            this.stressChange = stressChange;
            this.foodChange = foodChange;
            this.powerChange = powerChange;
            this.trustChange = trustChange;
            this.moralityChange = moralityChange;
        }
    }

    private static class Spinner {
        private final ModelInstance instance;
        private final float x;
        private final float y;
        private final float z;
        private final float width;
        private final float height;
        private final float depth;
        private final float speed;

        private Spinner(ModelInstance instance, float x, float y, float z, float width, float height, float depth,
            float speed) {
            this.instance = instance;
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.speed = speed;
        }

        private void update(float time) {
            instance.transform.idt().translate(x, y, z).rotate(Vector3.Y, time * speed).scale(width, height, depth);
        }
    }

    private static class Blinker {
        private final ModelInstance instance;
        private final Color color;
        private final float speed;

        private Blinker(ModelInstance instance, Color color, float speed) {
            this.instance = instance;
            this.color = color.cpy();
            this.speed = speed;
        }

        private void tint(float intensity) {
            instance.materials.first().set(ColorAttribute.createDiffuse(
                color.r * intensity, color.g * intensity, color.b * intensity, 1f));
        }
    }

    private static class LightPulse {
        private final PointLight light;
        private final Color color;
        private final boolean flickers;

        private LightPulse(PointLight light, Color color, boolean flickers) {
            this.light = light;
            this.color = color.cpy();
            this.flickers = flickers;
        }
    }

    private static class EffectParticle {
        private final ModelInstance instance;
        private final EffectKind kind;
        private final float x;
        private final float y;
        private final float z;
        private final float size;
        private final float offset;

        private EffectParticle(ModelInstance instance, EffectKind kind, float x, float y, float z, float size,
            float offset) {
            this.instance = instance;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
            this.offset = offset;
        }

        private void update(float time) {
            float cycle = (time * speed() + offset) % 1f;
            float px = x;
            float py = y;
            float pz = z;
            float scale = size;
            if (kind == EffectKind.SPARK) {
                px += MathUtils.sin((offset + 1f) * 17f) * cycle * 0.7f;
                py -= cycle * 0.7f;
                pz += MathUtils.cos((offset + 1f) * 13f) * cycle * 0.45f;
                scale *= cycle < 0.42f ? 1f : 0.03f;
            } else if (kind == EffectKind.STEAM) {
                px += MathUtils.sin(time * 2.2f + offset * 9f) * 0.18f;
                py += cycle * 1.4f;
                scale *= 0.8f + cycle;
            } else if (kind == EffectKind.SMOKE) {
                px += MathUtils.cos(time * 1.3f + offset * 7f) * 0.22f;
                py += cycle * 1.15f;
                pz += MathUtils.sin(time + offset * 5f) * 0.12f;
                scale *= 0.9f + cycle * 1.25f;
            } else if (kind == EffectKind.WATER) {
                py -= cycle * 2.15f;
                scale *= cycle < 0.92f ? 1f : 0.04f;
            }
            instance.transform.setToTranslation(px, py, pz).scale(scale, scale, scale);
        }

        private float speed() {
            if (kind == EffectKind.SPARK) {
                return 1.7f;
            }
            if (kind == EffectKind.WATER) {
                return 1.25f;
            }
            return 0.45f;
        }
    }

    private static class BunkerRoom extends WalkArea {
        private final String name;

        private BunkerRoom(String name, float x, float z, float width, float depth) {
            super(x, z, width, depth);
            this.name = name;
        }
    }

    private enum MiniGame {
        NONE,
        GENERATOR_TIMING,
        WIRE_RECONNECT,
        KEYPAD_TERMINAL
    }

    private enum Problem {
        GENERATOR,
        PIPE,
        RATIONS,
        AI_GLITCH,
        COMMS
    }

    private enum EffectKind {
        SPARK,
        STEAM,
        SMOKE,
        WATER
    }

    private enum UtilityScreen {
        NONE,
        LOADING,
        PAUSE,
        SETTINGS
    }
}
