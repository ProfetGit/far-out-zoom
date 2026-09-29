package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Zoom;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's Save Hotbar Activator is on C too. Held with a number key in creative it overwrites a saved hotbar, which
 * zooming and then switching slots would do by accident, so it is paused while it shares the zoom key (a one-time
 * toast says so; rebinding either key brings it back).
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void farout$shared(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && (Object) this == mc.options.keySaveHotbarActivator && Zoom.sharesHotbarKey(mc)) cir.setReturnValue(false);
    }
}
