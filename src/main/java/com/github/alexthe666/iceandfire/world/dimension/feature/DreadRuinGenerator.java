package com.github.alexthe666.iceandfire.world.dimension.feature;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement.getRandomBrick;
import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement.placeSafe;

/**
 * Generates procedural ruins for the Dreadlands dimension.
 *
 * Ruin types:
 *   WALL       — Broken wall segment with optional L-bend. 4-8 long, 2-4 high.
 *   PILLARS    — 2-4 freestanding columns with tile caps.
 *   FOUNDATION — Rectangular outline with partial tile floor, collapsed sections.
 *   ARCHWAY    — Two pillars connected by a crumbling horizontal span.
 *
 * All types use the shared weathered brick distribution from DreadlandsPlacement.
 *
 * To add a new ruin type:
 *   1. Add a static method following the existing pattern
 *   2. Add it to the placeRandom() switch
 *   3. Increment the rand.nextInt() bound
 */
public final class DreadRuinGenerator {

    private DreadRuinGenerator() {}

    /**
     * Places a random ruin type at the given position.
     */
    public static void placeRandom(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        DreadlandsBlocks.ensureResolved();
        switch (rand.nextInt(4)) {
            case 0 -> placeWall(level, x, surfY, z, rand);
            case 1 -> placePillars(level, x, surfY, z, rand);
            case 2 -> placeFoundation(level, x, surfY, z, rand);
            case 3 -> placeArchway(level, x, surfY, z, rand);
        }
    }

    /**
     * Broken wall: 4-8 blocks long, 2-4 high, with ~20% random gaps.
     * 40% chance of an L-bend at the origin.
     */
    public static void placeWall(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        int length = 4 + rand.nextInt(5);
        int maxH = 2 + rand.nextInt(3);
        boolean xAxis = rand.nextBoolean();

        for (int i = 0; i < length; i++) {
            int wx = xAxis ? x + i : x;
            int wz = xAxis ? z : z + i;
            int h = maxH - rand.nextInt(2);
            if (rand.nextFloat() < 0.2f) continue; // gap

            for (int dy = 0; dy < h; dy++) {
                placeSafe(level, new BlockPos(wx, surfY + dy, wz), getRandomBrick(rand));
            }
        }

        // L-bend
        if (rand.nextFloat() < 0.4f) {
            int bendLen = 2 + rand.nextInt(3);
            for (int i = 1; i <= bendLen; i++) {
                int wx = xAxis ? x : x + i;
                int wz = xAxis ? z + i : z;
                int h = Math.max(1, maxH - 1 - rand.nextInt(2));
                for (int dy = 0; dy < h; dy++) {
                    placeSafe(level, new BlockPos(wx, surfY + dy, wz), getRandomBrick(rand));
                }
            }
        }
    }

    /**
     * 2-4 freestanding pillars, 3-6 tall, scattered in a ~6-block radius.
     * Top and bottom blocks use dread stone tile for a "capped" look.
     */
    public static void placePillars(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        int count = 2 + rand.nextInt(3);
        for (int p = 0; p < count; p++) {
            int px = x + rand.nextInt(6) - 3;
            int pz = z + rand.nextInt(6) - 3;
            int h = 3 + rand.nextInt(4);

            for (int dy = 0; dy < h; dy++) {
                BlockState b = (dy == 0 || dy == h - 1)
                        ? DreadlandsBlocks.DREAD_STONE_TILE
                        : getRandomBrick(rand);
                placeSafe(level, new BlockPos(px, surfY + dy, pz), b);
            }
        }
    }

    /**
     * Rectangular foundation outline (5-8 x 4-7).
     * Walls 1-2 high, corners get an extra block of height.
     * 25% of edge blocks are missing (collapsed).
     * Interior has a 50% chance of dread stone tile flooring.
     * 30% chance of a dread torch on the origin corner.
     */
    public static void placeFoundation(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        int sX = 5 + rand.nextInt(4);
        int sZ = 4 + rand.nextInt(4);
        int wallH = 1 + rand.nextInt(2);

        for (int i = 0; i < sX; i++) {
            for (int j = 0; j < sZ; j++) {
                boolean edge = (i == 0 || i == sX - 1 || j == 0 || j == sZ - 1);
                if (!edge) {
                    // Interior floor
                    if (rand.nextFloat() < 0.5f) {
                        placeSafe(level, new BlockPos(x + i, surfY - 1, z + j),
                                DreadlandsBlocks.DREAD_STONE_TILE);
                    }
                    continue;
                }

                if (rand.nextFloat() < 0.25f) continue; // collapsed

                boolean corner = (i == 0 || i == sX - 1) && (j == 0 || j == sZ - 1);
                int h = corner ? wallH + 1 : wallH;
                for (int dy = 0; dy < h; dy++) {
                    placeSafe(level, new BlockPos(x + i, surfY + dy, z + j), getRandomBrick(rand));
                }
            }
        }

        if (rand.nextFloat() < 0.3f) {
            BlockState torch = IafBlockRegistry.DREAD_TORCH.get().defaultBlockState();
            placeSafe(level, new BlockPos(x, surfY + wallH + 1, z), torch);
        }
    }

    /**
     * Single archway: two pillars (3-5 tall) connected by a horizontal span (3-5 wide).
     * Right pillar may be 0-1 blocks shorter (broken).
     * Span has ~15% per-block chance of decay gaps.
     */
    public static void placeArchway(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        int h = 3 + rand.nextInt(3);
        int span = 3 + rand.nextInt(3);
        boolean xAxis = rand.nextBoolean();

        // Left pillar
        for (int dy = 0; dy < h; dy++) {
            placeSafe(level, new BlockPos(x, surfY + dy, z), getRandomBrick(rand));
        }

        // Right pillar
        int rx = xAxis ? x + span : x;
        int rz = xAxis ? z : z + span;
        int rh = h - rand.nextInt(2);
        for (int dy = 0; dy < rh; dy++) {
            placeSafe(level, new BlockPos(rx, surfY + dy, rz), getRandomBrick(rand));
        }

        // Horizontal span
        if (rh >= h - 1) {
            for (int i = 0; i <= span; i++) {
                int sx = xAxis ? x + i : x;
                int sz = xAxis ? z : z + i;
                if (rand.nextFloat() < 0.15f) continue; // decay
                placeSafe(level, new BlockPos(sx, surfY + h, sz), DreadlandsBlocks.DREAD_STONE_BRICKS);
            }
        }
    }
}