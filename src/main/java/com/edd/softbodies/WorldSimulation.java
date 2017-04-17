package com.edd.softbodies;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;

public class WorldSimulation {

    private static final float TIME_STEP = 1.0f / 300f;
    private static final float GRAVITY = -9.8f;

    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private float accumulator = 0f;

    private final Box2DDebugRenderer renderer;
    private final World world;

    public WorldSimulation() {
        this.renderer = new Box2DDebugRenderer(true, true, false, true, false, true);
        this.world = new World(new Vector2(0f, GRAVITY), true);
    }

    public World getWorld() {
        return world;
    }

    public void update(float dt) {
        float frameTime = Math.min(dt, 0.25f);

        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }
    }

    public void render(OrthographicCamera camera) {
        renderer.render(world, camera.combined);
    }
}