package com.lidao.moran;

import com.lidao.moran.systems.blocks.BlockSystem;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;

public class MoranModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider(MoranBlockLootTableProvider::new);
    }

    /**
     * 为所有方块生成"掉落自身"的战利品表，输出到 src/main/generated。
     * 桃花树叶（掉树苗）、桃源树枝（按生长度分档）、桃源木板（手写）已有手工战利品表，跳过以免生成重复资源。
     */
    private static class MoranBlockLootTableProvider extends FabricBlockLootTableProvider {
        private MoranBlockLootTableProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generate() {
            for (Block block : BlockSystem.getAllBlocks()) {
                if (block == BlockSystem.PEACH_BLOSSOM_LEAVES
                        || block == BlockSystem.PEACH_BRANCH
                        || block == BlockSystem.PEACH_PLANKS) {
                    continue;
                }
                addDrop(block);
            }
        }
    }
}
