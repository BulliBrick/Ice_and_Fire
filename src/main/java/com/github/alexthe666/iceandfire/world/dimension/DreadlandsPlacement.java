package com.github.alexthe666.iceandfire.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks.*;

/**
 * Shared placement utilities for Dreadlands feature generators.
 *
 * All feature classes (trees, ruins, spikes, etc.) should use these helpers
 * rather than implementing their own bounds-checking and block selection.
 */
public final class DreadlandsPlacement {

    private DreadlandsPlacement() {}

    /**
     * Place a block only if the target position is air, snow, or powder snow.
     * Silently ignores out-of-bounds positions (expected at WorldGenRegion edges).
     */
    public static void placeSafe(WorldGenRegion level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return;
        try {
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir()
                    || existing.getBlock() == Blocks.SNOW_BLOCK
                    || existing.getBlock() == Blocks.POWDER_SNOW) {
                level.setBlock(pos, state, 2);
            }
        } catch (Exception ignored) {
            // Out of WorldGenRegion bounds — expected at chunk edges
        }
    }

    /**
     * Force-place a block, overwriting whatever is there (except bedrock).
     * Used for terrain features that should replace surface blocks (e.g., ice patches).
     */
    public static void placeForce(WorldGenRegion level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return;
        try {
            BlockState existing = level.getBlockState(pos);
            if (existing.getBlock() != Blocks.BEDROCK) {
                level.setBlock(pos, state, 2);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Picks a random dread stone brick variant with weathering distribution:
     *   45% intact bricks, 25% cracked, 15% mossy, 15% tile
     */
    public static BlockState getRandomBrick(Random rand) {
        float r = rand.nextFloat();
        if (r < 0.45f) return DREAD_STONE_BRICKS;
        if (r < 0.70f) return DREAD_STONE_BRICKS_CRACKED;
        if (r < 0.85f) return DREAD_STONE_BRICKS_MOSSY;
        return DREAD_STONE_TILE;
    }
}