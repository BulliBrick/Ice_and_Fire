package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDreadPortal;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.enums.EnumParticles;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsPortalData;
import com.github.alexthe666.iceandfire.world.dimension.DreadlandsTeleporter;
import com.github.alexthe666.iceandfire.world.dimension.IafDimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;

import static com.github.alexthe666.iceandfire.entity.tile.IafTileEntityRegistry.DREAD_PORTAL;

public class BlockDreadPortal extends BaseEntityBlock implements IDreadBlock {

    /**
     * Whether this portal is active (can teleport). Set to false during boss fights.
     */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * Cooldown in ticks before a player can use the portal again after arriving.
     * Prevents instant re-teleport loops.
     */
    private static final long PORTAL_COOLDOWN_TICKS = 80L; // 4 seconds

    private static final String TAG_LAST_PORTAL_USE = "DreadPortalLastUse";

    public BlockDreadPortal() {
        super(
                Properties
                        .of(Material.PORTAL)
                        .dynamicShape()
                        .strength(-1, 100000)
                        .lightLevel((state) -> state.getValue(ACTIVE) ? 8 : 1)
                        .randomTicks()
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos,
                             @NotNull Entity entity) {
        if (worldIn.isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isPassenger() || player.isVehicle()) return;

        // Portal cooldown via persistent NBT — avoids relying on mapped vanilla field names
        CompoundTag playerData = player.getPersistentData();
        long now = worldIn.getGameTime();
        long lastUse = playerData.getLong(TAG_LAST_PORTAL_USE);
        if (now - lastUse < PORTAL_COOLDOWN_TICKS) {
            return;
        }

        // Check if portal block is active
        if (!state.getValue(ACTIVE)) return;

        // Check world-level portal data if we're in the Dreadlands
        if (worldIn.dimension() == IafDimensionRegistry.DREADLANDS_LEVEL) {
            DreadlandsPortalData portalData = DreadlandsPortalData.get((ServerLevel) worldIn);
            if (!portalData.arePortalsActive()) return;
        }

        ServerLevel currentLevel = (ServerLevel) worldIn;
        ServerLevel destLevel;

        if (currentLevel.dimension() == IafDimensionRegistry.DREADLANDS_LEVEL) {
            // In Dreadlands → go to Overworld
            destLevel = currentLevel.getServer().getLevel(Level.OVERWORLD);
        } else {
            // In Overworld (or anywhere else) → go to Dreadlands
            destLevel = currentLevel.getServer().getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);
        }

        if (destLevel == null) return;

        // Set cooldown and teleport
        playerData.putLong(TAG_LAST_PORTAL_USE, now);
        player.changeDimension(destLevel, new DreadlandsTeleporter(destLevel));
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos,
                                @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving) {
        if (!this.canSurviveAt(worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
        }
    }

    public boolean canSurviveAt(Level world, BlockPos pos) {
        return DragonUtils.isDreadBlock(world.getBlockState(pos.above()))
                && DragonUtils.isDreadBlock(world.getBlockState(pos.below()));
    }

    @Override
    public void animateTick(@NotNull BlockState stateIn, @NotNull Level worldIn, @NotNull BlockPos pos,
                            @NotNull Random rand) {
        // Only show particles when active
        if (!stateIn.getValue(ACTIVE)) return;

        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        if (tileentity instanceof TileEntityDreadPortal) {
            int i = 3;
            for (int j = 0; j < i; ++j) {
                double d0 = (float) pos.getX() + rand.nextFloat();
                double d1 = (float) pos.getY() + rand.nextFloat();
                double d2 = (float) pos.getZ() + rand.nextFloat();
                double d3 = ((double) rand.nextFloat() - 0.5D) * 0.25D;
                double d4 = ((double) rand.nextFloat()) * -0.25D;
                double d5 = ((double) rand.nextFloat() - 0.5D) * 0.25D;
                IceAndFire.PROXY.spawnParticle(EnumParticles.Dread_Portal, d0, d1, d2, d3, d4, d5);
            }
        }
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> entityType) {
        return createTickerHelper(entityType, DREAD_PORTAL.get(), TileEntityDreadPortal::tick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TileEntityDreadPortal(pos, state);
    }
}