package io.github.profetgit.farout.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** Test-only for the 1.21.9 to 1.21.11 targets: notes the entities and block entities each frame draws. */
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class LevelRendererDemoMixin {
    //? if >=1.21.9 <26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "submitEntities", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void farout$entities(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.state.LevelRenderState state, net.minecraft.client.renderer.SubmitNodeCollector collector, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        for (var s : state.entityRenderStates) io.github.profetgit.farout.demo.DemoDrawn.entity(s.x, s.y, s.z);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "submitBlockEntities", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void farout$blockEntities(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.state.LevelRenderState state, net.minecraft.client.renderer.SubmitNodeStorage storage, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        for (var s : state.blockEntityRenderStates) io.github.profetgit.farout.demo.DemoDrawn.blockEntity(s.blockPos);
    }
    *///?}
}
