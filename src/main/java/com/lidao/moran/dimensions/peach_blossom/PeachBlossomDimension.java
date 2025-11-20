// src/main/java/com/lidao/moran/dimensions/peach_blossom/PeachBlossomDimension.java
package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.dimensions.base.BaseDimension;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 桃花源维度 - 当前专注实现的维度
 */
public class PeachBlossomDimension extends BaseDimension {
    public static final String DIMENSION_ID = "peach_blossom";
    public static final RegistryKey<World> DIMENSION_KEY =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier("moran-mod", DIMENSION_ID));

    @Override
    public RegistryKey<World> getDimensionKey() {
        return DIMENSION_KEY;
    }

    @Override
    public String getDimensionId() {
        return DIMENSION_ID;
    }

    @Override
    public void onPlayerEnter(ServerPlayerEntity player) {
        // 欢迎消息和维度特定逻辑
        player.sendMessage(net.minecraft.text.Text.literal("§a§l欢迎来到桃花源！"), false);
        player.sendMessage(net.minecraft.text.Text.literal("§6此地祥和宁静，无怪物侵扰"), false);

        // 设置桃花源特有的游戏规则
        if (player.getWorld().getRegistryKey().equals(DIMENSION_KEY)) {
            player.getWorld().getGameRules().get(net.minecraft.world.GameRules.DO_TILE_DROPS).set(true, player.getServer());
            player.getWorld().getGameRules().get(net.minecraft.world.GameRules.DO_MOB_GRIEFING).set(true, player.getServer());
        }
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        player.sendMessage(net.minecraft.text.Text.literal("§8离开桃花源..."), false);
    }
}