package io.github.profetgit.farout.client;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * What the crosshair points at while zoomed, out to the edge of the loaded world: the nearest block or fluid surface on
 * the camera ray, or a nearer entity. Entities are named by type (players too), so the zoom doesn't read name tags
 * further away than vanilla shows them.
 */
public final class Rangefinder {
    static final long INTERVAL = 33_000_000L;
    static long lastNanos;
    static String label;
    /** Distance in blocks to the last hit, or -1; for the demo checks. */
    public static double distance = -1;
    public static String target = "";

    private Rangefinder() {
    }

    public static String label() {
        return label;
    }

    public static void update(Minecraft mc) {
        if (Zoom.progress() < 0.3 || mc.level == null || mc.player == null) {
            label = null;
            distance = -1;
            target = "";
            lastNanos = 0;
            return;
        }
        long now = System.nanoTime();
        if (lastNanos != 0 && now - lastNanos < INTERVAL) return;
        lastNanos = now;
        Camera cam = Compat.camera(mc);
        Vec3 from = Compat.position(cam);
        Vector3fc f = Compat.look(cam);
        Vec3 dir = new Vec3(f.x(), f.y(), f.z()).normalize();
        double reach = mc.options.getEffectiveRenderDistance() * 16 + 32;
        Vec3 to = from.add(dir.scale(reach));
        Entity self = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        BlockHitResult block = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, self));
        double best = block.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : block.getLocation().distanceToSqr(from);
        Entity hitEntity = null;
        Vec3 hitAt = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == self || e == mc.player || e.isSpectator() || e.isPassengerOfSameVehicle(self) || e.isInvisibleTo(mc.player)) continue;
            AABB box = e.getBoundingBox().inflate(Math.max(0.1, e.getPickRadius()));
            if (box.distanceToSqr(from) >= best) continue;
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(from);
            if (d < best) {
                best = d;
                hitEntity = e;
                hitAt = hit.get();
            }
        }
        Vec3 eye = mc.player.getEyePosition();
        if (hitEntity != null) {
            distance = hitAt.distanceTo(eye);
            target = hitEntity instanceof Player ? "Player" : hitEntity.getType().getDescription().getString();
        } else if (best < Double.MAX_VALUE) {
            distance = block.getLocation().distanceTo(eye);
            BlockPos pos = block.getBlockPos();
            target = mc.level.getBlockState(pos).getBlock().getName().getString();
        } else {
            distance = -1;
            target = "";
            // past the loaded chunks: Distant Horizons' LOD terrain, if it is installed
            DistantHorizons.Hit far = DistantHorizons.query(mc, from, dir, 4096);
            if (far != null) {
                double along = far.pos().subtract(from).dot(dir);
                distance = from.add(dir.scale(along)).distanceTo(eye);
                target = far.name();
                label = "\u2248 " + format(distance) + " · " + target;
                return;
            }
        }
        label = distance < 0 ? "> " + format(reach) : format(distance) + " · " + target;
    }

    static String format(double d) {
        return d < 100 ? String.format(Locale.ROOT, "%.1f m", d) : String.format(Locale.ROOT, "%d m", Math.round(d));
    }
}
