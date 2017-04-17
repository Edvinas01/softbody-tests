package com.edd.softbodies;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;

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

        this.verts = new float[30];

        this.mesh = new Mesh(
                false,
                6,
                0,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE)
        );

        int i = 0;
        float x,y; // Mesh location in the world
        float width,height; // Mesh width and height

        x = y = 1;
        width = height = 1;

        Vector2 botLeft = bodies[0][0].getPosition();
        Vector2 topLeft = bodies[0][this.height - 1].getPosition();

        Vector2 botRight = bodies[this.width - 1][0].getPosition();
        Vector2 topRight = bodies[this.width - 1][this.height - 1].getPosition();

        //Top Left Vertex Triangle 1
        verts[i++] = topLeft.x;   //X
        verts[i++] = topLeft.y; //Y
        verts[i++] = 0;    //Z
        verts[i++] = 0f;   //U
        verts[i++] = 0f;   //V

        //Top Right Vertex Triangle 1
        verts[i++] = topRight.x;
        verts[i++] = topRight.y;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 0f;

        //Bottom Left Vertex Triangle 1
        verts[i++] = botLeft.x;
        verts[i++] = botLeft.y;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i++] = 1f;

        //Top Right Vertex Triangle 2
        verts[i++] = topRight.x;
        verts[i++] = topRight.y;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 0f;

        //Bottom Right Vertex Triangle 2
        verts[i++] = botRight.x;
        verts[i++] = botRight.y;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 1f;

        //Bottom Left Vertex Triangle 2
        verts[i++] = botLeft.x;
        verts[i++] = botLeft.y;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i] = 1f;

        mesh.setVertices(verts);

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