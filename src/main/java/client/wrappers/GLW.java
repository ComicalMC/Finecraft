package client.wrappers;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
/**
 * This is a wrapper for OpenGL 3.3 CORE <br>
 * it is made so you can implement and upgrade things easier
 * @since alpha 0.1.0
 */
// TODO: add GL33 stuff
@SuppressWarnings("unused")
public final class GLW {

    private GLW() {}

    // primitives
    public static final int TRIANGLES = GL11.GL_TRIANGLES;
    public static final int LINES = GL11.GL_LINES;

    // data types
    public static final int FLOAT = GL11.GL_FLOAT;
    public static final int UNSIGNED_BYTE = GL11.GL_UNSIGNED_BYTE;
    public static final int UNSIGNED_INT = GL11.GL_UNSIGNED_INT;

    // capabilities
    public static final int TEXTURE_2D = GL11.GL_TEXTURE_2D;
    public static final int BLEND = GL11.GL_BLEND;
    public static final int DEPTH_TEST = GL11.GL_DEPTH_TEST;
    public static final int CULL_FACE = GL11.GL_CULL_FACE;

    // blend factors
    public static final int SRC_ALPHA = GL11.GL_SRC_ALPHA;
    public static final int ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
    public static final int ONE = GL11.GL_ONE;
    public static final int ZERO = GL11.GL_ZERO;

    // depth
    public static final int LEQUAL = GL11.GL_LEQUAL;
    public static final int LESS = GL11.GL_LESS;
    public static final int ALWAYS = GL11.GL_ALWAYS;

    // clear / buffer bits
    public static final int COLOR_BUFFER_BIT = GL11.GL_COLOR_BUFFER_BIT;
    public static final int DEPTH_BUFFER_BIT = GL11.GL_DEPTH_BUFFER_BIT;
    public static final int STENCIL_BUFFER_BIT = GL11.GL_STENCIL_BUFFER_BIT;

    // polygon mode
    public static final int FRONT_AND_BACK = GL11.GL_FRONT_AND_BACK;
    public static final int LINE = GL11.GL_LINE;
    public static final int FILL = GL11.GL_FILL;

    // texture filtering & wrapping
    public static final int NEAREST = GL11.GL_NEAREST;
    public static final int LINEAR = GL11.GL_LINEAR;
    public static final int TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
    public static final int TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
    public static final int TEXTURE_WRAP_S = GL11.GL_TEXTURE_WRAP_S;
    public static final int TEXTURE_WRAP_T = GL11.GL_TEXTURE_WRAP_T;
    public static final int REPEAT = GL11.GL_REPEAT;
    public static final int CLAMP_TO_EDGE = GL12.GL_CLAMP_TO_EDGE;

    // Pixel formats
    public static final int RGBA = GL11.GL_RGBA;
    public static final int RGB = GL11.GL_RGB;

    // buffer objects
    public static final int ARRAY_BUFFER = GL15.GL_ARRAY_BUFFER;
    public static final int ELEMENT_ARRAY_BUFFER = GL15.GL_ELEMENT_ARRAY_BUFFER;
    public static final int STATIC_DRAW = GL15.GL_STATIC_DRAW;
    public static final int DYNAMIC_DRAW = GL15.GL_DYNAMIC_DRAW;
    public static final int STREAM_DRAW = GL15.GL_STREAM_DRAW;

    // capability state
    public static void enable(int cap) { GL11.glEnable(cap); }
    public static void disable(int cap) { GL11.glDisable(cap); }

    // clearing
    public static void clear(int mask) { GL11.glClear(mask); }
    public static void clearColor(float r, float g, float b, float a) { GL11.glClearColor(r, g, b, a); }
    public static void clearDepth(double d) { GL11.glClearDepth(d); }

    // depth / blending / polygon mode
    public static void depthFunc(int f) { GL11.glDepthFunc(f); }
    public static void depthMask(boolean enabled) { GL11.glDepthMask(enabled); }
    public static void blendFunc(int sfactor, int dfactor) { GL11.glBlendFunc(sfactor, dfactor); }
    public static void polygonMode(int face, int mode) { GL11.glPolygonMode(face, mode); }

    // drawing - modern drawing uses indices offsets from the bound ELEMENT_ARRAY_BUFFER
    public static void drawArrays(int mode, int first, int count) { GL11.glDrawArrays(mode, first, count); }
    public static void drawElements(int mode, int count, int type, long indicesOffset) {
        GL11.glDrawElements(mode, count, type, indicesOffset);
    }

