package com.lidao.moran.systems.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class PeachBlossomDirtBlock extends Block implements Fertilizable {

    public PeachBlossomDirtBlock(Settings settings) {
        super(settings);
    }
    
    // 裸土不自发变草：由旁边桃源草方块的 randomTick 蔓延驱动（同原版草方块机制）

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return world.getBlockState(pos.up()).isAir();
    }
    
    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }
    
    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // 尝试转变为桃花草方块
        if (world.getLightLevel(pos.up()) >= 4) {
            world.setBlockState(pos, BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK.getDefaultState());
        }
        
        // 在上方生成草或花
        BlockPos upPos = pos.up();
        if (world.getBlockState(upPos).isAir() && world.getLightLevel(upPos) >= 4) {
            if (random.nextInt(5) == 0) {
                // 有概率生成花
                BlockState flower = random.nextInt(3) == 0 ? 
                    net.minecraft.block.Blocks.DANDELION.getDefaultState() : 
                    net.minecraft.block.Blocks.POPPY.getDefaultState();
                world.setBlockState(upPos, flower);
            } else {
                // 生成草
                world.setBlockState(upPos, net.minecraft.block.Blocks.GRASS.getDefaultState());
            }
        }
    }
    
    // 在Minecraft 1.20.1中， getType方法可能不存在或已在更高版本中添加
    // 如果编译错误，可以注释掉这个方法
}