package io.github.profetgit.farout.client;

import java.util.Map;

/**
 * English fallbacks for the mod's translation keys: Fabric without Fabric API doesn't load a mod's assets, so the
 * lang file only reaches the game on NeoForge and Forge.
 */
public final class Lang {
    private static final Map<String, String> EN = Map.of(
        "key.category.far_out_zoom.far_out_zoom", "Far Out Zoom",
        "key.far_out_zoom.zoom", "Zoom",
        "key.far_out_zoom.settings", "Zoom settings");

    private Lang() {
    }

    public static String fallback(String key) {
        return key.contains("far_out_zoom") ? EN.get(key) : null;
    }
}
