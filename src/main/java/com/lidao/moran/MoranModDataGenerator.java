package com.lidao.moran;

import com.lidao.moran.systems.blocks.BlockSystem;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;

import java.util.Set;

public class MoranModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider(MoranBlockLootTableProvider::new);
    }

    /**
     * 为所有方块生成"掉落自身"的战利品表，输出到 src/main/generated。
     * 已手写特殊掉落的方块跳过，避免与 src/main/resources 中的战利品表同路径重复。
     */
    private static class MoranBlockLootTableProvider extends FabricBlockLootTableProvider {
        private static final Set<Block> HAND_WRITTEN_LOOT = Set.of(
                BlockSystem.PEACH_BLOSSOM_LEAVES,
                BlockSystem.PEACH_BRANCH,
                BlockSystem.PEACH_PLANKS,
                BlockSystem.WILLOW_BRANCH,
                BlockSystem.PINE_BRANCH,
                BlockSystem.PLUM_BRANCH,
                BlockSystem.GINKGO_BRANCH,
                BlockSystem.WILLOW_SAPLING,
                BlockSystem.PINE_SAPLING,
                BlockSystem.PLUM_SAPLING,
                BlockSystem.GINKGO_SAPLING
        );

        private MoranBlockLootTableProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generate() {
            for (Block block : BlockSystem.getAllBlocks()) {
                if (HAND_WRITTEN_LOOT.contains(block)) {
                    continue;
                }
                addDrop(block);
            }
        }
    }
}
