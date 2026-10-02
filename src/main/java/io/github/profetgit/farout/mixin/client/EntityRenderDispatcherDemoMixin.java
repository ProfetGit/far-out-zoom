package io.github.profetgit.farout.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** Test-only for the 1.21.8 and older targets: notes the entities that get drawn. */
@Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherDemoMixin {
    //? if >=1.21.2 <1.21.9 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void farout$drawn(net.minecraft.world.entity.Entity entity, double x, double y, double z, float partial, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffer, int light, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.farout.demo.DemoDrawn.entity(entity.getX(), entity.getY(), entity.getZ());
    }
    *///?}
    //? if <1.21.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void farout$drawn(net.minecraft.world.entity.Entity entity, double x, double y, double z, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffer, int light, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.farout.demo.DemoDrawn.entity(entity.getX(), entity.getY(), entity.getZ());
    }
    *///?}
}
