package com.lidao.moran.systems.worldgen;

import com.lidao.moran.systems.blocks.MoranBranchBlock;
import com.lidao.moran.systems.trees.Trees;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.SimpleBlockFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * 野生树种子特征：放置一个 natural=true 的幼年枝干，并当场登记第一次生长请求。
 *
 * 不能用原版 simple_block——它以 flags=2 放置方块，onBlockAdded 不会被调用，
 * schedule_tick 配置字段在 1.20.1 又是被解析但被无视的死字段，
 * 野生树的第一次生长请求永远无法登记（表现为幼枝永不生长）。
 *
 * 放置前执行最小树干间距检查（树种档案 minSpacing，水平切比雪夫距离）：
 * 安全距离内已有同库树干则放弃该点——防止相邻区块的树贴脸长到一起。
 */
public class MoranTreeSeedFeature extends Feature<SimpleBlockFeatureConfig> {

    public static final MoranTreeSeedFeature INSTANCE = new MoranTreeSeedFeature();

    /** 垂直扫描窗口：已有树干可能位于坡上（上方 12）或坡下（下方 8） */
    private static final int SCAN_DOWN = 8;
    private static final int SCAN_UP = 12;

    private MoranTreeSeedFeature() {
        super(SimpleBlockFeatureConfig.CODEC);
    }

    @Override
    public boolean generate(FeatureContext<SimpleBlockFeatureConfig> context) {
        SimpleBlockFeatureConfig config = context.getConfig();
        BlockPos pos = context.getOrigin();
        BlockState state = config.toPlace().get(context.getRandom(), pos);
        if (!state.canPlaceAt(context.getWorld(), pos)) {
            return false;
        }
        if (hasNearbyTrunk(context.getWorld(), pos, Trees.PEACH.minSpacing())) {
            return false; // 安全距离内已有树干，放弃该点
        }
        // 树的成熟档位（max）与激素档案一样由树基坐标确定性推导，无需评估存储。
        context.getWorld().setBlockState(pos, state, 2);
        // 登记第一次野生生长请求（0.1 秒），此后由引擎链式自续
        context.getWorld().scheduleBlockTick(pos, state.getBlock(), MoranBranchBlock.NATURAL_INTERVAL);
        return true;
    }

    /** 水平切比雪夫距离 spacing 内（垂直 ±扫描窗口）是否已有树干方块 */
    private static boolean hasNearbyTrunk(net.minecraft.world.WorldView world, BlockPos pos, int spacing) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -spacing; dx <= spacing; dx++) {
            for (int dz = -spacing; dz <= spacing; dz++) {
                for (int dy = -SCAN_DOWN; dy <= SCAN_UP; dy++) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (world.getBlockState(m).getBlock() instanceof MoranBranchBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
