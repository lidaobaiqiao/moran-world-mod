package com.lidao.moran.systems.teleport;

import com.lidao.moran.dimensions.DimensionRegistry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 竹筏传送处理器——「缘溪行」入口。
 *
 * 仅竹筏（BAMBOO 船型）触发；玩家乘筏漂流 5 秒（缓慢漂移视为顺流，急划打断），
 * 传送至桃花源后进入「豁然开朗」过场：旁观者视角掠过 3 秒，终了回到生存。
 * 过场以 tick 计数在服务端主线程推进，不使用额外线程。
 */
public class RaftTeleportHandler {
    private static final Map<UUID, RaftData> playerRaftData = new ConcurrentHashMap<>();
    /** 「豁然开朗」过场倒计时（tick），按玩家分开计 */
    private static final Map<UUID, Integer> enlightenmentTicks = new ConcurrentHashMap<>();
    private static final int ENLIGHTENMENT_TOTAL = 60; // 3 秒
    private static int REQUIRED_SECONDS = 5;
    private static int REQUIRED_TICKS = REQUIRED_SECONDS * 20;

    public static void onPlayerTick(ServerPlayerEntity player) {
        if (player == null) return;

        // 「豁然开朗」过场推进（优先于竹筏逻辑——此时玩家已在桃花源）
        Integer remaining = enlightenmentTicks.get(player.getUuid());
        if (remaining != null) {
            tickEnlightenment(player, remaining);
            return;
        }

        // 已在桃花源维度：无需竹筏逻辑
        if (DimensionRegistry.getDimensionKey("peach_blossom") != null &&
                player.getWorld().getRegistryKey().equals(DimensionRegistry.getDimensionKey("peach_blossom"))) {
            playerRaftData.remove(player.getUuid());
            return;
        }

        // 仅竹筏（BAMBOO 船型）触发
        if (player.getVehicle() instanceof BoatEntity boat && boat.getVariant() == BoatEntity.Type.BAMBOO) {
            handleRaftTeleport(player, boat);
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

        // 漂流判定：乘筏顺流缓慢漂移视为静止（缘溪行），急划重置
        if (currentPos.distanceTo(data.lastPosition) < 2.0) {
            data.stationaryTicks++;

            // 每秒提示一次剩余时间
            if (data.stationaryTicks % 20 == 0) {
                int secondsLeft = (REQUIRED_TICKS - data.stationaryTicks) / 20;
                player.sendMessage(Text.literal(
                        "§e缘溪行：竹筏漂流中... §7(" + secondsLeft + "秒后忘路之远近)"), false);
            }

            if (data.stationaryTicks >= REQUIRED_TICKS) {
                triggerDimensionTravel(player);
                return;
            }
        } else {
            if (data.stationaryTicks > 0) {
                data.stationaryTicks = 0;
                player.sendMessage(Text.literal("§c急桨打断了漂流..."), false);
            }
        }

        data.lastPosition = currentPos;
    }

    private static void triggerDimensionTravel(ServerPlayerEntity player) {
        if (DimensionRegistry.getDimensionKey("peach_blossom") == null) {
            player.sendMessage(Text.literal("§c桃花源维度尚未准备好..."), false);
            return;
        }
        net.minecraft.server.world.ServerWorld targetWorld = player.getServer()
                .getWorld(DimensionRegistry.getDimensionKey("peach_blossom"));
        if (targetWorld == null) {
            player.sendMessage(Text.literal("§c桃花源维度尚未加载..."), false);
            return;
        }

        try {
            playerRaftData.remove(player.getUuid());
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
            startEnlightenmentProcess(player);
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c传送失败: " + e.getMessage()), false);
        }
    }

    /**
     * 「豁然开朗」过场：旁观者视角掠过 3 秒（SPECTATOR），
     * 终了回到生存并点亮夜视。阶段文案按《桃花源记》行进。
     */
    private static void startEnlightenmentProcess(ServerPlayerEntity player) {
        enlightenmentTicks.put(player.getUuid(), ENLIGHTENMENT_TOTAL);
        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        player.sendMessage(Text.literal("§8§l缘溪行，忘路之远近..."), false);
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.BLINDNESS, 70, 0));
        spawnEnlightenmentParticles(player);
    }

    /** 过场推进：中段文案 → 终了回生存、点亮夜视 */
    private static void tickEnlightenment(ServerPlayerEntity player, int remaining) {
        if (remaining == ENLIGHTENMENT_TOTAL - 30) {
            player.sendMessage(Text.literal("§5§l忽逢桃花林，夹岸数百步..."), false);
            player.sendMessage(Text.literal("§e§l复行数十步，即将豁然开朗..."), false);
        }
        if (remaining <= 1) {
            enlightenmentTicks.remove(player.getUuid());
            player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 100, 0));
            player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
            player.sendMessage(Text.literal("§a§l豁然开朗！土地平旷，屋舍俨然..."), false);
            player.sendMessage(Text.literal("§6欢迎来到桃花源！"), false);
            return;
        }
        enlightenmentTicks.put(player.getUuid(), remaining - 1);
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
    }

    private static class RaftData {
        public int stationaryTicks = 0;
        public Vec3d lastPosition = null;
    }
}
