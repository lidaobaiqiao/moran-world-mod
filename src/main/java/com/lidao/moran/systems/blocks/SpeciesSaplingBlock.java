package com.lidao.moran.systems.blocks;

import net.minecraft.block.SaplingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;

/**
 * 五树通用生长树苗——长成后原地转为对应树种的 g1 主干节,
 * 由基因文件驱动整树生长(与 PeachSaplingBlock 同语义,物种参数化)。
 */
public class SpeciesSaplingBlock extends SaplingBlock {

    private final Block branch;

    public SpeciesSaplingBlock(Block branch, Settings settings) {
        super(null, settings);
        this.branch = branch;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState down = world.getBlockState(pos.down());
        if (down.isOf(Blocks.DIRT) || down.isOf(Blocks.GRASS_BLOCK) || down.isOf(Blocks.PODZOL)
                || down.isOf(Blocks.COARSE_DIRT) || down.isOf(Blocks.MYCELIUM)
                || down.isOf(Blocks.ROOTED_DIRT) || down.isOf(Blocks.MOSS_BLOCK)) {
            return true;
        }
        return down.isOf(BlockSystem.PEACH_BLOSSOM_DIRT)
                || down.isOf(BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK);
    }

    @Override
    public void generate(ServerWorld world, BlockPos pos, BlockState state, Random random) {
        world.setBlockState(pos, branch.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
    }
}
