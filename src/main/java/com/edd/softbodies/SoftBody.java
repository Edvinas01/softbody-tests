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
        if (mesh == null) {
            mesh = createMesh();
        }

        // Must always update vertex array before drawing the mesh,
        // or else the body will be static.
        updateVertices(vertices);
        mesh.setVertices(vertices);

        // Rendering.
        Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);

        texture.bind();
        mesh.render(program, GL20.GL_TRIANGLE_STRIP);
    }

    /**
     * Update mesh vertex array. Called each time before rendering.
     *
     * @param vertices current array of vertices.
     */
    protected abstract void updateVertices(float[] vertices);

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