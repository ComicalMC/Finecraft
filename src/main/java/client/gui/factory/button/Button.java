package client.gui.factory.button;

import client.FontRenderer;

import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

/**
 * <p>
 * Make it with {@link #button}, render it with
 * {@link #render(FontRenderer, long)}, and wire mouse button events
 * to {@link #mouseClicked(double, double)} so it can do its job
 *
 * <pre>{@code
 * Button quit = Button.button(() -> minecraft.stop(), "Quit", 10, 10, 200, 20, "#hexcolor");
 * // each frame:
 * quit.render(fontRenderer, window);
 * // on a mouse click:
 * quit.mouseClicked(mouseX, mouseY);
 * }</pre>
 *
 * @since alpha 0.2.0
 */
public final class Button {

    // TODO: document this code
    @FunctionalInterface
    public interface ClickAction {
        void onClick();
    }
    // the default color is gray
    private static final float[] defaultColor = {0.545f, 0.545f, 0.545f, 1f};
    // when you hover, it turns white
    private static final float[] HOVER_TINT = {0.15f, 0.15f, 0.15f, 0f};

    private final ClickAction action;
    private final String label;
    private final int x, y, sizeX, sizeY;
    private final float[] color;

    private boolean hovered;

    private Button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor) {
        this.action = action;
        this.label = label;
        this.x = x;
        this.y = y;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.color = hexColor != null ? parseHex(hexColor) : defaultColor.clone();
    }

    // create a button with the color gray
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY) {
        return button(action, label, x, y, sizeX, sizeY, null);
    }

    /**
     * Creates a button.
     *
     * @param action   code to run when the button is clicked
     * @param label    text shown on the button, centered
     * @param x        pos X (top left), supported value: pixels
     * @param y        pos Y (top left), supported values: pixels
     * @param sizeX    width, supported values: pixels
     * @param sizeY    height, supported values: pixels
     * @param hexColor color, supported values: hex (#RRGGBBAA)
     */
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor) {
        return new Button(action, label, x, y, sizeX, sizeY, hexColor);
    }

    // input

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + sizeX && mouseY >= y && mouseY <= y + sizeY;
    }

    /** If the mouse button is clicked, <br>
     *  run the code (action).
     */
    public void mouseClicked(double mouseX, double mouseY) {
        if (isHovered(mouseX, mouseY) && action != null) {
            action.onClick();
        }
    }

    private void updateHover(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer mx = stack.mallocDouble(1);
            DoubleBuffer my = stack.mallocDouble(1);
            glfwGetCursorPos(window, mx, my);
            hovered = isHovered(mx.get(0), my.get(0));
        }
    }

    // rendering

    // Lazily-initialized shader/mesh shared by every Button instance
    private static int quadShader = -1;
    private static int quadVao = -1;
    private static int quadVbo = -1;

    private static final String QUAD_VERTEX_SRC =
            "#version 330 core\n" +
            "layout (location = 0) in vec2 pos;\n" +
            "uniform mat4 projection;\n" +
            "void main() {\n" +
            "gl_Position = projection * vec4(pos, 0.0, 1.0);\n" +
            "}\n";

    private static final String QUAD_FRAGMENT_SRC =
            "#version 330 core\n" +
            "out vec4 outColor;\n" +
            "uniform vec4 color;\n" +
            "void main() {\n" +
            "outColor = color;\n" +
            "}\n";

    private static void ensureShader() {
        if (quadShader != -1) return;

        int vs = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vs, QUAD_VERTEX_SRC);
        GL33.glCompileShader(vs);
        checkCompile(vs, "button quad vertex shader");

        int fs = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fs, QUAD_FRAGMENT_SRC);
        GL33.glCompileShader(fs);
        checkCompile(fs, "button quad fragment shader");

        quadShader = GL33.glCreateProgram();
        GL33.glAttachShader(quadShader, vs);
        GL33.glAttachShader(quadShader, fs);
        GL33.glLinkProgram(quadShader);

        if (GL33.glGetProgrami(quadShader, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Failed to link button quad shader: " + GL33.glGetProgramInfoLog(quadShader));
        }

        GL33.glDeleteShader(vs);
        GL33.glDeleteShader(fs);

        quadVao = GL33.glGenVertexArrays();
        quadVbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(quadVao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, quadVbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, 6L * 2 * Float.BYTES, GL33.GL_DYNAMIC_DRAW);
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 2, GL33.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);
    }

    private static void checkCompile(int shader, String name) {
        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Failed to compile " + name + ": " + GL33.glGetShaderInfoLog(shader));
        }
    }

    /** Renders the button, centers the text (label), updates the color when hovered */
    public void render(FontRenderer fontRenderer, long window) {
        ensureShader();
        updateHover(window);
        drawQuad();
        drawLabel(fontRenderer);
    }

    private void drawQuad() {
        GL33.glUseProgram(quadShader);

        int[] viewport = new int[4];
        GL33.glGetIntegerv(GL33.GL_VIEWPORT, viewport);
        float width = viewport[2];
        float height = viewport[3];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Same top-left-origin orthographic projection FontRenderer uses,
            // so button coordinates line up with text coordinates.
            FloatBuffer proj = stack.mallocFloat(16);
            proj.put(new float[]{
                    2f / width, 0f, 0f, 0f,
                    0f, -2f / height, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    -1f, 1f, 0f, 1f
            }).flip();
            GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(quadShader, "projection"), false, proj);
        }

        float[] fill = color;
        if (hovered) {
            fill = new float[]{
                    Math.min(color[0] + HOVER_TINT[0], 1f),
                    Math.min(color[1] + HOVER_TINT[1], 1f),
                    Math.min(color[2] + HOVER_TINT[2], 1f),
                    color[3]
            };
        }
        GL33.glUniform4f(GL33.glGetUniformLocation(quadShader, "color"), fill[0], fill[1], fill[2], fill[3]);

        float x0 = x, y0 = y, x1 = x + sizeX, y1 = y + sizeY;
        float[] vertices = {
                x0, y1,
                x0, y0,
                x1, y0,
                x0, y1,
                x1, y0,
                x1, y1
        };

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vbuf = stack.mallocFloat(vertices.length);
            vbuf.put(vertices).flip();
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, quadVbo);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, vbuf);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        }

        GL33.glBindVertexArray(quadVao);
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 6);
        GL33.glBindVertexArray(0);
        GL33.glUseProgram(0);
    }
    // label

    private void drawLabel(FontRenderer fontRenderer) {
        int textWidth = fontRenderer.getStringWidth(label);
        int textHeight = fontRenderer.getStringHeight();
        int textX = x + (sizeX - textWidth) / 2;
        int textY = y + (sizeY - textHeight) / 2;
        fontRenderer.drawString(label, textX, textY, true);
    }

    // color parser

    private static float[] parseHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 8) {
            throw new IllegalArgumentException("Button color must be 8 digits \"RRGGBBAA\"");
        }
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        int a = Integer.parseInt(h.substring(6, 8), 16);
        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }
}
