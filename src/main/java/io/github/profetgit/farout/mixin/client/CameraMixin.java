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
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float fovModifier;
    @Shadow
    private float oldFovModifier;

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void farout$zoom(float partial, CallbackInfoReturnable<Float> cir) {
        if (!Zoom.zoomed()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean scoping = mc.player != null && mc.player.isScoping();
        cir.setReturnValue(Zoom.fov(cir.getReturnValueF(), Mth.lerp(partial, oldFovModifier, fovModifier), scoping));
    }
}
