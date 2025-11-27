package com.lidao.moran;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.systems.commands.TeleportCommand;
import com.lidao.moran.systems.teleport.RaftTeleportHandler;
import com.lidao.moran.systems.items.ItemSystem;
import com.lidao.moran.systems.blocks.BlockSystem;
import com.lidao.moran.core.terrablender.BiomeDataCreator;
import com.lidao.moran.worldgen.PeachSurfaceRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import terrablender.api.TerraBlenderApi; // ✅ 添加导入

/**
 * 墨世界模组 - 主类
 * 初始化顺序：
 * 1. 方块（SurfaceRule 需要引用它们）
 * 2. 生物群系数据（JSON）
 * 3. TerraBlender 组件（必须在 onTerraBlenderInitialized 中）
 * 4. 其他系统
 */
public class MoranMod implements ModInitializer, TerraBlenderApi { // ✅ 实现接口

    public static final String MOD_ID = "moran_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("🎭 墨世界模组启动");

        // 1️⃣ 方块系统（最先）
        initializeBlockSystem();

        // 2️⃣ 生物群系数据（仅创建数据，不注册 TerraBlender）
        initializeBiomeSystem();

        // 3️⃣ 维度系统（只注册维度键，不注册 TerraBlender）
        initializeDimensionSystem();

        // 4️⃣ 其他系统
        initializeItemSystem();
        initializeCommandSystem();
        initializeRaftTeleportSystem();
    }

    // ✅ 新增：TerraBlender 初始化回调
    @Override
    public void onTerraBlenderInitialized() {
        LOGGER.info("🌍 TerraBlender 初始化中...");
        DimensionRegistry.registerTerraBlenderComponents();

        // ✅ 添加这一行
        PeachSurfaceRules.register(); // 立即注册地表规则

        LOGGER.info("✅ TerraBlender 组件注册完成");
    }

    private void initializeBlockSystem() {
        LOGGER.info("⛏️ 初始化墨彩方块系统...");
        BlockSystem.initialize();
        LOGGER.info("✅ 方块系统就绪");
    }

    private void initializeBiomeSystem() {
        LOGGER.info("🌍 初始化生物群系数据...");
        BiomeDataCreator.initialize();
        LOGGER.info("✅ 生物群系数据就绪");
    }

    private void initializeDimensionSystem() {
        LOGGER.info("🌀 初始化维度管理系统...");
        DimensionRegistry.initialize(); // ✅ 现在只注册维度键
        LOGGER.info("✅ 维度系统就绪");
    }

    private void initializeItemSystem() {
        LOGGER.info("💎 初始化墨韵物品系统...");
        ItemSystem.initialize();
    }

    private void initializeCommandSystem() {
        LOGGER.info("⌨️ 初始化传送命令系统...");
        TeleportCommand.initialize();
        LOGGER.info("✅ 命令系统就绪");
    }

    private void initializeRaftTeleportSystem() {
        LOGGER.info("🎣 初始化竹筏传送系统...");
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(RaftTeleportHandler::onPlayerTick);
        });
    }
}