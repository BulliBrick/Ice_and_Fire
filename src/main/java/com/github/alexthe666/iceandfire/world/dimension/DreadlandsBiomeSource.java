package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;

/**
 * Custom BiomeSource for the Dreadlands dimension.
 *
 * Distributes 5 biomes using two noise channels to create organic, large-scale regions:
 *   - Dreadlands Wastes (default, ~35%)
 *   - Dreaded Spikes (~15%)
 *   - Dread Dead Forest (~20%)
 *   - Frozen Dread Plains (~15%)
 *   - Dread Crags (~15%)
 */
public class DreadlandsBiomeSource extends BiomeSource {

    public static final ResourceKey<Biome> DREADLANDS_WASTES =
            ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(IceAndFire.MODID, "dreadlands_wastes"));
    public static final ResourceKey<Biome> DREADED_SPIKES =
            ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(IceAndFire.MODID, "dreaded_spikes"));
    public static final ResourceKey<Biome> DREAD_DEAD_FOREST =
            ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(IceAndFire.MODID, "dreaded_forest"));
    public static final ResourceKey<Biome> FROZEN_DREAD_PLAINS =
            ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(IceAndFire.MODID, "frozen_dread_plains"));
    public static final ResourceKey<Biome> DREAD_CRAGS =
            ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(IceAndFire.MODID, "dread_crags"));

    public static final Codec<DreadlandsBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY)
                            .forGetter(src -> src.biomeRegistry)
            ).apply(instance, DreadlandsBiomeSource::new)
    );

    private final Registry<Biome> biomeRegistry;
    private final Holder<Biome> wastesHolder;
    private final Holder<Biome> spikesHolder;
    private final Holder<Biome> forestHolder;
    private final Holder<Biome> frozenHolder;
    private final Holder<Biome> cragsHolder;

    public DreadlandsBiomeSource(Registry<Biome> biomeRegistry) {
        super(List.of(
                biomeRegistry.getHolderOrThrow(DREADLANDS_WASTES),
                biomeRegistry.getHolderOrThrow(DREADED_SPIKES),
                biomeRegistry.getHolderOrThrow(DREAD_DEAD_FOREST),
                biomeRegistry.getHolderOrThrow(FROZEN_DREAD_PLAINS),
                biomeRegistry.getHolderOrThrow(DREAD_CRAGS)
        ));
        this.biomeRegistry = biomeRegistry;
        this.wastesHolder = biomeRegistry.getHolderOrThrow(DREADLANDS_WASTES);
        this.spikesHolder = biomeRegistry.getHolderOrThrow(DREADED_SPIKES);
        this.forestHolder = biomeRegistry.getHolderOrThrow(DREAD_DEAD_FOREST);
        this.frozenHolder = biomeRegistry.getHolderOrThrow(FROZEN_DREAD_PLAINS);
        this.cragsHolder = biomeRegistry.getHolderOrThrow(DREAD_CRAGS);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return this;
    }

    /**
     * Called by the chunk generator to determine the biome at biome coordinates.
     * Biome coordinates are block coordinates >> 2 (divided by 4).
     *
     * Two independent noise channels with large scale for big biome patches:
     *   noise1 high + noise2 high  → Frozen Dread Plains (cold + wet = heavy snow)
     *   noise1 high + noise2 low   → Dreaded Spikes (cold + dry = exposed rock/ice)
     *   noise1 low  + noise2 high  → Dread Dead Forest (warmer + wet = growth)
     *   noise1 low  + noise2 low   → Dread Crags (warmer + dry = rocky)
     *   middle range               → Dreadlands Wastes (default)
     */
    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ, Climate.Sampler sampler) {
        double x = biomeX * 4.0;
        double z = biomeZ * 4.0;

        double noise1 = biomeNoise(x / 350.0, z / 350.0, 0);
        double noise2 = biomeNoise(x / 350.0, z / 350.0, 31337);

        // Wastes occupies the center band
        if (Math.abs(noise1) < 0.3 && Math.abs(noise2) < 0.3) {
            return wastesHolder;
        }

        // Quadrant selection
        if (noise1 > 0.3) {
            if (noise2 > 0.2) return frozenHolder;
            if (noise2 < -0.2) return spikesHolder;
        } else if (noise1 < -0.3) {
            if (noise2 > 0.2) return forestHolder;
            if (noise2 < -0.2) return cragsHolder;
        }

        return wastesHolder;
    }

    /**
     * Deterministic noise using sine mixing. Cheap, good enough for biome-scale.
     */
    private static double biomeNoise(double x, double z, long seedOffset) {
        double v = 0;
        v += Math.sin(x * 1.0 + seedOffset * 0.1) * Math.cos(z * 1.3 + seedOffset * 0.07) * 0.5;
        v += Math.sin(x * 2.3 + z * 1.7 + seedOffset * 0.13) * 0.3;
        v += Math.cos(x * 0.7 - z * 2.1 + seedOffset * 0.17) * 0.2;
        return Math.max(-1.0, Math.min(1.0, v));
    }
}