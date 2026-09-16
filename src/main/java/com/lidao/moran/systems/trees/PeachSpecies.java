package com.lidao.moran.systems.trees;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * 桃树——第一个以「性状集合」声明的品种（性状库模型，用户 2026-09-16 定稿）。
 *
 * <p>基因组 = 性状清单（粗粒度、全部组成型），读起来就是品种说明书；
 * 引擎零改动，行为与旧子类覆写逐种子一致（simTrees 比对验证）。
 * 诱导型性状（徒长诱发/避荫窜高）待表达上下文（环境快照/损伤史）接入后追加。
 */
public class PeachSpecies extends TreeSpecies {

    private PeachSpecies(Builder builder) {
        super(builder);
    }

    /** 链上侧腋开花概率：未长枝的侧位以花填充（现实桃树花芽满布一年生枝侧腋） */
    private static final float AXILLARY_BUD_CHANCE = 0.6F;
    /** 开花回溯的链深（末三节的侧腋参与开花） */
    private static final int AXILLARY_DEPTH = 3;

    /** 桃的基因组：性状集合声明（整树 6-8 格，矮桩开心形，顶腋侧腋着花，先花后叶） */
    public static PeachSpecies peach() {
        TreeGenome genome = TreeGenome.of("peach", List.of(
                TreeTrait.of("矮桩开心形潜能",
                        b -> b.biologicalTop(6, 8)),
                TreeTrait.of("主枝坚决生长",
                        b -> b.branchStopChance(0.06F)),
                TreeTrait.of("侧枝旺盛",
                        b -> b.subBranchChance(0.3F).forkMinChainPos(1).forkSpacing(1)),
                TreeTrait.of("栽培适应域广", b -> b.growChance(0.5F)
                        .minLight(9)
                        .temperature(0.3F, 1.2F)
                        .minHydration(0)
                        .minHumidity(0.0F)
                        .soilPreference(4, 8, 3, 8, 2, 6)
                        .pruneResponseChance(0.5F)),
                TreeTrait.of("表型:直立(顶端优势强,权重2)",
                        b -> b.phenotype("erect", 2F, 1.10F, 0.90F, 1.10F)),
                TreeTrait.of("表型:开张(侧芽旺盛,权重1)",
                        b -> b.phenotype("open", 1F, 0.80F, 1.20F, 0.90F)),
                TreeTrait.of("顶腋侧腋着花",
                        b -> b.onBranchStop(PeachSpecies::bloomingStop)),
                TreeTrait.of("先花后叶带顶冠",
                        b -> b.onBudMature(PeachSpecies::blossomIntoLeaf)),
                TreeTrait.of("外观:桃皮与截断面", b -> b.textures(
                                "moran_mod:block/peach_log",
                                "moran_mod:item/thick_peach_trunk_side")
                        .trunkModelBase("moran_mod:block/peach_branch_trunk"))
        ));
        return new PeachSpecies(genome.applyTo(TreeSpecies.builder("peach")));
    }

    /**
     * 性状效果【顶腋侧腋着花】：末端 + 左右（水平）/四向（垂直） + 链上侧腋回溯。
     * 上方要让位给上生子枝（冠层靠它抬起来）；垂直母枝取四水平向；
     * rotateY 对 UP/DOWN 会抛异常/返回自身，垂直枝必须走另一条分支。
     */
    private static void bloomingStop(TreeSpecies self, ServerWorld world, BlockPos pos,
                                     Direction facing, Random random, boolean natural) {
        // 末端花苞
        placeBudIfAir(self, world, pos.offset(facing), facing, natural);
        Direction[] around = facing.getAxis() == Direction.Axis.Y
                ? HORIZONTALS
                : new Direction[]{facing.rotateYCounterclockwise(), facing.rotateYClockwise()};
        for (Direction d : around) {
            placeBudIfAir(self, world, pos.offset(d), d, natural);
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
                placeBudIfAir(self, world, p.up(), Direction.UP, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction l = facing.rotateYCounterclockwise();
                placeBudIfAir(self, world, p.offset(l), l, natural);
            }
            if (random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction r = facing.rotateYClockwise();
                placeBudIfAir(self, world, p.offset(r), r, natural);
            }
            p = p.offset(facing.getOpposite());
        }
    }

    /** 性状效果【先花后叶带顶冠】：化为桃花树叶；顶部花苞生成小树冠（四向必放+四角半数） */
    private static void blossomIntoLeaf(TreeSpecies self, ServerWorld world, BlockPos pos,
                                        Direction facing, Random random) {
        world.setBlockState(pos, self.leavesBlock().getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
        if (facing == Direction.UP) {
            for (Direction d : HORIZONTALS) {
                placeLeafIfAir(self, world, pos.offset(d));
            }
            for (Direction d : HORIZONTALS) {
                if (random.nextBoolean()) {
                    placeLeafIfAir(self, world, pos.offset(d).up());
                }
            }
        }
    }
}
