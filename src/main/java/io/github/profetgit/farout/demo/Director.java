package io.github.profetgit.farout.demo;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.profetgit.farout.Config;
import io.github.profetgit.farout.FarOut;
import io.github.profetgit.farout.client.Keys;
import io.github.profetgit.farout.client.Rangefinder;
import io.github.profetgit.farout.client.Zoom;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import io.github.profetgit.farout.mixin.client.MouseHandlerDemoMixin;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal./*$ cow*/ cow.Cow /**/;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dev-only scene director and check runner; does nothing unless the JVM runs with -Dfar_out_zoom.demo=<dir> (ModTest).
 * In the flat demo world (surface y -10) the real player stands at the origin facing east, down a lane with a cow at
 * 110 blocks, a sign at 100 and a gold tower at 150. Scenes press the real zoom key (KeyMapping state, the same path a
 * key press takes), scroll through the real mouse handler and turn through its real turn code. Every frame's FOV and
 * magnification go to <dir>/<scene>/timing.txt (and a PNG with frames on); the checks go to <dir>/results.json.
 */
public final class Director {
    private static final String DIR = System.getProperty("far_out_zoom.demo");
    public static final boolean ACTIVE = DIR != null;
    static final Path OUT = Path.of(ACTIVE ? DIR : ".");
    static String settingsError = "";
    static final String[] SCENES = System.getProperty("far_out_zoom.demo.scenes", "zoom,scroll").split(",");
    static final boolean FRAMES = !"false".equals(System.getProperty("far_out_zoom.demo.frames"));
    static final ExecutorService WRITER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "far_out_zoom-demo-writer");
        t.setDaemon(true);
        return t;
    });
    static final int G = -10;
    static final double EYE = G + 1 + 1.62;
    static int tick = -1, scene = -1, sceneTick, frame;
    static boolean recording, done, stopped;
    static final AtomicInteger pending = new AtomicInteger();
    static long drainUntil;
    static final List<String> timing = new ArrayList<>();
    static final List<String> results = Collections.synchronizedList(new ArrayList<>());
    static double peakMag, baseFov, yawNormal;
    static int slotBefore;

    private Director() {
    }

    public static void onTick(Minecraft mc) {
        if (done) {
            if (!stopped && (pending.get() == 0 || System.currentTimeMillis() > drainUntil)) {
                stopped = true;
                System.out.println("[cndemo] done" + (pending.get() > 0 ? ", " + pending.get() + " frames not written" : ""));
                mc.stop();
            }
            return;
        }
        // multiplayer recordings: no one-time key toast in the corner
        if (MpDirector.CASE != null) Config.get().keyNoticeShown = true;
        if (mc.level == null || mc.player == null) return;
        if (mc.getSingleplayerServer() == null) {
            MpDirector.onTick(mc);
            return;
        }
        tick++;
        if (tick == 10) setup(mc);
        if (tick < 100) return;
        if (scene < 0 || sceneTick >= length(SCENES[scene])) {
            if (scene >= 0) {
                release(mc);
                if (recording) stopRecording();
            }
            if (++scene >= SCENES.length) {
                finish();
                return;
            }
            sceneTick = -1;
            peakMag = 0;
            startScene(mc, SCENES[scene]);
            record();
            System.out.println("[cndemo] scene " + SCENES[scene]);
        }
        sceneTick++;
        tickScene(mc, SCENES[scene], sceneTick);
    }

    static void setup(Minecraft mc) {
        ModTestHook.audit();
        cmd(mc, ModTestHook.commands().toArray(String[]::new));
        String p = name(mc);
        cmd(mc, "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
            "gamerule send_command_feedback false", "time set 6000", "weather clear", "difficulty peaceful", "gamemode creative " + p,
            "forceload add 60 -16 170 16", "kill @e[type=!player]", "clear " + p,
            "fill 150 " + (G + 1) + " -1 150 " + (G + 30) + " 1 minecraft:gold_block",
            "fill 99 " + (G + 1) + " 3 101 " + (G + 3) + " 5 minecraft:stone",
            "setblock 100 " + (G + 4) + " 4 minecraft:oak_sign[rotation=4]",
            "summon minecraft:cow 110.5 " + (G + 1) + " 0.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]}",
            "tp " + p + " 0.5 " + (G + 1) + " 0.5 -90 0");
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        io.github.profetgit.farout.client.Compat.selectSlot(mc, 0);
        Config c = Config.get();
        c.mode = Config.Mode.HOLD;
        c.defaultZoom = 4;
        c.rememberZoom = true;
        Zoom.debugReset();
    }

    static int length(String s) {
        return switch (s) {
            case "zoom", "scroll", "range" -> 90;
            case "far", "fog" -> 70;
            case "sens", "hotbar", "controls" -> 40;
            case "screen", "toggle", "hybrid" -> 80;
            case "overlays" -> 150;
            case "rebind" -> 120;
            case "show" -> 200;
            case "horizon" -> 700;
            default -> 60;
        };
    }

    static void startScene(Minecraft mc, String s) {
        String p = name(mc);
        cmd(mc, "tp " + p + " 0.5 " + (G + 1) + " 0.5 -90 0", "weather " + ("fog".equals(s) ? "rain" : "clear"));
        look(mc, -90, 0);
        Config c = Config.get();
        c.mode = switch (s) {
            case "toggle" -> Config.Mode.TOGGLE;
            case "hybrid" -> Config.Mode.HYBRID;
            default -> Config.Mode.HOLD;
        };
        Zoom.debugReset();
        baseFov = mc.options.fov().get();
    }

    static void tickScene(Minecraft mc, String s, int t) {
        switch (s) {
            case "zoom" -> {
                hold(t >= 5 && t < 50);
                if (t == 45) {
                    double want = expected(baseFov, 4);
                    double fov = fov(mc);
                    check("zoomed_fov", Math.abs(fov - want) < want * 0.01, String.format(Locale.ROOT, "fov %.3f, want %.3f (4x of %.0f)", fov, want, baseFov));
                    check("overshoot", peakMag > 4.02 && peakMag < 4.6, String.format(Locale.ROOT, "peak %.3fx on the way to 4x", peakMag));
                    check("hand_away", Zoom.handAway() > 0.99 && Zoom.bobScale() < 0.01, String.format(Locale.ROOT, "hand away %.3f, bob %.3f", Zoom.handAway(), Zoom.bobScale()));
                }
                if (t == 85) {
                    double fov = fov(mc);
                    check("back_to_normal", Math.abs(fov - baseFov) < 0.01 && !Zoom.zoomed(), String.format(Locale.ROOT, "fov %.3f, base %.0f", fov, baseFov));
                }
            }
            case "scroll" -> {
                hold(t >= 5 && t < 80);
                if (t == 10) slotBefore = io.github.profetgit.farout.client.Compat.slot(mc);
                if (t == 20) scroll(mc, 1);
                if (t == 22) scroll(mc, 1);
                if (t == 45) {
                    double fov = fov(mc), want = expected(baseFov, 8);
                    check("two_notches", Math.abs(Zoom.level() - 8) < 1e-6 && Math.abs(fov - want) < want * 0.01,
                        String.format(Locale.ROOT, "level %.4f, fov %.3f want %.3f", Zoom.level(), fov, want));
                    check("hotbar_kept", io.github.profetgit.farout.client.Compat.slot(mc) == slotBefore, "selected slot " + io.github.profetgit.farout.client.Compat.slot(mc) + ", was " + slotBefore);
                    for (int i = 0; i < 20; i++) scroll(mc, -1);
                }
                if (t == 50) check("min_clamp", Math.abs(Zoom.level() - Zoom.MIN) < 1e-6, String.format(Locale.ROOT, "level %.4f after scrolling far out", Zoom.level()));
                if (t == 55) for (int i = 0; i < 40; i++) scroll(mc, 1);
                if (t == 60) check("max_clamp", Math.abs(Zoom.level() - Config.get().maxZoom) < 1e-6, String.format(Locale.ROOT, "level %.2f, max %.0f", Zoom.level(), Config.get().maxZoom));
                if (t == 85) {
                    int before = io.github.profetgit.farout.client.Compat.slot(mc);
                    scroll(mc, -1);
                    check("scroll_unzoomed", io.github.profetgit.farout.client.Compat.slot(mc) != before, "not zoomed, the wheel changes the hotbar slot again");
                }
            }
            case "sens" -> {
                if (t == 2) yawNormal = turn(mc, 100);
                hold(t >= 5 && t < 35);
                if (t == 30) {
                    double zoomed = turn(mc, 100), ratio = zoomed / yawNormal;
                    check("steady_aim", Math.abs(ratio - 0.25) < 0.0025, String.format(Locale.ROOT, "turn %.4f deg zoomed vs %.4f normal: ratio %.4f (want 0.25 at 4x)", zoomed, yawNormal, ratio));
                    look(mc, -90, 0);
                }
            }
            case "far" -> {
                if (t == 3) {
                    Entity cow = cow(mc);
                    BlockEntity sign = mc.level.getBlockEntity(new BlockPos(100, G + 4, 4));
                    check("normal_culls", cow != null && !drawn(mc, cow) && sign != null && !drawn(mc, sign),
                        cow == null ? "no cow on the client" : sign == null ? "no sign on the client" : "cow at 110 and sign at 100 are culled at 1x (vanilla)");
                }
                hold(t >= 5 && t < 60);
                if (t == 45) {
                    Entity cow = cow(mc);
                    BlockEntity sign = mc.level.getBlockEntity(new BlockPos(100, G + 4, 4));
                    check("zoom_draws_far", cow != null && drawn(mc, cow) && sign != null && drawn(mc, sign), cow == null || sign == null ? "cow or sign missing" : "cow drawn " + drawn(mc, cow) + ", sign drawn " + drawn(mc, sign) + " at 4x");
                }
            }
            case "range" -> {
                hold(t >= 5 && t < 85);
                if (t == 40) {
                    double want = 150 - 0.5;
                    check("rangefinder_block", Math.abs(Rangefinder.distance - want) < 0.05 && "Block of Gold".equals(Rangefinder.target),
                        String.format(Locale.ROOT, "%.3f m to '%s', want %.2f to Block of Gold (label '%s')", Rangefinder.distance, Rangefinder.target, want, Rangefinder.label()));
                    // down at the cow's middle
                    double dy = EYE - (G + 1 + 0.7), dx = 110.5 - 0.5;
                    look(mc, -90, (float) Math.toDegrees(Math.atan2(dy, dx)));
                }
                if (t == 70) {
                    double want = 110.5 - 0.45 - 0.5;
                    check("rangefinder_entity", Math.abs(Rangefinder.distance - want) < 0.3 && "Cow".equals(Rangefinder.target),
                        String.format(Locale.ROOT, "%.3f m to '%s', want ~%.2f to Cow", Rangefinder.distance, Rangefinder.target, want));
                    look(mc, -90, 0);
                }
            }
            case "fog" -> {
                hold(t >= 5 && t < 50);
                //? if >=1.21.6 {
                if (t == 40) check("haze_thins", Math.abs(Zoom.hazeApplied - 2) < 0.02, String.format(Locale.ROOT, "haze distance x%.3f at 4x (want x2)", Zoom.hazeApplied));
                if (t == 65) check("haze_back", Zoom.hazeScale() <= 1.0001, String.format(Locale.ROOT, "haze x%.3f after zooming out", Zoom.hazeScale()));
                //?}
            }
            case "screen" -> {
                // the key stays physically held the whole time; a screen takes the keyboard while it is open
                hold(t >= 5 && t < 60);
                if (t == 25) io.github.profetgit.farout.client.Compat.setScreen(mc, io.github.profetgit.farout.client.Compat.chatScreen());
                if (t == 32) check("screen_scroll", !Zoom.scroll(1), "the wheel belongs to the screen");
                if (t == 30) check("screen_releases", !Zoom.active(), "opening chat lets go of a held zoom");
                if (t == 35) io.github.profetgit.farout.client.Compat.setScreen(mc, null);
                if (t == 75) check("closed_clean", !Zoom.zoomed(), "no zoom left after the screen closed");
            }
            case "overlays" -> {
                // the vignette and scope frame the zoomed view; the settings screen builds and shows
                hold(t >= 5 && t < 100);
                if (t == 8) Config.get().overlay = Config.Overlay.VIGNETTE;
                if (t == 40) Config.get().overlay = Config.Overlay.SPYGLASS;
                if (t == 90) Config.get().overlay = Config.Overlay.NONE;
                if (t == 105) {
                    try {
                        io.github.profetgit.farout.client.Compat.setScreen(mc, new io.github.profetgit.farout.client.ZoomConfigScreen(null));
                        settingsError = "";
                    } catch (Throwable e) {
                        settingsError = e.toString();
                    }
                }
                if (t == 125) check("settings_screen", settingsError.isEmpty() && io.github.profetgit.farout.client.Compat.screen(mc) instanceof io.github.profetgit.farout.client.ZoomConfigScreen,
                    settingsError.isEmpty() ? "the settings screen builds and stays open" : settingsError);
                if (t == 130) io.github.profetgit.farout.client.Compat.setScreen(mc, null);
            }
            case "rebind" -> {
                // what a player does in Controls: the zoom key moved to G, Toggle mode; then press, scroll, press again
                var g = com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_G);
                if (t == 2) {
                    Config.get().mode = Config.Mode.TOGGLE;
                    mc.options.setKey(Keys.ZOOM, g);
                    KeyMapping.resetMapping();
                }
                if (t == 10) {
                    KeyMapping.click(g);
                    KeyMapping.set(g, true);
                }
                if (t == 13) KeyMapping.set(g, false);
                if (t == 40) {
                    double want = expected(baseFov, 4);
                    double fov = fov(mc);
                    check("rebound_zoom", Zoom.active() && Math.abs(fov - want) < want * 0.02, String.format(Locale.ROOT, "key %s, active %s, fov %.3f, want %.3f, mode %s, screen %s, isDown %s", Keys.ZOOM.saveString(), Zoom.active(), fov, want, Config.get().mode, io.github.profetgit.farout.client.Compat.screen(mc), Keys.ZOOM.isDown()));
                }
                if (t == 45) scroll(mc, 2);
                if (t == 80) {
                    double want = expected(baseFov, 8);
                    double fov = fov(mc);
                    check("rebound_scroll", Math.abs(fov - want) < want * 0.02, String.format(Locale.ROOT, "fov %.3f, want %.3f (8x)", fov, want));
                }
                if (t == 85) {
                    KeyMapping.click(g);
                    KeyMapping.set(g, true);
                }
                if (t == 87) KeyMapping.set(g, false);
                if (t == 115) check("rebound_off", !Zoom.zoomed(), "a second press zooms out");
            }
            case "controls" -> {
                // the Controls screen sorts every binding (category order first); an unknown category used to crash it
                if (t == 5) {
                    String err = "";
                    try {
                        java.util.Arrays.stream(mc.options.keyMappings).sorted().count();
                    } catch (RuntimeException e) {
                        err = e.toString();
                    }
                    check("controls_sort", err.isEmpty(), err.isEmpty() ? "all bindings sort" : err);
                }
            }
            case "toggle" -> {
                if (t == 5) KeyMapping.click(Keys.ZOOM.getDefaultKey());
                if (t == 30) check("toggle_on", Zoom.active() && Zoom.zoomed(), "one tap zooms in and stays");
                if (t == 35) KeyMapping.click(Keys.ZOOM.getDefaultKey());
                if (t == 70) check("toggle_off", !Zoom.active() && !Zoom.zoomed(), "a second tap zooms out");
            }
            case "hybrid" -> {
                hold(t >= 5 && t < 7 || t >= 30 && t < 32 || t >= 45 && t < 70);
                if (t == 25) check("tap_sticks", Zoom.active(), "a short tap keeps the zoom");
                if (t == 40) check("tap_releases", !Zoom.active(), "the next tap zooms out");
                if (t == 65) check("hold_zooms", Zoom.active(), "holding zooms");
                if (t == 75) check("hold_releases", !Zoom.active(), "letting go after a hold zooms out");
            }
            case "hotbar" -> {
                hold(t >= 5 && t < 30);
                if (t == 20) {
                    var save = mc.options.keySaveHotbarActivator;
                    check("hotbar_save_paused", Zoom.sharesHotbarKey(mc) && Keys.ZOOM.isDown() && !save.isDown(),
                        "zoom key down, Save Hotbar Activator (same key) reads as up");
                    check("notice_shown", Config.get().keyNoticeShown, "one-time key notice toast");
                }
            }
            case "horizon" -> {
                // a landmark past the vanilla render distance: only Distant Horizons' LODs can show it; zooming on it
                // must keep the LOD world and the real chunks lined up (they share the zoomed projection)
                if (t == 0) cmd(mc, "forceload add 280 0 420 90", "fill 400 " + (G + 1) + " 38 402 " + (G + 60) + " 42 minecraft:gold_block",
                    "fill 300 " + (G + 1) + " 20 300 " + (G + 12) + " 80 minecraft:red_concrete");
                if (t == 400) look(mc, (float) -Math.toDegrees(Math.atan2(400.5, 40)), -1);
                hold(t >= 420 && t < 660);
                if (t == 480 || t == 486) scroll(mc, 1);
                if (t == 530) {
                    // the crosshair crosses the red wall at x 300, past the loaded chunks (render distance 12)
                    double want = 299.5 / Math.cos(Math.atan2(40, 400)) / Math.cos(Math.toRadians(1));
                    String l = Rangefinder.label();
                    check("dh_rangefinder", l != null && l.startsWith("\u2248") && "Red Concrete".equals(Rangefinder.target) && Math.abs(Rangefinder.distance - want) < 2,
                        String.format(Locale.ROOT, "'%s' (%.2f m, want ~%.1f to Red Concrete on Distant Horizons terrain)", l, Rangefinder.distance, want));
                }
                if (t == 560) scroll(mc, 2);
            }
            case "show" -> {
                // for the frames: zoom in, scroll twice, pan across the lane, scroll back, let go
                hold(t >= 20 && t < 170);
                if (t == 70 || t == 76) scroll(mc, 1);
                if (t >= 90 && t < 130) look(mc, -90 + (float) Math.sin((t - 90) / 40.0 * Math.PI) * 3, 0);
                if (t == 140) scroll(mc, -2);
            }
            default -> {
            }
        }
    }

    static Entity cow(Minecraft mc) {
        for (Entity e : mc.level.getEntitiesOfClass(Cow.class, new AABB(100, G - 2, -10, 120, G + 10, 10))) return e;
        return null;
    }

    /** The real entity check (distance, frustum, and whatever render mods add to it, e.g. Sodium's section culling). */
    static boolean drawn(Minecraft mc, Entity e) {
        //? if >=26.2 {
        var cam = io.github.profetgit.farout.client.Compat.camera(mc);
        Vec3 p = io.github.profetgit.farout.client.Compat.position(cam);
        //? if >=26.3 {
        return mc.getEntityRenderDispatcher().shouldRender(e, cam.getCullFrustum(), p.x, p.y, p.z, io.github.profetgit.farout.client.Compat.partial(mc));
        //?} else {
        /*return mc.getEntityRenderDispatcher().shouldRender(e, cam.getCullFrustum(), p.x, p.y, p.z);
        *///?}
        //?} else {
        /*return DemoDrawn.entityDrawn(e.getX(), e.getY(), e.getZ());
        *///?}
    }

    static boolean drawn(Minecraft mc, BlockEntity be) {
        //? if >=26.2 {
        float partial = io.github.profetgit.farout.client.Compat.partial(mc);
        return mc.getBlockEntityRenderDispatcher().tryExtractRenderState(be, partial, null, false) != null;
        //?} else {
        /*return DemoDrawn.blockEntityDrawn(be.getBlockPos());
        *///?}
    }

    static double expected(double fov, double mag) {
        return Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(fov) / 2) / mag));
    }

    static double fov(Minecraft mc) {
        //? if >=26.2 {
        return io.github.profetgit.farout.client.Compat.camera(mc).getFov();
        //?} else {
        /*return ((io.github.profetgit.farout.mixin.client.GameRendererDemoMixin) mc.gameRenderer).farout$fov(io.github.profetgit.farout.client.Compat.camera(mc), io.github.profetgit.farout.client.Compat.partial(mc), true);
        *///?}
    }

    static void hold(boolean down) {
        KeyMapping.set(Keys.ZOOM.getDefaultKey(), down);
    }

    static void release(Minecraft mc) {
        if (!Keys.ZOOM.isDefault()) {
            mc.options.setKey(Keys.ZOOM, Keys.ZOOM.getDefaultKey());
            KeyMapping.resetMapping();
        }
        hold(false);
        Zoom.debugReset();
        look(mc, -90, 0);
        Config.get().mode = Config.Mode.HOLD;
    }

    static void look(Minecraft mc, float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.setYHeadRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
    }

    /** The real scroll path (MouseHandler.onScroll), as a wheel notch up (+) or down (-). */
    static void scroll(Minecraft mc, double dy) {
        ((MouseHandlerDemoMixin) mc.mouseHandler).farout$scroll(io.github.profetgit.farout.client.Compat.window(mc), 0.0, dy);
    }

    /** The real turn code (MouseHandler.turnPlayer) for a mouse move of dx; returns the yaw change in degrees. */
    static double turn(Minecraft mc, double dx) {
        MouseHandlerDemoMixin mouse = (MouseHandlerDemoMixin) mc.mouseHandler;
        float before = mc.player.getYRot();
        mouse.farout$dx(dx);
        mouse.farout$dy(0);
        //? if >=1.21 {
        mouse.farout$turn(0.0);
        //?} else {
        /*mouse.farout$turn();
        *///?}
        mouse.farout$dx(0);
        return mc.player.getYRot() - before;
    }

    static void record() {
        recording = true;
        frame = 0;
        timing.clear();
    }

    static void stopRecording() {
        recording = false;
        Path dir = OUT.resolve(current());
        List<String> lines = new ArrayList<>(timing);
        lines.add(0, "# frame nanos scene_tick fov magnification progress level");
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve("timing.txt"), lines);
        } catch (IOException e) {
            System.out.println("[cndemo] timing write failed: " + e);
        }
    }

    /** Called after every rendered frame. */
    public static void onFrame(Minecraft mc) {
        if (!recording || done || mc.player == null) return;
        Path dir = OUT.resolve(current());
        int n = frame++;
        peakMag = Math.max(peakMag, Zoom.magnification());
        timing.add(n + " " + System.nanoTime() + " " + sceneTick + " " + f(fov(mc)) + " " + f(Zoom.magnification()) + " " + f(Zoom.progress()) + " " + f(Zoom.level()));
        if (!FRAMES) return;
        Path out = dir.resolve(String.format("f%05d.png", n));
        pending.incrementAndGet();
        io.github.profetgit.farout.client.Compat.screenshot(io.github.profetgit.farout.client.Compat.renderTarget(mc), (NativeImage img) -> WRITER.execute(() -> {
            try (img) {
                Files.createDirectories(dir);
                img.writeToFile(out);
            } catch (Exception e) {
                System.out.println("[cndemo] write failed " + out + ": " + e);
            } finally {
                pending.decrementAndGet();
            }
        }));
    }

    /** The running scene (in multiplayer, the case MpDirector runs). */
    static String current() {
        return MpDirector.CASE != null ? MpDirector.CASE : SCENES[scene];
    }

    static void check(String name, boolean pass, String detail) {
        String id = current() + "/" + name;
        results.add(String.format("{\"name\":\"%s\",\"pass\":%b,\"detail\":\"%s\"}", id, pass, detail.replace("\"", "'")));
        System.out.println("[cndemo] " + (pass ? "PASS " : "FAIL ") + id + "  " + detail);
    }

    static void finish() {
        done = true;
        try {
            Files.createDirectories(OUT);
            Files.writeString(OUT.resolve("results.json"), "{\"loader\":\"" + FarOut.loader() + "\",\"results\":[\n"
                + String.join(",\n", results) + "\n]}\n");
        } catch (IOException e) {
            System.out.println("[cndemo] results write failed: " + e);
        }
        drainUntil = System.currentTimeMillis() + 120_000;
    }

    static String name(Minecraft mc) {
        return io.github.profetgit.farout.client.Compat.playerName(mc);
    }

    static void cmd(Minecraft mc, String... commands) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            for (String c : commands) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        });
    }

    static String f(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
