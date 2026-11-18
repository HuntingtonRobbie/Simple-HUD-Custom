package com.soradgaming.simplehudenhanced.client;

import com.soradgaming.simplehudenhanced.config.ConfigScreenApplyButton;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.hud.HUD;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class SimpleHudEnhancedClient implements ClientModInitializer {
    private ConfigHolder<SimpleHudEnhancedConfig> configHolder;
    private HUD hud;

    @Override
    public void onInitializeClient() {
        // Register the config holder for SimpleHudEnhancedConfig
        this.configHolder = AutoConfig.register(SimpleHudEnhancedConfig.class, Toml4jConfigSerializer::new);
        // Add an Apply button to the settings screen
        ConfigScreenApplyButton.register();

        // Register a HUD element to render the custom HUD
        HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE , Identifier.fromNamespaceAndPath("simplehudenhanced", "hud"), (graphics, tickCounter) -> {
            if (this.hud == null) {
                this.hud = HUD.getInstance();
                // Render the HUD on next tick
            } else {
                this.hud.drawHud(graphics);
            }
        });
    }

    /** True only while the F3 overlay is open (never persisted, so it can't switch the HUD off in the config). */
    public static boolean isDebugOverlayOpen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.debugEntries != null && mc.debugEntries.isOverlayVisible();
    }
}
