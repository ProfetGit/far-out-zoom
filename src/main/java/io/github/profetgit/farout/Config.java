package io.github.profetgit.farout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings in config/far_out_zoom.json in the game directory. */
public final class Config {
    public enum Mode {
        /** Zoomed while the key is held. */
        HOLD,
        /** A press zooms in, the next press zooms out. */
        TOGGLE,
        /** Hold for a quick look; a short tap keeps the zoom until the next press. */
        HYBRID
    }

    public enum Overlay {
        NONE,
        /** Soft dark edges. */
        VIGNETTE,
        /** The spyglass's round scope. */
        SPYGLASS
    }

    public Mode mode = Mode.HOLD;
    /** Magnification a zoom starts at. */
    public double defaultZoom = 4;
    /** The most the scroll wheel can zoom in. */
    public double maxZoom = 64;
    /** The scroll wheel changes the magnification while zoomed. */
    public boolean scrollZoom = true;
    /** The scrolled magnification is kept for the next zoom (until the game closes); off, every zoom starts at defaultZoom. */
    public boolean rememberZoom = true;
    /** Zoom animation speed, 50 to 200 %. */
    public int zoomSpeed = 100;
    /** The zoom overshoots a little and settles; off, it glides in without overshoot. */
    public boolean springy = true;
    /**
     * Mouse sensitivity while zoomed, in % of what keeps the aim steady: 100 turns the view by the same amount on
     * screen at every magnification (the turn is divided by the magnification), 0 leaves vanilla sensitivity.
     */
    public int relativeSensitivity = 100;
    /** Vanilla's cinematic camera smoothing while zoomed. */
    public boolean smoothCamera = false;
    /** Sprinting, Speed, a drawn bow and the like don't change the zoomed view. */
    public boolean ignoreFovEffects = true;
    /** The hand and held item slide out of view. */
    public boolean hideHand = true;
    /** View bobbing fades out (a magnified bob shakes the whole view). */
    public boolean steadyView = true;
    /** Mobs, players and block entities (signs, chests, banners) stay visible as far away as the zoom brings them. */
    public boolean farEntities = true;
    /** Distance haze (and rain fog) thins while zoomed, so far terrain isn't washed out. */
    public boolean clearHaze = true;
    /** Distance and name of what the crosshair points at, far beyond reach. */
    public boolean rangefinder = true;
    /** The magnification (e.g. 4.0x) under the crosshair. */
    public boolean showMagnification = true;
    public Overlay overlay = Overlay.NONE;
    public boolean sounds = true;
    /** Zoom sounds' volume, 0 to 100 %. */
    public int soundVolume = 60;
    /** The one-time notice about sharing the C key with vanilla's Save Hotbar Activator was shown. */
    public boolean keyNoticeShown = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config current;
    private static Path file;

    public static synchronized Config get() {
        if (current == null) load(Path.of("."));
        return current;
    }

    public static synchronized void load(Path gameDir) {
        Path f = gameDir.resolve("config").resolve("far_out_zoom.json").toAbsolutePath().normalize();
        if (current != null && f.equals(file)) return;
        file = f;
        Config c = null;
        try {
            if (Files.isRegularFile(f)) c = GSON.fromJson(Files.readString(f), Config.class);
        } catch (Exception e) {
            FarOut.LOG.warn("Far Out Zoom: cannot read {}, using defaults ({})", f, e.toString());
        }
        current = c == null ? new Config() : c;
        current.clamp();
        save();
    }

    void clamp() {
        if (mode == null) mode = Mode.HOLD;
        if (overlay == null) overlay = Overlay.NONE;
        maxZoom = Math.max(2, Math.min(100, maxZoom));
        defaultZoom = Math.max(1.25, Math.min(maxZoom, defaultZoom));
        zoomSpeed = Math.max(50, Math.min(200, zoomSpeed));
        relativeSensitivity = Math.max(0, Math.min(150, relativeSensitivity));
        soundVolume = Math.max(0, Math.min(100, soundVolume));
    }

    public static synchronized void save() {
        if (current == null || file == null) return;
        current.clamp();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(current));
        } catch (Exception e) {
            FarOut.LOG.warn("Far Out Zoom: cannot write {} ({})", file, e.toString());
        }
    }
}
