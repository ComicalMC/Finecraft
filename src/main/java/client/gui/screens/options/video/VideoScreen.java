package client.gui.screens.options.video;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import client.gui.factory.button.Button;
import client.gui.factory.texture.TexturedRect;
import client.gui.screens.MainMenu;
import client.gui.screens.Screen;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_LINEAR;

/**
 * This is a screen for video configuration stuff
 */
public class VideoScreen extends Screen {
    private static final int BACKGROUND_TILE_SIZE = 16;

    private int windowWidth;
    private int windowHeight;
    private final int backgroundTexture;
    private TexturedRect background;
    public VideoScreen(int windowWidth, int windowHeight) {

        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.backgroundTexture = Textures.loadTexture("/client/gui/background/background.png", GL_LINEAR);

        layout();
    }
    private void layout() {
        clearButtons();

        this.background = TexturedRect.tiled(backgroundTexture, 0, 0, windowWidth, windowHeight, BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE);

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = windowWidth / 2 - buttonWidth / 2;
        int y = windowHeight / 2 + 60;
        int spacing = 24;
        addButton(Button.button(this::no_op, "coming soon", centerX, y, buttonWidth, buttonHeight));
        addButton(Button.button(this::back, "back", centerX, y - spacing, buttonWidth, buttonHeight));
    }
    private void no_op() {}
    private void back() {
        Minecraft.mc.setScreen(new MainMenu(Minecraft.mc, windowWidth, windowHeight));
    }
    @Override
    public void render(FontRenderer fontRenderer, long window) {
        background.render();

        String title = "Video Options";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (windowWidth - titleWidth) / 2, 60, Color.WHITE, true);

        renderButtons(fontRenderer, window);
    }
}
