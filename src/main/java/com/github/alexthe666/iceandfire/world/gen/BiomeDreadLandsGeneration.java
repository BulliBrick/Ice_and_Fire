package com.github.alexthe666.iceandfire.world.gen;

import com.github.alexthe666.iceandfire.world.biome.BiomeDreadLands;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraft.util.RegistryKey;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

import static net.minecraftforge.common.BiomeDictionary.Type.*;

public class BiomeDreadLandsGeneration {

    public static void generateBiomes() {
        addBiome(BiomeDreadLands.DREADLANDS_BIOME.get(), BiomeManager.BiomeType.COOL, 20, COLD, SPOOKY, DEAD);
    }

    public static void addBiome(Biome biome, BiomeManager.BiomeType type, int weight, BiomeDictionary.Type... types) {
        RegistryKey<Biome> key = RegistryKey.getOrCreateKey(ForgeRegistries.Keys.BIOMES,
                Objects.requireNonNull(ForgeRegistries.BIOMES.getKey(biome)));

        BiomeDictionary.addTypes(key, types);
        BiomeManager.addBiome(type, new BiomeManager.BiomeEntry(key, weight));
    }
}
