package com.lidao.moran.systems.trees;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * 桃的开花/成熟风格实现(性状库的行为注入单元,由基因文件按名选用)。
 *
 * <p>bloomingStyle「peach_axillary」= 顶腋+侧腋着花:末端花苞 + 左右(水平)/四向(垂直)
 * + 链上侧腋回溯;maturationStyle「peach_canopy」= 先花后叶:顶花苞生成小树冠,
 * 侧向花苞长成盛花小花团(水平8邻大半+斜上环+正上强+正下弱)。
 */
public final class PeachBloom {

    /** 链上侧腋开花概率：未长枝的侧位以花填充（现实桃树花芽满布一年生枝侧腋） */
    private static final float AXILLARY_BUD_CHANCE = 0.6F;
    /** 开花回溯的链深（末三节的侧腋参与开花） */
    private static final int AXILLARY_DEPTH = 3;

    private PeachBloom() {
    }

    /** 开花方式「peach_axillary」 */
    public static void axillaryStop(TreeSpecies self, ServerWorld world, BlockPos pos,
                                    Direction facing, Random random, boolean natural) {
        // 末端花苞
        TreeSpecies.placeBudIfAir(self, world, pos.offset(facing), facing, natural);
        // 上方要让位给上生子枝（冠层靠它抬起来）——水平母枝只取左右，垂直母枝取四水平向；
        // rotateY 对 UP/DOWN 会抛异常/返回自身，垂直枝必须走另一条分支
        Direction[] around = facing.getAxis() == Direction.Axis.Y
                ? TreeSpecies.HORIZONTALS
                : new Direction[]{facing.rotateYCounterclockwise(), facing.rotateYClockwise()};
        for (Direction d : around) {
            TreeSpecies.placeBudIfAir(self, world, pos.offset(d), d, natural);
        }
        // 链上回溯：未长出侧枝的侧位以花填充（依赖同朝向连续链，垂直链跳过）
        if (facing.getAxis() == Direction.Axis.Y) {
            return;
        }
        BlockPos p = pos.offset(facing.getOpposite());
        for (int i = 0; i < AXILLARY_DEPTH; i++) {
            net.minecraft.block.BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof com.lidao.moran.systems.blocks.MoranBranchBlock)
                    || s.get(com.lidao.moran.systems.blocks.MoranBranchBlock.FACING) != facing) {
                break;
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                TreeSpecies.placeBudIfAir(self, world, p.up(), Direction.UP, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction l = facing.rotateYCounterclockwise();
                TreeSpecies.placeBudIfAir(self, world, p.offset(l), l, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction r = facing.rotateYClockwise();
                TreeSpecies.placeBudIfAir(self, world, p.offset(r), r, natural);
            }
            p = p.offset(facing.getOpposite());
        }
    }

    /** 成熟形态「peach_canopy」：先花后叶；顶花苞小树冠（四向必放+四角半数），侧向花苞盛花小花团 */
    public static void canopyMature(TreeSpecies self, ServerWorld world, BlockPos pos,
                                    Direction facing, Random random) {
        world.setBlockState(pos, self.leavesBlock().getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
        if (facing == Direction.UP) {
            // 顶花苞:小树冠(四向必放+四角半数)
            for (Direction d : TreeSpecies.HORIZONTALS) {
                TreeSpecies.placeLeafIfAir(self, world, pos.offset(d));
            }
            for (Direction d : TreeSpecies.HORIZONTALS) {
                if (random.nextBoolean()) {
                    TreeSpecies.placeLeafIfAir(self, world, pos.offset(d).up());
                }
            }
        } else {
            // 【盛花体量】侧向花苞:小花团(水平8邻大半+斜上环+正上强+正下弱)——满树繁花
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    if (random.nextFloat() < 0.75F) {
                        TreeSpecies.placeLeafIfAir(self, world, pos.add(dx, 0, dz));
                    }
                    if (random.nextFloat() < 0.40F) {
                        TreeSpecies.placeLeafIfAir(self, world, pos.add(dx, 1, dz));
                    }
                }
            }
            if (random.nextFloat() < 0.85F) {
                TreeSpecies.placeLeafIfAir(self, world, pos.up());
            }
            if (random.nextFloat() < 0.25F) {
                TreeSpecies.placeLeafIfAir(self, world, pos.down());
            }
        }
    }
}
