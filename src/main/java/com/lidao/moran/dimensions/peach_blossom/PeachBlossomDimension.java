package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.dimensions.base.BaseDimension;
import com.lidao.moran.dimensions.base.TerrainGenerator;
import com.lidao.moran.dimensions.base.BiomeDistributionManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class PeachBlossomDimension extends BaseDimension {
    public static final String DIMENSION_ID = "peach_blossom";
    public static final RegistryKey<World> DIMENSION_KEY =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier("moran-mod", DIMENSION_ID));

    private TerrainGenerator terrainGenerator;
    private BiomeDistributionManager biomeManager;

    @Override
    public void registerBiomes() {
        System.out.println("🌸 初始化桃花源生物群系地形系统...");

        // 初始化地形生成器
        Random random = Random.create(12345L);
        this.terrainGenerator = new TerrainGenerator(random);
        this.biomeManager = new BiomeDistributionManager(random);

        System.out.println("✅ 桃花源地形噪声系统已初始化");

        // 注册生物群系到Minecraft系统
        registerBiomesToRegistry();
    }

    /**
     * 注册生物群系到游戏注册表
     */
    private void registerBiomesToRegistry() {
        // 这里将在后续步骤中实现具体的生物群系注册
        System.out.println("📝 准备注册桃花源七大生物群系...");

        for (RegistryKey<Biome> biomeKey : PeachBlossomBiomes.getAllBiomeKeys()) {
            System.out.println("  - " + biomeKey.getValue());
        }

        System.out.println("✅ 桃花源生物群系注册准备完成");
    }

    /**
     * 获取自定义区块生成器类
     */
    public Class<? extends ChunkGenerator> getChunkGeneratorClass() {
        return PeachChunkGenerator.class;
    }

    /**
     * 获取自定义生物群系源类
     */
    public Class<? extends BiomeSource> getBiomeSourceClass() {
        return PeachBiomeSource.class;
    }

    @Override
    public RegistryKey<World> getDimensionKey() {
        return DIMENSION_KEY;
    }

    @Override
    public String getDimensionId() {
        return DIMENSION_ID;
    }

    // 获取地形生成器（供世界生成系统调用）
    public TerrainGenerator getTerrainGenerator() {
        if (this.terrainGenerator == null) {
            // 延迟初始化
            Random random = Random.create(12345L);
            this.terrainGenerator = new TerrainGenerator(random);
        }
        return this.terrainGenerator;
    }

    // 获取生物群系管理器
    public BiomeDistributionManager getBiomeManager() {
        if (this.biomeManager == null) {
            // 延迟初始化
            Random random = Random.create(12345L);
            this.biomeManager = new BiomeDistributionManager(random);
        }
        return this.biomeManager;
    }

    @Override
    public void registerFeatures() {
        System.out.println("🌳 注册桃花源特征...");
        // 这里将注册桃花树、古代亭台等特征
    }

    @Override
    public void registerStructures() {
        System.out.println("🏯 注册桃花源结构...");
        // 这里将注册桃源村落等结构
    }

    @Override
    public void onPlayerEnter(ServerPlayerEntity player) {
        player.sendMessage(net.minecraft.text.Text.literal("§a§l欢迎来到桃花源！"), false);
        player.sendMessage(net.minecraft.text.Text.literal("§6此地祥和宁静，无怪物侵扰"), false);

        if (player.getWorld().getRegistryKey().equals(DIMENSION_KEY)) {
            player.getWorld().getGameRules().get(net.minecraft.world.GameRules.DO_TILE_DROPS).set(true, player.getServer());
            player.getWorld().getGameRules().get(net.minecraft.world.GameRules.DO_MOB_GRIEFING).set(true, player.getServer());
        }
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        player.sendMessage(net.minecraft.text.Text.literal("§8离开桃花源..."), false);
    }
}