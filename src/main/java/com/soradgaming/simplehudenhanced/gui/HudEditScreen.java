package com.soradgaming.simplehudenhanced.gui;

import com.soradgaming.simplehudenhanced.config.ConfigScreenApplyButton;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.hud.HudElement;
import com.soradgaming.simplehudenhanced.hud.HudLayout;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor. The whole HUD stays visible (including other mods' elements such as a minimap) while you select this
 * mod's elements with the mouse and move them by dragging or with the arrow keys.
 *
 * This extends ChatScreen on purpose: Xaero's Minimap hides itself under every screen except chat, its own screens
 * and the death screen, and we need the minimap on screen to position things relative to it. Everything that would
 * touch the chat input is overridden, so no chat behaviour is actually used.
 */
@Environment(EnvType.CLIENT)
public class HudEditScreen extends ChatScreen {
    private static final int FAST_STEP = 10;
    private static final int HIT_PADDING = 3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIM_TEXT_COLOR = 0xFFAAAAAA;
    private static final int OUTLINE_IDLE = 0x88FFFFFF;
    private static final int OUTLINE_HOVER = 0xFF88FF88;
    private static final int OUTLINE_SELECTED = 0xFFFFFF55;

    private final Screen returnParent;
    private final int returnTab;
    private final double returnScroll;
    private ButtonWidget doneButton;

    private HudElement selected;
    private HudElement dragging;
    private double grabOffsetX;
    private double grabOffsetY;

    /**
     * @param returnParent the screen the settings screen was opened from; the settings screen is reopened with
     *                     this parent (on the same tab and scroll position) when the editor is closed
     */
    public HudEditScreen(Screen returnParent, int returnTab, double returnScroll) {
        super("", false);
        this.returnParent = returnParent;
        this.returnTab = returnTab;
        this.returnScroll = returnScroll;
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

    // ---- Drawing ----

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No background, no dimming: the whole HUD should stay visible
        HudElement hovered = pick(mouseX, mouseY);
        for (HudElement element : HudElement.values()) {
            if (!HudLayout.isVisible(element)) continue;
            HudLayout.Rect rect = HudLayout.bounds(element);
            int color = element == selected ? OUTLINE_SELECTED : (element == hovered ? OUTLINE_HOVER : OUTLINE_IDLE);
            context.drawStrokedRectangle(rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, color);
        }
        if (selected != null && HudLayout.isVisible(selected)) {
            drawLabel(context, selected, HudLayout.bounds(selected));
        } else if (hovered != null) {
            drawLabel(context, hovered, HudLayout.bounds(hovered));
        }

        this.doneButton.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int y = this.height / 2 + 16;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("simplehudenhanced.edit.hint_mouse"), centerX, y, TEXT_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("simplehudenhanced.edit.hint_keys"), centerX, y + 12, TEXT_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("simplehudenhanced.edit.hint_exit"), centerX, y + 24, DIM_TEXT_COLOR);
        String status = selected == null ? Text.translatable("simplehudenhanced.edit.none_selected").getString()
                : selected.label() + "  (" + describePosition(selected) + ")";
        context.drawCenteredTextWithShadow(this.textRenderer, status, centerX, y + 40, OUTLINE_SELECTED);
    }

    private void drawLabel(DrawContext context, HudElement element, HudLayout.Rect rect) {
        TextRenderer font = this.textRenderer;
        int y = rect.y() >= 12 ? rect.y() - 11 : rect.y() + rect.height() + 3;
        context.drawTextWithShadow(font, element.label(), rect.x(), y, OUTLINE_SELECTED);
    }

    private String describePosition(HudElement element) {
        HudLayout.Rect rect = HudLayout.bounds(element);
        return rect == null ? "" : "x " + rect.x() + ", y " + rect.y();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    // ---- Selecting and moving ----

    /** The visible element under the mouse; when elements overlap, the smallest one wins. */
    private HudElement pick(double mouseX, double mouseY) {
        HudElement best = null;
        for (HudElement element : HudElement.values()) {
            if (!HudLayout.isVisible(element)) continue;
            HudLayout.Rect rect = HudLayout.bounds(element);
            if (rect.contains(mouseX, mouseY, HIT_PADDING) && (best == null || rect.area() < HudLayout.bounds(best).area())) {
                best = element;
            }
        }
        return best;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.doneButton.mouseClicked(click, doubled)) return true;
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        HudElement hit = pick(click.x(), click.y());
        selected = hit;
        dragging = hit;
        if (hit != null) {
            HudLayout.Rect rect = HudLayout.bounds(hit);
            grabOffsetX = click.x() - rect.x();
            grabOffsetY = click.y() - rect.y();
        }
        return hit != null;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging == null || !HudLayout.isVisible(dragging)) return false;

        // Keep the element under the same spot of the mouse it was grabbed at; works with the screen-edge limits
        HudLayout.Rect rect = HudLayout.bounds(dragging);
        int dx = (int) Math.round(click.x() - grabOffsetX - rect.x());
        int dy = (int) Math.round(click.y() - grabOffsetY - rect.y());
        if (dx != 0 || dy != 0) {
            HudLayout.move(dragging, dx, dy, getConfig());
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = dragging != null;
        dragging = null;
        return wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        boolean shift = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            finish();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            selectNext(shift ? -1 : 1);
            return true;
        }

        int step = shift ? FAST_STEP : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> moveSelected(-step, 0);
            case GLFW.GLFW_KEY_RIGHT -> moveSelected(step, 0);
            case GLFW.GLFW_KEY_UP -> moveSelected(0, -step);
            case GLFW.GLFW_KEY_DOWN -> moveSelected(0, step);
            case GLFW.GLFW_KEY_R -> {
                if (selected != null) HudLayout.reset(selected, getConfig());
            }
            default -> {
                return false; // let the game handle everything else (F1, F3, F11, screenshots...)
            }
        }
        return true;
    }

    private void moveSelected(int dx, int dy) {
        if (selected != null && HudLayout.isVisible(selected)) {
            HudLayout.move(selected, dx, dy, getConfig());
        }
    }

    /** Tab / Shift+Tab: select the next / previous visible element. */
    private void selectNext(int direction) {
        List<HudElement> visible = new ArrayList<>();
        for (HudElement element : HudElement.values()) {
            if (HudLayout.isVisible(element)) visible.add(element);
        }
        if (visible.isEmpty()) return;

        int index = selected == null ? -1 : visible.indexOf(selected);
        int next = index == -1 ? (direction > 0 ? 0 : visible.size() - 1)
                : Math.floorMod(index + direction, visible.size());
        selected = visible.get(next);
    }

    // ---- Closing ----

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

    private static SimpleHudEnhancedConfig getConfig() {
        return AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig();
    }

    /** Saves everything, re-anchors the effects display to the nearest screen edges and reopens the settings screen. */
    private void finish() {
        SimpleHudEnhancedConfig.MinimalEffects effects = getConfig().minimalEffects;
        HudLayout.Rect rect = HudLayout.bounds(HudElement.EFFECTS);
        if (rect != null && HudLayout.isVisible(HudElement.EFFECTS)) {
            int screenWidth = this.client.getWindow().getScaledWidth();
            int screenHeight = this.client.getWindow().getScaledHeight();
            // Anchor to whichever edge is closest so the position survives window/GUI-scale changes sensibly
            effects.anchorRight = rect.x() + rect.width() / 2 > screenWidth / 2;
            effects.anchorBottom = rect.y() + rect.height() / 2 > screenHeight / 2;
            effects.positionX = Math.max(0, effects.anchorRight ? screenWidth - rect.x() - rect.width() : rect.x());
            effects.positionY = Math.max(0, effects.anchorBottom ? screenHeight - rect.y() - rect.height() : rect.y());
        }

        AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).save();
        ConfigScreenApplyButton.reopen(this.returnParent, this.returnTab, this.returnScroll);
    }
}
