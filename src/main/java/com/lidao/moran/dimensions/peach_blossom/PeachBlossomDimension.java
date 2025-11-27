// src/main/java/com/lidao/moran/dimensions/peach_blossom/PeachBlossomDimension.java
package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.dimensions.base.BaseDimension;
import com.lidao.moran.worldgen.PeachRegion;
import com.lidao.moran.worldgen.PeachSurfaceRules;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import terrablender.api.Region;

public class PeachBlossomDimension extends BaseDimension {
    private static final String ID = "moran_mod:peach_blossom_dimension";
    private static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, new Identifier(ID));

    @Override
    public RegistryKey<World> getDimensionKey() { return KEY; }

    @Override
    public String getDimensionId() { return ID; }

    @Override
    public Region createRegion() {
        // ❌ 移除 seed 参数，Region 不需要它
        return new PeachRegion(new Identifier("moran_mod", "peach_dimension"));
    }

    @Override
    public void registerSurfaceRules() {
        PeachSurfaceRules.register();
    }
}