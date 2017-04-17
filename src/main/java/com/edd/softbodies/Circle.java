package com.edd.softbodies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.*;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.ArrayList;
import java.util.List;

public final class Circle extends SoftBody {

    private final List<Body> bodies;

    public Circle(Texture texture, World world, float radius, float x, float y) {
        super(texture, 0);

        this.bodies = createBodies(world, radius, x, y);
    }

    private List<Body> createBodies(World world, float radius, float x, float y) {
        List<Body> bodies = new ArrayList<>();
        return bodies;
    }

    @Override
    protected void updateVertices(float[] vertices) {
    }

    @Override
    protected short[] createIndices() {
        return new short[0];
    }
}