package io.github.profetgit.farout.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** Test-only for the 1.21.8 and older targets: notes the block entities that get drawn. */
@Mixin(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherDemoMixin {
    //? if >=1.21.5 <1.21.9 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "setupAndRender", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private static void farout$drawn(net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?> renderer, net.minecraft.world.level.block.entity.BlockEntity be, float partial, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffer, net.minecraft.world.phys.Vec3 camera, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.farout.demo.DemoDrawn.blockEntity(be.getBlockPos());
    }
    *///?}
    //? if <1.21.5 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "setupAndRender", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private static void farout$drawn(net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?> renderer, net.minecraft.world.level.block.entity.BlockEntity be, float partial, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.farout.demo.DemoDrawn.blockEntity(be.getBlockPos());
    }
    *///?}
}
