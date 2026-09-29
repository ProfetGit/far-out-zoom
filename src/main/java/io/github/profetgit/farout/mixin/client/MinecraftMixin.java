package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.Config;
import io.github.profetgit.farout.client.Rangefinder;
import io.github.profetgit.farout.client.Zoom;
import io.github.profetgit.farout.demo.Director;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void farout$boot(CallbackInfo ci) {
        Config.load(((Minecraft) (Object) this).gameDirectory.toPath());
    }

    /** The zoom moves on real time, once per frame, before the camera is set up. */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void farout$frame(boolean advanceGameTime, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Zoom.frame(mc);
        Rangefinder.update(mc);
    }

    /** Drives the dev demo (ModTest); inert unless the game runs with -Dfar_out_zoom.demo. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void farout$demoTick(CallbackInfo ci) {
        if (Director.ACTIVE) Director.onTick((Minecraft) (Object) this);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void farout$demoFrame(boolean advanceGameTime, CallbackInfo ci) {
        if (Director.ACTIVE) Director.onFrame((Minecraft) (Object) this);
    }
}
