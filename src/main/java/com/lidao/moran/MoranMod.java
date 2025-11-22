package com.lidao.moran;

import com.lidao.moran.core.config.ModConfig;
import com.lidao.moran.core.event.WorldEventListener;
import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import com.lidao.moran.systems.blocks.BlockSystem;
import com.lidao.moran.systems.commands.DebugCommands;
import com.lidao.moran.systems.items.ItemSystem;
import com.lidao.moran.systems.respawn.RespawnSystem; // 新增导入
import com.lidao.moran.systems.teleport.RaftTeleportHandler;
import com.lidao.moran.systems.worldgen.WorldGenSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
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

        // 1. 加载配置
        ModConfig.loadConfig();

        // 2. 注册维度系统
        DimensionRegistry.initialize();

        // 3. 初始化各系统
        initializeSystems();

        // 4. 注册世界事件监听器
        WorldEventListener.initialize();

        // 5. 注册重生系统 - 新增
        RespawnSystem.initialize();

        LOGGER.info("✅ 墨世界模组初始化完成！");
    }

    private void registerDimensions() {
        // 专注于桃花源维度
        if (ModConfig.Dimensions.ENABLE_PEACH_BLOSSOM) {
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
        // 应用配置到传送系统
        RaftTeleportHandler.setRequiredSeconds(ModConfig.Teleport.RAFT_STATIONARY_SECONDS);

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