package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.EquipmentAlignment;
import com.soradgaming.simplehudenhanced.config.EquipmentOrientation;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;

import java.util.Comparator;
import java.util.List;

/**
 * Draws the player's active status effects as just the effect icon plus a timer (no boxes),
 * in a vertical or horizontal layout at a configurable position.
 */
public class MinimalEffectsRenderer {
    private static final int ICON_SIZE = 18;
    private static final int TEXT_GAP = 2;      // space between icon and timer
    private static final int ENTRY_GAP = 2;     // space between two effects
    private static final int WHITE = 0xFFFFFFFF;
    private static final int WARNING_RED = 0xFFFF5555;

    public static void render(DrawContext context, SimpleHudEnhancedConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        SimpleHudEnhancedConfig.MinimalEffects settings = config.minimalEffects;

        // Beneficial effects first, then the rest; the sort is stable so vanilla order is kept within each group
        List<StatusEffectInstance> effects = player.getStatusEffects().stream()
                .filter(StatusEffectInstance::shouldShowIcon)
                .sorted(Comparator.comparing((StatusEffectInstance effect) -> !effect.getEffectType().value().isBeneficial()))
                .toList();
        if (effects.isEmpty()) return;

        TextRenderer font = client.textRenderer;
        float tickRate = client.world != null ? client.world.getTickManager().getTickRate() : 20f;

        boolean horizontal = settings.orientation == EquipmentOrientation.Horizontal;
        boolean onRightHalf = settings.locationX >= 50;
        // Timer goes on the left of the icon when asked to, or (Auto) when the element sits on the right half of the screen
        boolean timerOnLeft = settings.alignment == EquipmentAlignment.Left
                || (settings.alignment == EquipmentAlignment.Auto && onRightHalf);

        // Timer texts and their widths
        String[] texts = new String[effects.size()];
        int[] textWidths = new int[effects.size()];
        int widestText = 0;
        for (int i = 0; i < effects.size(); i++) {
            texts[i] = settings.showTimer
                    ? StatusEffectUtil.getDurationText(effects.get(i), 1.0f, tickRate).getString()
                    : "";
            textWidths[i] = texts[i].isEmpty() ? 0 : font.getWidth(texts[i]);
            widestText = Math.max(widestText, textWidths[i]);
        }

        // Size of the whole element (in unscaled pixels)
        int columnWidth = ICON_SIZE + (widestText > 0 ? TEXT_GAP + widestText : 0);
        int totalWidth;
        int totalHeight;
        if (horizontal) {
            totalWidth = 0;
            for (int width : textWidths) {
                totalWidth += ICON_SIZE + (width > 0 ? TEXT_GAP + width : 0);
            }
            totalWidth += ENTRY_GAP * (effects.size() - 1);
            totalHeight = ICON_SIZE;
        } else {
            totalWidth = columnWidth;
            totalHeight = effects.size() * ICON_SIZE + (effects.size() - 1) * ENTRY_GAP;
        }

        // Position on screen, same system as the other HUD elements (0-100 percent of the free space)
        float scale = settings.scale / 100f;
        ScreenManager screenManager = new ScreenManager(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        screenManager.setPadding(2);
        int originX = screenManager.calculateXAxis(settings.locationX, scale, totalWidth);
        int originY = screenManager.calculateYAxis(totalHeight, 1, settings.locationY, scale);

        screenManager.setScale(context, scale);

        int cursor = 0; // running offset along the layout direction
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance effect = effects.get(i);
            int entryX = originX + (horizontal ? cursor : 0);
            int entryY = originY + (horizontal ? 0 : cursor);

            // In a column, icons line up: text is right-aligned against the icon when on the left,
            // left-aligned next to it when on the right. In a row each entry is sized to its own text.
            int reserved = horizontal ? textWidths[i] : widestText;
            int iconX = entryX + ((timerOnLeft && reserved > 0) ? reserved + TEXT_GAP : 0);
            int textX = timerOnLeft ? iconX - TEXT_GAP - textWidths[i] : iconX + ICON_SIZE + TEXT_GAP;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(effect.getEffectType()),
                    iconX, entryY, ICON_SIZE, ICON_SIZE);

            if (!texts[i].isEmpty()) {
                int textY = entryY + (ICON_SIZE - font.fontHeight) / 2 + 1;
                context.drawTextWithShadow(font, texts[i], textX, textY, getTimerColor(effect, settings, tickRate));
            }

            if (horizontal) {
                cursor += ICON_SIZE + (textWidths[i] > 0 ? TEXT_GAP + textWidths[i] : 0) + ENTRY_GAP;
            } else {
                cursor += ICON_SIZE + ENTRY_GAP;
            }
        }

        screenManager.resetScale(context);
    }

    private static int getTimerColor(StatusEffectInstance effect, SimpleHudEnhancedConfig.MinimalEffects settings, float tickRate) {
        if (effect.isInfinite() || settings.lowTimeWarningSeconds <= 0) return WHITE;
        float secondsLeft = effect.getDuration() / tickRate;
        return secondsLeft <= settings.lowTimeWarningSeconds ? WARNING_RED : WHITE;
    }
}
