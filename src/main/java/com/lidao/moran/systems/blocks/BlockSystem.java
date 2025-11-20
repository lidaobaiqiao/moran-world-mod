// src/main/java/com/lidao/moran/systems/blocks/BlockSystem.java
package com.lidao.moran.systems.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class BlockSystem {
    private static final Map<String, Block> BLOCKS = new HashMap<>();

    // 墨石方块
    public static final Block MORAN_BLOCK = registerBlock("moran_block",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE).strength(3.0f, 6.0f).requiresTool()));

    public static void initialize() {
        // 注册所有方块的物品形式
        registerBlockItems();
        System.out.println("✅ 方块系统初始化完成");
        System.out.println("   已注册 " + BLOCKS.size() + " 个方块");
    }

    private static Block registerBlock(String id, Block block) {
        BLOCKS.put(id, block);
        return Registry.register(Registries.BLOCK, new Identifier("moran-mod", id), block);
    }

    private static void registerBlockItems() {
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            Registry.register(Registries.ITEM, new Identifier("moran-mod", entry.getKey()),
                    new BlockItem(entry.getValue(), new Item.Settings()));
        }
    }

    public static Block getBlock(String id) {
        return BLOCKS.get(id);
    }
}