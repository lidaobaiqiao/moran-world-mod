// src/main/java/com/lidao/moran/core/config/ModConfig.java
package com.lidao.moran.core.config;

public class ModConfig {
    // 传送相关配置
    public static class Teleport {
        public static int RAFT_STATIONARY_SECONDS = 30;
        public static boolean ENABLE_RAFT_TELEPORT = true;
    }

    // 维度相关配置
    public static class Dimensions {
        public static boolean ENABLE_PEACH_BLOSSOM = true;
        public static boolean ENABLE_WIND_REALM = false; // 预留
        public static boolean ENABLE_MEMORY_LANE = false; // 预留
    }

    // 游戏机制配置
    public static class Gameplay {
        public static boolean PEACEFUL_MOBS_ONLY = true;
        public static boolean ENABLE_CUSTOM_BIOMES = true;
    }

    public static void loadConfig() {
        // 这里可以添加从配置文件读取的逻辑
        System.out.println("⚙️ 模组配置已加载");
    }
}