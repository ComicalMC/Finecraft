package client;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class Textures {
    private static final Map<String, Integer> cache = new HashMap<>();

    public static int loadTexture(String resourceName, int mode) {
        String cacheKey = resourceName + "#" + mode;
        Integer cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (InputStream inputStream = Textures.class.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Texture resource not found on classpath: " + resourceName);
            }
            int id = uploadImage(ImageIO.read(inputStream), mode);
            cache.put(cacheKey, id);
            return id;
        } catch (IOException exception) {
            throw new RuntimeException("Could not load texture " + resourceName, exception);
        }
    }

    public static int loadTextureFromFile(File file, int mode) {
        if (file == null || !file.isFile()) return -1;
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return -1;
            return uploadImage(img, mode);
        } catch (IOException | RuntimeException exception) {
            return -1;
        }
    }

    private static int uploadImage(BufferedImage bufferedImage, int mode) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, mode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilterFor(mode));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        int[] pixels = new int[width * height];
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int pixel : pixels) {
            byteBuffer.put((byte) (pixel >> 16 & 0xFF)); // red
            byteBuffer.put((byte) (pixel >>  8 & 0xFF)); // green
            byteBuffer.put((byte) (pixel & 0xFF)); // blue
            byteBuffer.put((byte) (pixel >> 24 & 0xFF)); // alpha
        }
        byteBuffer.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        return id;
    }


    private static int magFilterFor(int minFilterMode) {
        boolean nearest = minFilterMode == GL_NEAREST || minFilterMode == GL_NEAREST_MIPMAP_NEAREST || minFilterMode == GL_NEAREST_MIPMAP_LINEAR;
        return nearest ? GL_NEAREST : GL_LINEAR;
    }

    /**
     * Deletes a texture that was NOT loaded through {@link #loadTexture}
     * (e.g. one returned by {@link #loadTextureFromFile}). Cached classpath
     * textures are shared across screens and are freed once by
     * {@link #clearCache()} instead.
     */
    public static void deleteTexture(int id) {
        if (id >= 0 && !cache.containsValue(id)) glDeleteTextures(id);
    }

    public static void clearCache() {
        for (int id : cache.values()) {
            glDeleteTextures(id);
        }
        cache.clear();
    }

    public static void bind(int id) {
        glBindTexture(GL_TEXTURE_2D, id);
    }
}