package com.soradgaming.simplehudenhanced.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.hud.MinimalEffectsRenderer;
import com.soradgaming.simplehudenhanced.hud.StatusEffectBarRenderer;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public class GuiMixin {
    @Unique
    SimpleHudEnhancedConfig config = AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig();

    // Read the config on every frame: cancelling the settings screen reloads it into a new object
    @Unique
    private static SimpleHudEnhancedConfig currentConfig() {
        return AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig();
    }

    // Optionally hide the scoreboard sidebar that servers show on the right side of the screen
    @Inject(method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (currentConfig().uiConfig.hideScoreboardSidebar) {
            ci.cancel();
        }
    }

    // Minimal effects style: draw our own icon + timer element and skip vanilla's boxes (and the ring drawn around them)
    @Inject(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void onExtractMinimalEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SimpleHudEnhancedConfig config = currentConfig();
        if (!config.minimalEffects.enabled || !config.uiConfig.toggleSimpleHUDEnhanced
                || com.soradgaming.simplehudenhanced.client.SimpleHudEnhancedClient.isDebugOverlayOpen()) return;

        MinimalEffectsRenderer.render(graphics, config);
        ci.cancel();
    }

    @Inject(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", ordinal = 0)
    )
    private void onRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "effect") Holder<MobEffect> effect, @Local(name = "x") int x, @Local(name = "y") int y) {
        StatusEffectBarRenderer.render(graphics, new MobEffectInstance(effect), x ,y, 24, 24, config);
    }
}
