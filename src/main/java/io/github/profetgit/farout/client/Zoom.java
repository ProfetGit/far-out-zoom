package io.github.profetgit.farout.client;

import io.github.profetgit.farout.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The zoom: key handling, the magnification spring and what the rest of the mod reads from it.
 * <p>
 * The magnification moves on a spring in log space (u = ln magnification), advanced every frame on real time, so 2x to
 * 4x feels the same as 16x to 32x and the motion doesn't depend on the frame rate. Zooming in is slightly underdamped
 * (a small overshoot that settles), zooming out is quicker and nearly critical.
 */
public final class Zoom {
    /** One scroll notch: a factor of sqrt 2 (2x, 2.8x, 4x, 5.7x, 8x, ...). */
    static final double NOTCH = Math.log(Math.sqrt(2));
    public static final double MIN = 1.25;
    static final long TAP_NANOS = 250_000_000L;
    static final long DOUBLE_TAP_NANOS = 300_000_000L;
    static final double SUBSTEP = 1 / 480.0;
    static final SystemToast.SystemToastId NOTICE = new SystemToast.SystemToastId(8_000L);

    static boolean active, sticky, suppress, wasDown;
    static long pressNanos, lastPressNanos, lastNanos;
    static double level = Double.NaN;
    /** Log magnification and its speed. */
    static double u, v;
    static int lastNotch;
    /** Last haze scale the fog used (demo checks). */
    public static float hazeApplied = 1;
    static boolean noticeChecked;

    private Zoom() {
    }

