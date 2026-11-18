package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ScreenManager {
    private final int screenWidth;
    private final int screenHeight;
    private int padding;

    // The HUD element this manager positions (null = not editable in the HUD editor) and the on-screen box
    // (GUI pixels, before the editor's pixel offset) worked out by calculateXAxis/calculateYAxis
    private final HudElement element;
    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;

    public ScreenManager(int screenWidth, int screenHeight) {
        this(screenWidth, screenHeight, null);
    }

    public ScreenManager(int screenWidth, int screenHeight, HudElement element) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.padding = 0;
        this.element = element;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int calculateXAxis(int configX, double scale, int lineWidth) {
        int result = computeXAxis(configX, scale, lineWidth);
        boxX = (int) Math.round(result * scale);
        boxWidth = (int) Math.round(lineWidth * scale);
        return result;
    }

    public int calculateYAxis(int lineHeight, int size, int configY, double scale) {
        int result = computeYAxis(lineHeight, size, configY, scale);
        boxY = (int) Math.round(result * scale);
        boxHeight = (int) Math.round((lineHeight * size) * scale);
        return result;
    }

    private int computeXAxis(int configX, double scale, int lineWidth) {
        int adjustedWidth = (int) Math.round(screenWidth - (padding) - (lineWidth * scale));
        int calculatedValue = (int) Math.round(adjustedWidth / scale * configX / 100);

        if (scale < 1) {
            int adjustedScreenWidth = (int) Math.round(screenWidth * scale);
            int maximumX = padding;
            int minimumX = Math.min(screenWidth - adjustedScreenWidth - padding, maximumX);
            return Math.max(Math.max(calculatedValue, minimumX), maximumX);
        } else {
            int adjustedLineWidth = (int) Math.round(lineWidth * scale);
            return Math.min(Math.max(calculatedValue, padding), screenWidth - padding - adjustedLineWidth);
        }
    }

    private int computeYAxis(int lineHeight, int size, int configY, double scale) {
        int adjustedHeight = (int) Math.round(screenHeight - padding - (lineHeight * size) * scale);
        int calculatedValue = (int) Math.round(adjustedHeight / scale * configY / 100);

        if (scale < 1) {
            return Math.max(calculatedValue, padding);
        } else {
            int adjustedLineHeight = (int) Math.round((lineHeight * size) * scale);
            return Math.min(Math.max(calculatedValue, padding), screenHeight - adjustedLineHeight - padding);
        }
    }

    public void setScale(GuiGraphicsExtractor context, float scale) {
        // Change pose stack to draw scaled HUD content.
        context.pose().pushMatrix();
        if (element != null) {
            // Apply the HUD editor's pixel offset (exact pixels, so it is applied before the scaling)
            SimpleHudEnhancedConfig config = AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig();
            HudLayout.Rect base = new HudLayout.Rect(boxX, boxY, boxWidth, boxHeight);
            int[] offset = HudLayout.effectiveOffset(element, base, config);
            context.pose().translate((float) offset[0], (float) offset[1]);
            HudLayout.record(element, new HudLayout.Rect(boxX + offset[0], boxY + offset[1], boxWidth, boxHeight));
        }
        context.pose().scale(scale, scale);
    }

    public void resetScale(GuiGraphicsExtractor context) {
        // Restore pose stack so following draws are unscaled.
        context.pose().popMatrix();
    }

    // xAxis & yAxis Setters and Getters
    private int SxAxis;
    private int SyAxis;
    private float sScale;

    public int getXAxis() {
        return SxAxis;
    }

    public int getYAxis() {
        return SyAxis;
    }

    public float getScale() {
        return sScale;
    }

    public void saveXAxis(int xAxis) {
        this.SxAxis = xAxis;
    }

    public void saveYAxis(int yAxis) {
        this.SyAxis = yAxis;
    }

    public void saveScale(float scale) {
        this.sScale = scale;
    }
}
