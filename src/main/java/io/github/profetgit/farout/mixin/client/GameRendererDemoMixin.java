package io.github.profetgit.farout.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** Test-only invoker for the 1.21.x targets: the field of view the frame is drawn with. */
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public interface GameRendererDemoMixin {
    //? if >=1.21.2 <26.2 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("getFov")
    float farout$fov(net.minecraft.client.Camera camera, float partial, boolean world);
    *///?}
    //? if <1.21.2 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("getFov")
    double farout$fov(net.minecraft.client.Camera camera, float partial, boolean world);
    *///?}
}
