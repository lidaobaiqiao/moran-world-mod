package com.lidao.moran.systems.entities;

import com.lidao.moran.MoranMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 墨世界实体注册系统。
 * 全部注册走显式 initialize()，类型化常量直接引用，不走字符串查找。
 */
public class EntitySystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoranMod.MOD_ID);

    public static final EntityType<MolingEntity> MOLING = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MoranMod.MOD_ID, "moling"),
            FabricEntityTypeBuilder.create(SpawnGroup.AMBIENT, MolingEntity::new)
                    .dimensions(EntityDimensions.changing(0.7F, 0.7F))
                    .build()
    );

    public static final Item MOLING_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            new Identifier(MoranMod.MOD_ID, "moling_spawn_egg"),
            new SpawnEggItem(MOLING, 0x1A1A24, 0x9FE8EF, new FabricItemSettings())
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(MOLING, MolingEntity.createMolingAttributes());
        registerItemGroupEntry();
        registerSpawns();
        LOGGER.info("✅ 实体系统就绪：墨灵已注册并接入桃花源群系");
    }

    private static void registerItemGroupEntry() {
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(MOLING_SPAWN_EGG));
    }

    /**
     * 墨灵只在桃花源维度的群系内出没，主世界不受影响。
     * 用 AMBIENT 分类（同原版蝙蝠的刷怪规则）：地表夜间与地下洞穴都会刷，
     * 与「夜晚无敌对」的设计不冲突——墨灵是和平生物。
     */
    private static void registerSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        biomeKey("yaozhuohuayuan"),
                        biomeKey("peach_valley"),
                        biomeKey("bamboo_grove"),
                        biomeKey("farm_plains"),
                        biomeKey("green_hills"),
                        biomeKey("blossom_stream"),
                        biomeKey("mirror_lakes"),
                        biomeKey("hidden_depths")
                ),
                SpawnGroup.AMBIENT,
                MOLING,
                8, 1, 2
        );
    }

    private static RegistryKey<Biome> biomeKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, new Identifier(MoranMod.MOD_ID, name));
    }
}
