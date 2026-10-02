package io.github.profetgit.farout.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** The 1.21.8 and older key map holds one binding per key; the zoom key takes it (see KeyMappingMixin). */
@Mixin(net.minecraft.client.KeyMapping.class)
public interface KeyMappingAccess {
    //? if <1.21.9 {
    @org.spongepowered.asm.mixin.gen.Accessor("CATEGORY_SORT_ORDER")
    static java.util.Map<String, Integer> farout$sortOrder() {
        throw new AssertionError();
    }
    //?}
}
