package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.EquipmentAlignment;
import com.soradgaming.simplehudenhanced.config.EquipmentOrientation;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.gui.HudEditScreen;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;

/**
 * Draws the player's active status effects as just the effect icon plus a timer (no boxes),
 * in a vertical or horizontal layout at a configurable pixel position.
 * Effects above level I get a Roman numeral in the bottom right corner of the icon.
 */
public class MinimalEffectsRenderer {
    private static final int ICON_SIZE = 18;
    private static final int TEXT_GAP = 2;      // space between icon and timer
    private static final int ENTRY_GAP = 2;     // space between two effects
    private static final int WHITE = 0xFFFFFFFF;
    private static final int WARNING_RED = 0xFFFF5555;
    private static final int OUTLINE = 0xAAFFFFFF;
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    // Size of the element as it was last drawn, in GUI pixels (after scaling). Used by the edit screen.
    private static int lastWidth = ICON_SIZE;
    private static int lastHeight = ICON_SIZE;

    public static int getLastWidth() {
        return lastWidth;
    }

    public static int getLastHeight() {
        return lastHeight;
    }

    // level is the Roman numeral drawn on the icon, empty for level I or when the option is off
    private record Entry(Identifier icon, String text, int color, String level) {}

    public static void render(GuiGraphicsExtractor context, SimpleHudEnhancedConfig config) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        SimpleHudEnhancedConfig.MinimalEffects settings = config.minimalEffects;
        boolean editing = client.gui.screen() instanceof HudEditScreen;
        float tickRate = client.level != null ? client.level.tickRateManager().tickrate() : 20f;

        List<Entry> entries = getEntries(player, settings, tickRate);
        // While positioning the element, show a sample if the player has no effects so there is something to move
        if (entries.isEmpty() && editing) entries = getSampleEntries(settings);
        if (entries.isEmpty()) return;

        Font font = client.font;
        boolean horizontal = settings.orientation == EquipmentOrientation.Horizontal;
        // Timer goes on the left of the icon when asked to, or (Auto) when the element is anchored to the right edge
        boolean timerOnLeft = settings.alignment == EquipmentAlignment.Left
                || (settings.alignment == EquipmentAlignment.Auto && settings.anchorRight);

        int[] textWidths = new int[entries.size()];
        int widestText = 0;
        for (int i = 0; i < entries.size(); i++) {
            String text = entries.get(i).text();
            textWidths[i] = text.isEmpty() ? 0 : font.width(text);
            widestText = Math.max(widestText, textWidths[i]);
        }

        // Size of the whole element (unscaled pixels)
        int totalWidth;
        int totalHeight;
        if (horizontal) {
            totalWidth = 0;
            for (int width : textWidths) {
                totalWidth += ICON_SIZE + (width > 0 ? TEXT_GAP + width : 0);
            }
            totalWidth += ENTRY_GAP * (entries.size() - 1);
            totalHeight = ICON_SIZE;
        } else {
            totalWidth = ICON_SIZE + (widestText > 0 ? TEXT_GAP + widestText : 0);
            totalHeight = entries.size() * ICON_SIZE + (entries.size() - 1) * ENTRY_GAP;
        }

        // Position on screen: pixel offset from the chosen edges, kept fully on screen
        float scale = settings.scale / 100f;
        int scaledWidth = Math.round(totalWidth * scale);
        int scaledHeight = Math.round(totalHeight * scale);
        lastWidth = scaledWidth;
        lastHeight = scaledHeight;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int originX = settings.anchorRight ? screenWidth - settings.positionX - scaledWidth : settings.positionX;
        int originY = settings.anchorBottom ? screenHeight - settings.positionY - scaledHeight : settings.positionY;
        originX = Math.max(0, Math.min(originX, screenWidth - scaledWidth));
        originY = Math.max(0, Math.min(originY, screenHeight - scaledHeight));
        HudLayout.record(HudElement.EFFECTS, new HudLayout.Rect(originX, originY, scaledWidth, scaledHeight));

        context.pose().pushMatrix();
        context.pose().translate((float) originX, (float) originY);
        context.pose().scale(scale, scale);

