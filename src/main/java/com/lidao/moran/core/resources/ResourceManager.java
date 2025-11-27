// src/main/java/com/lidao/moran/core/resources/ResourceManager.java
package com.lidao.moran.core.resources;

import net.minecraft.util.Identifier;

public class ResourceManager {

    // 纹理路径管理
    public static class Textures {
        public static Identifier getItemTexture(String itemId) {
            return new Identifier("moran_mod", "item/" + itemId);
        }

        public static Identifier getBlockTexture(String blockId) {
            return new Identifier("moran_mod", "block/" + blockId);
        }

        public static Identifier getEntityTexture(String entityId) {
            return new Identifier("moran_mod", "entity/" + entityId);
        }
    }

    // 音效路径管理
    public static class Sounds {
        public static Identifier getSound(String soundId) {
            return new Identifier("moran_mod", soundId);
        }
    }

    // 数据文件路径管理
    public static class Data {
        public static Identifier getDimensionJson(String dimensionId) {
            return new Identifier("moran_mod", "dimension/" + dimensionId + ".json");
        }

        public static Identifier getBiomeJson(String biomeId) {
            return new Identifier("moran_mod", "worldgen/biome/" + biomeId + ".json");
        }

        public static Identifier getStructureJson(String structureId) {
            return new Identifier("moran_mod", "worldgen/structure/" + structureId + ".json");
        }
    }
}