package com.github.alexthe666.iceandfire.world.dimension;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

/**
 * Defines how each Dreadlands biome shapes terrain.
 *
 * To add a new biome:
 *   1. Add the biome JSON under data/iceandfire/worldgen/biome/
 *   2. Add its ResourceKey to DreadlandsBiomeSource
 *   3. Add an enum entry here with its TerrainProfile
 *   4. Add decoration logic in DreadlandsDecorator
 */
public enum DreadBiome {

    WASTES(new TerrainProfile(64, 12.0, 5.0, 2.0, 1.0, 0.15f, false)),
    SPIKES(new TerrainProfile(66, 8.0, 6.0, 4.0, 0.3, 0.05f, false)),
    DEAD_FOREST(new TerrainProfile(60, 6.0, 3.0, 1.5, 0.5, 0.10f, false)),
    FROZEN_PLAINS(new TerrainProfile(58, 4.0, 2.0, 1.0, 0.0, 0.40f, true)),
    CRAGS(new TerrainProfile(72, 16.0, 8.0, 3.0, 0.8, 0.05f, false));

    private final TerrainProfile profile;

    DreadBiome(TerrainProfile profile) {
        this.profile = profile;
    }

    public TerrainProfile profile() {
        return profile;
    }

    /**
     * Map a biome registry holder to the corresponding DreadBiome enum.
     * Falls back to WASTES for unknown biomes.
     */
    public static DreadBiome classify(Holder<Biome> holder) {
        Optional<ResourceKey<Biome>> key = holder.unwrapKey();
        if (key.isPresent()) {
            ResourceKey<Biome> k = key.get();
            if (k.equals(DreadlandsBiomeSource.DREADED_SPIKES)) return SPIKES;
            if (k.equals(DreadlandsBiomeSource.DREAD_DEAD_FOREST)) return DEAD_FOREST;
            if (k.equals(DreadlandsBiomeSource.FROZEN_DREAD_PLAINS)) return FROZEN_PLAINS;
            if (k.equals(DreadlandsBiomeSource.DREAD_CRAGS)) return CRAGS;
        }
        return WASTES;
    }

    /**
     * Per-biome terrain shaping parameters.
     *
     * @param baseHeight       Y level around which terrain centers
     * @param largeAmp         Amplitude of large-scale hills (scale ~200 blocks)
     * @param medAmp           Amplitude of medium roughness (scale ~80 blocks)
     * @param smallAmp         Amplitude of fine detail (scale ~30 blocks)
     * @param ravineChance     0.0 = no ravines, 1.0 = maximum ravine density
     * @param powderSnowChance Fraction of snow cover that becomes powder snow
     * @param flattenTerrain   If true, halves large+med noise for flat biomes
     */
    public record TerrainProfile(
            int baseHeight,
            double largeAmp,
            double medAmp,
            double smallAmp,
            double ravineChance,
            float powderSnowChance,
            boolean flattenTerrain
    ) {}
}