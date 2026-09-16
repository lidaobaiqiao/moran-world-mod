package com.lidao.moran.systems.trees;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 品种基因组——收录一个品种全部性状的集合(「性状库」,用户 2026-09-16 定稿)。
 *
 * <p>声明式:一个品种 = 一张性状清单,读起来就是它的「品种说明书」。
 * 基因型只回答「携带什么」;{@link ExpressionGate} 结合外因回答「表达什么」;
 * 引擎读取的永远是表达后合成出的档案(Builder),引擎本身零改动。
 *
 * <p>声明期两条校验,把冲突挡在建档时:
 * <ul>
 *   <li>重复性状 id → 拒绝;</li>
 *   <li>声明过的互斥对(如 垂枝×柱型)同时出现 → 拒绝。引擎不做仲裁。</li>
 * </ul>
 *
 * <p>个体基因型仍由树基坐标确定性推导(零存储铁律不破);真存储要到「杂交育苗」
 * 玩法(子代性状重组)才需要。
 */
public final class TreeGenome {

    private final String speciesId;
    private final List<TreeTrait> traits;

    private TreeGenome(String speciesId, List<TreeTrait> traits, String[][] incompatiblePairs) {
        this.speciesId = speciesId;
        Set<String> seen = new HashSet<>();
        for (TreeTrait t : traits) {
            if (!seen.add(t.id())) {
                throw new IllegalStateException("品种 " + speciesId + " 性状重复: " + t.id());
            }
        }
        for (String[] pair : incompatiblePairs) {
            if (pair.length != 2) {
                throw new IllegalStateException("互斥对必须恰好两个性状 id: " + String.join("|", pair));
            }
            if (seen.contains(pair[0]) && seen.contains(pair[1])) {
                throw new IllegalStateException("品种 " + speciesId + " 携带互斥性状: "
                        + pair[0] + " × " + pair[1]);
            }
        }
        this.traits = List.copyOf(traits);
    }

    /** 无互斥约束的基因组 */
    public static TreeGenome of(String speciesId, List<TreeTrait> traits) {
        return new TreeGenome(speciesId, traits, new String[0][]);
    }

    /** 带互斥约束的基因组(声明期拒绝冲突组合) */
    public static TreeGenome of(String speciesId, List<TreeTrait> traits, String[][] incompatiblePairs) {
        return new TreeGenome(speciesId, traits, incompatiblePairs);
    }

    /**
     * 当前门控下表达的性状。
     * 表达上下文(环境快照/物候期/损伤史)接入后,此处成为基因表达的判定枢纽;
     * 表达重评时机=事件触发(种植/修剪/邻块变化),不在每拍轮询。
     */
    public List<TreeTrait> expressed() {
        List<TreeTrait> out = new ArrayList<>();
        for (TreeTrait t : traits) {
            if (t.gate().isExpressed()) {
                out.add(t);
            }
        }
        return out;
    }

    /** 把全部表达性状合成进品种 Builder(引擎读取路径不变) */
    public TreeSpecies.Builder applyTo(TreeSpecies.Builder builder) {
        for (TreeTrait t : expressed()) {
            t.express(builder);
        }
        return builder;
    }

    public String speciesId() {
        return speciesId;
    }

    public List<TreeTrait> traits() {
        return traits;
    }
}
