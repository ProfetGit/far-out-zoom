package io.github.profetgit.farout.demo;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/** What the game drew recently (filled on the 1.21.x targets; the 26.x targets ask the dispatchers directly). */
public final class DemoDrawn {
    private static final long RECENT = 250_000_000L;
    private static final ConcurrentHashMap<Long, long[]> ENTITIES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> BLOCKS = new ConcurrentHashMap<>();

    private DemoDrawn() {
    }

    public static void entity(double x, double y, double z) {
        ENTITIES.put(BlockPos.asLong((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)), new long[] {System.nanoTime()});
    }

    public static void blockEntity(BlockPos pos) {
        BLOCKS.put(pos.asLong(), System.nanoTime());
    }

    public static boolean entityDrawn(double x, double y, double z) {
        long now = System.nanoTime();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long[] t = ENTITIES.get(BlockPos.asLong((int) Math.floor(x) + dx, (int) Math.floor(y) + dy, (int) Math.floor(z) + dz));
                    if (t != null && now - t[0] < RECENT) return true;
                }
            }
        }
        return false;
    }

    public static boolean blockEntityDrawn(BlockPos pos) {
        Long t = BLOCKS.get(pos.asLong());
        return t != null && System.nanoTime() - t < RECENT;
    }
}
