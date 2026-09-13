package com.lidao.moran.systems.blocks;

import net.minecraft.block.*;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class PeachSaplingBlock extends SaplingBlock {
    
    public PeachSaplingBlock(SaplingGenerator generator, Settings settings) {
        super(generator, settings);
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downPos = pos.down();
        BlockState downState = world.getBlockState(downPos);
        
        // 允许种植在原版泥土类型上
        if (downState.isOf(Blocks.DIRT) || 
            downState.isOf(Blocks.GRASS_BLOCK) || 
            downState.isOf(Blocks.PODZOL) || 
            downState.isOf(Blocks.COARSE_DIRT) || 
            downState.isOf(Blocks.MYCELIUM) || 
            downState.isOf(Blocks.ROOTED_DIRT) || 
            downState.isOf(Blocks.MOSS_BLOCK)) {
            return true;
        }
        
        // 允许种植在桃源土和桃源草方块上
        return downState.isOf(BlockSystem.PEACH_BLOSSOM_DIRT) || 
               downState.isOf(BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK);
    }
    
    @Override
    public void generate(ServerWorld world, BlockPos pos, BlockState state, Random random) {
        // 树苗成熟后不再生成原版树结构：变成生长度 1 的桃源树枝，由 MoranBranchBlock 接管生长。
        // 生长前一次性环境评估 → 目标高度（光照/水分/温度/土壤各一分，8+n 格）
        int target = com.lidao.moran.systems.trees.Trees.PEACH.evaluateTargetHeight(world, pos);
        world.setBlockState(pos, BlockSystem.PEACH_BRANCH.getDefaultState()
                .with(MoranBranchBlock.TARGET, target), Block.NOTIFY_ALL);
    }
}