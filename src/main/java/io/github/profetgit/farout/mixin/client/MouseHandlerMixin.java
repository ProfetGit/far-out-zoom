package io.github.profetgit.farout.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While zoomed the wheel zooms, and the mouse turns the view less, so aim stays steady at any magnification. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void farout$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle() || minecraft.gui.screen() != null || minecraft.player == null) return;
        if (Zoom.scroll(yOffset)) ci.cancel();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double farout$yaw(double d) {
        return d * Zoom.turnScale();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double farout$pitch(double d) {
        return d * Zoom.turnScale();
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"))
    private boolean farout$smooth(boolean smooth) {
        return smooth || Zoom.smoothCamera();
    }
}
