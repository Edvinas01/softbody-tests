package com.edd.softbodies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;

import java.util.ArrayList;
import java.util.List;

public final class SoftRectangle extends SoftBody {

    // Joined body constants.
    private static final float RESTITUTION = 0.05f;
    private static final float FRICTION = 1f;
    private static final float DENSITY = 0.1f;
    private static final float RADIUS = 0.15f;

    // Joint constants.
    private static final float FREQUENCY = 10f;
    private static final float DAMPING = 0.1f;

    // Misc constants.
    private static final float SPACING = 0.5f;

    private final int width;
    private final int height;

    private final Body[][] bodies;

    public SoftRectangle(Texture texture, World world, float x, float y, int width, int height) {
        super(texture, width * height * COMPONENT_COUNT);

        this.width = width;
        this.height = height;

        this.bodies = createBody(world, x, y);
    }

    @Override
    protected void updateVertices(float[] vertices) {
        int idx = 0;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                Vector2 pas = bodies[i][j].getPosition();

                float u = (float) i / (width - 1);

                // v is facing down hence the -1.
                float v = 1 - (float) j / (height - 1);

                vertices[idx++] = pas.x;
                vertices[idx++] = pas.y;
                vertices[idx++] = 0f; // z
                vertices[idx++] = u;
                vertices[idx++] = v;
            }
        }
    }

    @Override
    protected short[] createIndices() {
        List<Integer> indices = new ArrayList<>();

        // See for more info:
        // http://www.learnopengles.com/android-lesson-eight-an-introduction-to-index-buffer-objects-ibos/
        for (int y = 0; y < height - 1; y++) {
            if (y > 0) {

                // Degenerate begin: repeat first vertex.
                indices.add(y * width);
            }

            for (int x = 0; x < width; x++) {

                // One part of the strip.
                indices.add((y * width) + x);
                indices.add(((y + 1) * width) + x);
            }

            if (y < height - 2) {

                // Degenerate end: repeat last vertex.
                indices.add((y + 1) * width + (width - 1));
            }
        }

        // Should probably just create an array initially to save resources,
        // though this is called only once so w/e.
        short[] indicesArray = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i).shortValue();
        }
        return indicesArray;
    }

    /**
     * @return created bodies in a matrix.
     */
    private Body[][] createBody(World world, float x, float y) {

        // Shape of the joined bodies.
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(RADIUS);

        // Fixture for the bodies that are to be joined.
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circleShape;
        fixtureDef.restitution = RESTITUTION;
        fixtureDef.density = DENSITY;
        fixtureDef.friction = FRICTION;

        // Definition for the bodies that are to be joined.
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        // Definition for the joints that will connect to bodies.
        DistanceJointDef jointDef = new DistanceJointDef();
        jointDef.collideConnected = true;
        jointDef.frequencyHz = FREQUENCY;
        jointDef.dampingRatio = DAMPING;

        Body[][] bodies = new Body[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bodyDef.position.set(i * SPACING + x, j * SPACING + y);

                Body curr = world.createBody(bodyDef);
                curr.createFixture(fixtureDef);

                // Create body matrix which will be used to create vertex buffer.
                bodies[i][j] = curr;

                // Body to which to connect.
                Body connect;

                // Connect to left.
                if (i - 1 >= 0 && (connect = bodies[i - 1][j]) != null) {
                    jointDef.initialize(curr, connect, curr.getWorldCenter(), connect.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to bottom.
                if (j - 1 >= 0 && (connect = bodies[i][j - 1]) != null) {
                    jointDef.initialize(curr, connect, curr.getWorldCenter(), connect.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to left bottom.
                if (i - 1 >= 0 && j - 1 >= 0 && (connect = bodies[i - 1][j - 1]) != null) {
                    jointDef.initialize(curr, connect, curr.getWorldCenter(), connect.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to left top.
                if (i - 1 >= 0 && j + 1 < height && (connect = bodies[i - 1][j + 1]) != null) {
                    jointDef.initialize(curr, connect, curr.getWorldCenter(), connect.getWorldCenter());
                    world.createJoint(jointDef);
                }
            }
        }
        circleShape.dispose();
        return bodies;
    }
}