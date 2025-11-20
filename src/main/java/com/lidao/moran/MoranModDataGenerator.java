package com.lidao.moran;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;

public class MoranModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider(MoranModelProvider::new);
        generator.createPack().addProvider(MoranLanguageProvider::new);
    }

    private static class MoranModelProvider extends FabricModelProvider {
        public MoranModelProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
            // 如果没有方块，可以留空
        }

        @Override
        public void generateItemModels(ItemModelGenerator itemModelGenerator) {
            // 修复：使用物品系统注册模型
            itemModelGenerator.register(
                    com.lidao.moran.systems.items.ItemSystem.MORAN,
                    Models.GENERATED
            );
        }
    }

    private static class MoranLanguageProvider extends FabricLanguageProvider {
        public MoranLanguageProvider(FabricDataOutput output) {
            super(output, "zh_cn");
        }

        @Override
        public void generateTranslations(TranslationBuilder builder) {
            // 修复：直接使用字符串注册翻译
            builder.add(com.lidao.moran.systems.items.ItemSystem.MORAN, "墨石");
            builder.add(com.lidao.moran.systems.items.ItemSystem.QINGFENG_JINGHUA, "清风精华");
            builder.add(com.lidao.moran.systems.items.ItemSystem.MORAN_SWORD, "墨剑");
            builder.add(com.lidao.moran.systems.blocks.BlockSystem.MORAN_BLOCK, "墨石块");
        }
    }
}



