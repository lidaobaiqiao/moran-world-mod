package com.lidao.moran.systems.respawn;

import com.lidao.moran.dimensions.DimensionRegistry; // ✅ 添加导入
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

public class RespawnSystem {

    public static void initialize() {
        // 监听玩家首次进入服务器
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();

            // ✅ 使用 DimensionRegistry 获取维度键
            if (world.getRegistryKey().equals(DimensionRegistry.getDimensionKey("peach_blossom")) &&
                    !hasValidSpawnPoint(world)) {
                handleFirstSpawn(player, world);
            }
        });

        // 监听玩家死亡事件
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, conqueredEnd) -> {
            handlePlayerRespawn(oldPlayer, newPlayer);
        });

        System.out.println("✅ 重生系统已初始化");
    }

    /**
     * 处理玩家首次进入桃花源
     */
    private static void handleFirstSpawn(ServerPlayerEntity player, ServerWorld world) {
        // 给予3秒失明效果
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.BLINDNESS,
                60, // 3秒 (20 ticks/秒)
                0,
                false,
                false
        ));

        // 延迟传送玩家到竹林边缘
        player.getServer().execute(() -> {
            BlockPos spawnPos = calculateBambooGroveSpawn(world);

            // ✅ 修正 teleport 调用（7个参数）
            player.teleport(
                    world,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY() + 1,
                    spawnPos.getZ() + 0.5,
                    180.0f, // yaw
                    0.0f    // pitch
            );

            // 设置临时重生点
            setTemporarySpawnPoint(world, spawnPos);

            // 发送欢迎消息
            player.sendMessage(Text.literal("§a§l你来到了桃花源的竹林边缘..."), false);
            player.sendMessage(Text.literal("§6失明效果将在几秒后消失"), false);

            System.out.println("🎯 玩家 " + player.getName().getString() + " 首次进入桃花源，已设置临时重生点");
        });
    }

    /**
     * 处理玩家重生
     */
    private static void handlePlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        ServerWorld deathWorld = (ServerWorld) oldPlayer.getWorld();

        // ✅ 使用 DimensionRegistry 获取维度键
        if (deathWorld.getRegistryKey().equals(DimensionRegistry.getDimensionKey("peach_blossom"))) {
            // 获取主世界
            ServerWorld overworld = newPlayer.getServer().getOverworld();

            // 获取主世界安全重生点
            BlockPos overworldSpawn = findSafeOverworldSpawn(overworld, newPlayer);

            // ✅ 修正 teleport 调用
            newPlayer.teleport(
                    overworld,
                    overworldSpawn.getX() + 0.5,
                    overworldSpawn.getY() + 1,
                    overworldSpawn.getZ() + 0.5,
                    newPlayer.getYaw(),
                    newPlayer.getPitch()
            );

            // 清除桃花源的临时重生点
            clearTemporarySpawnPoint(deathWorld);

            // 发送死亡消息
            newPlayer.sendMessage(Text.literal("§c§l桃花源的法则不允许亡魂停留..."), false);
            newPlayer.sendMessage(Text.literal("§e你的魂魄被遣返回了人间"), false);

            System.out.println("💀 玩家 " + newPlayer.getName().getString() + " 在桃花源死亡，已送回主世界");
        }
    }

    // ===== 下面的辅助方法保持不变 =====
    private static BlockPos calculateBambooGroveSpawn(ServerWorld world) {
        final int BAMBOO_GROVE_RADIUS = 2000;
        final int SPAWN_DISTANCE_FROM_EDGE = 50;
        final int SPAWN_RADIUS = BAMBOO_GROVE_RADIUS - SPAWN_DISTANCE_FROM_EDGE;

        double angle = world.random.nextDouble() * 2 * Math.PI;
        int x = (int) (SPAWN_RADIUS * Math.cos(angle));
        int z = (int) (SPAWN_RADIUS * Math.sin(angle));
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z);

        BlockPos spawnPos = new BlockPos(x, y, z);
        return ensureSafeSpawnLocation(world, spawnPos);
    }

    private static void setTemporarySpawnPoint(ServerWorld world, BlockPos spawnPos) {
        world.setSpawnPos(spawnPos, 0.0f);
        world.getGameRules().get(GameRules.DO_FIRE_TICK).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false, world.getServer());
    }

    private static void clearTemporarySpawnPoint(ServerWorld world) {
        world.setSpawnPos(new BlockPos(0, -64, 0), 0.0f);
    }

    private static boolean hasValidSpawnPoint(ServerWorld world) {
        BlockPos spawnPos = world.getSpawnPos();
        return spawnPos.getY() >= world.getBottomY() && spawnPos.getY() < world.getTopY();
    }

    private static BlockPos findSafeOverworldSpawn(ServerWorld overworld, ServerPlayerEntity player) {
        BlockPos bedSpawnPos = player.getSpawnPointPosition();
        if (bedSpawnPos != null) {
            ServerWorld bedWorld = overworld.getServer().getWorld(player.getSpawnPointDimension());
            if (bedWorld != null && isSafeSpawnLocation(bedWorld, bedSpawnPos)) {
                return bedSpawnPos;
            }
        }

        BlockPos worldSpawn = overworld.getSpawnPos();
        return isSafeSpawnLocation(overworld, worldSpawn) ? worldSpawn : ensureSafeSpawnLocation(overworld, worldSpawn);
    }

    private static BlockPos ensureSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        if (isSafeSpawnLocation(world, pos)) return pos;

        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockPos newPos = pos.add(dx, 0, dz);
                        int newY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, newPos.getX(), newPos.getZ());
                        newPos = new BlockPos(newPos.getX(), newY, newPos.getZ());
                        if (isSafeSpawnLocation(world, newPos)) return newPos;
                    }
                }
            }
        }
        return pos;
    }

    private static boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        var belowState = world.getBlockState(belowPos);
        if (belowState.isOf(net.minecraft.block.Blocks.WATER) ||
                belowState.isOf(net.minecraft.block.Blocks.LAVA) ||
                pos.getY() < world.getBottomY() + 5) {
            return false;
        }
        return world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir();
    }
}