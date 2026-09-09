package com.lidao.moran.systems.blocks;

import com.lidao.moran.MoranMod;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/**
 * 通用树苗生成器：按特征 ID 直接指向对应的树 configured_feature。
 */
public class MoranSaplingGenerator extends SaplingGenerator {
    private final RegistryKey<ConfiguredFeature<?, ?>> feature;

    public MoranSaplingGenerator(String featureId) {
        this.feature = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MoranMod.MOD_ID, featureId));
    }

    @Nullable
    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        return this.feature;
    }
}
