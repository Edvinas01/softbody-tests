package com.edd.softbodies;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;

public class SoftBodyTests extends Game {

    private static final float WALL_WIDTH = 1f;

    private OrthographicCamera camera;
    private WorldSimulation world;

    private ShaderProgram shaderProgram;

    private PolygonSpriteBatch polygonSpriteBatch;
    private SpriteBatch batch;

    private Texture texture;

    private MouseJointDef jointDef;


    public static void main(String... args) {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.foregroundFPS = 60;
        configuration.resizable = false;
        configuration.width = 800;
        configuration.height = 600;

        new LwjglApplication(new SoftBodyTests(), configuration);
    }

    // Mouse position.
    private Vector3 mousePos = new Vector3();

    // Target for the mouse joint.
    private Vector2 target = new Vector2();

    // Joint connecting the mouse and the body that is being dragged.
    private MouseJoint joint;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Units.meters(Gdx.graphics.getWidth()), Units.meters(Gdx.graphics.getHeight()));
        camera.zoom = 1f;

        world = new WorldSimulation();

        shaderProgram = new ShaderProgram(
                Gdx.files.internal("vertex.glsl"),
                Gdx.files.internal("fragment.glsl")
        );

        polygonSpriteBatch = new PolygonSpriteBatch();
        batch = new SpriteBatch();

        texture = new Texture(Gdx.files.internal("cube.png"));

        shaderProgram.setAttributef("a_color", 1f, 1f, 1f, 1f);

        softRectangle = new SoftRectangle(world.getWorld(), texture, 4, 3);
//        softRectanglePolygon = new SoftRectanglePolygon(world.getWorld(), texture, 4, 3);

        // Create static world.
        Body ground = createBounds();

        // Mouse joint stuff.
        jointDef = new MouseJointDef();
        jointDef.bodyA = ground;
        jointDef.collideConnected = true;
        jointDef.maxForce = 500f;

        // Drag physics objects.
        Gdx.input.setInputProcessor(new MouseDragAdapter());
    }

//    SoftRectanglePolygon softRectanglePolygon;
    SoftRectangle softRectangle;

    @Override
    public void render() {

        // Logic.
        camera.update();

        world.update(Gdx.graphics.getDeltaTime());

        // Rendering.
        Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl20.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Mesh.
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformi("u_texture", 0);
        softRectangle.render(shaderProgram);
        shaderProgram.end();

        world.render(camera);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.end();

//        polygonSpriteBatch.setProjectionMatrix(camera.combined);
//        polygonSpriteBatch.begin();
//        softRectanglePolygon.render(polygonSpriteBatch);
//        polygonSpriteBatch.end();
    }

    /**
     * Create bounding for the level.
     *
     * @return ground body.
     */
    private Body createBounds() {

        // Left.
        createWall(-WALL_WIDTH, 0, true);

        // Right.
        createWall(camera.viewportWidth + WALL_WIDTH, 0, true);

        // Top.
        createWall(0, camera.viewportHeight + WALL_WIDTH, false);

        // Bottom.
        return createWall(0, -WALL_WIDTH, false);
    }

    /**
     * Create a static wall at a given coordinate.
     */
    private Body createWall(float x, float y, boolean vertical) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(new Vector2(x, y));
        bodyDef.angle = vertical ? MathUtils.degRad * 90 : 0;

        Body body = world.getWorld().createBody(bodyDef);

        PolygonShape box = new PolygonShape();
        box.setAsBox(camera.viewportWidth, WALL_WIDTH);

        body.createFixture(box, 0.0f);
        box.dispose();

        return body;
    }

    /**
     * Drag objects with mouse.
     */
    private final class MouseDragAdapter extends InputAdapter {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            camera.unproject(mousePos.set(screenX, screenY, 0));
            world.getWorld().QueryAABB(fixture -> {
                if (!fixture.testPoint(mousePos.x, mousePos.y)) {
                    return true;
                }

                jointDef.bodyB = fixture.getBody();
                jointDef.target.set(mousePos.x, mousePos.y);
                joint = (MouseJoint) world.getWorld().createJoint(jointDef);

                return false;

            }, mousePos.x, mousePos.y, mousePos.x, mousePos.y);
            return true;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (joint == null)
                return false;

            camera.unproject(mousePos.set(screenX, screenY, 0));
            joint.setTarget(target.set(mousePos.x, mousePos.y));
            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (joint == null)
                return false;

            world.getWorld().destroyJoint(joint);
            joint = null;
            return true;
        }
    }
}