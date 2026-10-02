package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.ZoomHud;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui./*$ gfx*/ GuiGraphicsExtractor /**/;
import net.minecraft.client.gui./*$ gui*/ Hud /**/;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(/*$ gui*/ Hud /**/.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /** With the crosshair, so it hides with the HUD. NeoForge draws the HUD as layers, so this hooks the crosshair itself. */
    //? if >=1.21 {
    @Inject(method = /*$ crosshair*/ "extractCrosshair" /**/, at = @At("RETURN"))
    private void farout$text(/*$ gfx*/ GuiGraphicsExtractor /**/ g, DeltaTracker delta, CallbackInfo ci) {
        ZoomHud.text(g, minecraft);
    }

    @Inject(method = /*$ cameraOverlays*/ "extractCameraOverlays" /**/, at = @At("TAIL"))
    private void farout$overlay(/*$ gfx*/ GuiGraphicsExtractor /**/ g, DeltaTracker delta, CallbackInfo ci) {
        ZoomHud.overlay(g, minecraft, (graphics, scale) -> ((HudInvoker) (Object) this).farout$spyglass(graphics, scale));
    }
    //?} else {
    /*@Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void farout$text(GuiGraphicsExtractor g, CallbackInfo ci) {
        ZoomHud.text(g, minecraft);
    }

    // 1.20 has no camera overlay method: this is where render() has finished the vignette, scope, pumpkin, frost and portal overlays
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;", ordinal = 0))
    private void farout$overlay(GuiGraphicsExtractor g, float partial, CallbackInfo ci) {
        ZoomHud.overlay(g, minecraft, (graphics, scale) -> ((HudInvoker) (Object) this).farout$spyglass(graphics, scale));
    }
    *///?}
}
