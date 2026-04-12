package com.github.alexthe666.iceandfire.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.github.alexthe666.iceandfire.world.dimension.DreadlandsBlocks.*;

/**
 * Core chunk generator for the Dreadlands dimension.
 *
 * Responsibilities (kept here):
 *   - Terrain noise computation and column filling
 *   - Biome-aware surface block selection
 *   - Ravine carving
 *   - Underground block mixing
 *
 * Delegated (extracted):
 *   - Block palette:     {@link DreadlandsBlocks}
 *   - Biome profiles:    {@link DreadBiome}
 *   - Biome source:      {@link DreadlandsBiomeSource}
 *   - Feature placement: {@link DreadlandsDecorator}
 *   - Individual features: feature/ package
 */
public class DreadlandsChunkGenerator extends ChunkGenerator {

    // ─── Noise scales ─────────────────────────────────────────────────────
    private static final double LARGE_SCALE = 200.0;
    private static final double MED_SCALE = 80.0;
    private static final double SMALL_SCALE = 30.0;
    private static final double RAVINE_SCALE = 300.0;
    private static final double RAVINE_THRESHOLD = 0.85;
    private static final int RAVINE_DEPTH = 25;

    // ─── Noise fields ─────────────────────────────────────────────────────
    private PerlinSimplexNoise terrainNoise;
    private PerlinSimplexNoise roughnessNoise;
    private PerlinSimplexNoise detailNoise;
    private PerlinSimplexNoise ravineNoise;
    private PerlinSimplexNoise ravineWobble;
    private PerlinSimplexNoise stoneVariation;
    private volatile boolean noiseInitialized = false;
    private long noiseSeed = 0L;

