package com.lidao.moran;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import com.lidao.moran.systems.commands.TestTeleportCommand;
import com.lidao.moran.systems.teleport.RaftTeleportHandler;
import com.lidao.moran.systems.items.ItemSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 墨世界模组 - 主类
 * 
 * 一个具有桃花源自定义维度和神秘生物群系的Minecraft模组
 * 让玩家在充满诗意的水墨世界中探索
 * 
 * @author Lidao & AI Assistant
 * @version 1.0.0
 */
public class MoranMod implements ModInitializer {
    
    public static final String MOD_ID = "mo-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("🎭 墨世界模组启动 - 水墨丹青，诗意桃花");
        LOGGER.info("🌸 载入桃花源维度系统...");
        
        // 初始化维度系统
        initializeDimensionSystem();
        
        // 初始化物品系统
        initializeItemSystem();
        
        // 初始化命令系统
        initializeCommandSystem();
        
        // 初始化竹筏传送系统
        initializeRaftTeleportSystem();
        
        LOGGER.info("🎨 墨世界模组初始化完成！");
        LOGGER.info("🌸 桃花源维度已就绪");
        LOGGER.info("💎 墨韵物品系统已激活");
        LOGGER.info("🎣 竹筏传送系统已激活");
        LOGGER.info("🎮 玩家可以开始探索墨世界了！");
    }
    
    /**
     * 初始化维度系统
     */
    private void initializeDimensionSystem() {
        LOGGER.info("🌀 初始化桃花源维度系统...");
        DimensionRegistry.initialize();
        PeachBlossomDimension.register();
    }
    
    /**
     * 初始化物品系统
     */
    private void initializeItemSystem() {
        LOGGER.info("💎 初始化墨韵物品系统...");
        ItemSystem.initialize();
    }
    
    /**
     * 初始化命令系统
     */
    private void initializeCommandSystem() {
        LOGGER.info("⌨️ 初始化传送命令系统...");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TestTeleportCommand.register(dispatcher);
        });
    }
    
    /**
     * 初始化竹筏传送系统
     */
    private void initializeRaftTeleportSystem() {
        LOGGER.info("🎣 初始化竹筏传送系统...");
        
        // 注册服务器tick事件，用于检测竹筏静止
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                RaftTeleportHandler.onPlayerTick(player);
            });
        });
        
        LOGGER.info("✅ 竹筏传送系统已激活 - 乘坐竹筏静止5秒即可传送到桃花源");
    }
}