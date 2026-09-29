package io.github.profetgit.farout.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.farout.FarOut;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** C zooms; the settings key is unbound by default. Added to the options by OptionsMixin (no loader API needed). */
public final class Keys {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(FarOut.MOD_ID, FarOut.MOD_ID));
    public static final KeyMapping ZOOM = new KeyMapping("key.far_out_zoom.zoom", InputConstants.KEY_C, CATEGORY);
    public static final KeyMapping SETTINGS = new KeyMapping("key.far_out_zoom.settings", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private Keys() {
    }
}
