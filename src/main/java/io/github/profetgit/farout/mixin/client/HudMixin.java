package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.ZoomHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /** With the crosshair, so it hides with the HUD. NeoForge draws the HUD as layers, so this hooks the crosshair itself. */
    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void farout$text(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        ZoomHud.text(g, minecraft);
    }

    @Inject(method = "extractCameraOverlays", at = @At("TAIL"))
    private void farout$overlay(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        ZoomHud.overlay(g, minecraft, (graphics, scale) -> ((HudInvoker) (Object) this).farout$spyglass(graphics, scale));
    }
}
