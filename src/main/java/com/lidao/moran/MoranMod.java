package com.lidao.moran;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.core.config.ConfigManager;
import com.lidao.moran.core.event.WorldEventListener;
import com.lidao.moran.systems.commands.TeleportCommand;
import com.lidao.moran.systems.respawn.RespawnSystem;
import com.lidao.moran.systems.teleport.RaftTeleportHandler;
import com.lidao.moran.systems.items.ItemSystem;
import com.lidao.moran.systems.blocks.BlockSystem;
import com.lidao.moran.systems.entities.EntitySystem;
import com.lidao.moran.core.terrablender.BiomeDataCreator;
import com.lidao.moran.worldgen.PeachSurfaceRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item; // ✅ 新增导入
import net.minecraft.registry.Registries; // ✅ 新增导入
import net.minecraft.registry.Registry; // ✅ 新增导入
import net.minecraft.util.Identifier; // ✅ 新增导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import terrablender.api.TerraBlenderApi;

public class MoranMod implements ModInitializer, TerraBlenderApi {

    public static final String MOD_ID = "moran_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("🎭 墨世界模组启动");

        initializeConfigSystem();
        initializeBlockSystem();
        initializeBiomeSystem();
        initializeDimensionSystem();
        initializeItemSystem();
        initializeEntitySystem();
        initializeCommandSystem();
        initializeRaftTeleportSystem();
        initializeWorldEventListener();
        initializeRespawnSystem();
    }

    @Override
    public void onTerraBlenderInitialized() {
        LOGGER.info("🌍 TerraBlender 初始化中...");
        DimensionRegistry.registerTerraBlenderComponents();
        PeachSurfaceRules.register();
        LOGGER.info("✅ TerraBlender 组件注册完成");
    }

    // ... (后面的 initialize 方法保持不变) ...
    private void initializeConfigSystem() {
        LOGGER.info("⚙️ 初始化墨世界配置系统...");
        ConfigManager.initialize();
        LOGGER.info("✅ 配置系统就绪");
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
        DimensionRegistry.initialize();
        LOGGER.info("✅ 维度系统就绪");
    }

    private void initializeItemSystem() {
        LOGGER.info("💎 初始化墨韵物品系统...");
        ItemSystem.initialize();
    }

    private void initializeEntitySystem() {
        LOGGER.info("👻 初始化墨灵生物系统...");
        EntitySystem.initialize();
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

    private void initializeWorldEventListener() {
        LOGGER.info("🌐 初始化世界事件监听...");
        WorldEventListener.initialize();
        LOGGER.info("✅ 世界事件监听就绪");
    }

    private void initializeRespawnSystem() {
        LOGGER.info("💀 初始化重生遣返系统...");
        RespawnSystem.initialize();
        LOGGER.info("✅ 重生系统就绪");
    }
}
