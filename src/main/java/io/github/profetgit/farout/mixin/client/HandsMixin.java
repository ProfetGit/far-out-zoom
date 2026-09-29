package io.github.profetgit.farout.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.farout.client.Zoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The hand slides out of view while zoomed. Hooked in the hands renderer itself, not at vanilla's call in
 * GameRenderer: Iris draws the hand through its own HandRenderer, which calls this method directly.
 */
//? if >=26.3 {
@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
//?} else {
/*@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
*///?}
public abstract class HandsMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void farout$away(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) PoseStack pose) {
        pose.pushPose();
        float away = Zoom.handAway();
        if (away > 0) pose.translate(0, -0.95F * away, 0.2F * away);
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void farout$back(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) PoseStack pose) {
        pose.popPose();
    }
}
