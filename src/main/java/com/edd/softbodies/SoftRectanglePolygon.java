package com.edd.softbodies;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.utils.ShortArray;

@SuppressWarnings("Duplicates")
public class SoftRectanglePolygon {

    private final EarClippingTriangulator triangulator = new EarClippingTriangulator();

    private final int width;
    private final int height;

    private final PolygonRegion polygonRegion;
    private final Texture texture;

    private final Body[][] bodies;

    public SoftRectanglePolygon(World world, Texture texture, int width, int height) {
        this.width = width;
        this.height = height;
        this.texture = texture;
        this.verts = new float[width * height * 2];
        this.bodies = createBodies(world, width, height);

        updateVerts();

        ShortArray shortArray = triangulator.computeTriangles(verts);
        polygonRegion = new PolygonRegion(new TextureRegion(texture), verts, shortArray.toArray());
    }

    private Body[][] createBodies(World world, int width, int height) {
        float offset = 1f;
        float spacing = 0.5f;

        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(0.2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circleShape;
        fixtureDef.density = 0.1f;
        fixtureDef.restitution = 0.05f;
        fixtureDef.friction = 1.0f;

        DistanceJointDef jointDef = new DistanceJointDef();
        jointDef.length = 1f;
        jointDef.collideConnected = true;
        jointDef.frequencyHz = 10;
        jointDef.dampingRatio = 0.1f;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        Body[][] bodies = new Body[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bodyDef.position.set(i * spacing + offset, j * spacing + offset);

                Body curr = world.createBody(bodyDef);
                curr.createFixture(fixtureDef);

                bodies[i][j] = curr;

                Body test;

                // Connect to left.
                if (i - 1 >= 0 && (test = bodies[i - 1][j]) != null) {
                    jointDef.initialize(curr, test, curr.getWorldCenter(), test.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to bottom.
                if (j - 1 >= 0 && (test = bodies[i][j - 1]) != null) {
                    jointDef.initialize(curr, test, curr.getWorldCenter(), test.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to left bottom.
                if (i - 1 >= 0 && j - 1 >= 0 && (test = bodies[i - 1][j - 1]) != null) {
                    jointDef.initialize(curr, test, curr.getWorldCenter(), test.getWorldCenter());
                    world.createJoint(jointDef);
                }

                // Connect to left top.
                if (i - 1 >= 0 && j + 1 < height && (test = bodies[i - 1][j + 1]) != null) {
                    jointDef.initialize(curr, test, curr.getWorldCenter(), test.getWorldCenter());
                    world.createJoint(jointDef);
                }
            }
        }

        circleShape.dispose();
        return bodies;
    }

    private final float[] verts;

    /*
            polygonRegion.vertices[i * 2] = -body.getLocalPoint(vec).x.pixels * xRatio
            polygonRegion.vertices[i * 2 + 1] = -body.getLocalPoint(vec).y.pixels * yRatio
     */

    private float[] updateVerts() {
        int k = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Body body = bodies[i][j];

                verts[k * 2] = body.getPosition().x;
                verts[k * 2 + 1] = body.getPosition().y;
                k++;
            }
        }
        return verts;
    }

    public void render(PolygonSpriteBatch batch) {
        updateVerts();


        batch.draw(polygonRegion, 0, 0);
    }
}