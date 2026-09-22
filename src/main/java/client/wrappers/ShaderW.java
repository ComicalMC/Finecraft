package client.wrappers;

import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tiny helper for compiling and linking GLSL shader programs from classpath
 * @since alpha 0.2.0
 */
@SuppressWarnings("unused")
public final class ShaderW {

    private ShaderW() {}

    /**
     * Compiles the given vertex/fragment shader resources and links them into
     * a program. Resource paths are resolved on the classpath, so a file at
     * {@code src/main/resources/client/shaders/text/font.vs} is referenced as
     * {@code "/client/shaders/text/font.vs"}.
     */
    public static int loadProgram(String vertexResource, String fragmentResource) {
        int vertex = compile(vertexResource, GL20.GL_VERTEX_SHADER);
        int fragment = compile(fragmentResource, GL20.GL_FRAGMENT_SHADER);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Failed to link shader program (" + vertexResource + ", " + fragmentResource + "): "
                    + GL20.glGetProgramInfoLog(program));
        }

        // The shader objects are no longer needed once linked into the program
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);

        return program;
    }

    private static int compile(String resource, int type) {
        String source = readResource(resource);

        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Failed to compile shader " + resource + ": " + GL20.glGetShaderInfoLog(shader));
        }

        return shader;
    }

    private static String readResource(String path) {
        try (InputStream is = ShaderW.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Shader resource not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
