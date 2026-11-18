package com.soradgaming.simplehudenhanced.config;

import com.soradgaming.simplehudenhanced.gui.EffectsEditScreen;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

/**
 * Adds two buttons to this mod's Cloth Config screen, next to Cancel and Save & Quit:
 * "Move Effects" (enters the in-world edit mode) and "Apply" (saves without closing the screen).
 */
@Environment(EnvType.CLIENT)
public class ConfigScreenApplyButton {
    private static final String SCREEN_TITLE_KEY = "text.autoconfig.simplehudenhanced.title";
    private static final String APPLY_KEY = "simplehudenhanced.config.apply";
    private static final String MOVE_EFFECTS_KEY = "simplehudenhanced.config.move_effects";
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
        return screen.getTitle().getContent() instanceof TranslatableTextContent content
                && SCREEN_TITLE_KEY.equals(content.getKey());
    }

    private static void addButtons(AbstractConfigScreen screen) {
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

        // Split the space the two buttons used into four equal buttons with the same gaps:
        // [Move Effects] [Cancel] [Apply] [Save & Quit]
        int left = cancel.getX();
        int gap = saveAndQuit.getX() - cancel.getRight();
        int totalWidth = saveAndQuit.getRight() - left;
        int buttonWidth = (totalWidth - 3 * gap) / 4;
        int stride = buttonWidth + gap;

        cancel.setWidth(buttonWidth);
        cancel.setX(left + stride);
        saveAndQuit.setWidth(buttonWidth);
        saveAndQuit.setX(left + 3 * stride);

        ButtonWidget moveEffects = ButtonWidget.builder(Text.translatable(MOVE_EFFECTS_KEY), button -> {
                    // Save pending changes first so edit mode shows the current layout/scale/etc.
                    screen.saveAll(false);
                    MinecraftClient.getInstance().setScreen(new EffectsEditScreen(getParent(screen)));
                })
                .dimensions(left, barY, buttonWidth, cancel.getHeight())
                .build();

        ButtonWidget apply = ButtonWidget.builder(Text.translatable(APPLY_KEY), button -> screen.saveAll(false))
                .dimensions(left + 2 * stride, barY, buttonWidth, cancel.getHeight())
                .build();
        apply.active = false;

        buttons.add(moveEffects);
        buttons.add(apply);

        ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, tickDelta) -> {
            // Apply is only clickable while there are unsaved changes, like Save & Quit
            apply.active = screen.isEdited();
            // Edit mode only makes sense while the minimal display is (saved as) enabled
            moveEffects.active = AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig().minimalEffects.enabled;
        });
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
}
