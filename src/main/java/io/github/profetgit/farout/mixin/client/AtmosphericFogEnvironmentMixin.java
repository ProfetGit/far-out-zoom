package io.github.profetgit.farout.mixin.client;

//? if >=1.21.6 {
import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clear haze: the open-air distance fog (and rain fog) moves out while zoomed. Only the atmospheric environment, so
 * water, lava, powder snow, Blindness and Darkness keep their fog; the render-distance fog that hides the edge of the
 * loaded world is separate and stays. The boss fog of the dragon fight stays too.
 */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    //? if >=1.21.11 {
    private void farout$haze(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
    //?} else {
    /*private void farout$haze(FogData fog, net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos pos, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
    *///?}
        float s = Zoom.hazeScale();
        if (s <= 1 || io.github.profetgit.farout.client.Compat.boss(Minecraft.getInstance()).shouldCreateWorldFog()) return;
        fog.environmentalStart *= s;
        fog.environmentalEnd *= s;
        Zoom.hazeApplied = s;
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

// Before 1.21.6 there is no open-air fog environment (only the render-distance fog), so there is no haze to clear.
@Mixin(net.minecraft.client.renderer.FogRenderer.class)
public abstract class AtmosphericFogEnvironmentMixin {
}
*///?}
