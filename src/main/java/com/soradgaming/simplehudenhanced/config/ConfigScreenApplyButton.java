package com.soradgaming.simplehudenhanced.config;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.Comparator;
import java.util.List;

/**
 * Adds an "Apply" button between Cancel and Save & Quit on this mod's Cloth Config screen.
 * Apply saves the settings (and fires the save listeners, so the HUD updates) without closing the screen.
 */
@Environment(EnvType.CLIENT)
public class ConfigScreenApplyButton {
    private static final String SCREEN_TITLE_KEY = "text.autoconfig.simplehudenhanced.title";
    private static final String APPLY_KEY = "simplehudenhanced.config.apply";
    // Cloth places its bottom buttons at (screen height - 26)
    private static final int BOTTOM_BAR_OFFSET = 26;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractConfigScreen configScreen && isOurScreen(screen)) {
                addApplyButton(configScreen);
            }
        });
    }

    private static boolean isOurScreen(Screen screen) {
        return screen.getTitle().getContent() instanceof TranslatableTextContent content
                && SCREEN_TITLE_KEY.equals(content.getKey());
    }

    private static void addApplyButton(AbstractConfigScreen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        int barY = screen.height - BOTTOM_BAR_OFFSET;

        List<ClickableWidget> bottomBar = buttons.stream()
                .filter(button -> button instanceof ButtonWidget && button.getY() == barY)
                .sorted(Comparator.comparingInt(ClickableWidget::getX))
                .toList();
        // Expect exactly Cancel and Save & Quit; if Cloth's layout ever changes, leave the screen alone
        if (bottomBar.size() != 2) return;

        ClickableWidget cancel = bottomBar.get(0);
        ClickableWidget saveAndQuit = bottomBar.get(1);

        // Split the space the two buttons used into three equal buttons with the same gaps
        int gap = saveAndQuit.getX() - cancel.getRight();
        int totalWidth = saveAndQuit.getRight() - cancel.getX();
        int buttonWidth = (totalWidth - 2 * gap) / 3;

        cancel.setWidth(buttonWidth);
        saveAndQuit.setWidth(buttonWidth);
        saveAndQuit.setX(cancel.getX() + 2 * (buttonWidth + gap));

        ButtonWidget apply = ButtonWidget.builder(Text.translatable(APPLY_KEY), button -> screen.saveAll(false))
                .dimensions(cancel.getX() + buttonWidth + gap, barY, buttonWidth, cancel.getHeight())
                .build();
        apply.active = false;
        buttons.add(apply);

        // Only clickable while there are unsaved changes, like Save & Quit
        ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, tickDelta) -> apply.active = screen.isEdited());
    }
}
