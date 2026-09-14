package com.lidao.moran.client.model;

import com.google.gson.JsonObject;
import net.minecraft.util.math.Direction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 开发期校验工具：把 {@link BranchModelFactory} 为 blockstate 引用的全部动态 id
 * 生成的模型导出成 JSON，供与手作/金标准逐字段比对。
 *
 * <p>不是运行时组件，只在开发时手工调用。可安全删除。
 */
public final class BranchModelDump {

    public static void main(String[] args) throws IOException {
        Path listFile = Path.of(args.length > 0 ? args[0] : ".workbuddy/dynamic_ids.txt");
        Path outDir = Path.of(args.length > 1 ? args[1] : ".workbuddy/gen_java");
        Files.createDirectories(outDir);

        int n = 0;
        try (BufferedWriter idx = Files.newBufferedWriter(outDir.resolve("_index.txt"), StandardCharsets.UTF_8)) {
            for (String line : Files.readAllLines(listFile, StandardCharsets.UTF_8)) {
                String id = line.trim();
                if (id.isEmpty()) {
                    continue;
                }
                String path = id.substring("moran_mod:branch/".length());
                JsonObject model = parseAndBuild(path);
                if (model == null) {
                    System.err.println("FAILED: " + id);
                    continue;
                }
                String fileName = path.replace('/', '_') + ".json";
                Files.writeString(outDir.resolve(fileName), model.toString(), StandardCharsets.UTF_8);
                idx.write(id + "\t" + fileName);
                idx.newLine();
                n++;
            }
        }
        System.out.println("dumped " + n + " models -> " + outDir.toAbsolutePath());
    }

    /** 与 BranchModelPlugin.parse 同构 */
    static JsonObject parseAndBuild(String spec) {
        String facingPart = spec;
        String subPart = null;
        int sep = spec.indexOf("__");
        if (sep >= 0) {
            facingPart = spec.substring(0, sep);
            subPart = spec.substring(sep + 2);
        }
        int gi = facingPart.lastIndexOf("_g");
        if (gi < 0) {
            return null;
        }
        Direction facing = byName(facingPart.substring(0, gi));
        if (facing == null) {
            return null;
        }
        int growth;
        try {
            growth = Integer.parseInt(facingPart.substring(gi + 2));
        } catch (NumberFormatException e) {
            return null;
        }
        Set<Direction> subs = new LinkedHashSet<>();
        if (subPart != null && !subPart.isEmpty()) {
            for (String s : subPart.split("_")) {
                Direction d = byName(s);
                if (d == null) {
                    return null;
                }
                subs.add(d);
            }
        }
        return BranchModelFactory.build(facing, growth, subs);
    }

    private static Direction byName(String n) {
        for (Direction d : Direction.values()) {
            if (d.asString().equals(n)) {
                return d;
            }
        }
        return null;
    }
}
