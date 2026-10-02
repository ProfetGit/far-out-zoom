package io.github.profetgit.farout.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Signs, chests, banners and other block entities stay drawn as far as the zoom brings them (vanilla: 64 blocks).
 * NeoForge moves the check into an overload that also takes a Frustum, hence every overload.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @WrapOperation(method = /*$ berender*/ "tryExtractRenderState*" /**/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean farout$far(BlockEntityRenderer<?/*$ bergen*/, ?/**/> renderer, BlockEntity blockEntity, Vec3 camera, Operation<Boolean> original) {
        if (original.call(renderer, blockEntity, camera)) return true;
        double s = Zoom.distanceScale();
        return s > 1 && Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(camera, renderer.getViewDistance() * s);
    }
}
