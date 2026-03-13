package com.github.alexthe666.iceandfire.world.dimension.feature;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Generates ice and rock spike formations for the Dreadlands.
 *
 * Two modes:
 *   - Simple spikes: small clusters of dragon ice columns (for wastes/general)
 *   - Massive spikes: large tapered formations with material gradient (for spike biome)
 */
public final class DreadSpikeGenerator {

    private DreadSpikeGenerator() {}

    /**
     * Simple ice spike cluster. 2-5 thin columns of dragon ice.
     */
    public static void placeSmallCluster(WorldGenRegion level, int x, int surfY, int z,
                                         Random rand, int minCount, int maxCount,
                                         int minH, int maxH) {
        DreadlandsBlocks.ensureResolved();
        int count = minCount + rand.nextInt(maxCount - minCount + 1);
        for (int i = 0; i < count; i++) {
            int sx = x + rand.nextInt(5) - 2;
            int sz = z + rand.nextInt(5) - 2;
            int h = minH + rand.nextInt(maxH - minH + 1);

            for (int dy = 0; dy < h; dy++) {
                DreadlandsPlacement.placeSafe(level,
                        new BlockPos(sx, surfY + dy, sz), DreadlandsBlocks.DRAGON_ICE);
            }
        }
    }

    /**
     * Large tapered spike formation for the Dreaded Spikes biome.
     *
     * Material gradient (bottom to top):
     *   Base (0-30%):   dread stone + frozen stone
     *   Middle (30-70%): packed ice + dragon ice + dread stone
     *   Top (70-100%):  dragon ice + blue ice
     *
     * Base radius scales with height for a natural tapered look.
     *
     * @param spikeCount Number of individual spikes in the cluster (4-8 typical)
     * @param minH       Minimum spike height
     * @param maxH       Maximum spike height
     * @param spread     Horizontal spread of the cluster
     */
    public static void placeMassiveCluster(WorldGenRegion level, int x, int surfY, int z,
                                           Random rand, int spikeCount, int minH, int maxH,
                                           int spread) {
        DreadlandsBlocks.ensureResolved();
        for (int s = 0; s < spikeCount; s++) {
            int sx = x + rand.nextInt(spread) - spread / 2;
            int sz = z + rand.nextInt(spread) - spread / 2;
            int spikeH = minH + rand.nextInt(maxH - minH + 1);

            // Base radius: bigger for taller spikes
            int baseRadius = spikeH > 8 ? 2 : (spikeH > 5 ? 1 : 0);

            for (int dy = 0; dy < spikeH; dy++) {
                // Taper: radius decreases with height
                int radius = (int) (baseRadius * (1.0 - (double) dy / spikeH));
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz <= radius * radius + 1) {
                            BlockState mat = pickSpikeMaterial(dy, spikeH, rand);
                            DreadlandsPlacement.placeSafe(level,
                                    new BlockPos(sx + dx, surfY + dy, sz + dz), mat);
                        }
                    }
                }
            }
        }
    }

    /**
     * Scatters individual DRAGON_ICE_SPIKES blocks on the ground surface.
     * These are the half-block spike models that deal contact damage.
     */
    public static void scatterGroundSpikes(WorldGenRegion level, int cx, int cz,
                                           Random rand, int count,
                                           java.util.function.IntBinaryOperator surfaceHeightLookup) {
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16);
            int z = cz + rand.nextInt(16);
            int surfY = surfaceHeightLookup.applyAsInt(x, z);
            BlockState spikes = IafBlockRegistry.DRAGON_ICE_SPIKES.get().defaultBlockState();
            DreadlandsPlacement.placeSafe(level, new BlockPos(x, surfY + 1, z), spikes);
        }
    }

    /**
     * Picks spike block material based on vertical position within the spike.
     */
    private static BlockState pickSpikeMaterial(int dy, int totalHeight, Random rand) {
        double ratio = (double) dy / totalHeight;
        if (ratio > 0.7) {
            return rand.nextFloat() < 0.3f ? DreadlandsBlocks.BLUE_ICE : DreadlandsBlocks.DRAGON_ICE;
        } else if (ratio > 0.3) {
            float r = rand.nextFloat();
            if (r < 0.3f) return DreadlandsBlocks.PACKED_ICE;
            if (r < 0.6f) return DreadlandsBlocks.DRAGON_ICE;
            return DreadlandsBlocks.DREAD_STONE;
        } else {
            return rand.nextFloat() < 0.3f ? DreadlandsBlocks.FROZEN_STONE : DreadlandsBlocks.DREAD_STONE;
        }
    }
}