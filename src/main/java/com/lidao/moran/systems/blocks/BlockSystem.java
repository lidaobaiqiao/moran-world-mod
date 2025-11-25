package com.lidao.moran.systems.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 极简方块系统 - 只保证桃花原木和树叶正常显示
 */
public class BlockSystem {

    private static final Map<String, Block> BLOCKS = new HashMap<>();

    // 🌸 最核心的桃花树方块
    public static final Block PEACH_LOG = registerBlock("peach_log",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG)));
    
    public static final Block PEACH_BLOSSOM_LEAVES = registerBlock("peach_blossom_leaves",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)));

    // 为了兼容性，添加基础方块别名
    public static final Block PEACH_BLOSSOM_DIRT = Blocks.DIRT;
    public static final Block PEACH_BLOSSOM_GRASS_BLOCK = Blocks.GRASS_BLOCK;
    public static final Block MORAN_BLOCK = Blocks.STONE;

    // 初始化
    public static void initialize() {
        registerBlockItems();
        System.out.println("✅ 极简方块系统初始化完成");
        System.out.println("   已注册 " + BLOCKS.size() + " 个方块 (桃花核心)");
        
        // 输出详细的调试信息
        System.out.println("🔍 桃花树方块注册信息：");
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            Identifier id = Registries.BLOCK.getId(entry.getValue());
            System.out.println("   - " + entry.getKey() + " -> " + id);
        }
    }

    private static Block registerBlock(String id, Block block) {
        System.out.println("🌸 注册桃花方块: " + id);
        BLOCKS.put(id, block);
        return Registry.register(Registries.BLOCK, new Identifier("moran-mod", id), block);
    }

    private static void registerBlockItems() {
        System.out.println("🔧 注册桃花方块物品...");
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            String id = entry.getKey();
            Block block = entry.getValue();
            Item item = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, new Identifier("moran-mod", id), item);
            System.out.println("   - 注册物品: " + id);
        }
    }

    public static Block getBlock(String id) {
        return BLOCKS.get(id);
    }
}