package io.github.profetgit.farout.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** View bobbing fades out while zoomed (magnified, it shakes the whole view), and the hand slides out of view. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyVariable(method = "bobView", at = @At("STORE"), ordinal = 1)
    private float farout$bob(float bob) {
        return bob * Zoom.bobScale();
    }

    //? if >=26.3 {
    @ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"), index = 1)
    //?} else {
    /*@ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"), index = 1)
    *///?}
    private PoseStack farout$hand(PoseStack pose) {
        float away = Zoom.handAway();
        if (away > 0) pose.translate(0, -0.95F * away, 0.2F * away);
        return pose;
    }
}
