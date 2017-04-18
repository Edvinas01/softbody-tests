package com.edd.softbodies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public abstract class SoftBody {

    /**
     * Component count of the vertex array.
     * <pre>
     * 0 - x
     * 1 - y
     * 2 - z
     * 3 - u
     * 4 - v
     * ...
     * </pre>
     */
    protected static final int COMPONENT_COUNT = 5;

    // Joined body constants.
    protected static final float RESTITUTION = 0.05f;
    protected static final float FRICTION = 1f;
    protected static final float DENSITY = 0.1f;
    protected static final float RADIUS = 0.15f;

    // Joint constants.
    protected static final float FREQUENCY = 10f;
    protected static final float DAMPING = 0.1f;

    private final Texture texture;
    private float[] vertices;

    private Mesh mesh;

    public SoftBody(Texture texture, int verticesCount) {
        this.texture = texture;
        this.vertices = new float[verticesCount];
    }

    /**
     * Update mesh and draw the body.
     *
     * @param program shader program using for rendering.
     */
    public void act(ShaderProgram program) {

        // Must always update vertex array before drawing the mesh,
        // or else the body will be static.
        vertices = updateVertices(vertices);

        if (mesh == null) {
            mesh = createMesh();
        }
        mesh.setVertices(vertices);

        // Rendering.
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        texture.bind();
        render(mesh, program);
    }

    /**
     * Hook method called when rendering a mesh.
     */
    protected void render(Mesh mesh, ShaderProgram program) {
        mesh.render(program, GL20.GL_TRIANGLE_STRIP);
    }

    /**
     * Update mesh vertex array. Called each time before rendering.
     *
     * @param vertices current array of vertices.
     */
    protected abstract float[] updateVertices(float[] vertices);

    /**
     * Create a buffer of triangle indices. Called only once during mesh creation.
     *
     * @return created array of indices.
     */
    protected abstract short[] createIndices();

    /**
     * @return mesh based on created indices and vertex buffers.
     */
    private Mesh createMesh() {
        short[] indices = createIndices();

        Mesh mesh = new Mesh(
                false,
                vertices.length,
                indices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE)
        );

        // Indices will stay the same all the time, while vertex array will update,
        // so not setting the vertex array here.
        mesh.setIndices(indices);
        return mesh;
    }
}