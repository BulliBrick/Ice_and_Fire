package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueenSkull;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemDreadQueenStaff extends Item {

    public ItemDreadQueenStaff() {
        super(new Item.Properties().tab(IceAndFire.TAB_ITEMS).stacksTo(1));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == IafItemRegistry.DREAD_SHARD.get() || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, @NotNull InteractionHand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        if (!worldIn.isClientSide) {
            playerIn.startUsingItem(hand);
            playerIn.swing(hand);
            double d2 = playerIn.getLookAngle().x;
            double d3 = playerIn.getLookAngle().y;
            double d4 = playerIn.getLookAngle().z;
            float inaccuracy = 1.0F;
            d2 = d2 + playerIn.getRandom().nextGaussian() * 0.007499999832361937D * inaccuracy;
            d3 = d3 + playerIn.getRandom().nextGaussian() * 0.007499999832361937D * inaccuracy;
            d4 = d4 + playerIn.getRandom().nextGaussian() * 0.007499999832361937D * inaccuracy;
            EntityDreadQueenSkull charge = new EntityDreadQueenSkull(IafEntityRegistry.DREAD_QUEEN_SKULL.get(), worldIn,
                    playerIn, 6);
            charge.shoot(playerIn.getXRot(), playerIn.getYRot(), 0.0F, 7.0F, 1.0F);
            charge.setPos(playerIn.getX(), playerIn.getY() + 1, playerIn.getZ());
            worldIn.addFreshEntity(charge);
            charge.shoot(d2, d3, d4, 1, 1);
            playerIn.playSound(SoundEvents.ZOMBIE_INFECT, 1F, 0.75F + 0.5F * playerIn.getRandom().nextFloat());
            itemStackIn.hurtAndBreak(1, playerIn, (player) -> {
                player.broadcastBreakEvent(hand);
            });
            playerIn.getCooldowns().addCooldown(this, 4);
        }
        return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, itemStackIn);
    }

}
