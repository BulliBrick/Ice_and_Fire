package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.world.dimension.feature.DreadRuinGenerator;
import com.github.alexthe666.iceandfire.world.dimension.feature.DreadScatterFeatures;
import com.github.alexthe666.iceandfire.world.dimension.feature.DreadSpikeGenerator;
import com.github.alexthe666.iceandfire.world.dimension.feature.DreadTreeGenerator;
import net.minecraft.server.level.WorldGenRegion;

import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * Coordinates feature placement across all Dreadlands biomes.
 *
 * Each biome has its own decorate method that composes features from the
 * extracted generator classes. Feature density, size parameters, and
 * spawn chances are all defined here — the generators themselves are
 * stateless and reusable.
 *
 * To add a new biome's decoration:
 *   1. Add a decorateXxx() method
 *   2. Add the case to {@link #decorate}
 *
 * To add a new feature type:
 *   1. Create a generator class under feature/
 *   2. Call it from the relevant biome decorate methods
 */
public final class DreadlandsDecorator {

    private DreadlandsDecorator() {}

    /**
     * Main entry point — dispatches to the correct biome decorator.
     *
     * @param surfaceLookup (x, z) → surface Y height for the given terrain profile
     * @param ravineCheck   (x, z) → ravine depth (0 = no ravine)
     */
    public static void decorate(WorldGenRegion level, int chunkX, int chunkZ,
                                DreadBiome biome, Random rand,
                                IntBinaryOperator surfaceLookup,
                                IntBinaryOperator ravineCheck) {
        // Convenience: surface lookup that skips ravine positions
        IntBinaryOperator safeSurface = (x, z) -> {
            if (ravineCheck.applyAsInt(x, z) > 0) return -1;
            return surfaceLookup.applyAsInt(x, z);
        };

        switch (biome) {
            case WASTES -> decorateWastes(level, chunkX, chunkZ, rand, safeSurface, ravineCheck);
            case SPIKES -> decorateSpikes(level, chunkX, chunkZ, rand, safeSurface, ravineCheck);
            case DEAD_FOREST -> decorateDeadForest(level, chunkX, chunkZ, rand, safeSurface, ravineCheck);
            case FROZEN_PLAINS -> decorateFrozenPlains(level, chunkX, chunkZ, rand, safeSurface);
            case CRAGS -> decorateCrags(level, chunkX, chunkZ, rand, safeSurface, ravineCheck);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PER-BIOME DECORATION
    // ═══════════════════════════════════════════════════════════════════════

    private static void decorateWastes(WorldGenRegion level, int cx, int cz, Random rand,
                                       IntBinaryOperator surface, IntBinaryOperator ravine) {
        // Dead trees: 2-4
        placeTreesSafe(level, cx, cz, rand, 2 + rand.nextInt(3), 5, 10, surface);

        // Ruins: 15%
        placeRuinSafe(level, cx, cz, rand, 0.15f, surface);

        // Small ice spikes: 10%
        if (rand.nextFloat() < 0.10f) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy > 0) DreadSpikeGenerator.placeSmallCluster(level, x, sy, z, rand, 2, 5, 2, 6);
        }

        // Boulders: 8%
        DreadScatterFeatures.placeBoulder(level, cx, cz, rand, 0.08f, surface);

        // Cobwebs: 15%
        DreadScatterFeatures.scatterCobwebs(level, cx, cz, rand, 0.15f, 1, 2, surface);
    }

    private static void decorateSpikes(WorldGenRegion level, int cx, int cz, Random rand,
                                       IntBinaryOperator surface, IntBinaryOperator ravine) {
        // Massive spike clusters: 3-6 per chunk
        int clusterCount = 3 + rand.nextInt(4);
        for (int c = 0; c < clusterCount; c++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy <= 0) continue;
            DreadSpikeGenerator.placeMassiveCluster(level, x, sy, z, rand,
                    4 + rand.nextInt(5), 4, 15, 7);
        }

        // Ground damage spikes: 8 per chunk
        DreadSpikeGenerator.scatterGroundSpikes(level, cx, cz, rand, 8, surface);

        // Ruins among spikes: 8%
        placeRuinSafe(level, cx, cz, rand, 0.08f, surface);
    }

    private static void decorateDeadForest(WorldGenRegion level, int cx, int cz, Random rand,
                                           IntBinaryOperator surface, IntBinaryOperator ravine) {
        // Dense trees: 8-14, taller
        int treeCount = 8 + rand.nextInt(7);
        for (int i = 0; i < treeCount; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy <= 0) continue;
            DreadTreeGenerator.placeDeadTree(level, x, sy, z, rand, 6, 13);

            // Cobwebs IN the trees: 40%
            if (rand.nextFloat() < 0.40f) {
                int webY = sy + 3 + rand.nextInt(4);
                int dx = rand.nextInt(3) - 1, dz = rand.nextInt(3) - 1;
                DreadlandsPlacement.placeSafe(level, new net.minecraft.core.BlockPos(x + dx, webY, z + dz),
                        DreadlandsBlocks.COBWEB);
                if (rand.nextFloat() < 0.5f) {
                    DreadlandsPlacement.placeSafe(level,
                            new net.minecraft.core.BlockPos(x + dx + rand.nextInt(2), webY + 1, z + dz + rand.nextInt(2)),
                            DreadlandsBlocks.COBWEB);
                }
            }
        }

        // Ground cobwebs: 50% chance, 3-6
        DreadScatterFeatures.scatterCobwebs(level, cx, cz, rand, 0.50f, 3, 6, surface);

        // Fallen logs: 20%
        if (rand.nextFloat() < 0.20f) {
            int x = cx + rand.nextInt(14) + 1, z = cz + rand.nextInt(14) + 1;
            int sy = surface.applyAsInt(x, z);
            if (sy > 0) DreadTreeGenerator.placeFallenLog(level, x, sy, z, rand);
        }

        // Ruins: 12%
        placeRuinSafe(level, cx, cz, rand, 0.12f, surface);
    }

    private static void decorateFrozenPlains(WorldGenRegion level, int cx, int cz, Random rand,
                                             IntBinaryOperator surface) {
        // Snow drifts: 4-8
        DreadScatterFeatures.placeSnowDrifts(level, cx, cz, rand,
                4 + rand.nextInt(5), surface);

        // Rare sparse tree: 30%
        if (rand.nextFloat() < 0.30f) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy > 0) DreadTreeGenerator.placeDeadTree(level, x, sy, z, rand, 3, 6);
        }

        // Ice patches: 2-4
        DreadScatterFeatures.placeIcePatches(level, cx, cz, rand,
                2 + rand.nextInt(3), surface);

        // Ruins (partially buried): 10%
        placeRuinSafe(level, cx, cz, rand, 0.10f, surface);
    }

    private static void decorateCrags(WorldGenRegion level, int cx, int cz, Random rand,
                                      IntBinaryOperator surface, IntBinaryOperator ravine) {
        // Rock pillars: 2-5
        DreadScatterFeatures.placeRockPillars(level, cx, cz, rand,
                2 + rand.nextInt(4), surface);

        // Boulders: more common here, 25%
        DreadScatterFeatures.placeBoulder(level, cx, cz, rand, 0.25f, surface);

        // Ruins: 20% — ancient high-ground settlements
        placeRuinSafe(level, cx, cz, rand, 0.20f, surface);

        // Sparse tree: 15%
        if (rand.nextFloat() < 0.15f) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy > 0) DreadTreeGenerator.placeDeadTree(level, x, sy, z, rand, 4, 7);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private static void placeTreesSafe(WorldGenRegion level, int cx, int cz, Random rand,
                                       int count, int minH, int maxH,
                                       IntBinaryOperator surface) {
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int sy = surface.applyAsInt(x, z);
            if (sy > 0) DreadTreeGenerator.placeDeadTree(level, x, sy, z, rand, minH, maxH);
        }
    }

    private static void placeRuinSafe(WorldGenRegion level, int cx, int cz, Random rand,
                                      float chance, IntBinaryOperator surface) {
        if (rand.nextFloat() >= chance) return;
        int x = cx + 3 + rand.nextInt(10), z = cz + 3 + rand.nextInt(10);
        int sy = surface.applyAsInt(x, z);
        if (sy > 0) DreadRuinGenerator.placeRandom(level, x, sy, z, rand);
    }
}