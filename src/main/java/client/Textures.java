package client;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Textures {

    public static int loadTexture(String resourceName, int mode) {
        try (InputStream inputStream = Textures.class.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Texture resource not found on classpath: " + resourceName);
            }
            return uploadImage(ImageIO.read(inputStream), mode);
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

    public static void deleteTexture(int id) {
        if (id >= 0) glDeleteTextures(id);
    }

    public static void bind(int id) {
        glBindTexture(GL_TEXTURE_2D, id);
    }
}