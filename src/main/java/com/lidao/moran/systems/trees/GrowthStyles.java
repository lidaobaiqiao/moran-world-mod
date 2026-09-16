package com.lidao.moran.systems.trees;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.Map;

/**
 * 生长行为「风格」注册表——基因文件 branchStyle/budPosStyle/forkGainStyle 的取值来源。
 *
 * <p>与 BloomStyles 同构:风格是代码(行为注入型性状的实现),基因文件只选名字。
 * 未注册名回退中性默认(直线延伸/无逐节侧芽/向光+向顶评分)。
 */
public final class GrowthStyles {

    /** 枝条走向风格:名字 → 钩子 */
    private static final Map<String, TreeSpecies.BranchDirHook> BRANCH_DIR = Map.of(
            "drooping", GrowthStyles::droopingDir
    );

    /** 侧芽位风格:名字 → 钩子 */
    private static final Map<String, TreeSpecies.BudPosHook> BUD_POS = Map.of(
            "whorled_pine", GrowthStyles::whorledPine
    );

    /** 分叉收益风格:名字 → 钩子 */
    private static final Map<String, TreeSpecies.ForkGainHook> FORK_GAIN = Map.of(
            "horizontal_pine", GrowthStyles::horizontalOnly
    );

    private GrowthStyles() {
    }

    /** 按名取枝条走向;null = 直线延伸 */
    public static TreeSpecies.BranchDirHook branchDirection(String name) {
        return name == null ? null : BRANCH_DIR.get(name);
    }

    /** 按名取侧芽位;null = 无逐节侧芽 */
    public static TreeSpecies.BudPosHook budPosition(String name) {
        return name == null ? null : BUD_POS.get(name);
    }

    /** 按名取分叉收益;null = 向光+向顶默认 */
    public static TreeSpecies.ForkGainHook forkGain(String name) {
        return name == null ? null : FORK_GAIN.get(name);
    }

    public static boolean hasBranchDir(String name) {
        return name == null || "straight".equals(name) || BRANCH_DIR.containsKey(name);
    }

    public static boolean hasBudPos(String name) {
        return name == null || "none".equals(name) || BUD_POS.containsKey(name);
    }

    public static boolean hasForkGain(String name) {
        return name == null || "light_up".equals(name) || FORK_GAIN.containsKey(name);
    }

    /**
     * 「drooping」渐下垂:同向链上第 2 节起转向下垂(垂柳——冠缘外圈的枝条先横伸再垂落)。
     * DOWN 链自身保持向下直到触地;回溯统计与引擎 chainPosition 同一条路。
     */
    private static Direction droopingDir(TreeSpecies self, ServerWorld world, BlockPos pos, Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return facing;
        }
        int chain = 0;
        BlockPos p = pos;
        for (int i = 0; i < 16; i++) {
            BlockState s = world.getBlockState(p.offset(facing.getOpposite()));
            if (!(s.getBlock() instanceof com.lidao.moran.systems.blocks.MoranBranchBlock)
                    || s.get(com.lidao.moran.systems.blocks.MoranBranchBlock.TRUNK)
                    || s.get(com.lidao.moran.systems.blocks.MoranBranchBlock.FACING) != facing) {
                break;
            }
            chain++;
            p = p.offset(facing.getOpposite());
        }
        return chain >= 2 ? Direction.DOWN : facing;
    }

    /**
     * 「whorled_pine」轮生侧芽位:主干每 3 层一个轮生带(层性),
     * 且未到目标高——带内多节逐拍萌出即成一层轮生枝(松塔分层)。
     */
    private static boolean whorledPine(TreeSpecies self, ServerWorld world, BlockPos pos,
                                       int height, int targetHeight) {
        return targetHeight >= 5 && height < targetHeight && height % 3 == 0;
    }

    /**
     * 「horizontal_pine」分叉收益:松枝只认水平向(层内平展成塔盘),
     * 竖直候选判负分——由默认向光评分重写为水平专项。
     */
    private static int horizontalOnly(TreeSpecies self, ServerWorld world, BlockPos pos,
                                      Direction facing, Direction candidate, HormoneProfile hormones) {
        if (candidate.getAxis() == Direction.Axis.Y) {
            return -100;
        }
        return TreeSpecies.opennessAround(world, pos, candidate) * 2 + 1;
    }
}
