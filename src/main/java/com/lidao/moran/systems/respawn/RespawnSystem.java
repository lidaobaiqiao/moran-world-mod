package com.lidao.moran.systems.respawn;

import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

public class RespawnSystem {

    public static void initialize() {
        // 监听玩家首次进入服务器
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();

            // 检查玩家是否在桃花源维度且是首次进入
            if (world.getRegistryKey().equals(PeachBlossomDimension.DIMENSION_KEY) &&
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

            // 传送玩家
            player.teleport(
                    world,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY() + 1,
                    spawnPos.getZ() + 0.5,
                    java.util.Set.of(),
                    180.0f, // 面向竹林中心
                    0.0f
            );

            // 设置临时重生点（仅用于首次进入）
            setTemporarySpawnPoint(world, spawnPos);

            // 发送欢迎消息
            player.sendMessage(net.minecraft.text.Text.literal("§a§l你来到了桃花源的竹林边缘..."), false);
            player.sendMessage(net.minecraft.text.Text.literal("§6失明效果将在几秒后消失"), false);

            System.out.println("🎯 玩家 " + player.getName().getString() + " 首次进入桃花源，已设置临时重生点");
        });
    }

    /**
     * 处理玩家重生
     */
    private static void handlePlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        ServerWorld deathWorld = (ServerWorld) oldPlayer.getWorld();

        // 检查玩家是否在桃花源维度死亡
        if (deathWorld.getRegistryKey().equals(PeachBlossomDimension.DIMENSION_KEY)) {
            // 获取主世界
            ServerWorld overworld = newPlayer.getServer().getWorld(ServerWorld.OVERWORLD);

            // 获取主世界安全重生点
            BlockPos overworldSpawn = findSafeOverworldSpawn(overworld, newPlayer);

            // 传送玩家到主世界
            newPlayer.teleport(
                    overworld,
                    overworldSpawn.getX() + 0.5,
                    overworldSpawn.getY() + 1,
                    overworldSpawn.getZ() + 0.5,
                    java.util.Set.of(),
                    newPlayer.getYaw(),
                    newPlayer.getPitch()
            );

            // 清除桃花源的临时重生点
            clearTemporarySpawnPoint(deathWorld);

            // 发送死亡消息
            newPlayer.sendMessage(net.minecraft.text.Text.literal("§c§l桃花源的法则不允许亡魂停留..."), false);
            newPlayer.sendMessage(net.minecraft.text.Text.literal("§e你的魂魄被遣返回了人间"), false);

            System.out.println("💀 玩家 " + newPlayer.getName().getString() + " 在桃花源死亡，已送回主世界");
        }
    }

    /**
     * 计算竹林内圈边缘的出生点
     */
    private static BlockPos calculateBambooGroveSpawn(ServerWorld world) {
        // 竹林内圈参数
        final int BAMBOO_GROVE_RADIUS = 2000;  // 竹林内圈半径
        final int SPAWN_DISTANCE_FROM_EDGE = 50; // 距离边缘50格
        final int SPAWN_RADIUS = BAMBOO_GROVE_RADIUS - SPAWN_DISTANCE_FROM_EDGE;

        // 随机角度 (0-360度)
        double angle = world.random.nextDouble() * 2 * Math.PI;

        // 计算坐标
        int x = (int) (SPAWN_RADIUS * Math.cos(angle));
        int z = (int) (SPAWN_RADIUS * Math.sin(angle));

        // 获取地面高度
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z);

        // 确保出生点安全
        BlockPos spawnPos = new BlockPos(x, y, z);
        spawnPos = ensureSafeSpawnLocation(world, spawnPos);

        System.out.println("🎯 计算桃花源出生点: " + spawnPos + " (距离中心: " + SPAWN_RADIUS + "格)");
        return spawnPos;
    }

    /**
     * 设置临时重生点（仅用于首次进入）
     */
    private static void setTemporarySpawnPoint(ServerWorld world, BlockPos spawnPos) {
        // 设置世界重生点
        world.setSpawnPos(spawnPos, 0.0f);

        // 设置友好的游戏规则（不影响重生逻辑）
        world.getGameRules().get(GameRules.DO_FIRE_TICK).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_GRIEFING).set(false, world.getServer());
    }

    /**
     * 清除临时重生点
     */
    private static void clearTemporarySpawnPoint(ServerWorld world) {
        // 将重生点设置为无效位置（比如世界边界外）
        world.setSpawnPos(new BlockPos(0, -64, 0), 0.0f);
    }

    /**
     * 检查是否有有效的重生点
     */
    private static boolean hasValidSpawnPoint(ServerWorld world) {
        BlockPos spawnPos = world.getSpawnPos();
        return spawnPos.getY() >= world.getBottomY() &&
                spawnPos.getY() < world.getTopY();
    }

    /**
     * 寻找主世界安全重生点
     */
    private static BlockPos findSafeOverworldSpawn(ServerWorld overworld, ServerPlayerEntity player) {
        // 首先尝试玩家的床重生点
        BlockPos bedSpawnPos = player.getSpawnPointPosition();
        if (bedSpawnPos != null) {
            ServerWorld bedWorld = overworld.getServer().getWorld(player.getSpawnPointDimension());
            if (bedWorld != null && isSafeSpawnLocation(bedWorld, bedSpawnPos)) {
                return bedSpawnPos;
            }
        }

        // 如果没有床或床不安全，使用世界出生点
        BlockPos worldSpawn = overworld.getSpawnPos();
        if (isSafeSpawnLocation(overworld, worldSpawn)) {
            return worldSpawn;
        }

        // 如果世界出生点也不安全，寻找附近安全位置
        return ensureSafeSpawnLocation(overworld, worldSpawn);
    }

    /**
     * 确保出生点位置安全
     */
    private static BlockPos ensureSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        // 检查当前位置是否安全
        if (isSafeSpawnLocation(world, pos)) {
            return pos;
        }

        // 如果不安全，在周围寻找安全位置
        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockPos newPos = pos.add(dx, 0, dz);
                        int newY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, newPos.getX(), newPos.getZ());
                        newPos = new BlockPos(newPos.getX(), newY, newPos.getZ());

                        if (isSafeSpawnLocation(world, newPos)) {
                            System.out.println("🔄 调整出生点到安全位置: " + newPos);
                            return newPos;
                        }
                    }
                }
            }
        }

        // 如果找不到安全位置，返回原始位置
        System.out.println("⚠️ 无法找到更安全的出生点，使用原始位置");
        return pos;
    }

    /**
     * 检查位置是否安全（不在水中，有足够的空间）
     */
    private static boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        // 检查脚下方块是否安全
        BlockPos belowPos = pos.down();
        net.minecraft.block.BlockState belowState = world.getBlockState(belowPos);

        // 不允许在水上、熔岩上、虚空出生
        if (belowState.getBlock() == net.minecraft.block.Blocks.WATER ||
                belowState.getBlock() == net.minecraft.block.Blocks.LAVA ||
                pos.getY() < world.getBottomY() + 5) {
            return false;
        }

        // 检查玩家位置是否有足够空间
        BlockPos playerPos = pos;
        BlockPos headPos = pos.up();

        return world.getBlockState(playerPos).isAir() &&
                world.getBlockState(headPos).isAir();
    }
}