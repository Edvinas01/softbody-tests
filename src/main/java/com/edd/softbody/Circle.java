package com.edd.softbody;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;

import java.util.ArrayList;
import java.util.List;

public final class Circle extends SoftBody {

    private static final int SEGMENT_COUNT = 20;

    private final List<Body> bodies;

    public Circle(Texture texture, World world, float radius, float x, float y) {
        super(texture, 0);

        this.bodies = createBodies(world, radius, x, y);
    }

    @Override
    protected float[] updateVertices(float[] vertices) {
        if (vertices.length == 0) {
            vertices = new float[(bodies.size() + 1) * COMPONENT_COUNT];
        }

        int idx = 0;
        float deltaAngle = (2.f * MathUtils.PI) / (bodies.size() - 1);

        Vector2 center = bodies.get(bodies.size() - 1).getPosition();

        // Last body is always the middle, so starting from the end.
        for (int i = bodies.size() - 1; i >= 0; i--) {
            float theta = MathUtils.PI + (deltaAngle * i);

            // Extending the vectors in order for the mesh to take up the whole
            // body, including the joined circles.
            Vector2 pos = bodies.get(i).getLocalPoint(center);
            Vector2 nor = pos.cpy().nor().scl(-RADIUS);

            vertices[idx++] = center.x - pos.x + nor.x;
            vertices[idx++] = center.y - pos.y + nor.y;

            if (i + 1 < bodies.size()) {
                vertices[idx++] = 0.5f + MathUtils.cos(theta) * 0.5f * -1;
                vertices[idx++] = 0.5f + MathUtils.sin(theta) * 0.5f;
            } else {
                vertices[idx++] = 0.5f;
                vertices[idx++] = 0.5f;
            }
        }

        vertices[idx++] = vertices[4];
        vertices[idx++] = vertices[5];
        vertices[idx++] = vertices[6];
        vertices[idx] = vertices[7];

        return vertices;
    }

    @Override
    protected short[] createIndices() {

        // Wont need indices for this body.
        return new short[0];
    }

    @Override
    protected void render(Mesh mesh, ShaderProgram program) {
        mesh.render(program, GL20.GL_TRIANGLE_FAN);
    }

    /**
     * @return created circle bodies in a list.
     */
    private List<Body> createBodies(World world, float radius, float x, float y) {
        List<Body> bodies = new ArrayList<>();

        // Shape of the joined bodies.
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(RADIUS);

        // Fixture for the bodies that are to be jointed.
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.restitution = RESTITUTION;
        fixtureDef.friction = FRICTION;
        fixtureDef.density = DENSITY;
        fixtureDef.shape = circleShape;

        // Definition for the bodies that are to be joined.
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        int segments = (int) (SEGMENT_COUNT * radius);
        float deltaAngle = (2.f * MathUtils.PI) / segments;

        // See: http://www.java-gaming.org/index.php?topic=36531.0
        for (int i = 0; i < segments; i++) {

            // Current angle.
            float theta = deltaAngle * i;

            bodyDef.position.set(
                    x + radius * MathUtils.cos(theta),
                    y + radius * MathUtils.sin(theta)
            );

            // Create the body and fixture.
            Body body = world.createBody(bodyDef);
            body.createFixture(fixtureDef);
            bodies.add(body);
        }

        // Circle at the center (inner circle).
        BodyDef innerDef = new BodyDef();
        innerDef.position.set(new Vector2(x, y));
        innerDef.type = BodyDef.BodyType.DynamicBody;
        innerDef.fixedRotation = true;

        // Position is at the center.
        Body innerBody = world.createBody(innerDef);
        innerBody.createFixture(fixtureDef);
        bodies.add(innerBody);

        // Connect the joints.
        DistanceJointDef jointDef = new DistanceJointDef();
        for (int i = 0; i < segments; i++) {

            // The neighbor.
            int neighborIndex = (i + 1) % segments;

            // Get the current body and the neighbor.
            Body currentBody = bodies.get(i);
            Body neighborBody = bodies.get(neighborIndex);

            // Connect the outer circles to each other.
            jointDef.initialize(
                    currentBody,
                    neighborBody,
                    currentBody.getWorldCenter(),
                    neighborBody.getWorldCenter()
            );

            jointDef.collideConnected = false;
            jointDef.frequencyHz = FREQUENCY;
            jointDef.dampingRatio = DAMPING;

            world.createJoint(jointDef);

            // Connect the center circle with other circles.
            jointDef.initialize(currentBody, innerBody, currentBody.getWorldCenter(), new Vector2(x, y));
            jointDef.collideConnected = false;
            jointDef.frequencyHz = FREQUENCY;
            jointDef.dampingRatio = 0.5f;

            world.createJoint(jointDef);
        }
        return bodies;
    }
}