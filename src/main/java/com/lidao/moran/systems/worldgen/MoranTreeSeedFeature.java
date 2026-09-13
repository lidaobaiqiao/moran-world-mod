package com.lidao.moran.systems.worldgen;

import com.lidao.moran.systems.blocks.MoranBranchBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.feature.SimpleBlockFeatureConfig;

/**
 * 野生树种子特征：放置一个 natural=true 的幼年枝干，并当场登记第一次生长请求。
 *
 * 不能用原版 simple_block——它以 flags=2 放置方块，onBlockAdded 不会被调用，
 * schedule_tick 配置字段在 1.20.1 又是被解析但被无视的死字段，
 * 野生树的第一次生长请求永远无法登记（表现为幼枝永不生长）。
 */
public class MoranTreeSeedFeature extends Feature<SimpleBlockFeatureConfig> {

    public static final MoranTreeSeedFeature INSTANCE = new MoranTreeSeedFeature();

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
        // 生长前一次性环境评估 → 目标高度（持久化于方块，随生长继承）
        int target = com.lidao.moran.systems.trees.Trees.PEACH.evaluateTargetHeight(context.getWorld(), pos);
        state = state.with(com.lidao.moran.systems.blocks.MoranBranchBlock.TARGET, target);
        context.getWorld().setBlockState(pos, state, 2);
        // 登记第一次野生生长请求（0.1 秒），此后由引擎链式自续
        context.getWorld().scheduleBlockTick(pos, state.getBlock(), MoranBranchBlock.NATURAL_INTERVAL);
        return true;
    }
}
