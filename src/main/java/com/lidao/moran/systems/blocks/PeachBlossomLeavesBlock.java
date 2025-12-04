package com.lidao.moran.systems.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * 桃花树叶方块 - 带有落花效果
 * 复刻原版樱花树叶的粒子效果
 */
public class PeachBlossomLeavesBlock extends LeavesBlock {
    
    public PeachBlossomLeavesBlock(Settings settings) {
        super(settings);
    }
    
    /**
     * 每刻都有概率生成落花效果
     * 不调用super.randomTick()以避免树叶自然消失
     */
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // ⚠️【关键修复】不调用super.randomTick()，避免原版树叶消失逻辑
        // super.randomTick(state, world, pos, random); // 这行会导致树叶消失！
        
        // 🌸【直接使用原版樱花树叶粒子】
        if (random.nextInt(7) == 0) { // 约14%概率（减少30%触发频率）
            // 生成1-3个樱花花瓣（减少30%数量）
            int particleCount = random.nextBetween(1, 3);
            
            for (int i = 0; i < particleCount; i++) {
                // 在树叶周围位置生成花瓣
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble() * 0.8; // 从树叶中上部开始
                double z = pos.getZ() + random.nextDouble();
                
                // 直接使用原版樱花树叶的粒子效果
                world.addParticle(
                    ParticleTypes.CHERRY_LEAVES, // 原版樱花花瓣粒子
                    x, y, z,
                    (random.nextDouble() - 0.5) * 0.07, // 横向飘动（减少30%）
                    -0.035, // 向下飘落速度（减少30%）
                    (random.nextDouble() - 0.5) * 0.07
                );
            }
        }
        
        // 🍃【落叶堆生成逻辑】概率在下方生成落叶堆
        if (random.nextInt(200) == 0) { // 约0.5%概率生成落叶堆
            BlockPos downPos = pos.down();
            if (world.isAir(downPos) || world.getBlockState(downPos).getCollisionShape(world, downPos).isEmpty()) {
                world.setBlockState(downPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_FALLEN_LEAVES.getDefaultState());
            }
        }
    }
    
    /**
     * 客户端也有粒子效果（增强可见性）
     */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        // 🌸 客户端樱花花瓣效果
        if (random.nextInt(4) == 0) { // 25%概率（减少30%）
            // 每次生成1个樱花花瓣（减少30%）
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble() * 0.5; // 从树叶位置开始
            double z = pos.getZ() + random.nextDouble();
            
            // 直接使用原版樱花花瓣粒子
            world.addParticle(
                ParticleTypes.CHERRY_LEAVES, // 原版樱花花瓣粒子
                x, y, z,
                (random.nextDouble() - 0.5) * 0.07, // 横向飘动（减少30%）
                -0.021, // 向下飘落（减少30%）
                (random.nextDouble() - 0.5) * 0.07
            );
        }
    }
}