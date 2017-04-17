package com.edd.softbodies;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class SoftRectangle {

    private final int width;
    private final int height;

    private float[] verts;
    private final Body[][] bodies;

    private final Texture texture;
    private Mesh mesh;

    public SoftRectangle(World world, Texture texture, int width, int height) {
        this.width = width;
        this.height = height;

        this.bodies = new Body[width][height];

        createBody(world);

        this.texture = texture;

        calculateVertices();
    }

    private void createBody(World world) {
        float off = 1f;
        float pos = 0.5f;

        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(0.2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circleShape;
        fixtureDef.density = 0.1f;
        fixtureDef.restitution = 0.05f;
        fixtureDef.friction = 1.0f;

        DistanceJointDef jointDef = new DistanceJointDef();
        jointDef.collideConnected = true;
        jointDef.frequencyHz = 10;
        jointDef.dampingRatio = 0.1f;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bodyDef.position.set(i * pos + off, j * pos + off);

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
    }

    private void calculateVertices() {
        List<Float> vertices = new ArrayList<>();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Vector2 pos = bodies[i][j].getPosition();

                float u = (float) i / (height - 1);
                float v = 1 - (float) j / (width - 1);

                vertices.add(pos.x);
                vertices.add(pos.y);
                vertices.add(0f);
                vertices.add(u);
                vertices.add(v);
            }
        }

        short[] indices = new short[width * width * 6];
        int index = 0;

        for (int x = 0; x < width - 1; x++) {
            for (int z = 0; z < width - 1; z++) {
                int offset = x * width + z;
                indices[index] = (short) (offset);
                indices[index + 1] = (short) (offset + 1);
                indices[index + 2] = (short) (offset + width);
                indices[index + 3] = (short) (offset + 1);
                indices[index + 4] = (short) (offset + width + 1);
                indices[index + 5] = (short) (offset + width);
                index += 6;
            }
        }

        if (this.mesh == null) {
            this.mesh = new Mesh(
                    false,
                    vertices.size(), indices.length,
//                    indices.size(),
                    new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                    new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE)
            );
        }

        float[] ve = new float[vertices.size()];
//        short[] in = new short[indices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            ve[i] = vertices.get(i);
        }

/*        for (int i = 0; i < indices.size(); i++) {
            in[i] = indices.get(i).shortValue();
        }*/

        mesh.setVertices(ve);

        if (this.mesh.getNumIndices() == 0) {
            mesh.setIndices(indices);
        }

//        verts[i++] = x;   //X
//        verts[i++] = y + height; //Y
//        verts[i++] = 0;    //Z
//        verts[i++] = 0f;   //U
//        verts[i++] = 0f;   //V
    }

    public void render(ShaderProgram shaderProgram) {
        calculateVertices();

        texture.bind();
        mesh.render(shaderProgram, GL20.GL_TRIANGLES);
    }
}