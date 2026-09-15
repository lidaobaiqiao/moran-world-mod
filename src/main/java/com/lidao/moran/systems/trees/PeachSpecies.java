package com.lidao.moran.systems.trees;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * 桃树树种——从生长库延伸的第一个树种子类。
 *
 * 基类 {@link TreeSpecies} 只保留中性默认行为，桃树的全部范式在此定制：
 * - 参数：高大（环境评分定高 8-12）、密集侧芽（上限 6、首芽必出递减）；
 * - 开花方式（onBranchStop）：花芽着生一年生枝的末端与四周——上方 + 两侧，
 *   现实桃树花芽的顶腋着生；
 * - 花苞成熟（onBudMature）：先花后叶物候——化为桃花树叶，
 *   顶部花苞额外生成小树冠（四向必放 + 四角半数）。
 *
 * 后续树种同法：垂柳/劲松/寒梅/银杏各建子类，覆写差异点，共用生长引擎。
 */
public class PeachSpecies extends TreeSpecies {

    private PeachSpecies(Builder builder) {
        super(builder);
    }

    /** 桃树档案：8-12 米（环境评分定高），上部萌芽密集侧枝，侧枝 4-7 级 */
    public static PeachSpecies peach() {
        return new PeachSpecies(TreeSpecies.builder("peach")
                .biologicalTop(8, 12)
                .topBudGrowth(5)
                .maxBuds(6)
                .budChanceDenom(4)
                .branchMaxGrowth(7)
                .branchStopGrowth(4)
                .branchStopChance(0.25F)
                .subBranchChance(0.12F)
                .growChance(0.5F)
                .minLight(9)
                .temperature(0.3F, 1.2F)
                .minHydration(0)
                .minHumidity(0.0F)
                .soilPreference(4, 8, 3, 8, 2, 6)
                .pruneResponseChance(0.5F)
                // 树种提交的贴图（模型头部 textures 段）
                .textures("moran_mod:block/peach_log",
                          "moran_mod:item/thick_peach_trunk_side"));
    }

    /** 链上侧腋开花概率：未长枝的侧位以花填充（现实桃树花芽满布一年生枝侧腋） */
    private static final float AXILLARY_BUD_CHANCE = 0.6F;
    /** 开花回溯的链深（末三节的侧腋参与开花） */
    private static final int AXILLARY_DEPTH = 3;

    /** 桃树开花：末端与四周花苞 + 链上未分枝侧腋开花填充——现实桃树花芽顶腋着生 */
    @Override
    public void onBranchStop(ServerWorld world, BlockPos pos, Direction facing, Random random, boolean natural) {
        // 末端花苞
        placeBudIfAir(world, pos.offset(facing), facing, natural);
        // 四周花苞：水平母枝取「上 + 左 + 右」；垂直母枝（上生/下生）取四个水平向。
        // 注意 rotateYCounterclockwise/Clockwise 对 UP/DOWN 会抛 IllegalStateException，
        // 不能无条件调用——垂直枝必须走另一条分支。
        Direction[] around = facing.getAxis() == Direction.Axis.Y
                ? HORIZONTALS
                : new Direction[]{Direction.UP, facing.rotateYCounterclockwise(), facing.rotateYClockwise()};
        for (Direction d : around) {
            placeBudIfAir(world, pos.offset(d), d, natural);
        }
        // 链上回溯：未长出侧枝的侧位以花填充。
        // 该回溯依赖「同朝向连续链」，只对水平链成立，垂直链跳过。
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
                placeBudIfAir(world, p.up(), Direction.UP, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction l = facing.rotateYCounterclockwise();
                placeBudIfAir(world, p.offset(l), l, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction r = facing.rotateYClockwise();
                placeBudIfAir(world, p.offset(r), r, natural);
            }
            p = p.offset(facing.getOpposite());
        }
    }

    /** 桃树花苞成熟：先花后叶——化为桃花树叶；顶部花苞生成小树冠（四向 + 四角半数） */
    @Override
    public void onBudMature(ServerWorld world, BlockPos pos, Direction facing, Random random) {
        world.setBlockState(pos, leavesBlock().getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
        if (facing == Direction.UP) {
            for (Direction d : HORIZONTALS) {
                placeLeafIfAir(world, pos.offset(d));
            }
            for (Direction d : HORIZONTALS) {
                if (random.nextBoolean()) {
                    placeLeafIfAir(world, pos.offset(d).up());
                }
            }
        }
    }
}
