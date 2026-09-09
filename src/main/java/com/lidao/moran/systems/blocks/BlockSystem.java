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
 * 墨世界完整方块系统 - 包含所有桃花功能
 */
public class BlockSystem {

    private static final Map<String, Block> BLOCKS = new HashMap<>();

    // 🌸 最核心的桃花树方块
    public static final Block PEACH_LOG = registerBlock("peach_log",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG)));
    
    public static final Block PEACH_BLOSSOM_LEAVES = registerBlock("peach_blossom_leaves",
            new PeachBlossomLeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)
                .allowsSpawning((state, world, pos, type) -> false)
                .dropsNothing()));
    
    // 🍃 落花堆（特殊透明方块）
    public static final Block PEACH_FALLEN_LEAVES = registerBlock("peach_fallen_leaves",
            new Block(FabricBlockSettings.copyOf(Blocks.SAND)
                .strength(0.1f)
                .nonOpaque()
                .allowsSpawning((state, world, pos, type) -> false)));

    public static final Block PEACH_SAPLING = registerBlock("peach_sapling",
            new PeachSaplingBlock(new PeachSaplingGenerator(), 
                FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)
                    .noCollision()
                    .breakInstantly()));

    // 🌱 桃园基础方块（带自定义逻辑）
    public static final Block PEACH_BLOSSOM_DIRT = registerBlock("peach_blossom_dirt",
            new PeachBlossomDirtBlock(FabricBlockSettings.copyOf(Blocks.DIRT)));

    public static final Block PEACH_BLOSSOM_GRASS_BLOCK = registerBlock("peach_blossom_grass_block",
            new PeachBlossomGrassBlock(FabricBlockSettings.copyOf(Blocks.GRASS_BLOCK)));

    public static final Block PEACH_BLOSSOM_STONE = registerBlock("peach_blossom_stone",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE)));
    
    public static final Block PEACH_BLOSSOM_SAND = registerBlock("peach_blossom_sand",
            new SandBlock(0xF4D1AE, FabricBlockSettings.copyOf(Blocks.SAND)));
    
    public static final Block ANCIENT_PEACH_REALM_STONE = registerBlock("ancient_peach_realm_stone",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE)));

    // 初始化
    public static void initialize() {
        registerBlockItems();
        System.out.println("✅ 墨世界完整方块系统初始化完成");
        System.out.println("   已注册 " + BLOCKS.size() + " 个方块 (包含所有桃花逻辑)");
        
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
        return Registry.register(Registries.BLOCK, new Identifier("moran_mod", id), block);
    }

    private static void registerBlockItems() {
        System.out.println("🔧 注册桃花方块物品...");
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            String id = entry.getKey();
            // 跳过PEACH_SAPLING，因为已经在ItemSystem中处理
            if ("peach_sapling".equals(id)) {
                continue;
            }
            Block block = entry.getValue();
            Item item = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, new Identifier("moran_mod", id), item);
            System.out.println("   - 注册物品: " + id);
        }
    }

    public static Block getBlock(String id) {
        return BLOCKS.get(id);
    }

    public static java.util.Collection<Block> getAllBlocks() {
        return BLOCKS.values();
    }
}