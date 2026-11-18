package com.soradgaming.simplehudenhanced.config;

import com.soradgaming.simplehudenhanced.gui.HudEditScreen;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

/**
 * Adds two buttons to this mod's Cloth Config screen, next to Cancel and Save & Quit:
 * "Edit HUD" (opens the in-world HUD editor) and "Apply" (saves without closing the screen).
 */
@Environment(EnvType.CLIENT)
public class ConfigScreenApplyButton {
    private static final String SCREEN_TITLE_KEY = "text.autoconfig.simplehudenhanced.title";
    private static final String APPLY_KEY = "simplehudenhanced.config.apply";
    private static final String EDIT_HUD_KEY = "simplehudenhanced.config.edit_hud";
    // Cloth places its bottom buttons at (screen height - 26)
    private static final int BOTTOM_BAR_OFFSET = 26;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractConfigScreen configScreen && isOurScreen(screen)) {
                addButtons(configScreen);
            }
        });
    }

    private static boolean isOurScreen(Screen screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents content
                && SCREEN_TITLE_KEY.equals(content.getKey());
    }

    private static void addButtons(AbstractConfigScreen screen) {
        List<AbstractWidget> buttons = Screens.getWidgets(screen);
        int barY = screen.height - BOTTOM_BAR_OFFSET;

        List<AbstractWidget> bottomBar = buttons.stream()
                .filter(button -> button instanceof Button && button.getY() == barY)
                .sorted(Comparator.comparingInt(AbstractWidget::getX))
                .toList();
        // Expect exactly Cancel and Save & Quit; if Cloth's layout ever changes, leave the screen alone
        if (bottomBar.size() != 2) return;

        AbstractWidget cancel = bottomBar.get(0);
        AbstractWidget saveAndQuit = bottomBar.get(1);

        // Split the space the two buttons used into four equal buttons with the same gaps:
        // [Edit HUD] [Cancel] [Apply] [Save & Quit]
        int left = cancel.getX();
        int gap = saveAndQuit.getX() - cancel.getRight();
        int totalWidth = saveAndQuit.getRight() - left;
        int buttonWidth = (totalWidth - 3 * gap) / 4;
        int stride = buttonWidth + gap;

        cancel.setWidth(buttonWidth);
        cancel.setX(left + stride);
        saveAndQuit.setWidth(buttonWidth);
        saveAndQuit.setX(left + 3 * stride);

        Button editHud = Button.builder(Component.translatable(EDIT_HUD_KEY), button -> {
                    // Save pending changes first so the editor shows the current layout/scale/etc.
                    screen.saveAll(false);
                    Minecraft.getInstance().gui.setScreen(new HudEditScreen(getParent(screen), screen.selectedCategoryIndex, getScroll(screen)));
                })
                .bounds(left, barY, buttonWidth, cancel.getHeight())
                .build();

        Button apply = Button.builder(Component.translatable(APPLY_KEY), button -> {
                    screen.saveAll(false);
                    // Cloth decides "edited" by comparing each option with its value when the screen opened, so after
                    // saving, rebuild the screen: that makes the saved values the new starting point. Otherwise
                    // changing an option back to its old value would look like "no change" and could not be saved.
                    reopen(getParent(screen), screen.selectedCategoryIndex, getScroll(screen));
                })
                .bounds(left + 2 * stride, barY, buttonWidth, cancel.getHeight())
                .build();
        apply.active = false;

        buttons.add(editHud);
        buttons.add(apply);

        // Apply is only clickable while there are unsaved changes, like Save & Quit
        ScreenEvents.beforeExtract(screen).register((s, context, mouseX, mouseY, tickDelta) -> apply.active = screen.isEdited());
    }

    /**
     * Opens a fresh settings screen (built from the saved config) on the given tab and scroll position.
     *
     * @param parent the screen the settings screen was opened from (e.g. Mod Menu)
     */
    public static void reopen(Screen parent, int tab, double scroll) {
        Screen fresh = AutoConfigClient.getConfigScreen(SimpleHudEnhancedConfig.class, parent).get();
        if (fresh instanceof AbstractConfigScreen configScreen) {
            configScreen.selectedCategoryIndex = tab;
        }
        Minecraft.getInstance().gui.setScreen(fresh);
        setScroll(fresh, scroll);
    }

    /** The screen the settings screen was opened from (e.g. Mod Menu), so we can return there afterwards. */
    private static Screen getParent(AbstractConfigScreen screen) {
        try {
            Field parent = AbstractConfigScreen.class.getDeclaredField("parent");
            parent.setAccessible(true);
            return (Screen) parent.get(screen);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    private static double getScroll(AbstractConfigScreen screen) {
        return screen instanceof ClothConfigScreen cloth && cloth.listWidget != null ? cloth.listWidget.getScroll() : 0;
    }

    private static void setScroll(Screen screen, double scroll) {
        if (scroll <= 0 || !(screen instanceof ClothConfigScreen cloth) || cloth.listWidget == null) return;
        try {
            // The list widget's scroll position is a protected field on Cloth's base list class
            for (Class<?> type = cloth.listWidget.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField("scroll");
                    field.setAccessible(true);
                    field.setDouble(cloth.listWidget, scroll);
                    return;
                } catch (NoSuchFieldException ignored) {
                    // try the parent class
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not critical: the screen just opens scrolled to the top
        }
    }
}
