package com.lidao.moran;

import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.core.config.ConfigManager;
import com.lidao.moran.core.config.ModConfig;
import com.lidao.moran.core.event.WorldEventListener;
import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import com.lidao.moran.dimensions.peach_blossom.PeachBiomeSource;
import com.lidao.moran.dimensions.peach_blossom.PeachChunkGenerator;
import com.lidao.moran.systems.blocks.BlockSystem;
import com.lidao.moran.systems.commands.DebugCommands;
import com.lidao.moran.systems.items.ItemSystem;
import com.lidao.moran.systems.respawn.RespawnSystem;
import com.lidao.moran.systems.teleport.RaftTeleportHandler;
import com.lidao.moran.systems.worldgen.WorldGenSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoranMod implements ModInitializer {
    public static final String MOD_ID = "moran-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("🔄 初始化墨世界模组...");

        // 0. 检查前置模组依赖
        boolean allDependenciesLoaded = DependencyManager.checkDependencies();
        
        // 打印前置模组版本信息
        DependencyManager.printDependencyVersions();
        
        // 即使有依赖缺失，也继续初始化基础功能
        if (!allDependenciesLoaded) {
            LOGGER.warn("⚠️ 部分前置模组缺失，某些功能可能不可用");
        } else {
            LOGGER.info("✅ 所有前置模组依赖检查通过！");
        }

        // 打印前置模组版本信息
        DependencyManager.printDependencyVersions();

        // 1. 初始化配置系统
        com.lidao.moran.core.config.ConfigManager.initialize();

        // 2. 初始化 TerraBlender 集成
        initializeTerrablender();
        
        // 3. 注册自定义生成器类型
        registerChunkGenerators();
        
        // 4. 注册维度系统
        DimensionRegistry.initialize();

        // 5. 初始化各系统
        initializeSystems();

        // 6. 注册世界事件监听器
        WorldEventListener.initialize();

        // 7. 注册重生系统
        RespawnSystem.initialize();

        LOGGER.info("✅ 墨世界模组初始化完成！");
    }

    private void initializeTerrablender() {
        if (DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            LOGGER.info("🌍 TerraBlender 已加载，生物群系系统将使用高级管理");
            // TerraBlender 会自动通过 terrablender.json 调用我们的集成类
        } else {
            LOGGER.warn("⚠️ TerraBlender 未加载，将使用基础生物群系系统");
        }
    }

    private void registerChunkGenerators() {
        // 测试模式：暂时不注册自定义生成器，使用原版生成器
        LOGGER.info("🏔️ 测试模式：使用原版生成器，跳过自定义生成器注册");
    }

    private void registerDimensions() {
        // 专注于桃花源维度
        if (ConfigManager.isDimensionEnabled("peach_blossom")) {
            DimensionRegistry.registerDimension(new PeachBlossomDimension());
            LOGGER.info("🌸 桃花源维度已注册");
        }
    }

    private void initializeSystems() {
        // 初始化物品系统
        ItemSystem.initialize();

        // 初始化方块系统
        BlockSystem.initialize();

        // 初始化世界生成
        WorldGenSystem.initialize();

        // 初始化调试命令
        DebugCommands.initialize();

        // 初始化传送系统
        initializeTeleportSystem();
    }

    private void initializeTeleportSystem() {
        // 测试模式：强制设置为5秒
        RaftTeleportHandler.setRequiredSeconds(5);

        if (ModConfig.Teleport.ENABLE_RAFT_TELEPORT) {
            ServerTickEvents.START_SERVER_TICK.register(server -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    RaftTeleportHandler.onPlayerTick(player);
                }
            });
            LOGGER.info("🛶 竹筏传送系统已启用");
        } else {
            LOGGER.info("⏸️ 竹筏传送系统已禁用");
        }
    }
}