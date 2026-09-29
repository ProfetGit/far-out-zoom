package io.github.profetgit.farout.client;

import io.github.profetgit.farout.FarOut;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Rangefinder on Distant Horizons' terrain: past the loaded chunks the only terrain there is is DH's LODs, and DH's API
 * can raycast them. Optional and by reflection (no compile dependency). The query reads DH's database, so it runs on a
 * worker thread, one at a time; the rangefinder shows the latest answer.
 */
final class DistantHorizons {
    record Hit(Vec3 pos, String name, long nanos) {
    }

    static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "far_out_zoom-dh-raycast");
        t.setDaemon(true);
        return t;
    });
    static final AtomicBoolean BUSY = new AtomicBoolean();
    static volatile Hit last;
    static volatile boolean failed;
    static Boolean present;
    static Field terrainRepo, worldProxy, success, payload, pos, dataPoint, blockState, vx, vy, vz;
    static Method worldLoaded, levels, wrapped, raycast, createCache, blockWrapped;
    static Object cache;
    static final boolean DEBUG = Boolean.getBoolean("far_out_zoom.debug.dh");
    static int debugged;

    private DistantHorizons() {
    }

    static boolean present() {
        if (present == null) {
            try {
                Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
                terrainRepo = delayed.getField("terrainRepo");
                worldProxy = delayed.getField("worldProxy");
                Class<?> proxy = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy");
                worldLoaded = proxy.getMethod("worldLoaded");
                levels = proxy.getMethod("getAllLoadedLevelWrappers");
                wrapped = Class.forName("com.seibel.distanthorizons.api.interfaces.IDhApiUnsafeWrapper").getMethod("getWrappedMcObject");
                Class<?> repo = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo");
                Class<?> levelType = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper");
                Class<?> cacheType = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache");
                raycast = repo.getMethod("raycast", levelType, double.class, double.class, double.class, float.class, float.class, float.class, int.class, cacheType);
                createCache = repo.getMethod("createSoftCache");
                Class<?> result = Class.forName("com.seibel.distanthorizons.api.objects.DhApiResult");
                success = result.getField("success");
                payload = result.getField("payload");
                Class<?> ray = Class.forName("com.seibel.distanthorizons.api.objects.data.DhApiRaycastResult");
                pos = ray.getField("pos");
                dataPoint = ray.getField("dataPoint");
                blockState = Class.forName("com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint").getField("blockStateWrapper");
                Class<?> vec = Class.forName("com.seibel.distanthorizons.api.objects.math.DhApiVec3i");
                vx = vec.getField("x");
                vy = vec.getField("y");
                vz = vec.getField("z");
                present = true;
                FarOut.LOG.info("Far Out Zoom: Distant Horizons found, the rangefinder reaches its terrain");
            } catch (ReflectiveOperationException | LinkageError e) {
                present = false;
            }
        }
        return present && !failed;
    }

    /** Starts a raycast (unless one is running) and returns the latest finished answer, if any is recent. */
    static Hit query(Minecraft mc, Vec3 from, Vec3 dir, int maxBlocks) {
        if (!present()) return null;
        Level level = mc.level;
        if (level != null && BUSY.compareAndSet(false, true)) {
            WORKER.execute(() -> {
                try {
                    last = raycast(level, from, dir, maxBlocks);
                } catch (Throwable e) {
                    // an API change or a DH error: stop asking, keep the vanilla rangefinder
                    failed = true;
                    FarOut.LOG.warn("Far Out Zoom: Distant Horizons raycast failed, the rangefinder stops at the loaded chunks ({})", e.toString());
                } finally {
                    BUSY.set(false);
                }
            });
        }
        Hit h = last;
        return h != null && System.nanoTime() - h.nanos() < 500_000_000L ? h : null;
    }

    static Hit raycast(Level level, Vec3 from, Vec3 dir, int maxBlocks) throws ReflectiveOperationException {
        Object repo = terrainRepo.get(null), proxy = worldProxy.get(null);
        if (repo == null || proxy == null || !(Boolean) worldLoaded.invoke(proxy)) return null;
        Object wrapper = null;
        StringBuilder seen = new StringBuilder();
        for (Object w : (Iterable<?>) levels.invoke(proxy)) {
            Object mcLevel = wrapped.invoke(w);
            seen.append(w.getClass().getSimpleName()).append('/').append(mcLevel == null ? "null" : mcLevel.getClass().getSimpleName()).append(' ');
            // multiplayer: DH wraps the client level; singleplayer: only the server's levels, matched by dimension
            if (mcLevel == level) {
                wrapper = w;
                break;
            }
            if (wrapper == null && mcLevel instanceof Level l && l.dimension().equals(level.dimension())) wrapper = w;
        }
        if (DEBUG && debugged++ < 3) FarOut.LOG.info("Far Out Zoom DH debug: levels [{}] matched {}", seen, wrapper != null);
        if (wrapper == null) return null;
        if (cache == null) cache = createCache.invoke(repo);
        Object result = raycast.invoke(repo, wrapper, from.x, from.y, from.z, (float) dir.x, (float) dir.y, (float) dir.z, maxBlocks, cache);
        if (DEBUG && debugged++ < 6) FarOut.LOG.info("Far Out Zoom DH debug: raycast from {} dir {} -> {}", from, dir,
            result == null ? "null" : success.get(result) + " " + result.getClass().getField("message").get(result) + " " + payload.get(result));
        if (result == null || !(Boolean) success.get(result)) return null;
        Object hit = payload.get(result);
        if (hit == null) return null;
        Object p = pos.get(hit);
        Vec3 at = new Vec3(vx.getInt(p) + 0.5, vy.getInt(p) + 0.5, vz.getInt(p) + 0.5);
        String name = "Terrain";
        Object point = dataPoint.get(hit);
        Object bs = point == null ? null : blockState.get(point);
        Object mcState = bs == null ? null : wrapped.invoke(bs);
        if (mcState instanceof BlockState s) name = s.getBlock().getName().getString();
        return new Hit(at, name, System.nanoTime());
    }
}
