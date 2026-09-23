package client.gui.screens;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import client.gui.factory.button.Button;
import client.gui.factory.texture.TexturedRect;

import java.awt.Color;

import static org.lwjgl.opengl.GL11.GL_LINEAR;

public class MainMenu extends Screen {

    private final int windowWidth;
    private final int windowHeight;
    private final int backgroundTexture;
    private final TexturedRect background;

    public MainMenu(Minecraft minecraft, int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        // TODO: Cache the textures so we don't have to wait 10 hours for it to load
        this.backgroundTexture = Textures.loadTexture("/client/backgrounds/menu_background.png", GL_LINEAR);
        this.background = TexturedRect.sprite(backgroundTexture, 0, 0, windowWidth, windowHeight);

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = windowWidth / 2 - buttonWidth / 2;
        int startY = windowHeight / 2 - 20;
        int spacing = 24;

        addButton(Button.button(this::singleplayer, "Singleplayer", centerX, startY, buttonWidth, buttonHeight));
        addButton(Button.button(this::multiplayer, "Multiplayer", centerX, startY + spacing, buttonWidth, buttonHeight));
        addButton(Button.button(this::options, "Options", centerX, startY + spacing * 2, buttonWidth, buttonHeight));
        addButton(Button.button(minecraft::stop, "Quit", centerX, startY + spacing * 3, buttonWidth, buttonHeight, "#AA0000FF"));
    }
    // TODO: Add singleplayer & multiplayer screens
    private void singleplayer() {
        System.out.println("Singleplayer isn't done");
    }

    private void multiplayer() {
        System.out.println("Multiplayer isn't done");
    }

    private void options() {
        Minecraft.mc.setScreen(new OptionsScreen(windowWidth, windowHeight));
    }

    @Override
    public void render(FontRenderer fontRenderer, long window) {
        background.render();

        String title = "Finecraft";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (windowWidth - titleWidth) / 2, 60, Color.WHITE, true);

        renderButtons(fontRenderer, window);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY) {
        super.mouseClicked(mouseX, mouseY);
    }

    @Override
    public void dispose() {
        Textures.deleteTexture(backgroundTexture);
    }
}