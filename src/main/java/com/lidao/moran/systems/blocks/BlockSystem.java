package com.lidao.moran.systems.blocks;

import com.lidao.moran.MoranMod;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 墨世界方块注册系统。
 * 方块以类型化静态常量暴露；物品 ID 由注册表反查生成，杜绝字符串拼写错误。
 * 新增方块：加一个常量字段即可，无需再改任何地方。
 */
public class BlockSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoranMod.MOD_ID);

    private static final List<Block> ALL_BLOCKS = new ArrayList<>();

    // 🌸 桃花树方块
    public static final Block PEACH_LOG = register("peach_log",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG)));

    public static final Block PEACH_BLOSSOM_LEAVES = register("peach_blossom_leaves",
            new PeachBlossomLeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)
                    .allowsSpawning((state, world, pos, type) -> false)
                    .dropsNothing()));

    public static final Block PEACH_FALLEN_LEAVES = register("peach_fallen_leaves",
            new Block(FabricBlockSettings.copyOf(Blocks.SAND)
                    .strength(0.1f)
                    .nonOpaque()
                    .allowsSpawning((state, world, pos, type) -> false)));

    public static final Block PEACH_SAPLING = register("peach_sapling",
            new PeachSaplingBlock(new PeachSaplingGenerator(),
                    FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)
                            .noCollision()
                            .breakInstantly()));

    // 🌱 桃源基础方块（带自定义逻辑）
    public static final Block PEACH_BLOSSOM_DIRT = register("peach_blossom_dirt",
            new PeachBlossomDirtBlock(FabricBlockSettings.copyOf(Blocks.DIRT)));

    public static final Block PEACH_BLOSSOM_GRASS_BLOCK = register("peach_blossom_grass_block",
            new PeachBlossomGrassBlock(FabricBlockSettings.copyOf(Blocks.GRASS_BLOCK)));

    public static final Block PEACH_BLOSSOM_STONE = register("peach_blossom_stone",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE)));

    public static final Block PEACH_BLOSSOM_SAND = register("peach_blossom_sand",
            new SandBlock(0xF4D1AE, FabricBlockSettings.copyOf(Blocks.SAND)));

    public static final Block ANCIENT_PEACH_REALM_STONE = register("ancient_peach_realm_stone",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE)));

    /**
     * 为所有方块注册 BlockItem（树苗除外——其物品在 ItemSystem 注册）。
     */
    public static void initialize() {
        for (Block block : ALL_BLOCKS) {
            if (block == PEACH_SAPLING) {
                continue;
            }
            Identifier id = Registries.BLOCK.getId(block);
            Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        }
        LOGGER.info("✅ 方块系统就绪：{} 个方块及其物品已注册", ALL_BLOCKS.size());
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, new Identifier(MoranMod.MOD_ID, name), block);
        ALL_BLOCKS.add(registered);
        return registered;
    }

    public static Collection<Block> getAllBlocks() {
        return ALL_BLOCKS;
    }
}
