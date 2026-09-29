package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Zoom;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Far mobs: vanilla stops drawing an entity past a distance based on its size (a zombie at about 67 blocks), which a
 * zoom brings right into view. The distance check sees the camera that much closer (covers subclasses' overrides too).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyArg(method = "shouldRender(DDD)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;shouldRenderAtSqrDistance(D)Z"))
    private double farout$far(double sqrDistance) {
        double s = Zoom.distanceScale();
        return s > 1 ? sqrDistance / (s * s) : sqrDistance;
    }
}
