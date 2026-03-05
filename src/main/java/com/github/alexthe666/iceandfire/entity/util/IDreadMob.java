package com.github.alexthe666.iceandfire.entity.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface IDreadMob {

    void performRangedAttack(@NotNull LivingEntity target, float distanceFactor);

    Entity getCommander();
}
