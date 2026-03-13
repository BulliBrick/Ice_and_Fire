package com.github.alexthe666.iceandfire.world.dimension.feature;

import com.github.alexthe666.iceandfire.world.dimension.DreadBiome;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;
import java.util.function.IntBinaryOperator;

import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks.*;
import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement.placeSafe;

/**
 * Small scatter-type features for the Dreadlands: cobwebs, boulders,
 * snow drifts, ice patches, natural rock pillars.
 *
 * Each method is self-contained and takes a chunk origin + surface lookup
 * so the caller doesn't need to pass terrain data structures.
 */
public final class DreadScatterFeatures {

    private DreadScatterFeatures() {}

    // ─── Cobwebs ──────────────────────────────────────────────────────────

    /** Scatter cobweb blocks on the ground surface. */
    public static void scatterCobwebs(WorldGenRegion level, int cx, int cz, Random rand,
                                      float chance, int minCount, int maxCount,
                                      IntBinaryOperator surfaceLookup) {
        DreadlandsBlocks.ensureResolved();
        if (rand.nextFloat() >= chance) return;
        int count = minCount + rand.nextInt(maxCount - minCount + 1);
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            placeSafe(level, new BlockPos(x, surfaceLookup.applyAsInt(x, z) + 1, z), COBWEB);
        }
    }

    // ─── Dread stone face boulders ────────────────────────────────────────

    /** Place a single dread stone face boulder on the surface. */
    public static void placeBoulder(WorldGenRegion level, int cx, int cz, Random rand,
                                    float chance, IntBinaryOperator surfaceLookup) {
        DreadlandsBlocks.ensureResolved();
        if (rand.nextFloat() >= chance) return;
        int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
        placeSafe(level, new BlockPos(x, surfaceLookup.applyAsInt(x, z), z), DREAD_STONE_FACE);
    }

    // ─── Snow drifts (Frozen Plains) ──────────────────────────────────────

    /** Place snow drift stacks (2-3 high) with sideways spread. */
    public static void placeSnowDrifts(WorldGenRegion level, int cx, int cz, Random rand,
                                       int count, IntBinaryOperator surfaceLookup) {
        DreadlandsBlocks.ensureResolved();
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int surfY = surfaceLookup.applyAsInt(x, z);
            int driftH = 2 + rand.nextInt(2);

            for (int dy = 0; dy < driftH; dy++) {
                placeSafe(level, new BlockPos(x, surfY + dy, z),
                        rand.nextFloat() < 0.3f ? POWDER_SNOW : SNOW);
            }
            // Spread sideways
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (rand.nextFloat() < 0.4f) {
                        placeSafe(level, new BlockPos(x + dx, surfY, z + dz), SNOW);
                    }
                }
            }
        }
    }

    // ─── Ice patches (Frozen Plains) ──────────────────────────────────────

    /** Place circular packed ice patches on the surface. */
    public static void placeIcePatches(WorldGenRegion level, int cx, int cz, Random rand,
                                       int count, IntBinaryOperator surfaceLookup) {
        DreadlandsBlocks.ensureResolved();
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int surfY = surfaceLookup.applyAsInt(x, z);
            int radius = 1 + rand.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius && rand.nextFloat() < 0.7f) {
                        DreadlandsPlacement.placeForce(level,
                                new BlockPos(x + dx, surfY - 1, z + dz), PACKED_ICE);
                    }
                }
            }
        }
    }

    // ─── Natural rock pillars (Crags) ─────────────────────────────────────

    /** Place tapered natural dread stone columns. */
    public static void placeRockPillars(WorldGenRegion level, int cx, int cz, Random rand,
                                        int count, IntBinaryOperator surfaceLookup) {
        DreadlandsBlocks.ensureResolved();
        for (int i = 0; i < count; i++) {
            int x = cx + rand.nextInt(16), z = cz + rand.nextInt(16);
            int surfY = surfaceLookup.applyAsInt(x, z);
            int pillarH = 5 + rand.nextInt(10);
            int baseR = rand.nextInt(2) + 1;

            for (int dy = 0; dy < pillarH; dy++) {
                int r = (int) (baseR * (1.0 - 0.6 * dy / pillarH));
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz <= r * r + 1) {
                            BlockState mat = rand.nextFloat() < 0.15f ? DREAD_STONE_TILE : DREAD_STONE;
                            placeSafe(level, new BlockPos(x + dx, surfY + dy, z + dz), mat);
                        }
                    }
                }
            }
        }
    }
}