package io.github.profetgit.farout.client;

import com.mojang.serialization.Codec;
import io.github.profetgit.farout.Config;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * Settings on one page. Labels are plain English: without Fabric API a Fabric mod's language files aren't loaded, and a
 * translation key that isn't found is shown as it is.
 */
public final class ZoomConfigScreen extends OptionsSubScreen {
    public ZoomConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.literal("Far Out Zoom"));
    }

    @Override
    protected void addOptions() {
        Config c = Config.get();
        list.addHeader(Component.literal("Zoom"));
        list.addSmall(
            choice("Key", "Hold: zoomed while held. Toggle: press in, press out. Hybrid: hold for a quick look, tap to stay zoomed.",
                Config.Mode.values(), c.mode, m -> switch (m) {
                    case HOLD -> "Hold";
                    case TOGGLE -> "Toggle";
                    case HYBRID -> "Hybrid";
                }, v -> c.mode = v),
            new OptionInstance<>("Starting zoom", tip("Magnification each zoom starts at."),
                (caption, v) -> Component.literal("Starting zoom: " + times(notchLevel(v))), new OptionInstance.IntRange(1, 12), notchOf(c.defaultZoom), v -> {
                    c.defaultZoom = notchLevel(v);
                    Zoom.level = Math.min(c.maxZoom, c.defaultZoom);
                }),
            new OptionInstance<>("Most zoom", tip("How far the scroll wheel can zoom in."),
                (caption, v) -> Component.literal("Most zoom: " + times(notchLevel(v))), new OptionInstance.IntRange(2, 13), notchOf(c.maxZoom), v -> c.maxZoom = notchLevel(v)),
            bool("Scroll to zoom", "The scroll wheel zooms in and out while zoomed (instead of changing the hotbar slot).", c.scrollZoom, v -> c.scrollZoom = v),
            bool("Remember zoom", "The next zoom starts where you last scrolled to. Off: every zoom starts at the starting zoom. Double-tap the key to go back to it.",
                c.rememberZoom, v -> c.rememberZoom = v),
            new OptionInstance<>("Zoom speed", tip("How fast the view zooms in and out."),
                (caption, v) -> Component.literal("Zoom speed: " + v * 25 + "%"), new OptionInstance.IntRange(2, 8), Math.round(c.zoomSpeed / 25F), v -> c.zoomSpeed = v * 25),
            bool("Springy zoom", "The zoom overshoots a touch and settles. Off: it glides in.", c.springy, v -> c.springy = v));
        list.addHeader(Component.literal("Aim"));
        list.addSmall(
            new OptionInstance<>("Zoomed sensitivity", tip("100%: the view turns by the same amount on screen at every zoom, so aim stays steady. 0%: vanilla sensitivity."),
                (caption, v) -> Component.literal("Zoomed sensitivity: " + v * 10 + "%"), new OptionInstance.IntRange(0, 15), Math.round(c.relativeSensitivity / 10F),
                v -> c.relativeSensitivity = v * 10),
            bool("Cinematic camera", "Vanilla's smooth, drifting camera while zoomed.", c.smoothCamera, v -> c.smoothCamera = v),
            bool("Ignore FOV effects", "Sprinting, Speed and drawing a bow don't change the zoomed view.", c.ignoreFovEffects, v -> c.ignoreFovEffects = v),
            bool("Steady view", "View bobbing fades out while zoomed; magnified, it shakes the whole view.", c.steadyView, v -> c.steadyView = v),
            bool("Hide hand", "The hand and held item slide out of view.", c.hideHand, v -> c.hideHand = v));
        list.addHeader(Component.literal("Seeing far"));
        list.addSmall(
            bool("Far mobs", "Mobs, players, signs, chests and banners stay visible as far as the zoom brings them (up to what the server sends).",
                c.farEntities, v -> c.farEntities = v),
            bool("Clear haze", "Distance haze and rain fog thin out while zoomed. The edge of the render distance stays hidden.", c.clearHaze, v -> c.clearHaze = v),
            bool("Rangefinder", "Distance and name of what the crosshair points at.", c.rangefinder, v -> c.rangefinder = v),
            bool("Show zoom level", "The magnification under the crosshair.", c.showMagnification, v -> c.showMagnification = v),
            choice("Overlay", "What frames the zoomed view.", Config.Overlay.values(), c.overlay, o -> switch (o) {
                case NONE -> "None";
                case VIGNETTE -> "Vignette";
                case SPYGLASS -> "Spyglass";
            }, v -> c.overlay = v),
            bool("Sounds", "A soft whoosh in and out, and a click per scroll step.", c.sounds, v -> c.sounds = v),
            new OptionInstance<>("Sound volume", tip("Volume of the zoom sounds (also follows the Interface volume)."),
                (caption, v) -> Component.literal("Sound volume: " + v * 10 + "%"), new OptionInstance.IntRange(0, 10), Math.round(c.soundVolume / 10F),
                v -> c.soundVolume = v * 10));
    }

    static OptionInstance<Boolean> bool(String name, String tooltip, boolean value, OptionInstance.ValueUpdateListener<Boolean> set) {
        return OptionInstance.createBoolean(name, tip(tooltip), value, set);
    }

    static <T extends Enum<T>> OptionInstance<T> choice(String name, String tooltip, T[] values, T value, Function<T, String> label, OptionInstance.ValueUpdateListener<T> set) {
        Class<T> type = value.getDeclaringClass();
        Codec<T> codec = Codec.STRING.xmap(s -> Enum.valueOf(type, s), Enum::name);
        return new OptionInstance<>(name, tip(tooltip), (caption, v) -> Component.literal(name + ": " + label.apply(v)),
            new OptionInstance.Enum<>(List.of(values), codec), value, set);
    }

    static <T> OptionInstance.TooltipSupplier<T> tip(String text) {
        return OptionInstance.cachedConstantTooltip(Component.literal(text));
    }

    /** Scroll steps are powers of sqrt 2; the sliders move by the same steps. */
    static double notchLevel(int n) {
        return Math.pow(2, n / 2.0);
    }

    static int notchOf(double level) {
        return (int) Math.round(Math.log(level) / Math.log(Math.sqrt(2)));
    }

    static String times(double v) {
        return (v < 10 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%d", Math.round(v))) + "×";
    }

    @Override
    public void removed() {
        Config.save();
        super.removed();
    }
}
