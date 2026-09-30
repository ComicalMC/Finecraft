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
 * Button quit = Button.button(() -> minecraft.stop(), "Quit", 10, 10, 200, 20, "#808080FF");
 * // each frame:
 * quit.render(fontRenderer, window);
 * // on a mouse click:
 * quit.mouseClicked(mouseX, mouseY);
 * }</pre>
 *
 * @since alpha 0.2.0
 */
public final class Button {

    /**
     * A no-argument callback invoked when the button is clicked, e.g. a method
     * reference like {@code minecraft::stop} or a lambda.
     */
    @FunctionalInterface
    public interface ClickAction {
        /** Runs whatever this button is supposed to do. */
        void onClick();
    }

    // the default color is gray
    private static final float[] defaultColor = {0.545f, 0.545f, 0.545f, 1f};
    // the default border is white
    private static final float[] defaultBorderColor = {1f, 1f, 1f, 1f};
    // when you hover, it turns white
    private static final float[] HOVER_TINT = {1f, 1f, 1f, 1f};

    private final ClickAction action;
    private final String label;
    private final int x, y, sizeX, sizeY;
    private final float[] color;
    private final float[] borderColor;
    private final boolean borderEnabled;

    private boolean hovered;

    private Button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor, boolean borderEnabled, String hexBorderColor) {
        this.action = action;
        this.label = label;
        this.x = x;
        this.y = y;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.color = hexColor != null && !hexColor.isEmpty() ? parseHex(hexColor) : defaultColor.clone();
        this.borderEnabled = borderEnabled;
        this.borderColor = hexBorderColor != null && !hexBorderColor.isEmpty() ? parseHex(hexBorderColor) : defaultBorderColor.clone();
    }

    // create a button with the color gray and a border
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY) {
        return button(action, label, x, y, sizeX, sizeY, null, true, null);
    }

    /**
     * Creates a button with a border.
     *
     * @param action    code to run when the button is clicked
     * @param label     text shown on the button, centered
     * @param x         pos X (top left), supported value: pixels
     * @param y         pos Y (top left), supported values: pixels
     * @param sizeX     width, supported values: pixels
     * @param sizeY     height, supported values: pixels
     * @param hexColor  color, supported values: hex (#RRGGBBAA)
     */
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor) {
        return button(action, label, x, y, sizeX, sizeY, hexColor, true, null);
    }

    /**
     * Creates a button.
     *
     * @param action         code to run when the button is clicked
     * @param label          text shown on the button, centered
     * @param x              pos X (top left), supported value: pixels
     * @param y              pos Y (top left), supported values: pixels
     * @param sizeX          width, supported values: pixels
     * @param sizeY          height, supported values: pixels
     * @param hexColor       color, supported values: hex (#RRGGBBAA)
     * @param borderEnabled whether to render the border
     */
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor, boolean borderEnabled) {
        return button(action, label, x, y, sizeX, sizeY, hexColor, borderEnabled, null);
    }

    /**
     * Creates a button with a custom border color.
     *
     * @param action          code to run when the button is clicked
     * @param label           text shown on the button, centered
     * @param x               pos X (top left), supported value: pixels
     * @param y               pos Y (top left), supported values: pixels
     * @param sizeX            width, supported values: pixels
     * @param sizeY            height, supported values: pixels
     * @param hexColor        color, supported values: hex (#RRGGBBAA)
     * @param borderEnabled   whether to render the border
     * @param hexBorderColor  border color, supported values: hex (#RRGGBBAA)
     */
    public static Button button(ClickAction action, String label, int x, int y, int sizeX, int sizeY, String hexColor, boolean borderEnabled, String hexBorderColor) {
        return new Button(action, label, x, y, sizeX, sizeY, hexColor, borderEnabled, hexBorderColor);
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

        GL33.glEnable(GL33.GL_BLEND);
        GL33.glBlendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);

        drawButton();

        GL33.glDisable(GL33.GL_BLEND);

        drawLabel(fontRenderer);
    }

    private void drawButton() {
        float[] fill = color;

        if (hovered) {
            fill = new float[]{
                    color[0] + (HOVER_TINT[0] - color[0]) * 0.15f,
                    color[1] + (HOVER_TINT[1] - color[1]) * 0.15f,
                    color[2] + (HOVER_TINT[2] - color[2]) * 0.15f,
                    color[3]
            };
        }

        if (borderEnabled) {
            drawQuad(x, y, sizeX, sizeY, borderColor);
            drawQuad(x + 1f, y + 1f, sizeX - 2f, sizeY - 2f, fill);
        } else {
            drawQuad(x, y, sizeX, sizeY, fill);
        }
    }

    private void drawQuad(float x, float y, float width, float height, float[] quadColor) {
        GL33.glUseProgram(quadShader);

        int[] viewport = new int[4];
        GL33.glGetIntegerv(GL33.GL_VIEWPORT, viewport);
        float screenWidth = viewport[2];
        float screenHeight = viewport[3];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer proj = stack.mallocFloat(16);
            proj.put(new float[]{
                    2f / screenWidth, 0f, 0f, 0f,
                    0f, -2f / screenHeight, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    -1f, 1f, 0f, 1f
            }).flip();
            GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(quadShader, "projection"), false, proj);
        }

        GL33.glUniform4f(
                GL33.glGetUniformLocation(quadShader, "color"),
                quadColor[0], quadColor[1], quadColor[2], quadColor[3]
        );

        float x0 = x, y0 = y, x1 = x + width, y1 = y + height;
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