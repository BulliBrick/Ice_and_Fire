package com.github.alexthe666.iceandfire.entity;

import javax.annotation.Nullable;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.ai.DreadAITargetNonDread;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.entity.util.IAnimalFear;
import com.github.alexthe666.iceandfire.entity.util.IDreadMob;
import com.github.alexthe666.iceandfire.entity.util.IVillagerFear;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.google.common.base.Predicate;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;

public class EntityDreadQueen extends EntityDreadMob implements IAnimatedEntity, IVillagerFear, IAnimalFear, IRangedAttackMob {

    public static Animation ANIMATION_SPAWN = Animation.create(40);
    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityDreadQueen.class, DataSerializers.VARINT);
    private int fireCooldown = 0;

    private final ServerBossInfo bossInfo = (new ServerBossInfo(this.getDisplayName(), BossInfo.Color.BLUE, BossInfo.Overlay.PROGRESS));
    private int animationTick;
    private Animation currentAnimation;

    public EntityDreadQueen(EntityType t, World worldIn) {
        super(t, worldIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, IDreadMob.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, new Predicate<LivingEntity>() {
            @Override
            public boolean apply(@Nullable LivingEntity entity) {
                return DragonUtils.canHostilesTarget(entity);
            }
        }));
        this.targetSelector.addGoal(3, new DreadAITargetNonDread(this, LivingEntity.class, false, new Predicate<LivingEntity>() {
            @Override
            public boolean apply(LivingEntity entity) {
                return entity instanceof LivingEntity && DragonUtils.canHostilesTarget(entity);
            }
        }));
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MobEntity.func_233666_p_()
                //HEALTH
                .createMutableAttribute(Attributes.MAX_HEALTH, IafConfig.dreadQueenMaxHealth)
                //SPEED
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.25D)
                //ATTACK
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 5D)
                //FOLLOW RANGE
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 256.0D)
                //ARMOR
                .createMutableAttribute(Attributes.ARMOR, 25.0D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(VARIANT, Integer.valueOf(0));
    }

    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);

        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
    }

    protected void updateAITasks() {
        super.updateAITasks();
        this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
    }

    public void setCustomName(ITextComponent name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }


    public void addTrackingPlayer(ServerPlayerEntity player) {
        super.addTrackingPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    public void removeTrackingPlayer(ServerPlayerEntity player) {
        super.removeTrackingPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    public void adjustAttributesBasedOnDistance(LivingEntity target) {
        double distance = this.getDistance(target);

        // Adjust movement speed based on distance
        double newSpeed = calculateSpeedBasedOnDistance(distance);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);

        // Adjust range attack cooldown based on distance
        int newCooldown = calculateCooldownBasedOnDistance(distance);
        this.setFireCooldown(newCooldown);
    }

    private double calculateSpeedBasedOnDistance(double distance) {
        //Increase speed as distance increases
        if (distance < 10) {
            return 0.2D; // High speed
        } else if (distance < 30) {
            return 0.3D; // Medium speed
        } else {
            return 0.5D; // Low speed
        }
    }

    private int calculateCooldownBasedOnDistance(double distance) {
        // Decrease cooldown as distance increases
        if (distance < 20) {
            return 60; // Long cooldown
        } else if (distance < 50) {
            return 40; // Medium cooldown
        } else {
            return 20; // Short cooldown
        }
    }

    private void setFireCooldown(int fireCooldown) {
        this.fireCooldown = fireCooldown;
    }


    @Nullable
    public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        ILivingEntityData data = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setAnimation(ANIMATION_SPAWN);
        this.setEquipmentBasedOnDifficulty(difficultyIn);
        return data;
    }

    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        super.setEquipmentBasedOnDifficulty(difficulty);
        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD));
        this.setItemStackToSlot(EquipmentSlotType.OFFHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF));
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_SPAWN};
    }

    @Override
    public boolean shouldAnimalsFear(Entity entity) {
        return true;
    }

    @Override
    public boolean shouldFear() {
        return true;
    }

    @Override
    public boolean isNoDespawnRequired() {
        return true;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void attackEntityWithRangedAttack(LivingEntity target, float distanceFactor) {
        if (fireCooldown == 0 && !(target instanceof IDreadMob)) {
            this.swingArm(Hand.OFF_HAND);
            this.playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, this.getSoundVolume(), this.getSoundPitch());

            EntityDreadQueenSkull skull = new EntityDreadQueenSkull(IafEntityRegistry.DREAD_QUEEN_SKULL.get(), world, this, 6);
            double d0 = target.getPosX() - this.getPosX();
            double d1 = target.getBoundingBox().minY + target.getHeight() * 0.5 - skull.getPosY();
            double d2 = target.getPosZ() - this.getPosZ();
            double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
            skull.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, 14 - this.world.getDifficulty().getId() * 4);

            this.world.addEntity(skull);
            fireCooldown = 100;
        }
    }

}