    // ─── Codec ────────────────────────────────────────────────────────────
    //
    // On 1.18.2, ChunkGenerator's constructor requires:
    //   (Registry<StructureSet>, Optional<HolderSet<StructureSet>>, BiomeSource)
    //
    // The codec must retrieve both the StructureSet registry and the Biome registry.
    // We pass Optional.empty() for structure overrides since we don't need custom
    // structure placement in the Dreadlands (yet).
    //
    public static final Codec<DreadlandsChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY)
                            .forGetter(gen -> gen.structureSets),
                    RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY)
                            .forGetter(gen -> gen.biomeRegistry)
            ).apply(instance, DreadlandsChunkGenerator::new)
    );

    private final Registry<StructureSet> structureSets;
    private final Registry<Biome> biomeRegistry;

    public DreadlandsChunkGenerator(Registry<StructureSet> structureSets, Registry<Biome> biomeRegistry) {
        super(structureSets, Optional.empty(), new DreadlandsBiomeSource(biomeRegistry));
        this.structureSets = structureSets;
        this.biomeRegistry = biomeRegistry;
    }

    private synchronized void initNoise(long seed) {
        if (noiseInitialized && this.noiseSeed == seed) return;
        this.noiseSeed = seed;
        // 3 octaves (-2,-1,0) for richer terrain variation between biomes
        terrainNoise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed)), List.of(-2, -1, 0));
        roughnessNoise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed + 1111L)), List.of(-1, 0));
        detailNoise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed + 2222L)), List.of(-1, 0));
        ravineNoise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed + 3333L)), List.of(-2, -1, 0));
        ravineWobble = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed + 4444L)), List.of(-1, 0));
        stoneVariation = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed + 5555L)), List.of(-1, 0));
        noiseInitialized = true;

        // Propagate seed to the biome source so biome layout is world-dependent
        if (biomeSource instanceof DreadlandsBiomeSource dbs) {
            dbs.initSeed(seed);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TERRAIN HEIGHT
    // ═══════════════════════════════════════════════════════════════════════

    public int getSurfaceHeight(int x, int z, DreadBiome.TerrainProfile p) {
        double large = terrainNoise.getValue(x / LARGE_SCALE, z / LARGE_SCALE, false) * p.largeAmp();
        double med = roughnessNoise.getValue(x / MED_SCALE, z / MED_SCALE, false) * p.medAmp();
        double small = detailNoise.getValue(x / SMALL_SCALE, z / SMALL_SCALE, false) * p.smallAmp();
        if (p.flattenTerrain()) { large *= 0.5; med *= 0.5; }
        return p.baseHeight() + (int) Math.round(large + med + small);
    }

    public int getRavineDepth(int x, int z, DreadBiome.TerrainProfile p) {
        if (p.ravineChance() <= 0) return 0;
        double rv = ravineNoise.getValue(x / RAVINE_SCALE, z / RAVINE_SCALE, false);
        double wobble = ravineWobble.getValue(x / 40.0, z / 40.0, false) * 2.0;
        double adjusted = rv + Math.abs(wobble) * 0.05;
        double threshold = 1.0 - (1.0 - RAVINE_THRESHOLD) * p.ravineChance();
        if (adjusted < threshold) return 0;
        return (int) (RAVINE_DEPTH * ((adjusted - threshold) / (1.0 - threshold)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BLOCK SELECTION
    // ═══════════════════════════════════════════════════════════════════════

    private BlockState getUndergroundBlock(int x, int y, int z, DreadBiome biome) {
        double sv = stoneVariation.getValue(x / 20.0, y / 15.0 + z / 20.0, false);

        if (biome == DreadBiome.FROZEN_PLAINS && sv > 0.6)
            return (y > 40) ? PACKED_ICE : FROZEN_STONE;
        if (biome == DreadBiome.SPIKES && sv > 0.5)
            return (y > 45) ? DRAGON_ICE : FROZEN_STONE;
        if (biome == DreadBiome.CRAGS && sv < -0.5)
            return DREAD_STONE_TILE;

        if (sv > 0.75) return (y < 30) ? SOUL_SAND : FROZEN_GRAVEL;
        if (sv > 0.55) return FROZEN_STONE;
        if (sv < -0.7) return FROZEN_COBBLESTONE;

        double yNoise = Math.sin(y * 0.3 + sv * 2.0);
        if (yNoise > 0.85 && y > 15 && y < 50) return DREAD_STONE_TILE;

        return DREAD_STONE;
    }

    private BlockState getSurfaceBlock(int x, int z, DreadBiome biome) {
        double n = stoneVariation.getValue(x / 25.0, z / 25.0, false);
        return switch (biome) {
            case CRAGS -> n > 0.3 ? DREAD_STONE_TILE : (n < -0.4 ? FROZEN_COBBLESTONE : DREAD_STONE);
            case FROZEN_PLAINS -> n > 0.7 ? FROZEN_GRASS : SNOW;
            case SPIKES -> n > 0.4 ? DRAGON_ICE : (n < -0.3 ? FROZEN_STONE : DREAD_STONE);
            case DEAD_FOREST -> n > 0.6 ? FROZEN_DIRT : (n < -0.5 ? FROZEN_COBBLESTONE : FROZEN_GRASS);
            default -> n > 0.6 ? DREAD_STONE : (n < -0.5 ? FROZEN_COBBLESTONE : FROZEN_GRASS);
        };
    }

    private BlockState getSnowBlock(int x, int z, DreadBiome.TerrainProfile p) {
        double n = detailNoise.getValue(x / 15.0, z / 15.0, false);
        return n > (1.0 - p.powderSnowChance() * 2.0) ? POWDER_SNOW : SNOW;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CHUNK GENERATOR OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════

    @Override protected Codec<? extends ChunkGenerator> codec() { return CODEC; }

    @Override public ChunkGenerator withSeed(long seed) {
        DreadlandsChunkGenerator gen = new DreadlandsChunkGenerator(structureSets, biomeRegistry);
        gen.initNoise(seed);
        return gen;
    }

    @Override public Climate.Sampler climateSampler() { return Climate.empty(); }
    @Override public void applyCarvers(WorldGenRegion l, long s, BiomeManager b, StructureFeatureManager sm, ChunkAccess c, GenerationStep.Carving st) {}
    @Override public void buildSurface(WorldGenRegion l, StructureFeatureManager s, ChunkAccess c) {}
    @Override public void spawnOriginalMobs(WorldGenRegion l) {}
    @Override public int getGenDepth() { return 256; }
    @Override public int getSeaLevel() { return 62; }
    @Override public int getMinY() { return 0; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        StructureFeatureManager structureManager,
                                                        ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            DreadlandsBlocks.ensureResolved();
            // noiseSeed is set by withSeed() or setSeed() before generation begins.
            // If still 0L, terrain still generates — just not seed-unique.
            if (!noiseInitialized) initNoise(noiseSeed);
            fillTerrain(chunk);
            return chunk;
        }, executor);
    }

    /**
     * Called externally (e.g. from a WorldEvent.Load hook) to set the world seed.
     * Must be called before chunks generate to ensure terrain is seed-dependent.
     */
    public void setSeed(long seed) {
        initNoise(seed);
    }

    private void fillTerrain(ChunkAccess chunk) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int cx = chunk.getPos().getMinBlockX(), cz = chunk.getPos().getMinBlockZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int bx = cx + lx, bz = cz + lz;

                Holder<Biome> bh = biomeSource.getNoiseBiome(bx >> 2, 64 >> 2, bz >> 2, climateSampler());
                DreadBiome biome = DreadBiome.classify(bh);
                DreadBiome.TerrainProfile p = biome.profile();

                int surfY = getSurfaceHeight(bx, bz, p);
                int ravD = getRavineDepth(bx, bz, p);
                int ravFloor = Math.max(10, surfY - ravD);

                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    mutable.set(bx, y, bz);
                    BlockState state;

                    if (y == 0) {
                        state = BEDROCK;
                    } else if (ravD > 0 && y >= ravFloor && y <= surfY) {
                        state = (y < ravFloor + 2)
                                ? (stoneVariation.getValue(bx / 8.0, bz / 8.0, false) > 0.3 ? SOUL_SOIL : GRAVEL)
                                : AIR;
                    } else if (y < surfY - 4) {
                        state = getUndergroundBlock(bx, y, bz, biome);
                    } else if (y < surfY - 1) {
                        double sub = stoneVariation.getValue(bx / 12.0, bz / 12.0, false);
                        state = biome == DreadBiome.FROZEN_PLAINS
                                ? (sub > 0.3 ? PACKED_ICE : FROZEN_DIRT)
                                : (sub > 0.5 ? FROZEN_COBBLESTONE : FROZEN_DIRT);
                    } else if (y == surfY - 1) {
                        state = getSurfaceBlock(bx, bz, biome);
                    } else if (y == surfY) {
                        if (ravD > 0) state = AIR;
                        else if (biome == DreadBiome.CRAGS) {
                            state = detailNoise.getValue(bx / 20.0, bz / 20.0, false) > 0.5 ? SNOW : AIR;
                        } else state = getSnowBlock(bx, bz, p);
                    } else {
                        state = AIR;
                    }

                    chunk.setBlockState(mutable, state, false);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DECORATION — delegates to DreadlandsDecorator
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                     StructureFeatureManager structureManager) {
        if (!(level instanceof WorldGenRegion region)) return;
        DreadlandsBlocks.ensureResolved();
        initNoise(region.getSeed());

        int cx = chunk.getPos().getMinBlockX(), cz = chunk.getPos().getMinBlockZ();
        Random rand = new Random(chunk.getPos().toLong() ^ region.getSeed());

        Holder<Biome> bh = biomeSource.getNoiseBiome((cx + 8) >> 2, 64 >> 2, (cz + 8) >> 2, climateSampler());
        DreadBiome biome = DreadBiome.classify(bh);
        DreadBiome.TerrainProfile profile = biome.profile();

        DreadlandsDecorator.decorate(region, cx, cz, biome, rand,
                (x, z) -> getSurfaceHeight(x, z, profile),
                (x, z) -> getRavineDepth(x, z, profile));
    }

    // ─── Standard overrides ───────────────────────────────────────────────

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level) {
        if (!noiseInitialized) initNoise(noiseSeed);
        return getSurfaceHeight(x, z, DreadBiome.WASTES.profile()) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level) {
        DreadlandsBlocks.ensureResolved();
        if (!noiseInitialized) initNoise(noiseSeed);
        int surfY = getSurfaceHeight(x, z, DreadBiome.WASTES.profile());
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = i + level.getMinBuildHeight();
            if (y == 0) states[i] = BEDROCK;
            else if (y < surfY - 4) states[i] = DREAD_STONE;
            else if (y < surfY - 1) states[i] = FROZEN_DIRT;
            else if (y == surfY - 1) states[i] = FROZEN_GRASS;
            else if (y == surfY) states[i] = SNOW;
            else states[i] = AIR;
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, BlockPos pos) {
        if (!noiseInitialized) initNoise(noiseSeed);
        Holder<Biome> bh = biomeSource.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, climateSampler());
        DreadBiome biome = DreadBiome.classify(bh);
        DreadBiome.TerrainProfile p = biome.profile();
        int surfY = getSurfaceHeight(pos.getX(), pos.getZ(), p);
        int ravD = getRavineDepth(pos.getX(), pos.getZ(), p);
        info.add("Dreadlands [" + biome + "] surface: " + surfY
                + (ravD > 0 ? " (ravine: -" + ravD + ")" : ""));
    }
}