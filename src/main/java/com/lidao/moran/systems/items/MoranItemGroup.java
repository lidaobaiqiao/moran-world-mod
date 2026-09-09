package com.lidao.moran.systems.items;

import com.lidao.moran.MoranMod;
import com.lidao.moran.systems.blocks.BlockSystem;
import com.lidao.moran.systems.entities.EntitySystem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 墨世界专属创造模式物品栏。
 * 所有模组物品集中收录，按「材料 → 工具 → 方块 → 生物」分区排列。
 */
public class MoranItemGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoranMod.MOD_ID);

    public static final ItemGroup MORAN_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MoranMod.MOD_ID, "main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ItemSystem.MORAN))
                    .displayName(Text.translatable("itemgroup.moran_mod.main"))
                    .entries((context, entries) -> {
                        // 材料
                        entries.add(ItemSystem.MORAN);
                        entries.add(ItemSystem.QINGFENG_JINGHUA);
                        entries.add(ItemSystem.CRYSTAL_SHARD);
                        entries.add(ItemSystem.IMMORTAL_INGOT);
                        // 工具
                        entries.add(ItemSystem.MORAN_SWORD);
                        entries.add(ItemSystem.BAMBOO_RAFT);
                        // 桃木系方块
                        entries.add(BlockSystem.PEACH_LOG);
                        entries.add(BlockSystem.PEACH_BLOSSOM_LEAVES);
                        entries.add(BlockSystem.PEACH_FALLEN_LEAVES);
                        entries.add(BlockSystem.PEACH_SAPLING);
                        // 地面系方块
                        entries.add(BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK);
                        entries.add(BlockSystem.PEACH_BLOSSOM_DIRT);
                        entries.add(BlockSystem.PEACH_BLOSSOM_STONE);
                        entries.add(BlockSystem.PEACH_BLOSSOM_SAND);
                        entries.add(BlockSystem.ANCIENT_PEACH_REALM_STONE);
                        // 生物
                        entries.add(EntitySystem.MOLING_SPAWN_EGG);
                    })
                    .build()
    );

    public static void initialize() {
        LOGGER.info("📦 墨世界专属物品栏已注册，收录全部模组物品");
    }
}
