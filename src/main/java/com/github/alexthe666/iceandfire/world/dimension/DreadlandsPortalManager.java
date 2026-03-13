package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.block.BlockDreadPortal;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Manages the global activation/deactivation of dread portals during boss fights.
 *
 * Two-layer approach:
 *   1. DreadlandsPortalData (SavedData) — persistent flag checked by BlockDreadPortal.entityInside().
 *      This is the AUTHORITATIVE source. Covers ALL portals, even in unloaded chunks.
 *   2. Visual update — scans blocks near online players in the Dreadlands to flip
 *      the ACTIVE block state on nearby portal blocks. This gives immediate visual
 *      feedback (light level, particles) for portals players can actually see.
 *
 * Portals far from any player keep their old block state, but entityInside() still
 * checks the SavedData flag and refuses teleportation regardless.
 */
public class DreadlandsPortalManager {

    /**
     * Radius around each player to scan for portal blocks during visual update.
     * 64 blocks covers a reasonable view distance.
     */
    private static final int SCAN_RADIUS_XZ = 64;
    private static final int SCAN_RADIUS_Y = 16;

    /**
     * Call when the Dread Queen boss fight is triggered.
     * Deactivates all portals in the Dreadlands.
     */
    public static void deactivateAllPortals(MinecraftServer server) {
        ServerLevel dreadlands = server.getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);
        if (dreadlands == null) return;

        DreadlandsPortalData data = DreadlandsPortalData.get(dreadlands);
        data.onBossFightStart();

        updatePortalVisualsNearPlayers(server, dreadlands, false);
    }

    /**
     * Call when the Dread Queen is defeated.
     * Reactivates all portals in the Dreadlands.
     */
    public static void reactivateAllPortals(MinecraftServer server) {
        ServerLevel dreadlands = server.getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);
        if (dreadlands == null) return;

        DreadlandsPortalData data = DreadlandsPortalData.get(dreadlands);
        data.onBossFightEnd();

        updatePortalVisualsNearPlayers(server, dreadlands, true);
    }

    /**
     * Check if portals are currently deactivated in the Dreadlands.
     */
    public static boolean arePortalsActive(MinecraftServer server) {
        ServerLevel dreadlands = server.getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);
        if (dreadlands == null) return true;
        return DreadlandsPortalData.get(dreadlands).arePortalsActive();
    }

    /**
     * Scans blocks near each player in the Dreadlands dimension and updates
     * portal block states. Uses only public APIs:
     *   - server.getPlayerList().getPlayers() for player iteration
     *   - level.getBlockState() / level.setBlock() for block access
     *
     * This only runs on boss fight start/end (not per-tick), so the scan cost
     * is acceptable. For a dimension with very few portal blocks, this
     * completes almost instantly.
     */
    private static void updatePortalVisualsNearPlayers(MinecraftServer server,
                                                       ServerLevel dreadlands,
                                                       boolean active) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level != dreadlands) continue;

            BlockPos playerPos = player.blockPosition();
            int minX = playerPos.getX() - SCAN_RADIUS_XZ;
            int maxX = playerPos.getX() + SCAN_RADIUS_XZ;
            int minY = Math.max(dreadlands.getMinBuildHeight(), playerPos.getY() - SCAN_RADIUS_Y);
            int maxY = Math.min(dreadlands.getMaxBuildHeight(), playerPos.getY() + SCAN_RADIUS_Y);
            int minZ = playerPos.getZ() - SCAN_RADIUS_XZ;
            int maxZ = playerPos.getZ() + SCAN_RADIUS_XZ;

            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            // Coarse scan: step by 4 to find portal blocks quickly, then no fine pass needed
            // since portal blocks are placed in small clusters (1-3 blocks typically)
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y < maxY; y++) {
                        mutable.set(x, y, z);

                        if (!dreadlands.isLoaded(mutable)) continue;

                        BlockState state = dreadlands.getBlockState(mutable);
                        if (state.getBlock() == IafBlockRegistry.DREAD_PORTAL.get()) {
                            if (state.getValue(BlockDreadPortal.ACTIVE) != active) {
                                dreadlands.setBlock(mutable, state.setValue(BlockDreadPortal.ACTIVE, active), 3);
                            }
                        }
                    }
                }
            }
        }
    }
}