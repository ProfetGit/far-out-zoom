package io.github.profetgit.farout.mixin.client;

import io.github.profetgit.farout.client.Keys;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the keys to the options before they load, so their bindings are saved and show in Controls (every loader). */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void farout$keys(CallbackInfo ci) {
        if (Arrays.asList(keyMappings).contains(Keys.ZOOM)) return;
        List<KeyMapping> all = new java.util.ArrayList<>(Arrays.asList(keyMappings));
        all.add(Keys.ZOOM);
        all.add(Keys.SETTINGS);
        keyMappings = all.toArray(KeyMapping[]::new);
    }
}
