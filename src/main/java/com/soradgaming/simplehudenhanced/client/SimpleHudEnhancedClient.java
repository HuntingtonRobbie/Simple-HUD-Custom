package com.soradgaming.simplehudenhanced.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.hud.HUD;
import com.soradgaming.simplehudenhanced.utli.Utilities;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class SimpleHudEnhancedClient implements ClientModInitializer {
    private ConfigHolder<SimpleHudEnhancedConfig> configHolder;
    private HUD hud;

    @Override
    public void onInitializeClient() {
        // Register the config holder for SimpleHudEnhancedConfig
        this.configHolder = AutoConfig.register(SimpleHudEnhancedConfig.class, Toml4jConfigSerializer::new);
        // Register the keybindings
        this.registerKeybindings();

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

    void registerKeybindings() {
        KeyMapping toggleHudKeybinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simplehudenhanced.toggle_hud",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_GRAVE,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simplehudenhanced","hud"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            SimpleHudEnhancedConfig config = this.configHolder.getConfig();

            if (toggleHudKeybinding.consumeClick()) {
                String chatMessage = "key.simplehudenhanced.toggle_hud.chat_message.on";
                if (config.uiConfig.toggleSimpleHUDEnhanced) {
                    chatMessage = "key.simplehudenhanced.toggle_hud.chat_message.off";
                }

                client.player.sendOverlayMessage(Utilities.translatable(chatMessage));
                config.uiConfig.toggleSimpleHUDEnhanced = !config.uiConfig.toggleSimpleHUDEnhanced;
                AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).save();
            }
        });
    }
}
