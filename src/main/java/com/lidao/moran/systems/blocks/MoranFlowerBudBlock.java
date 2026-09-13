package com.lidao.moran.systems.blocks;

import com.lidao.moran.systems.trees.SoilProfile;
import com.lidao.moran.systems.trees.Soils;
import com.lidao.moran.systems.trees.TreeSpecies;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;

/**
 * 通用花苞方块——生长引擎的生殖生长部分，所有树种共用。
 * 复用请求-检测-休眠 AI（含水度/土壤因子）；阶段满后由树种档案决定成熟形态
 * （桃：先花后叶化为树叶；未来松/杏可直接变叶簇）。
 */
public class MoranFlowerBudBlock extends Block implements Fertilizable {

    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
    public static final IntProperty STAGE = IntProperty.of("stage", 1, 3);
    public static final IntProperty FAILS = IntProperty.of("fails", 0, TreeSpecies.MAX_FAILS);
    /** 野生模式（世界生成）：0.1s 请求、立即生长、失败冻结 */
    public static final BooleanProperty NATURAL = BooleanProperty.of("natural");

    private final TreeSpecies species;

    public MoranFlowerBudBlock(TreeSpecies species, Settings settings) {
        super(settings);
        this.species = species;
        setDefaultState(getDefaultState()
                .with(FACING, Direction.UP).with(STAGE, 1).with(FAILS, 0).with(NATURAL, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE, FAILS, NATURAL);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        if (!world.isClient && world.getBlockState(pos).getBlock() instanceof MoranFlowerBudBlock) {
            BlockState self = world.getBlockState(pos);
            world.scheduleBlockTick(pos, this,
                    self.contains(NATURAL) && self.get(NATURAL)
                            ? MoranBranchBlock.NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean natural = state.get(NATURAL);
        int interval = natural ? MoranBranchBlock.NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL;

        int fails = state.get(FAILS);
        if (fails >= TreeSpecies.MAX_FAILS) {
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, interval);
            return;
        }

        // 环境取样：花苞悬空生长，根部沿下方找土壤与水层
        BlockPos root = MoranBranchBlock.rootPos(world, pos, 24);
        SoilProfile soil = Soils.of(world, root.down());
        int hydration = TreeSpecies.hydration(world, root);

        if (!species.checkEnvironment(world, pos, hydration)) {
            if (natural) {
                return; // 野生花苞：条件不满足直接冻结
            }
            int next = fails + 1;
            world.setBlockState(pos, state.with(FAILS, next), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this,
                    next >= TreeSpecies.MAX_FAILS ? TreeSpecies.DORMANT_TICKS : TreeSpecies.REQUEST_INTERVAL);
            return;
        }

        if (natural || random.nextFloat() < species.effectiveGrowChance(world, pos, hydration, soil)) {
            int stage = state.get(STAGE);
            if (stage >= 3) {
                species.onBudMature(world, pos, state.get(FACING), random);
                return;
            }
            world.setBlockState(pos, state.with(STAGE, stage + 1), Block.NOTIFY_ALL);
        }
        world.scheduleBlockTick(pos, this, interval);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    // 骨粉：直接催一阶段
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int stage = state.get(STAGE);
        if (stage >= 3) {
            species.onBudMature(world, pos, state.get(FACING), random);
        } else {
            world.setBlockState(pos, state.with(STAGE, stage + 1), Block.NOTIFY_ALL);
        }
        // 骨粉同时确保花苞重新进入请求循环（冻结中的花苞由此复活）
        world.scheduleBlockTick(pos, this, state.get(NATURAL)
                ? MoranBranchBlock.NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
    }
}
