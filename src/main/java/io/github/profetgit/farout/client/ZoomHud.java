package io.github.profetgit.farout.client;

import io.github.profetgit.farout.Config;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** The zoom's HUD: the magnification and rangefinder under the crosshair, and the optional vignette or scope. */
public final class ZoomHud {
    static final Identifier VIGNETTE = Identifier.withDefaultNamespace("textures/misc/vignette.png");

    private ZoomHud() {
    }

    /** Under the crosshair (drawn right after it, so it hides with the HUD). */
    public static void text(GuiGraphicsExtractor g, Minecraft mc) {
        double p = Zoom.progress();
        if (p < 0.05) return;
        Config c = Config.get();
        int alpha = (int) (255 * Zoom.smooth((p - 0.05) / 0.6));
        if (alpha < 8) return;
        int x = g.guiWidth() / 2, y = g.guiHeight() / 2 + 11;
        if (c.showMagnification) {
            double lvl = Zoom.level();
            String s = (lvl < 10 ? String.format(Locale.ROOT, "%.1f", lvl) : String.format(Locale.ROOT, "%d", Math.round(lvl))) + "×";
            g.centeredText(mc.font, s, x, y, ARGB.color(alpha, 0xFFFFFF));
            y += 11;
        }
        if (c.rangefinder && Rangefinder.label() != null) {
            g.centeredText(mc.font, Rangefinder.label(), x, y, ARGB.color(alpha * 7 / 8, 0xD8D8D8));
        }
    }

    /** With the other camera overlays, under the HUD. */
    public static void overlay(GuiGraphicsExtractor g, Minecraft mc, Scope scope) {
        double p = Zoom.progress();
        if (p <= 0.001 || !mc.options.getCameraType().isFirstPerson()) return;
        switch (Config.get().overlay) {
            case VIGNETTE -> {
                float s = (float) (0.6 * Zoom.smooth(p));
                g.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, g.guiWidth(), g.guiHeight(), g.guiWidth(), g.guiHeight(), ARGB.colorFromFloat(1, s, s, s));
            }
            case SPYGLASS -> {
                // the scope closes in from beyond the screen edges to vanilla's size
                if (mc.player != null && mc.player.isScoping()) return;
                scope.draw(g, (float) Zoom.lerp(Zoom.smooth(p), 2.4, 1.125));
            }
            case NONE -> {
            }
        }
    }

    /** Vanilla's spyglass overlay (a Hud invoker). */
    public interface Scope {
        void draw(GuiGraphicsExtractor g, float scale);
    }
}
