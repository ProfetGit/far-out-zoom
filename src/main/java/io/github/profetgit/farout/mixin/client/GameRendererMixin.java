package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** View bobbing fades out while zoomed (magnified, it shakes the whole view). The hand is in HandsMixin. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyVariable(method = "bobView", at = @At("STORE"), ordinal = 1)
    private float farout$bob(float bob) {
        return bob * Zoom.bobScale();
    }
}