    /** Once per frame, before the camera is set up. */
    public static void frame(Minecraft mc) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        Config c = Config.get();
        if (Double.isNaN(level)) level = c.defaultZoom;
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            if (active) stop(mc);
            sticky = suppress = wasDown = false;
            while (Keys.ZOOM.consumeClick()) {
            }
        } else {
            input(mc, c, now);
            if (!noticeChecked) notice(mc, c);
        }
        while (Keys.SETTINGS.consumeClick()) {
            if (mc.gui.screen() == null) mc.gui.setScreen(new ZoomConfigScreen(null));
        }
        step(dt, c);
    }

    static void input(Minecraft mc, Config c, long now) {
        // an open screen (chat, inventory, pause) has the keyboard: the zoom key counts as up
        boolean down = Keys.ZOOM.isDown() && mc.gui.screen() == null;
        int clicks = 0;
        while (Keys.ZOOM.consumeClick()) clicks++;
        boolean pressed = down && !wasDown;
        boolean released = !down && wasDown;
        // a tap that started and ended between two frames
        boolean tap = clicks > 0 && !down && !wasDown;
        wasDown = down;
        if (pressed || tap) onPress(mc, c, now);
        if (released || tap) onRelease(mc, c, now);
        if (c.mode == Config.Mode.HOLD && active != down && !tap) {
            if (down) start(mc, c);
            else stop(mc);
        }
    }

    static void onPress(Minecraft mc, Config c, long now) {
        boolean doubleTap = now - lastPressNanos < DOUBLE_TAP_NANOS;
        lastPressNanos = now;
        pressNanos = now;
        switch (c.mode) {
            case HOLD -> {
                // a double tap goes back to the starting magnification
                if (doubleTap && Math.abs(level - c.defaultZoom) > 1e-3) {
                    level = c.defaultZoom;
                    lastNotch = notch(level);
                    tick(mc, c);
                }
            }
            case TOGGLE -> {
                if (active) stop(mc);
                else start(mc, c);
            }
            case HYBRID -> {
                if (sticky) {
                    sticky = false;
                    suppress = true;
                    stop(mc);
                } else if (!active) start(mc, c);
            }
        }
    }

    static void onRelease(Minecraft mc, Config c, long now) {
        if (c.mode != Config.Mode.HYBRID) return;
        if (suppress) {
            suppress = false;
            return;
        }
        if (!active || sticky) return;
        if (now - pressNanos < TAP_NANOS && mc.gui.screen() == null) sticky = true;
        else stop(mc);
    }

    static void start(Minecraft mc, Config c) {
        if (active) return;
        active = true;
        if (!c.rememberZoom) level = c.defaultZoom;
        level = Math.max(MIN, Math.min(c.maxZoom, level));
        lastNotch = notch(level);
        play(mc, SoundEvents.SPYGLASS_USE, 1.15F, 0.7F);
    }

    static void stop(Minecraft mc) {
        if (!active) return;
        active = false;
        sticky = false;
        play(mc, SoundEvents.SPYGLASS_STOP_USING, 1.15F, 0.7F);
    }

    static void step(double dt, Config c) {
        double target = active ? Math.log(level) : 0;
        double speed = c.zoomSpeed / 100.0;
        double w = (active ? 15 : 22) * speed;
        double zeta = c.springy ? (active ? 0.7 : 0.9) : 1.0;
        if (!c.springy) w *= 1.25;
        for (double left = dt; left > 1e-9; left -= SUBSTEP) {
            double h = Math.min(SUBSTEP, left);
            double a = w * w * (target - u) - 2 * zeta * w * v;
            v += a * h;
            u += v * h;
            if (!active && u < 0) {
                // back at 1x: never overshoot into a wider view than normal
                u = 0;
                v = 0;
            }
        }
        if (!active && u < 1e-4 && Math.abs(v) < 1e-3) u = v = 0;
    }

    /** The scroll wheel while zoomed; true when it was used for the zoom. */
    public static boolean scroll(double dy) {
        Config c = Config.get();
        if (!active || !c.scrollZoom || dy == 0 || Minecraft.getInstance().gui.screen() != null) return false;
        level = Math.max(MIN, Math.min(c.maxZoom, level * Math.exp(dy * NOTCH)));
        int n = notch(level);
        if (n != lastNotch) {
            lastNotch = n;
            tick(Minecraft.getInstance(), c);
        }
        return true;
    }

    static int notch(double lvl) {
        return (int) Math.round(Math.log(lvl) / NOTCH);
    }

    /** A soft click per scroll step, higher the further in. */
    static void tick(Minecraft mc, Config c) {
        float pitch = (float) (0.75 + 0.9 * Math.min(1, Math.log(level) / Math.log(64)));
        play(mc, SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.35F);
    }

    static void play(Minecraft mc, SoundEvent sound, float pitch, float volume) {
        Config c = Config.get();
        if (!c.sounds || c.soundVolume <= 0) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume * c.soundVolume / 100F));
    }

    /** Vanilla's Save Hotbar Activator is also on C by default: say once that zooming wins while they share a key. */
    static void notice(Minecraft mc, Config c) {
        noticeChecked = true;
        if (c.keyNoticeShown || !sharesHotbarKey(mc)) return;
        SystemToast.add(mc.gui.toastManager(), NOTICE, Component.literal("Far Out Zoom: zoom is on " + Keys.ZOOM.getTranslatedKeyMessage().getString()),
            Component.literal("Save Hotbar Activator shares the key and is paused. Rebind either in Controls."));
        c.keyNoticeShown = true;
        Config.save();
    }

    public static boolean sharesHotbarKey(Minecraft mc) {
        return mc.options != null && !Keys.ZOOM.isUnbound() && Keys.ZOOM.same(mc.options.keySaveHotbarActivator);
    }

    // ---- what the rest of the mod reads ----

    /** The key wants the zoom (the view may still be moving). */
    public static boolean active() {
        return active;
    }

    /** Current magnification, 1 when not zoomed. */
    public static double magnification() {
        return Math.exp(u);
    }

    /** Magnification the zoom is going to (what the scroll wheel set). */
    public static double level() {
        return Double.isNaN(level) ? Config.get().defaultZoom : level;
    }

    /** 0 at 1x, 1 at the scrolled magnification. */
    public static double progress() {
        if (u <= 0) return 0;
        return Math.min(1, u / Math.max(Math.log(level()), 0.05));
    }

    public static boolean zoomed() {
        return u > 1e-5;
    }

    /**
     * The zoomed field of view: the same optical magnification at any base FOV (the tangent of the half angle shrinks
     * by the magnification). With ignoreFovEffects, the FOV modifier (sprint, Speed, bows) fades out as the zoom comes in.
     */
    public static float fov(float fov, float modifier, boolean scoping) {
        if (!zoomed()) return fov;
        double base = fov;
        if (Config.get().ignoreFovEffects && !scoping && modifier > 0.05F) base = lerp(smooth(progress()), fov, fov / modifier);
        double half = Math.toRadians(base) / 2;
        return (float) Math.toDegrees(2 * Math.atan(Math.tan(half) / magnification()));
    }

    /** Mouse turn scale: 1/magnification at 100 % relative sensitivity. */
    public static double turnScale() {
        if (!zoomed()) return 1;
        return Math.pow(magnification(), -Config.get().relativeSensitivity / 100.0);
    }

    public static boolean smoothCamera() {
        return active && Config.get().smoothCamera;
    }

    /** How far the first-person hand has slid away, 0 to 1. */
    public static float handAway() {
        return Config.get().hideHand ? (float) smooth(Math.min(1, progress() * 1.6)) : 0;
    }

    /** View bobbing strength, 1 = vanilla. */
    public static float bobScale() {
        return Config.get().steadyView ? (float) (1 - smooth(progress())) : 1;
    }

    /** Entity and block entity render distances are multiplied by this. */
    public static double distanceScale() {
        return Config.get().farEntities ? Math.max(1, magnification()) : 1;
    }

    /** Distance haze starts and ends this many times further away. */
    public static float hazeScale() {
        return Config.get().clearHaze ? (float) Math.sqrt(Math.max(1, magnification())) : 1;
    }

    static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    // ---- dev: the demo drives these ----

    public static void debugSet(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (on) start(mc, Config.get());
        else stop(mc);
    }

    public static void debugReset() {
        active = sticky = suppress = wasDown = false;
        u = v = 0;
        level = Config.get().defaultZoom;
    }
}
