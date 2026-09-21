package client;

import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class FontRenderer {

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR  = 126;
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;

    private final int textureId;
    private final int[] charWidth = new int[128];
    private int charHeight;

    private int atlasWidth;
    private int atlasHeight;

    private final int shaderProgram;
    private final int vao;
    private final int vbo;

    public FontRenderer(Font font, int shaderProgram) {
        this.shaderProgram = shaderProgram;

        BufferedImage atlas = buildAtlas(font);
        this.textureId = upload(atlas);

        // Setup OpenGL 3.3 Core Mesh buffers
        this.vao = GL33.glGenVertexArrays();
        this.vbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        // Preallocate static capacity space dynamically: 6 vertices per quad, 4 components per vertex (x,y,u,v)
        long bufferSize = 6L * 4 * Float.BYTES;
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, bufferSize, GL33.GL_DYNAMIC_DRAW);

        // Configure Layout attribute location matching location 0 in shader vertex array
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 4, GL33.GL_FLOAT, false, 4 * Float.BYTES, 0);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);
    }

    private BufferedImage buildAtlas(Font font) {
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        charHeight = fm.getHeight();

        int cols = 16;
        int rows = (int) Math.ceil(CHAR_COUNT / (float) cols);

        // Keeping your original layout: giving 16 pixels per column bounds
        atlasWidth = cols * 16;
        atlasHeight = rows * charHeight;

        // Switched from TYPE_4BYTE_ABGR to clean native Int-packed layouts
        BufferedImage img = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);

        g = img.createGraphics();
        g.setFont(font);

        // Disable antialiasing to retain the sharp pixel aesthetic requested
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setColor(Color.WHITE);

        int x = 0;
        int y = fm.getAscent();
        int index = 0;

        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            char ch = (char) c;
            charWidth[c] = fm.charWidth(ch);

            g.drawString(String.valueOf(ch), x, y);

            x += 16;
            index++;

            if (index % cols == 0) {
                x = 0;
                y += charHeight;
            }
        }

        g.dispose();
        return img;
    }

    private int upload(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);

        // Safely map ARGB into a raw direct OpenGL-ordered buffer stream
        ByteBuffer buffer = MemoryUtil.memAlloc(w * h * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        int tex = GL33.glGenTextures();
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, tex);

        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE);

        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, w, h, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);

        MemoryUtil.memFree(buffer); // Prevent off-heap system leaks
        return tex;
    }

    public void drawString(String text, int x, int y, float r, float g, float b, float a) {
        GL33.glUseProgram(shaderProgram);

        // Retrieve current viewport dimension sizes natively to assemble projection arrays
        int[] viewport = new int[4];
        GL33.glGetIntegerv(GL33.GL_VIEWPORT, viewport);
        int windowWidth = viewport[2];
        int windowHeight = viewport[3];

        // Safely push layout bounds inside stack frame allocations
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projBuffer = stack.mallocFloat(16);
            float left = 0.0f; float right = (float) windowWidth;
            float bottom = (float) windowHeight; float top = 0.0f;

            projBuffer.put(new float[]{
                    2.0f / (right - left), 0.0f, 0.0f, 0.0f,
                    0.0f, 2.0f / (top - bottom), 0.0f, 0.0f,
                    0.0f, 0.0f, -1.0f, 0.0f,
                    -(right + left) / (right - left), -(top + bottom) / (top - bottom), 0.0f, 1.0f
            }).flip();

            GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(shaderProgram, "projection"), false, projBuffer);
        }

        // Apply Color states into uniforms
        GL33.glUniform4f(GL33.glGetUniformLocation(shaderProgram, "textColor"), r, g, b, a);

        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
        GL33.glBindVertexArray(vao);

        int cx = x;
        int cy = y;

        // Loop through characters and dynamically update the VBO array
        for (char c : text.toCharArray()) {
            if (c < FIRST_CHAR || c > LAST_CHAR) continue;

            int index = c - FIRST_CHAR;
            int tx = (index % 16) * 16;
            int ty = (index / 16) * charHeight;

            float u0 = tx / (float) atlasWidth;
            float v0 = ty / (float) atlasHeight;
            float u1 = (tx + charWidth[c]) / (float) atlasWidth;
            float v1 = (ty + charHeight) / (float) atlasHeight;

            int w = charWidth[c];

            // Setup vertices formatting properties matching: x, y, u, v
            float[] vertices = {
                    cx, cy + charHeight, u0, v1,
                    cx, cy, u0, v0,
                    cx + w, cy, u1, v0,
                    cx, cy + charHeight, u0, v1,
                    cx + w, cy, u1, v0,
                    cx + w, cy + charHeight, u1, v1
            };

            // Stream quad geometry chunk maps via standard Stack frames
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer vertexBuffer = stack.mallocFloat(vertices.length);
                vertexBuffer.put(vertices).flip();

                GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
                GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, vertexBuffer);
                GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
            }

            // Execute draw streams
            GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 6);

            cx += w;
        }

        GL33.glBindVertexArray(0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);
    }

    public void drawString(String text, int x, int y) {
        drawString(text, x, y, 1f, 1f, 1f, 1f);
    }

    public void drawString(String text, int x, int y, boolean shadow) {
        int shadowOffsetX = 2;
        int shadowOffsetY = 2;

        if (shadow) {
            drawString(text, x + shadowOffsetX, y + shadowOffsetY, 0f, 0f, 0f, 1f);
        }
        drawString(text, x, y, 1f, 1f, 1f, 1f);
    }

    public void drawString(String text, int x, int y, Color color, boolean shadow) {
        int shadowOffsetX = 2;
        int shadowOffsetY = 2;

        if (shadow) {
            drawString(text, x + shadowOffsetX, y + shadowOffsetY, 0f, 0f, 0f, 1f);
        }
        drawString(text, x, y, color);
    }

    public void drawString(String text, int x, int y, Color color) {
        if (color == null) color = Color.WHITE;
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        drawString(text, x, y, r, g, b, a);
    }
    // gets string width
    public int getStringWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            if (c < FIRST_CHAR || c > LAST_CHAR) continue;
            width += charWidth[c];
        }
        return width;
    }
    // gets string height
    public int getStringHeight() {
        return charHeight;
    }

    public void cleanup() {
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
        GL33.glDeleteTextures(textureId);
    }
}
