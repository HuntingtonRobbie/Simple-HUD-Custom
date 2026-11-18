package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.EquipmentAlignment;
import com.soradgaming.simplehudenhanced.config.EquipmentOrientation;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.gui.HudEditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;

/**
 * Draws the player's active status effects as just the effect icon plus a timer (no boxes),
 * in a vertical or horizontal layout at a configurable pixel position.
 */
public class MinimalEffectsRenderer {
    private static final int ICON_SIZE = 18;
    private static final int TEXT_GAP = 2;      // space between icon and timer
    private static final int ENTRY_GAP = 2;     // space between two effects
    private static final int WHITE = 0xFFFFFFFF;
    private static final int WARNING_RED = 0xFFFF5555;
    private static final int OUTLINE = 0xAAFFFFFF;

    // Size of the element as it was last drawn, in GUI pixels (after scaling). Used by the edit screen.
    private static int lastWidth = ICON_SIZE;
    private static int lastHeight = ICON_SIZE;

    public static int getLastWidth() {
        return lastWidth;
    }

    public static int getLastHeight() {
        return lastHeight;
    }

    private record Entry(Identifier icon, String text, int color) {}

    public static void render(DrawContext context, SimpleHudEnhancedConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        SimpleHudEnhancedConfig.MinimalEffects settings = config.minimalEffects;
        boolean editing = client.currentScreen instanceof HudEditScreen;
        float tickRate = client.world != null ? client.world.getTickManager().getTickRate() : 20f;

        List<Entry> entries = getEntries(player, settings, tickRate);
        // While positioning the element, show a sample if the player has no effects so there is something to move
        if (entries.isEmpty() && editing) entries = getSampleEntries(settings);
        if (entries.isEmpty()) return;

        TextRenderer font = client.textRenderer;
        boolean horizontal = settings.orientation == EquipmentOrientation.Horizontal;
        // Timer goes on the left of the icon when asked to, or (Auto) when the element is anchored to the right edge
        boolean timerOnLeft = settings.alignment == EquipmentAlignment.Left
                || (settings.alignment == EquipmentAlignment.Auto && settings.anchorRight);

        int[] textWidths = new int[entries.size()];
        int widestText = 0;
        for (int i = 0; i < entries.size(); i++) {
            String text = entries.get(i).text();
            textWidths[i] = text.isEmpty() ? 0 : font.getWidth(text);
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

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int originX = settings.anchorRight ? screenWidth - settings.positionX - scaledWidth : settings.positionX;
        int originY = settings.anchorBottom ? screenHeight - settings.positionY - scaledHeight : settings.positionY;
        originX = Math.max(0, Math.min(originX, screenWidth - scaledWidth));
        originY = Math.max(0, Math.min(originY, screenHeight - scaledHeight));
        HudLayout.record(HudElement.EFFECTS, new HudLayout.Rect(originX, originY, scaledWidth, scaledHeight));

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) originX, (float) originY);
        context.getMatrices().scale(scale);

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

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, entry.icon(), iconX, entryY, ICON_SIZE, ICON_SIZE);

            if (!entry.text().isEmpty()) {
                int textY = entryY + (ICON_SIZE - font.fontHeight) / 2 + 1;
                context.drawTextWithShadow(font, entry.text(), textX, textY, entry.color());
            }

            cursor += horizontal
                    ? ICON_SIZE + (textWidths[i] > 0 ? TEXT_GAP + textWidths[i] : 0) + ENTRY_GAP
                    : ICON_SIZE + ENTRY_GAP;
        }

        // Outline so you can see exactly what you are moving
        if (editing) {
            context.drawStrokedRectangle(-1, -1, totalWidth + 2, totalHeight + 2, OUTLINE);
        }

        context.getMatrices().popMatrix();
    }

    private static List<Entry> getEntries(ClientPlayerEntity player, SimpleHudEnhancedConfig.MinimalEffects settings, float tickRate) {
        // Beneficial effects first, then the rest; the sort is stable so vanilla order is kept within each group
        return player.getStatusEffects().stream()
                .filter(StatusEffectInstance::shouldShowIcon)
                .sorted(Comparator.comparing((StatusEffectInstance effect) -> !effect.getEffectType().value().isBeneficial()))
                .map(effect -> new Entry(
                        InGameHud.getEffectTexture(effect.getEffectType()),
                        settings.showTimer ? StatusEffectUtil.getDurationText(effect, 1.0f, tickRate).getString() : "",
                        getTimerColor(effect, settings, tickRate)))
                .toList();
    }

    private static List<Entry> getSampleEntries(SimpleHudEnhancedConfig.MinimalEffects settings) {
        return List.of(
                new Entry(InGameHud.getEffectTexture(StatusEffects.SPEED), settings.showTimer ? "8:00" : "", WHITE),
                new Entry(InGameHud.getEffectTexture(StatusEffects.STRENGTH), settings.showTimer ? "1:10" : "", WHITE),
                new Entry(InGameHud.getEffectTexture(StatusEffects.FIRE_RESISTANCE), settings.showTimer ? "0:05" : "",
                        settings.lowTimeWarningSeconds >= 5 ? WARNING_RED : WHITE));
    }

    private static int getTimerColor(StatusEffectInstance effect, SimpleHudEnhancedConfig.MinimalEffects settings, float tickRate) {
        if (effect.isInfinite() || settings.lowTimeWarningSeconds <= 0) return WHITE;
        float secondsLeft = effect.getDuration() / tickRate;
        return secondsLeft <= settings.lowTimeWarningSeconds ? WARNING_RED : WHITE;
    }
}