        int cursor = 0; // running offset along the layout direction
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int entryX = horizontal ? cursor : 0;
            int entryY = horizontal ? 0 : cursor;

            // In a column, icons line up: text is right-aligned against the icon when on the left,
            // left-aligned next to it when on the right. In a row each entry is sized to its own text.
            int reserved = horizontal ? textWidths[i] : widestText;
            int iconX = entryX + ((timerOnLeft && reserved > 0) ? reserved + TEXT_GAP : 0);
            int textX = timerOnLeft ? iconX - TEXT_GAP - textWidths[i] : iconX + ICON_SIZE + TEXT_GAP;

            context.blitSprite(RenderPipelines.GUI_TEXTURED, entry.icon(), iconX, entryY, ICON_SIZE, ICON_SIZE);

            // Level numeral in the bottom right corner of the icon, placed like an item's stack count.
            // Capitals are 7px tall plus 1px shadow, so y + 10 ends on the icon's last row.
            if (!entry.level().isEmpty()) {
                int levelX = iconX + ICON_SIZE - font.width(entry.level());
                context.text(font, entry.level(), levelX, entryY + ICON_SIZE - 8, WHITE);
            }

            if (!entry.text().isEmpty()) {
                int textY = entryY + (ICON_SIZE - font.lineHeight) / 2 + 1;
                context.text(font, entry.text(), textX, textY, entry.color());
            }

            cursor += horizontal
                    ? ICON_SIZE + (textWidths[i] > 0 ? TEXT_GAP + textWidths[i] : 0) + ENTRY_GAP
                    : ICON_SIZE + ENTRY_GAP;
        }

        // Outline so you can see exactly what you are moving
        if (editing) {
            context.outline(-1, -1, totalWidth + 2, totalHeight + 2, OUTLINE);
        }

        context.pose().popMatrix();
    }

    private static List<Entry> getEntries(LocalPlayer player, SimpleHudEnhancedConfig.MinimalEffects settings, float tickRate) {
        // Beneficial effects first, then the rest; the sort is stable so vanilla order is kept within each group
        return player.getActiveEffects().stream()
                .filter(MobEffectInstance::showIcon)
                .sorted(Comparator.comparing((MobEffectInstance effect) -> !effect.getEffect().value().isBeneficial()))
                .map(effect -> new Entry(
                        Hud.getMobEffectSprite(effect.getEffect()),
                        settings.showTimer ? MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString() : "",
                        getTimerColor(effect, settings, tickRate),
                        settings.showLevel ? getLevelText(effect.getAmplifier() + 1) : ""))
                .toList();
    }

    private static List<Entry> getSampleEntries(SimpleHudEnhancedConfig.MinimalEffects settings) {
        return List.of(
                new Entry(Hud.getMobEffectSprite(MobEffects.SPEED), settings.showTimer ? "8:00" : "", WHITE,
                        settings.showLevel ? getLevelText(2) : ""),
                new Entry(Hud.getMobEffectSprite(MobEffects.STRENGTH), settings.showTimer ? "1:10" : "", WHITE, ""),
                new Entry(Hud.getMobEffectSprite(MobEffects.FIRE_RESISTANCE), settings.showTimer ? "0:05" : "",
                        settings.lowTimeWarningSeconds >= 5 ? WARNING_RED : WHITE, ""));
    }

    // Roman numeral for levels II and up; level I gets nothing, like vanilla ("Speed" vs "Speed II").
    // Absurd levels from commands fall back to digits.
    private static String getLevelText(int level) {
        if (level < 2) return "";
        if (level > 3999) return Integer.toString(level);
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (level >= ROMAN_VALUES[i]) {
                roman.append(ROMAN_SYMBOLS[i]);
                level -= ROMAN_VALUES[i];
            }
        }
        return roman.toString();
    }

    private static int getTimerColor(MobEffectInstance effect, SimpleHudEnhancedConfig.MinimalEffects settings, float tickRate) {
        if (effect.isInfiniteDuration() || settings.lowTimeWarningSeconds <= 0) return WHITE;
        float secondsLeft = effect.getDuration() / tickRate;
        return secondsLeft <= settings.lowTimeWarningSeconds ? WARNING_RED : WHITE;
    }
}
