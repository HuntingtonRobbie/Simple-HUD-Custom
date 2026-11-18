package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import net.minecraft.client.MinecraftClient;

import java.util.EnumMap;
import java.util.Map;

/**
 * Keeps track of where each HUD element was last drawn (in GUI pixels, on screen) and moves them for the HUD editor.
 * Elements report their box every frame; an element that stops reporting (e.g. toggled off) stops being editable.
 */
public final class HudLayout {
    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py, int padding) {
            return px >= x - padding && px <= x + width + padding && py >= y - padding && py <= y + height + padding;
        }

        public int area() {
            return width * height;
        }
    }

    private static final Map<HudElement, Rect> BOUNDS = new EnumMap<>(HudElement.class);
    private static final Map<HudElement, Integer> LAST_SEEN = new EnumMap<>(HudElement.class);
    private static int frame = 0;

    private HudLayout() {}

    /** Called once at the start of every HUD frame. */
    public static void beginFrame() {
        frame++;
    }

    public static void record(HudElement element, Rect rect) {
        if (rect.width() <= 0 || rect.height() <= 0) return;
        BOUNDS.put(element, rect);
        LAST_SEEN.put(element, frame);
    }

    public static boolean isVisible(HudElement element) {
        Integer seen = LAST_SEEN.get(element);
        return seen != null && frame - seen <= 2;
    }

    public static Rect bounds(HudElement element) {
        return BOUNDS.get(element);
    }

    /**
     * The pixel offset to actually apply to an element whose un-offset box is {@code base}: the stored offset,
     * limited so the element stays fully on screen (the window may have shrunk since the offset was saved).
     */
    public static int[] effectiveOffset(HudElement element, Rect base, SimpleHudEnhancedConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int offsetX = clamp(config.layoutOffsets.getX(element), -base.x(), screenWidth - base.width() - base.x());
        int offsetY = clamp(config.layoutOffsets.getY(element), -base.y(), screenHeight - base.height() - base.y());
        return new int[]{offsetX, offsetY};
    }

    /** Moves an element by dx/dy GUI pixels (screen directions), keeping it on screen. */
    public static void move(HudElement element, int dx, int dy, SimpleHudEnhancedConfig config) {
        Rect rect = BOUNDS.get(element);
        if (rect == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int newX = clamp(rect.x() + dx, 0, screenWidth - rect.width());
        int newY = clamp(rect.y() + dy, 0, screenHeight - rect.height());
        int actualDx = newX - rect.x();
        int actualDy = newY - rect.y();
        if (actualDx == 0 && actualDy == 0) return;

        if (element == HudElement.EFFECTS) {
            SimpleHudEnhancedConfig.MinimalEffects settings = config.minimalEffects;
            // Position is measured from an edge: moving right means a smaller distance from the right edge
            settings.positionX = Math.max(0, settings.positionX + (settings.anchorRight ? -actualDx : actualDx));
            settings.positionY = Math.max(0, settings.positionY + (settings.anchorBottom ? -actualDy : actualDy));
        } else {
            config.layoutOffsets.add(element, actualDx, actualDy);
        }
        // Keep our copy current so several moves within one frame (dragging) add up correctly
        BOUNDS.put(element, new Rect(newX, newY, rect.width(), rect.height()));
    }

    /** Puts an element back at its slider position (effects: back to the top right corner). */
    public static void reset(HudElement element, SimpleHudEnhancedConfig config) {
        if (element == HudElement.EFFECTS) {
            SimpleHudEnhancedConfig.MinimalEffects settings = config.minimalEffects;
            settings.positionX = 2;
            settings.anchorRight = true;
            settings.positionY = 2;
            settings.anchorBottom = false;
        } else {
            config.layoutOffsets.reset(element);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(value, max));
    }
}
