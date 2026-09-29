package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Lang;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** English names for the mod's keys and key category when no lang file was loaded (Fabric without Fabric API). */
@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private void farout$fallback(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (cir.getReturnValue() != null && !cir.getReturnValue().equals(fallback)) return;
        String en = Lang.fallback(key);
        if (en != null) cir.setReturnValue(en);
    }

    @Inject(method = "has", at = @At("RETURN"), cancellable = true)
    private void farout$has(String key, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && Lang.fallback(key) != null) cir.setReturnValue(true);
    }
}
