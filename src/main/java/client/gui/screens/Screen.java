package client.gui.screens;

import client.FontRenderer;
import client.gui.factory.button.Button;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen {

    private final List<Button> buttons = new ArrayList<>();

    /** Called once per frame while this screen is active. */
    public abstract void render(FontRenderer fontRenderer, long window);

    public void resize(int windowWidth, int windowHeight) {}

    /** Called on a mouse-button-release while this screen is active. */
    public void mouseClicked(double mouseX, double mouseY) {
        for (Button button : new ArrayList<>(buttons)) {
            button.mouseClicked(mouseX, mouseY);
        }
    }

    /** Adds a button to this screen. */
    protected void addButton(Button button) {
        buttons.add(button);
    }

    /** Removes every button added so far, so a screen can rebuild its layout from scratch. */
    protected void clearButtons() {
        buttons.clear();
    }

    /** Renders all buttons belonging to this screen. */
    protected void renderButtons(FontRenderer fontRenderer, long window) {
        for (Button button : buttons) {
            button.render(fontRenderer, window);
        }
    }

    public void dispose() {}
}