    // texture lifecycle
    public static int genTexture() { return GL11.glGenTextures(); }
    public static void deleteTexture(int id) { GL11.glDeleteTextures(id); }
    public static void bindTexture(int target, int id) { GL11.glBindTexture(target, id); }
    public static void texParameteri(int target, int pname, int value) { GL11.glTexParameteri(target, pname, value); }
    public static void texImage2D(int target, int level, int internalFmt, int w, int h, int border, int fmt, int type, ByteBuffer pixels) {
        GL11.glTexImage2D(target, level, internalFmt, w, h, border, fmt, type, pixels);
    }

    // viewport
    public static void viewport(int x, int y, int w, int h) { GL11.glViewport(x, y, w, h); }

    // pixel readbacks
    public static void readPixels(int x, int y, int w, int h, int fmt, int type, ByteBuffer dst) {
        GL11.glReadPixels(x, y, w, h, fmt, type, dst);
    }

    // --- Core VBOs ---
    public static int genBuffer() { return GL15.glGenBuffers(); }
    public static void deleteBuffer(int id) { GL15.glDeleteBuffers(id); }
    public static void bindBuffer(int target, int id) { GL15.glBindBuffer(target, id); }
    public static void bufferData(int target, FloatBuffer data, int usage) { GL15.glBufferData(target, data, usage); }
    public static void bufferData(int target, IntBuffer data, int usage) { GL15.glBufferData(target, data, usage); }
    public static void bufferData(int target, ByteBuffer data, int usage) { GL15.glBufferData(target, data, usage); }

    // --- Core VAOs ---
    public static int genVertexArray() { return GL30.glGenVertexArrays(); }
    public static void deleteVertexArray(int id) { GL30.glDeleteVertexArrays(id); }
    public static void bindVertexArray(int id) { GL30.glBindVertexArray(id); }

    // --- Shader pipeline ---
    public static final int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    public static final int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
    public static final int COMPILE_STATUS = GL20.GL_COMPILE_STATUS;
    public static final int LINK_STATUS = GL20.GL_LINK_STATUS;

    public static boolean hasShaderSupport() {
        return GL.getCapabilities().OpenGL20;
    }

    public static int createShader(int type) { return GL20.glCreateShader(type); }
    public static void shaderSource(int shader, CharSequence source) { GL20.glShaderSource(shader, source); }
    public static void compileShader(int shader) { GL20.glCompileShader(shader); }
    public static int getShaderi(int shader, int pname) { return GL20.glGetShaderi(shader, pname); }
    public static String getShaderInfoLog(int shader) { return GL20.glGetShaderInfoLog(shader); }
    public static void deleteShader(int shader) { GL20.glDeleteShader(shader); }

    public static int createProgram() { return GL20.glCreateProgram(); }
    public static void attachShader(int program, int shader) { GL20.glAttachShader(program, shader); }
    public static void linkProgram(int program) { GL20.glLinkProgram(program); }
    public static int getProgrami(int program, int pname) { return GL20.glGetProgrami(program, pname); }
    public static String getProgramInfoLog(int program) { return GL20.glGetProgramInfoLog(program); }
    public static void useProgram(int program) { GL20.glUseProgram(program); }
    public static void deleteProgram(int program) { GL20.glDeleteProgram(program); }

    public static int getUniformLocation(int program, CharSequence name) { return GL20.glGetUniformLocation(program, name); }
    public static int getAttribLocation(int program, CharSequence name) { return GL20.glGetAttribLocation(program, name); }
    public static void bindAttribLocation(int program, int index, CharSequence name) { GL20.glBindAttribLocation(program, index, name); }

    public static void uniform1i(int loc, int v) { GL20.glUniform1i(loc, v); }
    public static void uniform1f(int loc, float v) { GL20.glUniform1f(loc, v); }
    public static void uniform3f(int loc, float a, float b, float c) { GL20.glUniform3f(loc, a, b, c); }
    public static void uniform4f(int loc, float a, float b, float c, float d) { GL20.glUniform4f(loc, a, b, c, d); }
    public static void uniformMatrix4fv(int loc, boolean transpose, FloatBuffer matrices) { GL20.glUniformMatrix4fv(loc, transpose, matrices); }

    // vertex attribute arrays
    public static void enableVertexAttribArray(int index) { GL20.glEnableVertexAttribArray(index); }
    public static void disableVertexAttribArray(int index) { GL20.glDisableVertexAttribArray(index); }
    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    // multitexture
    public static final int TEXTURE0 = GL13.GL_TEXTURE0;
    public static void activeTexture(int unit) { GL13.glActiveTexture(unit); }
}