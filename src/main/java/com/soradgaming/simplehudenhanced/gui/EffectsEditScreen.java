package com.soradgaming.simplehudenhanced.gui;

import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.hud.MinimalEffectsRenderer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Edit mode for the minimal effects display. The HUD stays fully visible (including other mods' elements such as
 * a minimap) while the arrow keys move the effects display one pixel at a time.
 *
 * This extends ChatScreen on purpose: Xaero's Minimap hides itself under every screen except chat, its own screens
 * and the death screen, and we need the minimap on screen to position things relative to it. Everything that would
 * touch the chat input is overridden, so no chat behaviour is actually used.
 */
@Environment(EnvType.CLIENT)
public class EffectsEditScreen extends ChatScreen {
    private static final int FAST_STEP = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIM_TEXT_COLOR = 0xFFAAAAAA;

    private final Screen returnParent;
    private ButtonWidget doneButton;

    /**
     * @param returnParent the screen the settings screen was opened from; the settings screen is reopened
     *                     with this parent when edit mode ends
     */
    public EffectsEditScreen(Screen returnParent) {
        super("", false);
        this.returnParent = returnParent;
    }

    @Override
    protected void init() {
        // Deliberately not calling super.init(): it builds the chat input box we do not want
        this.doneButton = ButtonWidget.builder(Text.translatable("simplehudenhanced.edit.done"), button -> finish())
                .dimensions(this.width / 2 - 50, this.height / 2 - 10, 100, 20)
                .build();
    }

    @Override
    protected void setInitialFocus() {
    }

    @Override
    public void resize(int width, int height) {
        this.init(width, height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No background, no dimming: the whole HUD should stay visible
        this.doneButton.render(context, mouseX, mouseY, delta);

        SimpleHudEnhancedConfig.MinimalEffects settings = getSettings();
        int centerX = this.width / 2;
        int y = this.height / 2 + 16;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("simplehudenhanced.edit.hint"), centerX, y, TEXT_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("simplehudenhanced.edit.hint_exit"), centerX, y + 12, DIM_TEXT_COLOR);
        String position = "X: " + settings.positionX + " px from " + (settings.anchorRight ? "right" : "left")
                + "    Y: " + settings.positionY + " px from " + (settings.anchorBottom ? "bottom" : "top");
        context.drawCenteredTextWithShadow(this.textRenderer, position, centerX, y + 28, TEXT_COLOR);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            finish();
            return true;
        }

        int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? FAST_STEP : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> nudge(-step, 0);
            case GLFW.GLFW_KEY_RIGHT -> nudge(step, 0);
            case GLFW.GLFW_KEY_UP -> nudge(0, -step);
            case GLFW.GLFW_KEY_DOWN -> nudge(0, step);
            default -> {
                return false; // let the game handle everything else (F1, F3, F11, screenshots...)
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return this.doneButton.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void removed() {
    }

    // Chat internals other code may call on a ChatScreen: nothing to do here
    @Override
    protected void addScreenNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public void insertText(String text, boolean override) {
    }

    @Override
    public void setChatFromHistory(int scrollDirection) {
    }

    @Override
    public void sendMessage(String chatText, boolean addToHistory) {
    }

    private static SimpleHudEnhancedConfig.MinimalEffects getSettings() {
        return AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig().minimalEffects;
    }

    /** Moves the element by dx/dy GUI pixels in screen directions, whichever edge it is measured from. */
    private void nudge(int dx, int dy) {
        SimpleHudEnhancedConfig.MinimalEffects settings = getSettings();
        int maxX = Math.max(0, this.client.getWindow().getScaledWidth() - MinimalEffectsRenderer.getLastWidth());
        int maxY = Math.max(0, this.client.getWindow().getScaledHeight() - MinimalEffectsRenderer.getLastHeight());

        settings.positionX = clamp(settings.positionX + (settings.anchorRight ? -dx : dx), maxX);
        settings.positionY = clamp(settings.positionY + (settings.anchorBottom ? -dy : dy), maxY);
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    /** Saves the position, re-anchors to the nearest screen edges and goes back to the settings screen. */
    private void finish() {
        SimpleHudEnhancedConfig.MinimalEffects settings = getSettings();
        int width = MinimalEffectsRenderer.getLastWidth();
        int height = MinimalEffectsRenderer.getLastHeight();
        int screenWidth = this.client.getWindow().getScaledWidth();
        int screenHeight = this.client.getWindow().getScaledHeight();

        // Anchor to whichever edge is closest so the position survives window/GUI-scale changes sensibly
        int left = settings.anchorRight ? screenWidth - settings.positionX - width : settings.positionX;
        int top = settings.anchorBottom ? screenHeight - settings.positionY - height : settings.positionY;
        settings.anchorRight = left + width / 2 > screenWidth / 2;
        settings.anchorBottom = top + height / 2 > screenHeight / 2;
        settings.positionX = clamp(settings.anchorRight ? screenWidth - left - width : left, screenWidth);
        settings.positionY = clamp(settings.anchorBottom ? screenHeight - top - height : top, screenHeight);

        AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).save();
        this.client.setScreen(AutoConfigClient.getConfigScreen(SimpleHudEnhancedConfig.class, this.returnParent).get());
    }
}
