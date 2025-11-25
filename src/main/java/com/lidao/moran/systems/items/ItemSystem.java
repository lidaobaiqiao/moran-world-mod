package com.lidao.moran.systems.items;

import com.lidao.moran.item.MoranSwordItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ItemSystem {
    private static final Map<String, Item> ITEMS = new HashMap<>();

    public static final Item MORAN = register("moran", new Item(new FabricItemSettings()));
    public static final Item QINGFENG_JINGHUA = register("qingfeng", new Item(new FabricItemSettings().maxCount(16)));
    public static final Item MORAN_SWORD = register("moran_sword", new MoranSwordItem());
    
    // 🍑 桃树相关物品
    public static final Item PEACH_SAPLING = register("peach_sapling", new Item(new FabricItemSettings()));
    public static final Item CRYSTAL_SHARD = register("crystal_shard", new Item(new FabricItemSettings()));
    public static final Item IMMORTAL_INGOT = register("immortal_ingot", new Item(new FabricItemSettings()));
    public static final Item BAMBOO_SHOOT = register("bamboo_shoot", new Item(new FabricItemSettings().maxCount(64)));
    
    // 🎣 竹筏相关物品
    public static final Item BAMBOO_RAFT = register("bamboo_raft", new Item(new FabricItemSettings().maxCount(1)));

    public static void initialize() {
        System.out.println("✅ 物品系统初始化完成");
        System.out.println("   已注册 " + ITEMS.size() + " 个物品");
        System.out.println("   🎣 竹筏已注册 - 乘坐竹筏静止5秒可传送到桃花源");
    }

    private static Item register(String id, Item item) {
        ITEMS.put(id, item);
        return Registry.register(Registries.ITEM, new Identifier("moran-mod", id), item);
    }

    public static Item getItem(String id) {
        return ITEMS.get(id);
    }

    public static Map<String, Item> getAllItems() {
        return new HashMap<>(ITEMS);
    }
}