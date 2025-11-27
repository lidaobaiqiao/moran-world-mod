package com.lidao.moran.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.Registry; // ✅ 正确路径
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.*;

public class PeachRegion extends Region {

    private static final int WEIGHT = 100; // ✅ 改为大写常量命名

    public PeachRegion(Identifier name) {
        super(name, RegionType.OVERWORLD, WEIGHT);
    }

    public void addBiomes(Registry<Biome> registry, Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();

        // 只添加妖灼华原群系，并将其限制在桃花源维度特有的噪声参数范围内
        new ParameterPointListBuilder()
                .temperature(Temperature.WARM, Temperature.HOT)
                .humidity(Humidity.ARID, Humidity.DRY)
                .continentalness(Continentalness.INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_0, Erosion.EROSION_1)
                .depth(Depth.SURFACE)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING)
                .build().forEach(point -> builder.add(point, RegistryKey.of(
                        RegistryKeys.BIOME,
                        new Identifier("moran_mod", "yaozhuohuayuan")
                )));

        builder.build().forEach(mapper::accept);
    }
}