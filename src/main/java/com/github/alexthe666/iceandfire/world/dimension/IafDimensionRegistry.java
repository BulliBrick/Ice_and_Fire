package com.github.alexthe666.iceandfire.world.dimension;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Central registry for the Dreadlands dimension.
 *
 * Call {@link #register()} from the IceAndFire constructor BEFORE DeferredRegister
 * lines, as the datapack dimension JSON references these codec types.
 */
public class IafDimensionRegistry {

    public static final ResourceKey<Level> DREADLANDS_LEVEL =
            ResourceKey.create(Registry.DIMENSION_REGISTRY,
                    new ResourceLocation(IceAndFire.MODID, "dreadlands"));

    public static final ResourceKey<DimensionType> DREADLANDS_TYPE =
            ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY,
                    new ResourceLocation(IceAndFire.MODID, "dreadlands"));

    public static void register() {
        // Chunk generator codec — referenced by dimension JSON "generator.type"
        Registry.register(Registry.CHUNK_GENERATOR,
                new ResourceLocation(IceAndFire.MODID, "dreadlands"),
                DreadlandsChunkGenerator.CODEC);

        // Biome source codec — referenced by chunk generator internally
        Registry.register(Registry.BIOME_SOURCE,
                new ResourceLocation(IceAndFire.MODID, "dreadlands"),
                DreadlandsBiomeSource.CODEC);
    }
}