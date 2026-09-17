package com.lidao.moran.systems.test;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 测试桥——外部程序（开发 AI/脚本）经文件通道驱动服务器做自动化验证。
 *
 * <p>通道：{@code ai_test/cmd.txt}（每行一条；# 为注释）→ 每 20 tick 读取执行并删除 →
 * 结果追加 {@code ai_test/result.txt}。写入方应先写 cmd.tmp 再改名，避免读到半行。
 *
 * <p>命令：
 * <ul>
 *   <li>任意服务器命令（控制台权限执行）：forceload / setblock / execute / time / stop…</li>
 *   <li>{@code SCAN x1 y1 z1 x2 y2 z2 blockId [dimId]}——区域方块计数（体积上限 400 万）</li>
 *   <li>{@code BONEMEAL x y z [times] [dimId]}——对可催熟方块施用骨粉 n 次（生长验收核心）</li>
 *   <li>{@code GETSTATE x y z [dimId]}——打印方块与全部属性</li>
 * </ul>
 */
public final class AiTestBridge {

    private AiTestBridge() {
    }

    private static int cooldown = 20;
    private static long resumeAt = 0L;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                System.out.println("[AI桥] 就绪: " + dir().toAbsolutePath() + " (写入 cmd.txt 即可驱动)"));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                Files.createDirectories(dir());
            } catch (Exception ignored) {
            }
            if (--cooldown > 0) {
                return;
            }
            cooldown = 20;
            if (System.currentTimeMillis() < resumeAt) {
                return;
            }
            try {
                Path cmd = dir().resolve("cmd.txt");
                if (!Files.exists(cmd)) {
                    return;
                }
                List<String> lines = Files.readAllLines(cmd, StandardCharsets.UTF_8);
                Files.delete(cmd);
                StringBuilder out = new StringBuilder();
                out.append("==== AI桥执行 ").append(LocalDateTime.now()).append(" ====\n");
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (line.startsWith("SCAN ")) {
                        doScan(server, line, out);
                        continue;
                    }
                    if (line.startsWith("SETSURFACE ")) {
                        doSetSurface(server, line, out);
                        continue;
                    }
                    if (line.startsWith("BONEMEAL ")) {
                        doBonemeal(server, line, out);
                        continue;
                    }
                    if (line.startsWith("ENV ")) {
                        doEnv(server, line, out);
                        continue;
                    }
                    if (line.startsWith("ENV ")) {
                        doEnv(server, line, out);
                        continue;
                    }
                    if (line.startsWith("GETSTATE ")) {
                        doGetState(server, line, out);
                        continue;
                    }
                    if (line.startsWith("WAIT ")) {
                        long ms = Long.parseLong(line.substring(5).trim());
                        List<String> rest = lines.subList(lines.indexOf(raw) + 1, lines.size());
                        Files.write(cmd, rest, StandardCharsets.UTF_8);
                        resumeAt = System.currentTimeMillis() + ms;
                        out.append("WAIT ").append(ms).append("ms(剩余 ").append(rest.size()).append(" 行延迟续跑)\n");
                        flush(out);
                        return;
                    }
                    out.append("> ").append(line).append('\n');
                    server.getCommandManager().executeWithPrefix(
                            server.getCommandSource().withLevel(4), line);
                }
                flush(out);
            } catch (Exception e) {
                System.err.println("[AI桥] 执行异常: " + e);
            }
        });
    }

    private static void flush(StringBuilder out) {
        if (out.length() == 0) {
            return;
        }
        try {
            Files.writeString(dir().resolve("result.txt"), out.toString(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            out.setLength(0);
        } catch (Exception e) {
            System.err.println("[AI桥] 结果写盘失败: " + e);
        }
    }

    private static Path dir() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("ai_test");
        } catch (Throwable notOnLoader) {
            return Path.of("run", "ai_test");
        }
    }

    private static ServerWorld worldOf(MinecraftServer server, String dimId) {
        return dimId == null
                ? server.getOverworld()
                : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimId)));
    }

    /**
     * SETSURFACE x z blockId [dimId]——在地表(WORLD_SURFACE 高度图之上)放置方块。
     * 若是生长方块(枝干/花苞),自动置 natural=true(野生节奏,0.1s/拍)——
     * 解决"埋在地下/黑暗处自然枝被冻结"的测试位置问题。
     */
    private static void doSetSurface(MinecraftServer server, String line, StringBuilder out) {
        String[] p = line.split("\s+");
        if (p.length < 4) {
            out.append("SETSURFACE 参数不足\n");
            return;
        }
        try {
            int x = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
            Block block = Registries.BLOCK.get(new Identifier(p[3]));
            ServerWorld world = worldOf(server, p.length >= 5 ? p[4] : null);
            if (world == null) {
                out.append("SETSURFACE 维度不存在\n");
                return;
            }
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = block.getDefaultState();
            if (state.contains(com.lidao.moran.systems.blocks.MoranBranchBlock.NATURAL)) {
                state = state.with(com.lidao.moran.systems.blocks.MoranBranchBlock.NATURAL, true);
            }
            if (state.contains(com.lidao.moran.systems.blocks.MoranFlowerBudBlock.NATURAL)) {
                state = state.with(com.lidao.moran.systems.blocks.MoranFlowerBudBlock.NATURAL, true);
            }
            world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_ALL);
            out.append("SETSURFACE 结果: ").append(state).append(" @ [").append(x).append(',')
               .append(y).append(',').append(z).append("]\n");
        } catch (Exception e) {
            out.append("SETSURFACE 异常: ").append(e).append('\n');
        }
    }


    /**
     * ENV x y z [dimId]
     * 环境四因子诊断(天光/温度/降水湿度/水度)——自然枝冻结时的排查入口
     */
    private static void doEnv(MinecraftServer server, String line, StringBuilder out) {
        String[] p = line.split("\n\ns+");
        if (p.length < 4) { out.append("ENV 参数不足\n"); return; }
        try {
            int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
            ServerWorld world = worldOf(server, p.length >= 5 ? p[4] : null);
            if (world == null) { out.append("ENV 维度不存在\n"); return; }
            BlockPos pos = new BlockPos(x, y, z);
            int sky = world.getLightLevel(net.minecraft.world.LightType.SKY, pos);
            float temp = world.getBiome(pos).value().getTemperature();
            boolean precip = world.getBiome(pos).value().hasPrecipitation();
            String biome = world.getBiome(pos).getKey().map(k -> k.getValue().toString()).orElse("?");
            int water = com.lidao.moran.systems.trees.TreeSpecies.hydration(world, pos);
            float humidity = com.lidao.moran.systems.trees.TreeSpecies.biomeHumidity(world, pos);
            out.append("ENV [").append(x).append(",").append(y).append(",").append(z)
               .append("] 群系=").append(biome)
               .append(" 天光=").append(sky)
               .append(" 温度=").append(temp).append(" 降水=").append(precip)
               .append(" 湿度=").append(humidity).append(" 水度=").append(water)
               .append("\n");
        } catch (Exception e) {
            out.append("ENV 异常: ").append(e).append("\n");
        }
    }

    /** SCAN x1 y1 z1 x2 y2 z2 blockId [dimId]——区域方块计数 */
    private static void doScan(MinecraftServer server, String line, StringBuilder out) {
        String[] p = line.split("\\s+");
        if (p.length < 8) {
            out.append("SCAN 参数不足\n");
            return;
        }
        try {
            int x1 = Integer.parseInt(p[1]), y1 = Integer.parseInt(p[2]), z1 = Integer.parseInt(p[3]);
            int x2 = Integer.parseInt(p[4]), y2 = Integer.parseInt(p[5]), z2 = Integer.parseInt(p[6]);
            Block target = Registries.BLOCK.get(new Identifier(p[7]));
            ServerWorld world = worldOf(server, p.length >= 9 ? p[8] : null);
            if (world == null) {
                out.append("SCAN 维度不存在\n");
                return;
            }
            int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
            if ((long) dx * dy * dz > 4_000_000L) {
                out.append("SCAN 区域过大(上限400万方块)\n");
                return;
            }
            int x0 = Math.min(x1, x2), y0 = Math.min(y1, y2), z0 = Math.min(z1, z2);
            int count = 0;
            BlockPos.Mutable m = new BlockPos.Mutable();
            for (int x = 0; x <= dx; x++) {
                for (int y = 0; y <= dy; y++) {
                    for (int z = 0; z <= dz; z++) {
                        m.set(x0 + x, y0 + y, z0 + z);
                        if (world.getBlockState(m).isOf(target)) {
                            count++;
                        }
                    }
                }
            }
            out.append("SCAN 结果: ").append(count).append(" × ").append(p[7])
                    .append(" @ [").append(x0).append(',').append(y0).append(',').append(z0)
                    .append("]..[").append(x0 + dx).append(',').append(y0 + dy).append(',').append(z0 + dz)
                    .append("] (").append(world.getRegistryKey().getValue()).append(")\n");
        } catch (Exception e) {
            out.append("SCAN 异常: ").append(e).append('\n');
        }
    }

    /**
     * BONEMEAL x y z [times] [dimId]——对可催熟方块施用骨粉 n 次。
     * 生长验收的核心动作:等效玩家连点骨粉;催满档后进一步催熟被 GROWTH_MAX 闸住(即崩溃修复的验证点)。
     */
    private static void doBonemeal(MinecraftServer server, String line, StringBuilder out) {
        String[] p = line.split("\\s+");
        if (p.length < 4) {
            out.append("BONEMEAL 参数不足\n");
            return;
        }
        try {
            int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
            int times = p.length >= 5 ? Integer.parseInt(p[4]) : 1;
            ServerWorld world = worldOf(server, p.length >= 6 ? p[5] : null);
            if (world == null) {
                out.append("BONEMEAL 维度不存在\n");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            int done = 0;
            String stopReason = "次数用尽";
            for (int i = 0; i < times; i++) {
                BlockState state = world.getBlockState(pos);
                if (!(state.getBlock() instanceof Fertilizable fert)) {
                    stopReason = "不可催熟: " + state.getBlock();
                    break;
                }
                if (!fert.isFertilizable(world, pos, state, false)) {
                    stopReason = "已达上限,不可再催";
                    break;
                }
                fert.grow(world, world.getRandom(), pos, state);
                done++;
            }
            out.append("BONEMEAL 结果: 成功 ").append(done).append("/").append(times)
                    .append(" @ [").append(x).append(',').append(y).append(',').append(z)
                    .append("] 停止原因: ").append(stopReason)
                    .append(" 终态: ").append(world.getBlockState(pos)).append('\n');
        } catch (Exception e) {
            out.append("BONEMEAL 异常: ").append(e).append('\n');
        }
    }

    /** GETSTATE x y z [dimId]——打印方块与其全部属性 */
    private static void doGetState(MinecraftServer server, String line, StringBuilder out) {
        String[] p = line.split("\\s+");
        if (p.length < 4) {
            out.append("GETSTATE 参数不足\n");
            return;
        }
        try {
            int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
            ServerWorld world = worldOf(server, p.length >= 5 ? p[4] : null);
            if (world == null) {
                out.append("GETSTATE 维度不存在\n");
                return;
            }
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            out.append("GETSTATE [").append(x).append(',').append(y).append(',').append(z)
                    .append("] = ").append(state).append('\n');
        } catch (Exception e) {
            out.append("GETSTATE 异常: ").append(e).append('\n');
        }
    }
}
