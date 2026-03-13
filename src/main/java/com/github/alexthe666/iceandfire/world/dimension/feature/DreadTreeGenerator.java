package com.github.alexthe666.iceandfire.world.dimension.feature;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Generates dead dreadwood trees for the Dreadlands dimension.
 *
 * Tree types:
 *   - Standing dead tree: straight trunk + 2-4 bare branches angled upward
 *   - Fallen log: horizontal trunk on the ground
 *
 * All trees use dreadwood logs with axis rotation for branches.
 * No leaves — these are skeletal dead trees.
 */
public final class DreadTreeGenerator {

    private DreadTreeGenerator() {}

    /**
     * Places a standing dead tree.
     *
     * @param minH Minimum trunk height (inclusive)
     * @param maxH Maximum trunk height (inclusive)
     */
    public static void placeDeadTree(WorldGenRegion level, int x, int surfaceY, int z,
                                     Random rand, int minH, int maxH) {
        DreadlandsBlocks.ensureResolved();
        int height = minH + rand.nextInt(maxH - minH + 1);

        // Trunk — vertical dreadwood log
        for (int dy = 0; dy < height; dy++) {
            DreadlandsPlacement.placeSafe(level, new BlockPos(x, surfaceY + dy, z),
                    DreadlandsBlocks.DREADWOOD_LOG);
        }

        // Branches — 2-4, from upper half of trunk, angled upward
        int branchCount = 2 + rand.nextInt(3);
        for (int b = 0; b < branchCount; b++) {
            int branchY = surfaceY + (height / 2) + rand.nextInt(Math.max(1, height / 2));
            int branchLen = 2 + rand.nextInt(3);

            Direction.Axis axis;
            int dx, dz;
            switch (rand.nextInt(4)) {
                case 0 -> { dx = 1; dz = 0; axis = Direction.Axis.X; }
                case 1 -> { dx = -1; dz = 0; axis = Direction.Axis.X; }
                case 2 -> { dx = 0; dz = 1; axis = Direction.Axis.Z; }
                default -> { dx = 0; dz = -1; axis = Direction.Axis.Z; }
            }

            BlockState branchLog = IafBlockRegistry.DREADWOOD_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);

            for (int i = 1; i <= branchLen; i++) {
                DreadlandsPlacement.placeSafe(level,
                        new BlockPos(x + dx * i, branchY + (i / 2), z + dz * i), branchLog);
            }
        }
    }

    /**
     * Places a fallen log on the ground surface.
     * 4-8 blocks long, oriented randomly along X or Z axis.
     */
    public static void placeFallenLog(WorldGenRegion level, int x, int surfY, int z, Random rand) {
        DreadlandsBlocks.ensureResolved();
        boolean xAxis = rand.nextBoolean();
        int length = 4 + rand.nextInt(5);
        Direction.Axis axis = xAxis ? Direction.Axis.X : Direction.Axis.Z;
        BlockState log = IafBlockRegistry.DREADWOOD_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis);

        for (int i = 0; i < length; i++) {
            int lx = xAxis ? x + i : x;
            int lz = xAxis ? z : z + i;
            DreadlandsPlacement.placeSafe(level, new BlockPos(lx, surfY, lz), log);
        }
    }
}