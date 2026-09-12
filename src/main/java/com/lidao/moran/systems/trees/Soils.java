package com.lidao.moran.systems.trees;

import com.lidao.moran.systems.blocks.BlockSystem;
import net.minecraft.block.Block;
import net.minecraft.world.WorldView;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 土壤注册表：方块 → 土壤档案。
 * 新土壤（墨壤/河泥/白陶土等）在此登记理化性质即可被生长系统感知。
 */
public final class Soils {

    private static final Map<Block, SoilProfile> PROFILES = new HashMap<>();

    static {
        // 桃花源土壤（数值即初始参数，随玩法调平衡）
        PROFILES.put(BlockSystem.PEACH_BLOSSOM_DIRT, new SoilProfile(6, 5, 4));
        PROFILES.put(BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK, new SoilProfile(6, 5, 4));
        PROFILES.put(BlockSystem.PEACH_BLOSSOM_SAND, new SoilProfile(8, 7, 1));   // 沙：排水极好、保水差
        PROFILES.put(BlockSystem.PEACH_BLOSSOM_STONE, new SoilProfile(2, 2, 2));  // 石面：贫瘠
        // 原版参照
        PROFILES.put(net.minecraft.block.Blocks.DIRT, new SoilProfile(5, 5, 5));
        PROFILES.put(net.minecraft.block.Blocks.GRASS_BLOCK, new SoilProfile(5, 5, 5));
        PROFILES.put(net.minecraft.block.Blocks.SAND, new SoilProfile(8, 7, 1));
    }

    public static SoilProfile of(WorldView world, BlockPos pos) {
        return PROFILES.getOrDefault(world.getBlockState(pos).getBlock(), SoilProfile.DEFAULT);
    }

    private Soils() {
    }
}
