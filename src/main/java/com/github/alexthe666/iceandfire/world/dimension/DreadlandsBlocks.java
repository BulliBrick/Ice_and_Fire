package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Central block palette for the Dreadlands dimension.
 *
 * All block states are lazy-resolved on first access because RegistryObjects
 * aren't available during codec deserialization. Thread-safe via synchronized init.
 *
 * Any class that needs dreadlands blocks should reference DreadlandsBlocks.XXX
 * rather than resolving IafBlockRegistry themselves.
 */
public final class DreadlandsBlocks {

    private DreadlandsBlocks() {} // utility class

    // ─── Dread stone family ───────────────────────────────────────────────
    public static BlockState DREAD_STONE;
    public static BlockState DREAD_STONE_BRICKS;
    public static BlockState DREAD_STONE_BRICKS_CRACKED;
    public static BlockState DREAD_STONE_BRICKS_MOSSY;
    public static BlockState DREAD_STONE_TILE;
    public static BlockState DREAD_STONE_FACE;

    // ─── Dreadwood ────────────────────────────────────────────────────────
    public static BlockState DREADWOOD_LOG;

    // ─── Frozen blocks ────────────────────────────────────────────────────
    public static BlockState FROZEN_DIRT;
    public static BlockState FROZEN_GRASS;
    public static BlockState FROZEN_STONE;
    public static BlockState FROZEN_COBBLESTONE;
    public static BlockState FROZEN_GRAVEL;
    public static BlockState DRAGON_ICE;

    // ─── Vanilla ──────────────────────────────────────────────────────────
    public static BlockState SNOW;
    public static BlockState POWDER_SNOW;
    public static BlockState BEDROCK;
    public static BlockState AIR;
    public static BlockState GRAVEL;
    public static BlockState SOUL_SAND;
    public static BlockState SOUL_SOIL;
    public static BlockState COBWEB;
    public static BlockState PACKED_ICE;
    public static BlockState BLUE_ICE;

    private static volatile boolean resolved = false;

    /**
     * Must be called before any block state is accessed.
     * Safe to call multiple times — only resolves once.
     */
    public static synchronized void ensureResolved() {
        if (resolved) return;

        DREAD_STONE = IafBlockRegistry.DREAD_STONE.get().defaultBlockState();
        DREAD_STONE_BRICKS = IafBlockRegistry.DREAD_STONE_BRICKS.get().defaultBlockState();
        DREAD_STONE_BRICKS_CRACKED = IafBlockRegistry.DREAD_STONE_BRICKS_CRACKED.get().defaultBlockState();
        DREAD_STONE_BRICKS_MOSSY = IafBlockRegistry.DREAD_STONE_BRICKS_MOSSY.get().defaultBlockState();
        DREAD_STONE_TILE = IafBlockRegistry.DREAD_STONE_TILE.get().defaultBlockState();
        DREAD_STONE_FACE = IafBlockRegistry.DREAD_STONE_FACE.get().defaultBlockState();
        DREADWOOD_LOG = IafBlockRegistry.DREADWOOD_LOG.get().defaultBlockState();
        FROZEN_DIRT = IafBlockRegistry.FROZEN_DIRT.get().defaultBlockState();
        FROZEN_GRASS = IafBlockRegistry.FROZEN_GRASS.get().defaultBlockState();
        FROZEN_STONE = IafBlockRegistry.FROZEN_STONE.get().defaultBlockState();
        FROZEN_COBBLESTONE = IafBlockRegistry.FROZEN_COBBLESTONE.get().defaultBlockState();
        FROZEN_GRAVEL = IafBlockRegistry.FROZEN_GRAVEL.get().defaultBlockState();
        DRAGON_ICE = IafBlockRegistry.DRAGON_ICE.get().defaultBlockState();
        SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
        POWDER_SNOW = Blocks.POWDER_SNOW.defaultBlockState();
        BEDROCK = Blocks.BEDROCK.defaultBlockState();
        AIR = Blocks.AIR.defaultBlockState();
        GRAVEL = Blocks.GRAVEL.defaultBlockState();
        SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
        SOUL_SOIL = Blocks.SOUL_SOIL.defaultBlockState();
        COBWEB = Blocks.COBWEB.defaultBlockState();
        PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
        BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();

        resolved = true;
    }
}