// 完善 PeachBlossomBiomes.java
package com.lidao.moran.dimensions.peach_blossom;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.List;

public class PeachBlossomBiomes {
    public static final String MOD_ID = "moran-mod";

    // 桃花源维度七大生物群系
    public static final RegistryKey<Biome> PEACH_VALLEY =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "peach_valley"));

    public static final RegistryKey<Biome> BAMBOO_GROVE =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "bamboo_grove"));

    public static final RegistryKey<Biome> FARM_PLAINS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "farm_plains"));

    public static final RegistryKey<Biome> GREEN_HILLS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "green_hills"));

    public static final RegistryKey<Biome> BLOSSOM_STREAM =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "blossom_stream"));

    public static final RegistryKey<Biome> MIRROR_LAKES =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "mirror_lakes"));

    public static final RegistryKey<Biome> HIDDEN_DEPTHS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "hidden_depths"));

    public static List<RegistryKey<Biome>> getAllBiomeKeys() {
        return List.of(
                PEACH_VALLEY,      // 妖灼华原
                BAMBOO_GROVE,      // 隐竹之界
                FARM_PLAINS,       // 千耕平畴
                GREEN_HILLS,       // 叠翠微岚
                BLOSSOM_STREAM,    // 落花寻溪原
                MIRROR_LAKES,      // 镜湖百池
                HIDDEN_DEPTHS      // 晦暗幽深处
        );
    }
}