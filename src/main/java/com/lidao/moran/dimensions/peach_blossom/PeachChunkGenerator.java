package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.dimensions.base.BiomeDistributionManager;
import com.lidao.moran.dimensions.base.TerrainGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PeachChunkGenerator extends ChunkGenerator {

    private final TerrainGenerator terrainGenerator;
    private final BiomeDistributionManager biomeManager;
    private final long worldSeed;

    public static final Codec<PeachChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(PeachChunkGenerator::getBiomeSource),
                    Codec.LONG.fieldOf("seed").orElse(0L).forGetter(generator -> generator.worldSeed)
            ).apply(instance, instance.stable(PeachChunkGenerator::new))
    );

    public PeachChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.worldSeed = seed;

        Random terrainRandom = Random.create(seed);
        this.terrainGenerator = new TerrainGenerator(terrainRandom);
        this.biomeManager = new BiomeDistributionManager(terrainRandom);

        System.out.println("🌸 PeachChunkGenerator initialized with seed: " + seed);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public int getWorldHeight() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 62;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        double biomeWeight = getDominantBiomeWeight(x, z);
        double base = terrainGenerator.generateTerrainHeight(x, z, biomeWeight);
        double finalD = terrainGenerator.generateWaterFeatures(x, z, base);

        int h = MathHelper.floor(finalD);
        h = MathHelper.clamp(h, getMinimumY(), getWorldHeight() - 1);
        return h;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        int top = world.getTopY();
        int len = top - bottom;
        BlockState[] states = new BlockState[len];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        return new VerticalBlockSample(bottom, states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Peach Dimension - custom terrain");
        text.add("Seed: " + this.worldSeed);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        int minY = chunk.getBottomY();
        int maxY = chunk.getTopY();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                double biomeWeight = getDominantBiomeWeight(worldX, worldZ);
                double baseH = terrainGenerator.generateTerrainHeight(worldX, worldZ, biomeWeight);
                double finalHD = terrainGenerator.generateWaterFeatures(worldX, worldZ, baseH);
                int finalH = MathHelper.clamp(MathHelper.floor(finalHD), minY, maxY - 1);

                int SURFACE_BUFFER = 3;
                int fillTop = finalH - SURFACE_BUFFER;
                if (fillTop < minY) continue;

                for (int y = minY; y <= fillTop; y++) {
                    boolean nearSurface = y > fillTop - 4;
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (nearSurface) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else {
                        chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    private double getDominantBiomeWeight(int x, int z) {
        double maxWeight = Double.NEGATIVE_INFINITY;
        // 遍历所有我们注册的生物群系
        for (RegistryKey<Biome> biomeKey : com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes.getAllBiomeKeys()) {
            // 从 RegistryKey 获取路径，例如 "peach_valley"
            String biomeIdPath = biomeKey.getValue().getPath();
            // 计算该生物群系在此坐标的权重
            double weight = this.biomeManager.calculateBiomeWeight(x, z, biomeIdPath);
            if (weight > maxWeight) {
                maxWeight = weight;
            }
        }
        return maxWeight;
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                int surfaceY = this.getHeight(worldX, worldZ, Heightmap.Type.WORLD_SURFACE_WG, region, noiseConfig);

                if (surfaceY <= this.getSeaLevel()) {
                    continue;
                }

                // 关键修复：通过 biomeSource 获取当前坐标的生物群系
                // 使用正确的 getBiome 方法
                RegistryEntry<Biome> biomeEntry = region.getBiome(new BlockPos(worldX, 0, worldZ));
                if (biomeEntry == null) continue;

                BlockPos topPos = new BlockPos(worldX, surfaceY - 1, worldZ);
                BlockPos underPos = new BlockPos(worldX, surfaceY - 2, worldZ);

                // 根据不同的生物群系放置不同的自定义方块
                if (biomeEntry.matchesKey(PeachBlossomBiomes.PEACH_VALLEY)) {
                    chunk.setBlockState(topPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK.getDefaultState(), false);
                } else if (biomeEntry.matchesKey(PeachBlossomBiomes.BAMBOO_GROVE)) {
                    chunk.setBlockState(topPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_BLOSSOM_DIRT.getDefaultState(), false);
                } else if (biomeEntry.matchesKey(PeachBlossomBiomes.FARM_PLAINS)) {
                    chunk.setBlockState(topPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_BLOSSOM_DIRT.getDefaultState(), false);
                } else {
                    // 默认情况，放置草方块
                    chunk.setBlockState(topPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_BLOSSOM_GRASS_BLOCK.getDefaultState(), false);
                }

                // 把下一层设置为泥土，防止挖穿后露馅
                if (underPos.getY() >= getMinimumY()) {
                    chunk.setBlockState(underPos, com.lidao.moran.systems.blocks.BlockSystem.PEACH_BLOSSOM_DIRT.getDefaultState(), false);
                }
            }
        }
    }


    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {}

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, net.minecraft.world.biome.source.BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {}

    @Override
    public void populateEntities(ChunkRegion region) {}



    public BiomeSource getBiomeSource() {
        return this.biomeSource;
    }
}
