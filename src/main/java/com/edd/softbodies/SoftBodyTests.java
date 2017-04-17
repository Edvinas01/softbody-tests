package com.edd.softbodies;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

public class SoftBodyTests extends Game {

    private enum Mode {
        DRAG_BODIES,
        SPAWN_RECTANGLES
    }

    // Box2d constants.
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private static final float TIME_STEP = 1.0f / 300f;
    private static final float GRAVITY = -9.8f;

    private static final int PPM = 100;
    private static final float MPP = 1f / PPM;

    // General constants.
    private static final float WALL_HEIGHT = 0.5f;

    // All soft bodies in the scene.
    private final List<SoftBody> bodies = new ArrayList<>();

    private float accumulator = 0f;

    private Box2DDebugRenderer renderer;
    private OrthographicCamera camera;
    private ShaderProgram shaderProgram;
    private World world;

    // Joint definition for mouse and a dragged object.
    private MouseJointDef jointDef;

    // Mouse position.
    private Vector3 mousePos = new Vector3();

    // Target for the mouse joint.
    private Vector2 target = new Vector2();

    // Joint connecting the mouse and the body that is being dragged.
    private MouseJoint joint;

    private Texture texture;

    // Current mode.
    private Mode mode = Mode.DRAG_BODIES;

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
        camera.setToOrtho(false, meters(Gdx.graphics.getWidth()), meters(Gdx.graphics.getHeight()));
//        camera.zoom = 2f;

        // Box2d setup.
        renderer = new Box2DDebugRenderer(true, true, false, true, false, true);
        world = new World(new Vector2(0f, GRAVITY), true);

        // Initialize shader program with some default shaders.
        shaderProgram = new ShaderProgram(Gdx.files.internal("vertex.glsl"), Gdx.files.internal("fragment.glsl"));
        shaderProgram.setAttributef("a_color", 1f, 1f, 1f, 1f);

        // Load test texture.
        texture = new Texture(Gdx.files.internal("cube.png"));

        // Initialize dragging of physics objects.
        Gdx.input.setInputProcessor(new Inputs());

        // Create static world.
        Body ground = createBounds();

        // Initialize mouse joint definition.
        jointDef = new MouseJointDef();
        jointDef.bodyA = ground;
        jointDef.collideConnected = true;
        jointDef.maxForce = 500f;

        // Add some initial soft bodies.
        bodies.add(new SoftRectangle(texture, world, 1, 1, 3, 4));
    }

    @Override
    public void render() {
        Gdx.graphics.setTitle("fps: " + Gdx.graphics.getFramesPerSecond());

        // Update camera transforms.
        camera.update();

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

        // Render soft bodies.
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformi("u_texture", 0);
        bodies.forEach(b -> b.act(shaderProgram));
        shaderProgram.end();

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
        createWall(-camera.viewportWidth / 2, camera.viewportHeight / 2, true);

        // Right.
        createWall(camera.viewportWidth / 2, camera.viewportHeight / 2, true);
//
        // Top.
        createWall(0, camera.viewportHeight, false);

        // Bottom.
        return createWall(0, 0, false);
    }

    /**
     * Create a static wall at a given coordinate.
     */
    private Body createWall(float x, float y, boolean vertical) {
        float hw = camera.viewportWidth / 2;
        float hh = WALL_HEIGHT / 2;

        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(new Vector2(x + hw, y));
        bodyDef.angle = vertical ? MathUtils.degRad * 90 : 0;

        Body body = world.createBody(bodyDef);

        PolygonShape box = new PolygonShape();
        box.setAsBox(hw, hh);

        body.createFixture(box, 0.0f);
        box.dispose();

        return body;
    }

    /**
     * Main scene listener.
     */
    private final class Inputs extends InputAdapter {

        @Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
                case Input.Keys.NUM_1:
                    return switchMode(Mode.DRAG_BODIES);

                case Input.Keys.NUM_2:
                    return switchMode(Mode.SPAWN_RECTANGLES);

                case Input.Keys.R:
                    bodies.clear();

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
                case SPAWN_RECTANGLES:
                    bodies.add(new SoftRectangle(
                            texture,
                            world,
                            mousePos.x,
                            mousePos.y,
                            MathUtils.random(2, 5),
                            MathUtils.random(2, 5))
                    );

                    break;
                case DRAG_BODIES:
                    world.QueryAABB(fixture -> {
                        if (!fixture.testPoint(mousePos.x, mousePos.y)) {
                            return true;
                        }

                        if (BodyDef.BodyType.StaticBody == fixture.getBody().getType()) {
                            return false;
                        }

                        jointDef.bodyB = fixture.getBody();
                        jointDef.target.set(mousePos.x, mousePos.y);
                        joint = (MouseJoint) world.createJoint(jointDef);

                        return false;

                    }, mousePos.x, mousePos.y, mousePos.x, mousePos.y);
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

        System.out.println("Mode: " + mode);
        return true;
    }

    /**
     * @return number converted to meters.
     */
    private static float meters(float convert) {
        return convert * MPP;
    }
}