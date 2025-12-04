package com.lidao.moran.systems.blocks;

import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeachBlossomGrassBlock extends GrassBlock implements Fertilizable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("peach-grass");
    
    public PeachBlossomGrassBlock(Settings settings) {
        super(settings);
    }
    
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
        BlockPos upPos = pos.up();
        
        // 简单的骨粉效果：只在受击处生成草
        if (world.getBlockState(upPos).isAir() && world.getLightLevel(upPos) >= 4) {
            if (random.nextBoolean()) {
                // 50%概率生成草
                world.setBlockState(upPos, Blocks.GRASS.getDefaultState());
            } else {
                // 50%概率生成高草丛
                world.setBlockState(upPos, Blocks.TALL_GRASS.getDefaultState());
            }
        }
    }
    
    // ⭐️【关键修复】覆盖退化逻辑，确保退化成桃花泥土而不是原版泥土
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // 如果方块上方不透明，则退化
        if (!world.isChunkLoaded(pos)) {
            return;
        }
        
        if (world.getLightLevel(pos.up()) < 4 && world.getBlockState(pos.up()).getOpacity(world, pos.up()) > 2) {
            LOGGER.info("🌿 桃花草方块在 {} 退化成桃花泥土", pos);
            world.setBlockState(pos, BlockSystem.PEACH_BLOSSOM_DIRT.getDefaultState());
        } else {
            // 否则尝试传播到周围的泥土方块
            if (world.getLightLevel(pos.up()) >= 9) {
                for (int i = 0; i < 4; ++i) {
                    BlockPos blockPos = pos.add(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                    if (blockPos.getY() >= world.getBottomY() && blockPos.getY() < world.getTopY() && 
                        world.getBlockState(blockPos).isOf(BlockSystem.PEACH_BLOSSOM_DIRT)) {
                        BlockState blockState = this.getDefaultState();
                        
                        if (world.getLightLevel(blockPos.up()) >= 4 && 
                            world.getBlockState(blockPos.up()).getOpacity(world, blockPos.up()) <= 2) {
                            world.setBlockState(blockPos, blockState);
                        }
                    }
                }
            }
        }
    }
}