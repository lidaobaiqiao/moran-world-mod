package com.lidao.moran.dimensions;

public class DimensionConfig {
    // 维度尺寸配置（单位：格）
    public static final int TOTAL_RADIUS = 20000 + 5000 + 5000 + 8000 + 1000; // 39000格总半径

    // 各区域边界（从中心向外）
    public static final int CORE_RADIUS = 1000;           // 核心区：0-1000格
    public static final int LAKE_PLAIN_RADIUS = 9000;     // 桃花源+千盘湖：1000-9000格
    public static final int HILLS_RADIUS = 14000;         // 丘陵：9000-14000格
    public static final int STREAM_RADIUS = 19000;        // 溪流：14000-19000格
    public static final int BORDER_RADIUS = 39000;        // 隐竹之界：19000-39000格

    // 区域宽度
    public static final int CORE_WIDTH = 1000;
    public static final int LAKE_PLAIN_WIDTH = 8000;
    public static final int HILLS_WIDTH = 5000;
    public static final int STREAM_WIDTH = 5000;
    public static final int BORDER_WIDTH = 20000;
}
