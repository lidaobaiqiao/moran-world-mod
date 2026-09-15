package com.lidao.moran.client.model;

import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
                String path;
                if (id.startsWith("moran_mod:branch/")) {
                    path = id.substring("moran_mod:branch/".length());
                } else if (id.startsWith("moran_mod:trunk/")) {
                    path = id.substring("moran_mod:trunk/".length());
                } else {
                    System.err.println("SKIP(非动态前缀): " + id);
                    continue;
                }
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

    /**
     * 直接复用 {@link BranchModelPlugin#parse(String, boolean)} —— 解析规则只写一份，
     * 改一处不会漏一处（包括新的 {@code <species>/} 前缀与 {@code trunk/} 主干动态模型）。
     */
    static JsonObject parseAndBuild(String spec) {
        return BranchModelPlugin.parse(spec);
    }
}
