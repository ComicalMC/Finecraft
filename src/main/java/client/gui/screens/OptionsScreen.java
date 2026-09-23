package client.gui.screens;

import client.FontRenderer;
import client.Minecraft;
import client.gui.factory.button.Button;

import java.awt.Color;
import java.util.List;

/**
 * This is a screen for the options menu
 */
public class OptionsScreen extends Screen {

    private final int windowWidth;
    private final int windowHeight;
    private List<Button> buttons;

    public OptionsScreen(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        buildButtons();
    }

    private void buildButtons() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = windowWidth / 2 - buttonWidth / 2;
        int y = windowHeight / 2 + 60;
        int spacing = 24;

        buttons = List.of(
                Button.button(this::toggleVsync, vsyncLabel(), centerX, y - spacing, buttonWidth, buttonHeight),
                Button.button(this::done, "Done", centerX, y, buttonWidth, buttonHeight)
        );
    }

    private String vsyncLabel() {
        return "VSync: " + (Minecraft.mc.isVsyncEnabled() ? "ON" : "OFF");
    }

    private void toggleVsync() {
        Minecraft.mc.setVsync(!Minecraft.mc.isVsyncEnabled());
        buildButtons();
    }

    private void done() {
        Minecraft.mc.setScreen(new MainMenu(Minecraft.mc, windowWidth, windowHeight));
    }

    @Override
    public void render(FontRenderer fontRenderer, long window) {
        String title = "Options";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (windowWidth - titleWidth) / 2, 60, Color.WHITE, true);

        for (Button button : buttons) {
            button.render(fontRenderer, window);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY) {
        for (Button button : buttons) {
            button.mouseClicked(mouseX, mouseY);
        }
    }
}
