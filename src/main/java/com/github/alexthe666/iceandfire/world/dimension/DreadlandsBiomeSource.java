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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * Custom BiomeSource for the Dreadlands dimension.
 *
 * Distributes 5 biomes using two Perlin noise channels (seeded) for organic regions:
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

    // Seed-aware noise for biome distribution
    private PerlinSimplexNoise biomeNoise1;
    private PerlinSimplexNoise biomeNoise2;
    private boolean noiseReady = false;

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

    /**
     * Lazily initializes biome noise. Called on first biome query.
     * Uses a fixed biome-layer seed offset so biome layout is consistent
     * but still depends on the world seed passed through withSeed → initNoise.
     */
    private synchronized void ensureNoise(long seed) {
        if (noiseReady) return;
        biomeNoise1 = new PerlinSimplexNoise(
                new WorldgenRandom(new LegacyRandomSource(seed + 77777L)), List.of(-2, -1, 0));
        biomeNoise2 = new PerlinSimplexNoise(
                new WorldgenRandom(new LegacyRandomSource(seed + 88888L)), List.of(-2, -1, 0));
        noiseReady = true;
    }

    /** Called by the chunk generator once the world seed is known. */
    public void initSeed(long seed) {
        noiseReady = false;
        ensureNoise(seed);
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
     * Two independent Perlin noise channels at biome scale (~400 blocks):
     *   noise1 high + noise2 high  → Frozen Dread Plains (cold + wet)
     *   noise1 high + noise2 low   → Dreaded Spikes (cold + dry)
     *   noise1 low  + noise2 high  → Dread Dead Forest (warmer + wet)
     *   noise1 low  + noise2 low   → Dread Crags (warmer + dry)
     *   middle range               → Dreadlands Wastes (default)
     */
    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ, Climate.Sampler sampler) {
        // Lazy init with seed 0 as fallback — will be re-initialized with real seed
        // once the chunk generator calls initSeed()
        ensureNoise(0L);

        double x = biomeX * 4.0;
        double z = biomeZ * 4.0;

        // Scale of ~400 blocks per biome region, 3 octaves for organic edges
        double n1 = biomeNoise1.getValue(x / 400.0, z / 400.0, false);
        double n2 = biomeNoise2.getValue(x / 400.0, z / 400.0, false);

        // Wastes occupies the center band — both noise channels near zero
        if (Math.abs(n1) < 0.25 && Math.abs(n2) < 0.25) {
            return wastesHolder;
        }

        // Quadrant selection with asymmetric thresholds to prevent
        // too much wastes at the edges
        if (n1 > 0.25) {
            if (n2 > 0.15) return frozenHolder;    // cold + wet
            if (n2 < -0.15) return spikesHolder;   // cold + dry
        } else if (n1 < -0.25) {
            if (n2 > 0.15) return forestHolder;     // warm + wet
            if (n2 < -0.15) return cragsHolder;     // warm + dry
        }

        // Edge cases between quadrants → wastes
        return wastesHolder;
    }
}