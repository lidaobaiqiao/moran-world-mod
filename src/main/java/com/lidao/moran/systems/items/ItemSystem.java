package com.lidao.moran.systems.items;

import com.lidao.moran.MoranMod;
import com.lidao.moran.item.MoranSwordItem;
import com.lidao.moran.systems.blocks.BlockSystem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 墨世界物品注册系统。
 * 物品以类型化静态常量暴露；新增物品加一个常量字段即可。
 */
public class ItemSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoranMod.MOD_ID);

    public static final Item MORAN = register("moran", new Item(new FabricItemSettings()));
    public static final Item QINGFENG_JINGHUA = register("qingfeng", new Item(new FabricItemSettings().maxCount(16)));
    public static final Item MORAN_SWORD = register("moran_sword", new MoranSwordItem());

    // 🍑 桃树相关物品
    public static final Item PEACH_SAPLING = register("peach_sapling",
            new BlockItem(BlockSystem.PEACH_SAPLING, new FabricItemSettings()));
    public static final Item CRYSTAL_SHARD = register("crystal_shard", new Item(new FabricItemSettings()));
    public static final Item IMMORTAL_INGOT = register("immortal_ingot", new Item(new FabricItemSettings()));
    public static final Item BAMBOO_SHOOT = register("bamboo_shoot", new Item(new FabricItemSettings().maxCount(64)));

    // 🎣 竹筏相关物品
    public static final Item BAMBOO_RAFT = register("bamboo_raft", new Item(new FabricItemSettings().maxCount(1)));

    // 🌳 桃源木材料（树枝方块本身是 BlockItem，在 BlockSystem 自动注册）
    public static final Item THICK_PEACH_BRANCH = register("thick_peach_branch", new Item(new FabricItemSettings()));
    public static final Item THICK_PEACH_TRUNK = register("thick_peach_trunk", new Item(new FabricItemSettings()));

    public static void initialize() {
        LOGGER.info("✅ 物品系统就绪（竹筏：乘坐静止 5 秒传送至桃花源）");
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MoranMod.MOD_ID, name), item);
    }
}
