package com.edd.softbody;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;

import static com.edd.softbody.Units.toMeters;

public class SoftBodyTests extends Game {

    private enum Mode {
        DRAG_BODIES,
        SPAWN_RECTANGLES,
        SPAWN_CIRCLES
    }

    // Box2d constants.
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private static final float TIME_STEP = 1.0f / 300f;
    private static final float GRAVITY = -9.8f;

    // Mouse position.
    private final Vector3 mousePos = new Vector3();

    // Target for the mouse joint.
    private final Vector2 target = new Vector2();

    // General constants.
    private static final float WALL_HEIGHT = 0.5f;

    private float accumulator = 0f;

    private Box2DDebugRenderer renderer;
    private OrthographicCamera camera;
    private World world;

    // Joint definition for mouse and a dragged object.
    private MouseJointDef jointDef;

    // Joint connecting the mouse and the body that is being dragged.
    private MouseJoint joint;

    // Current mode.
    private Mode mode = Mode.DRAG_BODIES;

    // Helper for creating simple rigid bodies.
    private RigidBodyCreator bodyCreator;

    public static void main(String... args) {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.foregroundFPS = 60;
        configuration.resizable = false;
        configuration.width = 800;
        configuration.height = 600;

        new LwjglApplication(new SoftBodyTests(), configuration);
    }

    @Override
    public void create() {

        // Scale viewport to meters.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, toMeters(Gdx.graphics.getWidth()), toMeters(Gdx.graphics.getHeight()));

        // Box2d setup.
        renderer = new Box2DDebugRenderer(true, true, false, true, false, true);
        world = new World(new Vector2(0f, GRAVITY), true);

        bodyCreator = new RigidBodyCreator(world);

        // Initialize dragging of physics objects.
        Gdx.input.setInputProcessor(new Inputs());

        // Create static world.
        Body ground = createBounds();

        // Initialize mouse joint definition.
        jointDef = new MouseJointDef();
        jointDef.bodyA = ground;
        jointDef.collideConnected = true;
        jointDef.maxForce = 500f;
    }

    @Override
    public void render() {

        // Update camera transforms.
        camera.update();
        camera.unproject(mousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0f));

        Gdx.graphics.setTitle(""
                + "fps: " + Gdx.graphics.getFramesPerSecond()
                + " x: " + mousePos.x
                + " y: " + mousePos.y
        );

        // Update box2d world, for more info see:
        // https://github.com/libgdx/libgdx/wiki/Box2d#stepping-the-simulation
        float frameTime = Math.min(Gdx.graphics.getDeltaTime(), 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }

        // Cleanup after last rendering.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render the box2d world.
        renderer.render(world, camera.combined);
    }

    /**
     * Create bounding for the level.
     *
     * @return ground body.
     */
    private Body createBounds() {

        // Left.
//        createWall(-camera.viewportWidth / 2, camera.viewportHeight / 2, true);
//
//        // Right.
//        createWall(camera.viewportWidth / 2, camera.viewportHeight / 2, true);
//
//        // Top.
//        createWall(0, camera.viewportHeight, false);

        // Bottom.
        return bodyCreator.createPlatform(camera.viewportWidth / 2, 0, camera.viewportWidth, 1f);
    }

    /**
     * Input listener for handling mouse and keyboard events.
     */
    private final class Inputs extends InputAdapter {

        @Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
                case Input.Keys.NUM_1:
                    return switchMode(Mode.DRAG_BODIES);

                case Input.Keys.NUM_2:
                    return switchMode(Mode.SPAWN_RECTANGLES);

                case Input.Keys.NUM_3:
                    return switchMode(Mode.SPAWN_CIRCLES);

                case Input.Keys.R:

                    // Cleanup all bodies and joints, exclude static bodies.
                    Array<Body> bodies = new Array<>();
                    world.getBodies(bodies);
                    bodies.forEach(b -> {
                        if (BodyDef.BodyType.StaticBody != b.getType()) {
                            world.destroyBody(b);
                        }
                    });

                    Array<Joint> joints = new Array<>();
                    world.getJoints(joints);
                    joints.forEach(j -> world.destroyJoint(j));

                    // Prevent crash.
                    joint = null;
                    return true;
            }
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            camera.unproject(mousePos.set(screenX, screenY, 0));

            switch (mode) {
                case DRAG_BODIES:
                    world.QueryAABB(fixture -> {

                        if (BodyDef.BodyType.StaticBody == fixture.getBody().getType()) {
                            return false;
                        }

                        jointDef.bodyB = fixture.getBody();
                        jointDef.target.set(mousePos.x, mousePos.y);
                        joint = (MouseJoint) world.createJoint(jointDef);

                        return false;

                    }, mousePos.x - 0.2f, mousePos.y - 0.2f, mousePos.x + 0.2f, mousePos.y + 0.2f);
                    break;

                case SPAWN_RECTANGLES:
                    bodyCreator.createRectangle(mousePos.x, mousePos.y);
                    break;

                case SPAWN_CIRCLES:
                    bodyCreator.createCircle(mousePos.x, mousePos.y);
                    break;
            }
            return true;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (joint == null) {
                return false;
            }

            camera.unproject(mousePos.set(screenX, screenY, 0));
            joint.setTarget(target.set(mousePos.x, mousePos.y));
            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (joint == null || button == 1) {
                return false;
            }

            world.destroyJoint(joint);
            joint = null;
            return true;
        }
    }

    /**
     * Switch current mode.
     */
    private boolean switchMode(Mode newMode) {
        mode = newMode;

        System.out.println("Mode set to: " + mode);
        return true;
    }
}