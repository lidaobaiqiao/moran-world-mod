package com.lidao.moran;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import com.lidao.moran.systems.commands.TestTeleportCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
        
        // 初始化命令系统
        initializeCommandSystem();
        
        LOGGER.info("🎨 墨世界模组初始化完成！");
        LOGGER.info("🌸 桃花源维度已就绪");
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
     * 初始化命令系统
     */
    private void initializeCommandSystem() {
        LOGGER.info("⌨️ 初始化传送命令系统...");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TestTeleportCommand.register(dispatcher);
        });
    }
}