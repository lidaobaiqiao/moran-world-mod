package com.lidao.moran.worldgen;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import com.lidao.moran.systems.blocks.BlockSystem;
import terrablender.api.SurfaceRuleManager;

public class PeachSurfaceRules {
    public static void register() {
        SurfaceRuleManager.addSurfaceRules(
                SurfaceRuleManager.RuleCategory.OVERWORLD,
                "moran_mod",
                makeRules()
        );
    }

    /**
     * Create surface rules with safe block references
     */
    public static MaterialRules.MaterialRule makeRules() {
        // Create conditions using static factory methods
        MaterialRules.MaterialCondition isYaozhuohuayuanBiome = MaterialRules.biome(RegistryKey.of(
                RegistryKeys.BIOME,
                new Identifier("moran_mod", "yaozhuohuayuan")
        ));

        MaterialRules.MaterialCondition isMirrorLakesBiome = MaterialRules.biome(RegistryKey.of(
                RegistryKeys.BIOME,
                new Identifier("moran_mod", "mirror_lakes")
        ));

        // Underwater (strict)
        MaterialRules.MaterialCondition waterCheck =
                MaterialRules.not(MaterialRules.water(0, 0));

        // Grass: 1 layer
        MaterialRules.MaterialCondition grassLayer =
                MaterialRules.stoneDepth(0, false, VerticalSurfaceType.FLOOR);

        // Dirt: 3 layers (math corrected)
        MaterialRules.MaterialCondition dirt3Layers =
                MaterialRules.stoneDepth(2, false, VerticalSurfaceType.FLOOR);

        return MaterialRules.sequence(
                // ===== Yao Zhuo Hua Yuan Biome =====
                MaterialRules.condition(
                        isYaozhuohuayuanBiome,
                        MaterialRules.sequence(
                                // Use runtime block retrieval with safe conversion
                                MaterialRules.condition(
                                        waterCheck,
                                        MaterialRules.condition(
                                                grassLayer,
                                                MaterialRules.block(
                                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_sand")).getDefaultState()
                                                )
                                        )
                                ),

                                // Surface -> grass
                                MaterialRules.condition(
                                        grassLayer,
                                        MaterialRules.block(
                                                net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_grass_block")).getDefaultState()
                                        )
                                ),

                                // Lower 3 layers -> dirt (exclude grass)
                                MaterialRules.condition(
                                        dirt3Layers,
                                        MaterialRules.condition(
                                                MaterialRules.not(grassLayer),
                                                MaterialRules.block(
                                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_dirt")).getDefaultState()
                                                )
                                        )
                                ),

                                // Deeper -> stone
                                MaterialRules.block(
                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_stone")).getDefaultState()
                                )
                        )
                ),

                // ===== Mirror Lakes Biome =====
                MaterialRules.condition(
                        isMirrorLakesBiome,
                        MaterialRules.sequence(
                                MaterialRules.condition(
                                        waterCheck,
                                        MaterialRules.condition(
                                                grassLayer,
                                                MaterialRules.block(
                                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_sand")).getDefaultState()
                                                )
                                        )
                                ),
                                MaterialRules.condition(
                                        grassLayer,
                                        MaterialRules.block(
                                                net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_dirt")).getDefaultState()
                                        )
                                ),
                                MaterialRules.condition(
                                        dirt3Layers,
                                        MaterialRules.condition(
                                                MaterialRules.not(grassLayer),
                                                MaterialRules.block(
                                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_dirt")).getDefaultState()
                                                )
                                        )
                                ),
                                MaterialRules.block(
                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_stone")).getDefaultState()
                                )
                        )
                ),

                // ===== Default fallback =====
                MaterialRules.sequence(
                        MaterialRules.condition(
                                waterCheck,
                                MaterialRules.block(
                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_sand")).getDefaultState()
                                )
                        ),
                        MaterialRules.condition(
                                grassLayer,
                                MaterialRules.block(
                                        net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_grass_block")).getDefaultState()
                                )
                        ),
                        MaterialRules.condition(
                                dirt3Layers,
                                MaterialRules.condition(
                                        MaterialRules.not(grassLayer),
                                        MaterialRules.block(
                                                net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_dirt")).getDefaultState()
                                        )
                                )
                        ),
                        MaterialRules.block(
                                net.minecraft.registry.Registries.BLOCK.get(new Identifier("moran_mod", "peach_blossom_stone")).getDefaultState()
                        )
                )
        );
    }
}
