package io.github.profetgit.farout.demo;

import io.github.profetgit.farout.client.Rangefinder;
import io.github.profetgit.farout.client.Zoom;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * The client's half of dev/server/dh_mp.py: Far Out Zoom with Distant Horizons on a real dedicated server. The script
 * builds a red concrete wall at x 300 (past the view distance) and ops this player; this director aims at the wall,
 * zooms, and waits until the rangefinder reads it from DH's terrain. Cases (-Dfar_out_zoom.demo.mp):
 * <ul>
 * <li>{@code dh_server}: DH on the server too, which sends its LODs to the client;
 * <li>{@code client_only}: a vanilla server; DH only has LODs of chunks this client has seen, so the player first
 * visits the wall, then comes back;
 * <li>{@code no_visit}: the control: a vanilla server and no visit, so there is no LOD of the wall and the rangefinder
 * must not claim it.
 * </ul>
 */
final class MpDirector {
    static final String CASE = System.getProperty("far_out_zoom.demo.mp");
    static final int G = Director.G;
    /** Ticks to wait for the rangefinder to read DH terrain. */
    static final int PATIENCE = Integer.getInteger("far_out_zoom.demo.mp.patience", 2400);
    static int tick = -1, aimAt = -1, passedAt = -1;
    static double want;

    private MpDirector() {
    }

    static void onTick(Minecraft mc) {
        if (CASE == null || Director.done) return;
        tick++;
        if (tick == 1) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Zoom.debugReset();
        }
        boolean visit = "client_only".equals(CASE);
        // the script ops the player within a few seconds of the join
        if (tick == 100) {
            if (visit) command(mc, "tp @s 285.5 " + (G + 1) + " 50.5 -90 0");
            else command(mc, "tp @s 0.5 " + (G + 1) + " 0.5 -90 0");
        }
        if (visit && tick == 700) command(mc, "tp @s 0.5 " + (G + 1) + " 0.5 -90 0");
        int start = visit ? 800 : 160;
        if (tick == start) {
            aimAt = tick;
            mc.gui.hud.getChat().clearMessages(false);
            Director.record();
            System.out.println("[cndemo] mp " + CASE + " aiming at the wall");
        }
        if (aimAt < 0) return;
        // aim at the gold pillar at (400, 40), 1 degree up: the ray crosses the red wall at x 300 on the way
        Director.look(mc, (float) -Math.toDegrees(Math.atan2(400.5, 40)), -1);
        Director.hold(true);
        want = 299.5 / Math.cos(Math.atan2(40, 400)) / Math.cos(Math.toRadians(1));
        int t = tick - aimAt;
        if ("no_visit".equals(CASE)) {
            if (t == 600) {
                String l = Rangefinder.label();
                Director.check("no_lod_no_reading", !"Red Concrete".equals(Rangefinder.target),
                    String.format(Locale.ROOT, "'%s' after 30 s zoomed: nothing to read past the view distance without a LOD", l));
                end(mc);
            }
            return;
        }
        if (passedAt < 0 && t > 20 && t % 10 == 0) {
            String l = Rangefinder.label();
            boolean ok = l != null && l.startsWith("≈") && "Red Concrete".equals(Rangefinder.target) && Math.abs(Rangefinder.distance - want) < 2;
            if (ok) {
                passedAt = tick;
                Director.check("dh_rangefinder", true, String.format(Locale.ROOT, "'%s' (%.2f m, want ~%.1f) after %.1f s zoomed on a %s server",
                    l, Rangefinder.distance, want, t / 20.0, "dh_server".equals(CASE) ? "Distant Horizons" : "vanilla"));
                Director.scroll(mc, 2);
            } else if (t >= PATIENCE) {
                Director.check("dh_rangefinder", false, String.format(Locale.ROOT, "still '%s' (%.2f m to '%s') after %d s, want ~%.1f to Red Concrete",
                    l, Rangefinder.distance, Rangefinder.target, PATIENCE / 20, want));
                end(mc);
            }
        }
        if (passedAt >= 0 && tick - passedAt == 55) {
            Director.check("zoom_on_lod", Zoom.magnification() > 7.9, String.format(Locale.ROOT, "zoomed to %.2fx on the LOD wall", Zoom.magnification()));
            Director.scroll(mc, 2);
        }
        if (passedAt >= 0 && tick - passedAt == 130) end(mc);
    }

    static void end(Minecraft mc) {
        Director.hold(false);
        Director.stopRecording();
        Director.finish();
    }

    static void command(Minecraft mc, String cmd) {
        mc.player.connection.sendCommand(cmd);
    }
}
