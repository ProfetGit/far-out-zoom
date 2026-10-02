package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom narrows the world's field of view (the hand keeps its own). */
//? if >=26.2 {
@Mixin(Camera.class)
//?} else {
/*@Mixin(net.minecraft.client.renderer.GameRenderer.class)
*///?}
public abstract class CameraMixin {
    //? if >=1.21.2 {
    @Shadow
    private float fovModifier;
    @Shadow
    private float oldFovModifier;
    //?} else {
    /*@Shadow
    private float fov;
    @Shadow
    private float oldFov;
    *///?}

    //? if >=26.2 {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void farout$zoom(float partial, CallbackInfoReturnable<Float> cir) {
        if (!Zoom.zoomed()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean scoping = mc.player != null && mc.player.isScoping();
        cir.setReturnValue(Zoom.fov(cir.getReturnValueF(), Mth.lerp(partial, oldFovModifier, fovModifier), scoping));
    }
    //?} else if >=1.21.2 {
    /*@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void farout$zoom(Camera camera, float partial, boolean world, CallbackInfoReturnable<Float> cir) {
        if (!world || !Zoom.zoomed()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean scoping = mc.player != null && mc.player.isScoping();
        cir.setReturnValue(Zoom.fov(cir.getReturnValueF(), Mth.lerp(partial, oldFovModifier, fovModifier), scoping));
    }
    *///?} else {
    /*@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void farout$zoom(Camera camera, float partial, boolean world, CallbackInfoReturnable<Double> cir) {
        if (!world || !Zoom.zoomed()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean scoping = mc.player != null && mc.player.isScoping();
        cir.setReturnValue((double) Zoom.fov((float) cir.getReturnValueD(), Mth.lerp(partial, oldFov, fov), scoping));
    }
    *///?}
}
