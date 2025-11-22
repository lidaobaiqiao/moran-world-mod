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

public class BlockSystem {

    private static final Map<String, Block> BLOCKS = new HashMap<>();

    // -----------------------------------------------------
    // 🍑 桃源维度核心方块（已全部修复为正确 Block 类）
    // -----------------------------------------------------

    // 原木：必须用 PillarBlock，因为它有 axis 属性
    public static final Block PEACH_BLOSSOM_WOOD = registerBlock("peach_blossom_wood",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG)));

    // 木板：普通方块即可
    public static final Block PEACH_BLOSSOM_PLANKS = registerBlock("peach_blossom_planks",
            new Block(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));

    // 树叶：必须用 LeavesBlock，否则没有 decay 属性
    public static final Block PEACH_BLOSSOM_LEAVES = registerBlock("peach_blossom_leaves",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)));

    // 沙子：必须 FallingBlock，否则不会掉落
    public static final Block PEACH_BLOSSOM_SAND = registerBlock("peach_blossom_sand",
            new FallingBlock(FabricBlockSettings.copyOf(Blocks.SAND)));

    // 土：普通 Block
    public static final Block PEACH_BLOSSOM_DIRT = registerBlock("peach_blossom_dirt",
            new Block(FabricBlockSettings.copyOf(Blocks.DIRT)));

    // 草方块：必须 GrassBlock，否则不会变绿、不会蔓延
    public static final Block PEACH_BLOSSOM_GRASS_BLOCK = registerBlock("peach_blossom_grass_block",
            new GrassBlock(FabricBlockSettings.copyOf(Blocks.GRASS_BLOCK)));

    // 灰化土（Podzol）：必须 SnowyBlock，否则顶部雪模型会出错
//    public static final Block PEACH_BLOSSOM_PODZOL = registerBlock("peach_blossom_podzol",
//            new SnowyBlock(FabricBlockSettings.copyOf(Blocks.PODZOL)));


    // -----------------------------------------------------
    // 🪨 墨石方块（普通方块）
    // -----------------------------------------------------
    public static final Block MORAN_BLOCK = registerBlock("moran_block",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE).strength(3.0f, 6.0f).requiresTool()));


    // -----------------------------------------------------
    // 初始化
    // -----------------------------------------------------
    public static void initialize() {
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
