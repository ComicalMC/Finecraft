package client.gui.factory.texture;

import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * <pre>{@code
 * int texId = Textures.loadTexture("/client/backgrounds/menu_background.png", GL_LINEAR);
 * TexturedRect background = TexturedRect.sprite(texId, 0, 0, windowWidth, windowHeight);
 * // each frame:
 * background.render();
 * }</pre>
 *
 * @since alpha 0.2.0
 */
public final class TexturedRect {

    private final int textureId;
    private final int x, y, sizeX, sizeY;

    private TexturedRect(int textureId, int x, int y, int sizeX, int sizeY) {
        this.textureId = textureId;
        this.x = x;
        this.y = y;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    /**
     * @param textureId a GL texture id, as returned by {@link client.Textures#loadTexture}
     * @param x         top-left X position, in pixels
     * @param y         top-left Y position, in pixels
     * @param sizeX     width to draw at, in pixels (independent of the texture's own size)
     * @param sizeY     height to draw at, in pixels
     */
    public static TexturedRect sprite(int textureId, int x, int y, int sizeX, int sizeY) {
        return new TexturedRect(textureId, x, y, sizeX, sizeY);
    }

    // rendering

    // Lazily-initialized shader/mesh shared by every TexturedRect instance
    private static int shader = -1;
    private static int vao = -1;
    private static int vbo = -1;

    private static final String VERTEX_SRC =
            "#version 330 core\n" +
            "layout (location = 0) in vec4 vertex;\n" + // vec2 pos, vec2 texCoords
            "out vec2 TexCoords;\n" +
            "uniform mat4 projection;\n" +
            "void main() {\n" +
            "    gl_Position = projection * vec4(vertex.xy, 0.0, 1.0);\n" +
            "    TexCoords = vertex.zw;\n" +
            "}\n";

    private static final String FRAGMENT_SRC =
            "#version 330 core\n" +
            "in vec2 TexCoords;\n" +
            "out vec4 outColor;\n" +
            "uniform sampler2D image;\n" +
            "void main() {\n" +
            "    outColor = texture(image, TexCoords);\n" +
            "}\n";

    private static void ensureShader() {
        if (shader != -1) return;

        int vs = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vs, VERTEX_SRC);
        GL33.glCompileShader(vs);
        checkCompile(vs, "sprite vertex shader");

        int fs = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fs, FRAGMENT_SRC);
        GL33.glCompileShader(fs);
        checkCompile(fs, "sprite fragment shader");

        shader = GL33.glCreateProgram();
        GL33.glAttachShader(shader, vs);
        GL33.glAttachShader(shader, fs);
        GL33.glLinkProgram(shader);

        if (GL33.glGetProgrami(shader, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Failed to link sprite shader: " + GL33.glGetProgramInfoLog(shader));
        }

        GL33.glDeleteShader(vs);
        GL33.glDeleteShader(fs);

        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, 6L * 4 * Float.BYTES, GL33.GL_DYNAMIC_DRAW);
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 4, GL33.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);
    }

    private static void checkCompile(int shaderId, String name) {
        if (GL33.glGetShaderi(shaderId, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Failed to compile " + name + ": " + GL33.glGetShaderInfoLog(shaderId));
        }
    }

    /** Draws this sprite's texture into its rectangle. */
    public void render() {
        ensureShader();

        GL33.glUseProgram(shader);

        int[] viewport = new int[4];
        GL33.glGetIntegerv(GL33.GL_VIEWPORT, viewport);
        float width = viewport[2];
        float height = viewport[3];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Same top-left-origin orthographic projection used by FontRenderer
            // and Button, so sprite coordinates line up with the rest of the UI.
            FloatBuffer proj = stack.mallocFloat(16);
            proj.put(new float[]{
                    2f / width, 0f, 0f, 0f,
                    0f, -2f / height, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    -1f, 1f, 0f, 1f
            }).flip();
            GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(shader, "projection"), false, proj);
        }

        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
        GL33.glUniform1i(GL33.glGetUniformLocation(shader, "image"), 0);

        float x0 = x, y0 = y, x1 = x + sizeX, y1 = y + sizeY;
        float[] vertices = {
                // pos          // texCoords
                x0, y1,         0f, 1f,
                x0, y0,         0f, 0f,
                x1, y0,         1f, 0f,
                x0, y1,         0f, 1f,
                x1, y0,         1f, 0f,
                x1, y1,         1f, 1f
        };

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vbuf = stack.mallocFloat(vertices.length);
            vbuf.put(vertices).flip();
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, vbuf);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        }

        GL33.glBindVertexArray(vao);
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 6);
        GL33.glBindVertexArray(0);
        GL33.glUseProgram(0);
    }
}
