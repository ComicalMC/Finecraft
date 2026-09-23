package client;

import client.gui.screens.MainMenu;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import client.gui.screens.Screen;
import client.wrappers.ShaderW;

import java.awt.Font;
import java.io.InputStream;
import java.nio.DoubleBuffer;

public class Minecraft {
    private static final int WINDOW_WIDTH = 854;
    private static final int WINDOW_HEIGHT = 480;
    public static Minecraft mc;
    private long window;
    private FontRenderer fontRenderer;
    private Screen currentScreen;
    private boolean vsyncEnabled = true;
    public Minecraft() {
        mc = this;
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Request OpenGL 3.3 Core Profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Required for macOS support

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Finecraft 0.2.0-alpha", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Set up a key callback to escape
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Forward left-click releases to whichever screen is showing
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    DoubleBuffer mouseX = stack.mallocDouble(1);
                    DoubleBuffer mouseY = stack.mallocDouble(1);
                    glfwGetCursorPos(window, mouseX, mouseY);

                    if (currentScreen != null) {
                        currentScreen.mouseClicked(mouseX.get(0), mouseY.get(0));
                    }
                }
            }
        });

        glfwMakeContextCurrent(window);
        setVsync(vsyncEnabled); // Enable v-sync by default; toggled from the options screen

        // Binds GLFW's current context to LWJGL's OpenGL bindings.
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int fontShader = ShaderW.loadProgram("/client/shaders/text/font.vs", "/client/shaders/text/font.fs");
        fontRenderer = new FontRenderer(loadFont(), fontShader);

        setScreen(new MainMenu(this, WINDOW_WIDTH, WINDOW_HEIGHT));

        glfwShowWindow(window);
    }

    private Font loadFont() {
        try (InputStream is = Minecraft.class.getResourceAsStream("/client/fonts/Minecraft.ttf")) {
            if (is == null) {
                throw new RuntimeException("RuntimeException: Cannot find that font");
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
        } catch (Exception e) {
            throw new RuntimeException("RuntimeException: cannot load that font\nerror message: " + e.getMessage());
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            if (currentScreen != null) {
                currentScreen.render(fontRenderer, window);
            }

            glfwSwapBuffers(window); // Swap the color buffers (removing this will not render the main menu for some reason)
            glfwPollEvents(); // Poll for window events (keys, mouse, resizing)
        }
    }

    /** Swaps the active GUI screen, e.g. {@code Minecraft.mc.setScreen(new OptionsScreen(...))}. */
    public void setScreen(Screen screen) {
        if (currentScreen != null) {
            currentScreen.dispose(); // free the outgoing screen's resources (e.g. MainMenu's background texture) since we no longer keep it around as "previous"
        }
        this.currentScreen = screen;
    }

    /** Referenced by the Quit button; requests the window close so run() unwinds cleanly. */
    public void stop() {
        glfwSetWindowShouldClose(window, true);
    }

    /** Turns v-sync on or off and immediately applies it to the current window. */
    public void setVsync(boolean enabled) {
        this.vsyncEnabled = enabled;
        glfwSwapInterval(enabled ? 1 : 0);
    }

    /** @return whether v-sync is currently enabled. */
    public boolean isVsyncEnabled() {
        return vsyncEnabled;
    }

    private void cleanup() {
        if (currentScreen != null) {
            currentScreen.dispose();
        }
        fontRenderer.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) {
        new Minecraft().run();
    }
}
