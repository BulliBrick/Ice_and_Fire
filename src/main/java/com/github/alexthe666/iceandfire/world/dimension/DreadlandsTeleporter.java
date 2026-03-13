package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handles teleportation between the Overworld and the Dreadlands.
 *
 * Overworld → Dreadlands: 1:1 coordinate mapping. Searches for an existing
 * dread portal block near the target position, or places the player at
 * the surface if none found.
 *
 * Dreadlands → Overworld: Uses stored entry position from the player's
 * persistent data, falling back to world spawn.
 */
public class DreadlandsTeleporter implements ITeleporter {

    private static final String TAG_ENTRY_X = "DreadlandsEntryX";
    private static final String TAG_ENTRY_Y = "DreadlandsEntryY";
    private static final String TAG_ENTRY_Z = "DreadlandsEntryZ";
    private static final int PORTAL_SEARCH_RADIUS = 64;

    private final ServerLevel destination;

    public DreadlandsTeleporter(ServerLevel destination) {
        this.destination = destination;
    }

    @Override
    @Nullable
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        // 1:1 coordinate mapping
        WorldBorder border = destWorld.getWorldBorder();
        double x = entity.getX();
        double z = entity.getZ();

        // Clamp to world border
        x = Math.max(border.getMinX() + 16, Math.min(border.getMaxX() - 16, x));
        z = Math.max(border.getMinZ() + 16, Math.min(border.getMaxZ() - 16, z));

        boolean goingToDreadlands = destWorld.dimension() == IafDimensionRegistry.DREADLANDS_LEVEL;

        if (goingToDreadlands) {
            // Store entry position before teleporting
            if (entity instanceof ServerPlayer player) {
                player.getPersistentData().putInt(TAG_ENTRY_X, player.blockPosition().getX());
                player.getPersistentData().putInt(TAG_ENTRY_Y, player.blockPosition().getY());
                player.getPersistentData().putInt(TAG_ENTRY_Z, player.blockPosition().getZ());
            }
        }

        BlockPos targetPos;

        if (!goingToDreadlands && entity instanceof ServerPlayer player) {
            // Returning to overworld — use stored entry position
            if (player.getPersistentData().contains(TAG_ENTRY_X)) {
                targetPos = new BlockPos(
                        player.getPersistentData().getInt(TAG_ENTRY_X),
                        player.getPersistentData().getInt(TAG_ENTRY_Y),
                        player.getPersistentData().getInt(TAG_ENTRY_Z)
                );
            } else {
                targetPos = destWorld.getSharedSpawnPos();
            }
        } else {
            targetPos = new BlockPos(x, 64, z);
        }

        // Search for an existing portal near the target
        Optional<BlockPos> portalPos = findPortalNear(destWorld, targetPos);

        Vec3 landingPos;
        if (portalPos.isPresent()) {
            BlockPos pp = portalPos.get();
            // Land on top of or adjacent to the portal
            landingPos = new Vec3(pp.getX() + 0.5, pp.getY() + 1.0, pp.getZ() + 0.5);
        } else if (goingToDreadlands) {
            // No portal found — land at surface level
            int surfaceY = findSurface(destWorld, (int) x, (int) z);
            landingPos = new Vec3(x, surfaceY + 1.0, z);
        } else {
            landingPos = new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        }

        return new PortalInfo(landingPos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                              float yaw, Function<Boolean, Entity> repositionEntity) {
        return repositionEntity.apply(false);
    }

    /**
     * Searches for a dread portal block near the given position.
     */
    private Optional<BlockPos> findPortalNear(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockState portalState = IafBlockRegistry.DREAD_PORTAL.get().defaultBlockState();
        double closestDist = Double.MAX_VALUE;
        BlockPos closestPortal = null;

        int minY = Math.max(level.getMinBuildHeight(), center.getY() - 32);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + 32);

        for (int dx = -PORTAL_SEARCH_RADIUS; dx <= PORTAL_SEARCH_RADIUS; dx += 4) {
            for (int dz = -PORTAL_SEARCH_RADIUS; dz <= PORTAL_SEARCH_RADIUS; dz += 4) {
                for (int y = minY; y < maxY; y += 2) {
                    mutable.set(center.getX() + dx, y, center.getZ() + dz);
                    if (level.getBlockState(mutable).getBlock() == IafBlockRegistry.DREAD_PORTAL.get()) {
                        double dist = mutable.distSqr(center);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestPortal = mutable.immutable();
                        }
                    }
                }
            }
        }

        // If coarse search found something, do a fine-grained search nearby
        if (closestPortal != null) {
            BlockPos coarse = closestPortal;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int y = Math.max(minY, coarse.getY() - 4); y <= Math.min(maxY, coarse.getY() + 4); y++) {
                        mutable.set(coarse.getX() + dx, y, coarse.getZ() + dz);
                        if (level.getBlockState(mutable).getBlock() == IafBlockRegistry.DREAD_PORTAL.get()) {
                            double dist = mutable.distSqr(center);
                            if (dist < closestDist) {
                                closestDist = dist;
                                closestPortal = mutable.immutable();
                            }
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(closestPortal);
    }

    /**
     * Finds the highest solid block at the given xz in the destination.
     */
    private int findSurface(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        while (mutable.getY() > level.getMinBuildHeight() && level.getBlockState(mutable).isAir()) {
            mutable.move(Direction.DOWN);
        }
        return mutable.getY();
    }
}