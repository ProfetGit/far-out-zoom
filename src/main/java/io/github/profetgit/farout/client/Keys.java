package io.github.profetgit.farout.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.farout.FarOut;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources./*$ id*/ Identifier /**/;

/** C zooms; the settings key is unbound by default. Added to the options by OptionsMixin (no loader API needed). */
public final class Keys {
    //? if >=1.21.9 {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(/*$ id*/ Identifier /**/.fromNamespaceAndPath(FarOut.MOD_ID, FarOut.MOD_ID));
    //?} else {
    /*public static final String CATEGORY = "key.categories.far_out_zoom";

    // Controls sorts by this map and fails on a category it doesn't know (Fabric API's key helper normally adds it).
    static {
        var order = io.github.profetgit.farout.mixin.client.KeyMappingAccess.farout$sortOrder();
        order.putIfAbsent(CATEGORY, order.values().stream().max(Integer::compare).orElse(0) + 1);
    }
    *///?}
    public static final KeyMapping ZOOM = new KeyMapping("key.far_out_zoom.zoom", InputConstants.KEY_C, CATEGORY);
    public static final KeyMapping SETTINGS = new KeyMapping("key.far_out_zoom.settings", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private Keys() {
    }
}
