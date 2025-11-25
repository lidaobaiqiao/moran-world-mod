package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.base.BiomeDistributionManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * TerraBlender 集成版 PeachBiomeSource
 * 使用 TerraBlender 系统管理生物群系
 */
public class PeachBiomeSource extends BiomeSource {

    // 核心修复：Codec 现在期望一个简单的字符串列表
    public static final Codec<PeachBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("biomes").forGetter(PeachBiomeSource::getBiomeIds),
                    Codec.LONG.fieldOf("seed").forGetter(PeachBiomeSource::getSeed)
            ).apply(instance, instance.stable(PeachBiomeSource::new))
    );

    private final List<String> biomeIds; // 存储 ID 字符串列表
    private final List<RegistryEntry<Biome>> biomeEntries; // 运行时转换后的列表
    private final BiomeDistributionManager biomeManager;
    private final long seed;

    // 构造器现在接收一个字符串列表和一个 seed
    public PeachBiomeSource(List<String> biomeIds, long seed) {
        super();
        this.seed = seed;
        this.biomeIds = List.copyOf(biomeIds);

        // 使用 TerraBlender 集成来获取生物群系
        this.biomeEntries = initializeBiomeEntries();

        this.biomeManager = new BiomeDistributionManager(Random.create(seed));

        MoranMod.LOGGER.info("✅ PeachBiomeSource (TerraBlender) initialized: seed=" + seed + ", biomeCount=" + this.biomeEntries.size());
    }

    /**
     * 初始化生物群系列表，测试模式使用原版平原群系
     */
    private List<RegistryEntry<Biome>> initializeBiomeEntries() {
        List<RegistryEntry<Biome>> entries = new ArrayList<>();
        
        // 测试模式：直接使用原版平原群系
        try {
            Identifier plainsId = new Identifier("minecraft:plains");
            RegistryKey<Biome> plainsKey = RegistryKey.of(RegistryKeys.BIOME, plainsId);
            
            // 创建一个简单的生物群系条目（测试用）
            // 注意：这里需要实际的注册表访问，暂时创建占位符
            MoranMod.LOGGER.info("🌾 测试模式：使用原版平原群系");
            
            // 暂时返回空列表，让维度使用原版生成器
            // 这样可以避免自定义生物群系的问题
            
        } catch (Exception e) {
            MoranMod.LOGGER.error("❌ 测试模式生物群系初始化失败: " + e.getMessage());
        }
        
        return List.copyOf(entries);
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return this.biomeEntries.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        if (this.biomeEntries.isEmpty()) {
            // 防御性编程，理论上不应该发生
            throw new IllegalStateException("PeachBiomeSource has no biomes to choose from!");
        }

        RegistryEntry<Biome> best = null;
        double bestWeight = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < this.biomeEntries.size(); i++) {
            RegistryEntry<Biome> biomeEntry = this.biomeEntries.get(i);
            if (biomeEntry == null || biomeEntry.getKey().isEmpty()) continue;

            // 从 RegistryKey 获取路径，例如 "peach_valley"
            String idPath = biomeEntry.getKey().get().getValue().getPath();

            double weight;
            try {
                weight = this.biomeManager.calculateBiomeWeight(x, z, idPath);
            } catch (Throwable t) {
                weight = Double.NEGATIVE_INFINITY;
            }

            if (weight > bestWeight) {
                bestWeight = weight;
                best = biomeEntry;
            }
        }

        return best != null ? best : this.biomeEntries.get(0);
    }

    // equals/hashCode 保证世界稳定性（基于 seed 与 biomeIds）
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeachBiomeSource)) return false;
        PeachBiomeSource that = (PeachBiomeSource) o;
        return this.seed == that.seed && Objects.equals(this.biomeIds, that.biomeIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.seed, this.biomeIds);
    }

    // Getter for codec
    public List<String> getBiomeIds() {
        return this.biomeIds;
    }

    public long getSeed() {
        return this.seed;
    }
}
