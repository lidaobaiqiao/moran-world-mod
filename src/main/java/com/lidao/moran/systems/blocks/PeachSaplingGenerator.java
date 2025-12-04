package com.lidao.moran.systems.blocks;

import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

public class PeachSaplingGenerator extends SaplingGenerator {
    
    // 使用你已有的桃花树配置
    private static final RegistryKey<ConfiguredFeature<?, ?>> PEACH_TREE_ONE = 
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("moran_mod", "peach_yaozhuohuayuan_one_tree"));
    
    private static final RegistryKey<ConfiguredFeature<?, ?>> PEACH_TREE_TWO = 
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("moran_mod", "peach_yaozhuohuayuan_two_tree"));
    
    @Nullable
    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        // 随机选择一种桃花树配置
        if (random.nextBoolean()) {
            return PEACH_TREE_ONE;
        } else {
            // 第二种树有蜂巢装饰器，更特别
            return PEACH_TREE_TWO;
        }
    }
}