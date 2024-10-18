package com.github.alexthe666.iceandfire.world.biome;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.*;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.Features;
import net.minecraft.world.gen.surfacebuilders.ConfiguredSurfaceBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class BiomeDreadLands {
    public static final DeferredRegister<Biome> BIOMES
            = DeferredRegister.create(ForgeRegistries.BIOMES, "iceandfire");

    public static final RegistryObject<Biome> DREADLANDS_BIOME = BIOMES.register("dreadlands_bimome",
            () -> makeDreadLandsBiome(() -> DreadLandsSurfaceBuilder.DREADLANDS_SURFACE, 0.125F, 0.05F));

    public static Biome makeDreadLandsBiome(final Supplier<ConfiguredSurfaceBuilder<?>> surfaceBuilder, float depth, float scale) {
        MobSpawnInfo.Builder mobSpawnInfo$Builder = new MobSpawnInfo.Builder();
        BiomeGenerationSettings.Builder biomeGenerationSettings$Builder =
                (new BiomeGenerationSettings.Builder()).withSurfaceBuilder(surfaceBuilder);

        DefaultBiomeFeatures.withPassiveMobs(mobSpawnInfo$Builder);
        DefaultBiomeFeatures.withBatsAndHostiles(mobSpawnInfo$Builder);
        mobSpawnInfo$Builder.withSpawner(EntityClassification.CREATURE,
                new MobSpawnInfo.Spawners(EntityType.PIG, 100, 5, 10)
        );
        mobSpawnInfo$Builder.withSpawner(EntityClassification.CREATURE,
                new MobSpawnInfo.Spawners(EntityType.SHEEP, 10, 2, 8)
        );

        DefaultBiomeFeatures.withFossils(biomeGenerationSettings$Builder);
        biomeGenerationSettings$Builder.withFeature(GenerationStage.Decoration.SURFACE_STRUCTURES, Features.FOSSIL);

        DefaultBiomeFeatures.withForestRocks(biomeGenerationSettings$Builder);
        DefaultBiomeFeatures.withForestRocks(biomeGenerationSettings$Builder);
        DefaultBiomeFeatures.withCavesAndCanyons(biomeGenerationSettings$Builder);

        return (new Biome.Builder()).precipitation(Biome.RainType.SNOW).category(Biome.Category.ICY).depth(depth).scale(scale)
                .temperature(1.5F).downfall(0.9F).setEffects((new BiomeAmbience.Builder()).setWaterColor(-3407872).setWaterFogColor(-16777216)
                        .setFogColor(-65536).withSkyColor(getSkyColorWithTemperatureModifier(0.8F)).withFoliageColor(-3407872).withGrassColor(-3407872)
                        .setParticle(new ParticleEffectAmbience(ParticleTypes.WHITE_ASH, 0.003f)).withSkyColor(-65536)
                        .setAmbientSound(SoundEvents.AMBIENT_CAVE)
                        .setMoodSound(new MoodSoundAmbience(SoundEvents.AMBIENT_WARPED_FOREST_MOOD, 6000,8,2.00D))
                        .setAdditionsSound(new SoundAdditionsAmbience(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.0111D))
                        .build())
                .withMobSpawnSettings(mobSpawnInfo$Builder.build()).withGenerationSettings(biomeGenerationSettings$Builder.build()).build();
    }

    private static int getSkyColorWithTemperatureModifier(float temperature) {
        float lvt_1_1 = temperature / 3.0F;
        lvt_1_1 = MathHelper.clamp(lvt_1_1, -1.0F, 1.0F);
        return MathHelper.hsvToRGB(0.2460909F - lvt_1_1 * 0.05F, 0.05F + lvt_1_1 * 0.1F, 1.0F);
    }

    public static void register(IEventBus eventBus) {
        BIOMES.register(eventBus);
    }
}
