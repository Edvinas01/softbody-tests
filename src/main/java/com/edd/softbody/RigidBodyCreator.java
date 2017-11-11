package com.edd.softbody;

import com.badlogic.gdx.physics.box2d.*;

import static com.badlogic.gdx.math.MathUtils.random;

/**
 * Helper for creating simple bodies.
 */
public final class RigidBodyCreator {

    private static final float MIN_FRICTION = 0.1f;
    private static final float MAX_FRICTION = 1f;

    private static final float MIN_DENSITY = 0.1f;
    private static final float MAX_DENSITY = 1;

    // Min and max size for rectangle bodies.
    private static final float MIN_SIZE = 0.1f;
    private static final float MAX_SIZE = 1f;

    // Min and max size for circles.
    private static final float MIN_RADIUS = 0.1f;
    private static final float MAX_RADIUS = 0.7f;

    private final World world;

    public RigidBodyCreator(World world) {
        this.world = world;
    }

    /**
     * Create a static rectangle at given coordinates.
     */
    public Body createPlatform(float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x, y);

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2, height / 2);

        body.createFixture(shape, 0);
        shape.dispose();

        return body;
    }

    /**
     * Create a dynamic rectangle at given coordinates with a random size.
     */
    public Body createRectangle(float x, float y) {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(random(MIN_SIZE, MAX_SIZE) / 2, random(MIN_SIZE, MAX_SIZE) / 2);

        return createBody(x, y, shape);
    }

    /**
     * Create a dynamic circle at given coordinates with a random radius.
     */
    public Body createCircle(float x, float y) {
        CircleShape shape = new CircleShape();
        shape.setRadius(random(MIN_RADIUS, MAX_RADIUS));

        return createBody(x, y, shape);
    }

    /**
     * Create a body with random parameters at a given position.
     */
    private Body createBody(float x, float y, Shape shape) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = random(MIN_FRICTION, MAX_FRICTION);
        fixtureDef.density = random(MIN_DENSITY, MAX_DENSITY);
        fixtureDef.shape = shape;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        shape.dispose();
        return body;
    }
}