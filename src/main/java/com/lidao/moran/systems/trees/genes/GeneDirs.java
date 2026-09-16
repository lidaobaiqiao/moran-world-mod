package com.lidao.moran.systems.trees.genes;

import java.nio.file.Path;

/**
 * 基因文件目录的解析。
 *
 * <p>优先级:系统属性 {@code moran.genes.dir} → FabricLoader 配置目录
 * (开发期=run/config,发布期=.minecraft/config)下的 {@code moran_mod/genes} →
 * 兜底当前工作目录 {@code config/moran_mod/genes}(无 FabricLoader 的工具环境,
 * 如 simTrees/dump 的 JavaExec)。
 */
public final class GeneDirs {

    private GeneDirs() {
    }

    public static Path geneDir() {
        String override = System.getProperty("moran.genes.dir");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                    .resolve("moran_mod").resolve("genes");
        } catch (Throwable notOnFabricLoader) {
            return Path.of("config", "moran_mod", "genes");
        }
    }
}
