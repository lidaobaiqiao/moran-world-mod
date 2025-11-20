// src/main/java/com/lidao/moran/systems/teleport/RaftTeleportHandler.java
package com.lidao.moran.systems.teleport;

import com.lidao.moran.dimensions.DimensionRegistry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 竹筏传送处理器 - 专门处理桃花源传送
 */
public class RaftTeleportHandler {
    private static final Map<UUID, RaftData> playerRaftData = new ConcurrentHashMap<>();
    private static int REQUIRED_SECONDS = 30;
    private static int REQUIRED_TICKS = REQUIRED_SECONDS * 20;

    public static void onPlayerTick(ServerPlayerEntity player) {
        if (player == null) return;

        // 检查玩家是否已在桃花源维度
        if (DimensionRegistry.getDimension("peach_blossom") != null &&
                player.getWorld().getRegistryKey().equals(DimensionRegistry.getDimension("peach_blossom").getDimensionKey())) {
            playerRaftData.remove(player.getUuid());
            return;
        }

        // 检查玩家是否骑乘竹筏
        if (player.getVehicle() instanceof BoatEntity boat) {
            if (boat.getVariant() == BoatEntity.Type.BAMBOO) {
                handleRaftTeleport(player, boat);
            }
        } else {
            playerRaftData.remove(player.getUuid());
        }
    }

    private static void handleRaftTeleport(ServerPlayerEntity player, BoatEntity raft) {
        UUID playerId = player.getUuid();
        RaftData data = playerRaftData.computeIfAbsent(playerId, k -> new RaftData());
        Vec3d currentPos = raft.getPos();

        if (data.lastPosition == null) {
            data.lastPosition = currentPos;
            return;
        }

        if (currentPos.distanceTo(data.lastPosition) < 2.0) {
            data.stationaryTicks++;

            // 每30秒提示一次
            if (data.stationaryTicks % 600 == 0) {
                int secondsLeft = (REQUIRED_TICKS - data.stationaryTicks) / 20;
                player.sendMessage(net.minecraft.text.Text.literal(
                        "§e竹筏在神秘水域静止... §7(" + secondsLeft + "秒后进入桃花源)"), false);
            }

            // 达到设定时间，触发传送
            if (data.stationaryTicks >= REQUIRED_TICKS) {
                System.out.println("🎯 触发传送: " + player.getEntityName());
                triggerDimensionTravel(player);
                return;
            }
        } else {
            if (data.stationaryTicks > 0) {
                System.out.println("移动重置: " + player.getEntityName() + " 原计时: " + data.stationaryTicks);
                data.stationaryTicks = 0;
                player.sendMessage(net.minecraft.text.Text.literal("§c移动打断了传送进程..."), false);
            }
        }

        data.lastPosition = currentPos;
    }

    private static void triggerDimensionTravel(ServerPlayerEntity player) {
        // 使用维度注册表获取桃花源维度
        if (DimensionRegistry.getDimension("peach_blossom") == null) {
            player.sendMessage(net.minecraft.text.Text.literal("§c桃花源维度尚未准备好..."), false);
            return;
        }

        net.minecraft.server.world.ServerWorld targetWorld = player.getServer()
                .getWorld(DimensionRegistry.getDimension("peach_blossom").getDimensionKey());

        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.literal("§c桃花源维度尚未加载..."), false);
            return;
        }

        try {
            // 传送前清除计时数据
            playerRaftData.remove(player.getUuid());

            // 执行传送
            net.fabricmc.fabric.api.dimension.v1.FabricDimensions.teleport(
                    player,
                    targetWorld,
                    new net.minecraft.world.TeleportTarget(
                            new Vec3d(
                                    targetWorld.getSpawnPos().getX() + 0.5,
                                    targetWorld.getSpawnPos().getY() + 1,
                                    targetWorld.getSpawnPos().getZ() + 0.5
                            ),
                            Vec3d.ZERO,
                            player.getYaw(),
                            player.getPitch()
                    )
            );

            // 触发"豁然开朗"体验
            startEnlightenmentProcess(player);

        } catch (Exception e) {
            System.out.println("传送失败: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(net.minecraft.text.Text.literal("§c传送失败: " + e.getMessage()), false);
        }
    }

    // 保持原有的"豁然开朗"体验流程
    private static void startEnlightenmentProcess(ServerPlayerEntity player) {
        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        player.sendMessage(net.minecraft.text.Text.literal("§8§l缘溪行，忘路之远近..."), false);

        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.BLINDNESS, 60, 0
        ));

        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
        player.sendMessage(net.minecraft.text.Text.literal("§5§l忽逢桃花林，夹岸数百步..."), false);
        player.sendMessage(net.minecraft.text.Text.literal("§e§l复行数十步，即将豁然开朗..."), false);

        // 3秒后清除失明
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                player.getServer().execute(() -> {
                    player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
                    player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 100, 0
                    ));

                    player.sendMessage(net.minecraft.text.Text.literal("§a§l豁然开朗！土地平旷，屋舍俨然..."), false);
                    player.sendMessage(net.minecraft.text.Text.literal("§6欢迎来到桃花源！"), false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        spawnEnlightenmentParticles(player);
    }

    private static void spawnEnlightenmentParticles(ServerPlayerEntity player) {
        net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getWorld();

        world.spawnParticles(
                net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 1.5, 1.5, 1.5, 0.1
        );

        world.playSound(
                null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.0f
        );
    }

    public static void setRequiredSeconds(int seconds) {
        REQUIRED_SECONDS = seconds;
        REQUIRED_TICKS = REQUIRED_SECONDS * 20;
        System.out.println("已更新静止时间: " + REQUIRED_SECONDS + " 秒 (" + REQUIRED_TICKS + " ticks)");
    }

    private static class RaftData {
        public int stationaryTicks = 0;
        public Vec3d lastPosition = null;
    }
}