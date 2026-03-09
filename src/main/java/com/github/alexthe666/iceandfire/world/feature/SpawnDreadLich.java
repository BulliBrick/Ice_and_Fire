
package com.github.alexthe666.iceandfire.world.feature;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDreadLich;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.util.WorldUtil;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class SpawnDreadLich extends Feature<NoneFeatureConfiguration> {

    public SpawnDreadLich(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Random random = context.random();
        BlockPos origin = context.origin();

        if (!IafConfig.spawnLiches) {
            return false;
        }

        if (!WorldUtil.canGenerate(IafConfig.lichSpawnRate, level, random, origin, "dread_lich", false)) {
            return false;
        }

        EntityDreadLich lich = IafEntityRegistry.DREAD_LICH.get().create(level.getLevel());
        if (lich == null) {
            return false;
        }

        lich.moveTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);

        if (!EntityDreadLich.canLichSpawnOn(IafEntityRegistry.DREAD_LICH.get(), level, MobSpawnType.CHUNK_GENERATION, origin, random)) {
            lich.discard();
            return false;
        }

        lich.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), MobSpawnType.CHUNK_GENERATION, null, null);
        level.addFreshEntityWithPassengers(lich);

        return true;
    }
}
