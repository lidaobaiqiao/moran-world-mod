package com.lidao.moran.systems.trees.genes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lidao.moran.systems.trees.BloomStyles;
import com.lidao.moran.systems.trees.TreeSpecies;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 树种基因文件加载器——「读取基因文件直接修改树种,类似模组配置文件」
 * (用户 2026-09-16 定稿)。
 *
 * <p>规则:
 * <ol>
 *   <li>基因文件在 {@link GeneDirs#geneDir()} 下,一个树种一个 {@code <id>.json};</li>
 *   <li>文件缺失 → 自动把内置默认(assets/moran_mod/genes/)抄出去,供用户编辑;</li>
 *   <li>优先解析配置文件;解析失败 → 回退内置默认,绝不崩档;</li>
 *   <li>改文件后重启生效(游戏)/重跑生效(simTrees);系统属性 moran.genes.dir 可换目录。</li>
 * </ol>
 *
 * <p>行为类性状(开花方式/成熟形态)以「风格名」在文件里选用,实现注册在
 * {@link BloomStyles};未注册名回退中性默认。参数类性状全部文件可调。
 */
public final class SpeciesGeneLoader {

    private SpeciesGeneLoader() {
    }

    /** 加载一个树种:配置文件优先,内置默认兜底 */
    public static TreeSpecies load(String id) {
        Path file = GeneDirs.geneDir().resolve(id + ".json");
        if (!Files.exists(file)) {
            writeDefault(id, file);
        }
        try {
            TreeSpecies s = parse(id, Files.readString(file, StandardCharsets.UTF_8));
            if (s != null) {
                System.out.println("[基因库] " + id + " 已从基因文件加载: " + file);
                return s;
            }
        } catch (Exception e) {
            System.err.println("[基因库] 基因文件解析失败(" + file + "): " + e + " —— 回退内置默认");
        }
        String fallback = readClasspathDefault(id);
        TreeSpecies s = parse(id, fallback);
        if (s == null) {
            throw new IllegalStateException("[基因库] 内置默认基因文件损坏: " + id);
        }
        System.out.println("[基因库] " + id + " 已从内置默认加载(配置文件不可用)");
        return s;
    }

    /** 配置文件缺失时,把内置默认抄出去(用户由此拿到可编辑的起点) */
    private static void writeDefault(String id, Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (var in = SpeciesGeneLoader.class.getClassLoader()
                    .getResourceAsStream("assets/moran_mod/genes/" + id + ".json")) {
                if (in == null) {
                    System.err.println("[基因库] 无内置默认可抄: " + id + "(跳过写文件)");
                    return;
                }
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[基因库] 已抄出默认基因文件: " + file + "(可直接编辑)");
            }
        } catch (Exception e) {
            System.err.println("[基因库] 抄出默认基因文件失败: " + e);
        }
    }

    private static String readClasspathDefault(String id) {
        try (var in = SpeciesGeneLoader.class.getClassLoader()
                .getResourceAsStream("assets/moran_mod/genes/" + id + ".json")) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON → Builder。所有键可选;未知键忽略;不设的键走 Builder 中性默认 */
    static TreeSpecies parse(String id, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String sid = root.has("id") && !root.get("id").getAsString().isBlank()
                ? root.get("id").getAsString() : id;
        TreeSpecies.Builder b = TreeSpecies.builder(sid);

        if (root.has("biologicalTop")) {
            JsonArray a = root.getAsJsonArray("biologicalTop");
            b.biologicalTop(a.get(0).getAsInt(), a.get(1).getAsInt());
        }
        if (num(root, "trunkMaxGrowth") != null) b.trunkMaxGrowth(num(root, "trunkMaxGrowth").intValue());
        if (num(root, "limbCount") != null) b.limbCount(num(root, "limbCount").intValue());
        if (num(root, "maxBuds") != null) b.maxBuds(num(root, "maxBuds").intValue());
        if (num(root, "budChanceDenom") != null) b.budChanceDenom(num(root, "budChanceDenom").intValue());
        if (num(root, "branchStopChance") != null) b.branchStopChance(num(root, "branchStopChance").floatValue());
        if (num(root, "subBranchChance") != null) b.subBranchChance(num(root, "subBranchChance").floatValue());
        if (num(root, "forkMinChainPos") != null) b.forkMinChainPos(num(root, "forkMinChainPos").intValue());
        if (num(root, "forkSpacing") != null) b.forkSpacing(num(root, "forkSpacing").intValue());
        if (num(root, "forkDecay") != null) b.forkDecay(num(root, "forkDecay").floatValue());
        if (num(root, "forkMinGain") != null) b.forkMinGain(num(root, "forkMinGain").intValue());
        if (num(root, "growChance") != null) b.growChance(num(root, "growChance").floatValue());
        if (num(root, "minLight") != null) b.minLight(num(root, "minLight").intValue());
        if (root.has("temperature")) {
            JsonArray a = root.getAsJsonArray("temperature");
            b.temperature(a.get(0).getAsFloat(), a.get(1).getAsFloat());
        }
        if (num(root, "minHydration") != null) b.minHydration(num(root, "minHydration").intValue());
        if (num(root, "minHumidity") != null) b.minHumidity(num(root, "minHumidity").floatValue());
        if (root.has("soilPreference")) {
            JsonArray a = root.getAsJsonArray("soilPreference");
            b.soilPreference(a.get(0).getAsInt(), a.get(1).getAsInt(),
                    a.get(2).getAsInt(), a.get(3).getAsInt(),
                    a.get(4).getAsInt(), a.get(5).getAsInt());
        }
        if (num(root, "pruneResponseChance") != null) b.pruneResponseChance(num(root, "pruneResponseChance").floatValue());
        if (num(root, "minSpacing") != null) b.minSpacing(num(root, "minSpacing").intValue());
        if (num(root, "branchNutritionDecay") != null) b.branchNutritionDecay(num(root, "branchNutritionDecay").intValue());
        if (num(root, "chainNutritionDecay") != null) b.chainNutritionDecay(num(root, "chainNutritionDecay").intValue());
        if (root.has("hormones")) {
            JsonObject h = root.getAsJsonObject("hormones");
            b.hormones(num(h, "auxin") == null ? 1F : num(h, "auxin").floatValue(),
                    num(h, "cytokinin") == null ? 1F : num(h, "cytokinin").floatValue(),
                    num(h, "gibberellin") == null ? 1F : num(h, "gibberellin").floatValue());
        }
        if (num(root, "hormoneVariation") != null) b.hormoneVariation(num(root, "hormoneVariation").floatValue());
        if (root.has("phenotypes")) {
            for (var el : root.getAsJsonArray("phenotypes")) {
                JsonObject p = el.getAsJsonObject();
                b.phenotype(p.get("id").getAsString(),
                        num(p, "weight") == null ? 1F : num(p, "weight").floatValue(),
                        num(p, "auxin") == null ? 1F : num(p, "auxin").floatValue(),
                        num(p, "cytokinin") == null ? 1F : num(p, "cytokinin").floatValue(),
                        num(p, "gibberellin") == null ? 1F : num(p, "gibberellin").floatValue());
            }
        }
        if (root.has("textures")) {
            JsonObject t = root.getAsJsonObject("textures");
            b.textures(t.get("bark").getAsString(), t.get("cap").getAsString());
        }
        if (root.has("trunkModelBase")) {
            b.trunkModelBase(root.get("trunkModelBase").getAsString());
        }
        if (root.has("bloomingStyle")) {
            String name = root.get("bloomingStyle").getAsString();
            if (!BloomStyles.hasBlooming(name)) {
                System.err.println("[基因库] 未知 bloomingStyle: " + name + " —— 用中性默认");
            } else {
                b.onBranchStop(BloomStyles.blooming(name));
            }
        }
        if (root.has("maturationStyle")) {
            String name = root.get("maturationStyle").getAsString();
            if (!BloomStyles.hasMaturation(name)) {
                System.err.println("[基因库] 未知 maturationStyle: " + name + " —— 用中性默认");
            } else {
                b.onBudMature(BloomStyles.maturation(name));
            }
        }
        return b.build();
    }

    private static Float num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsFloat() : null;
    }
}
