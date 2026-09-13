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
                .subBranchChance(0.25F)
                .growChance(0.5F)
                .minLight(9)
                .temperature(0.3F, 1.2F)
                .minHydration(0)
                .minHumidity(0.0F)
                .soilPreference(4, 8, 3, 8, 2, 6)
                .pruneResponseChance(0.5F));
    }

    /** 桃树开花：末端与四周（上方 + 两侧垂直向）生成花苞——现实桃树花芽顶腋着生 */
    @Override
    public void onBranchStop(ServerWorld world, BlockPos pos, Direction facing, Random random, boolean natural) {
        placeBudIfAir(world, pos.offset(facing), facing, natural);
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        placeBudIfAir(world, pos.up(), Direction.UP, natural);
        placeBudIfAir(world, pos.offset(left), left, natural);
        placeBudIfAir(world, pos.offset(right), right, natural);
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
