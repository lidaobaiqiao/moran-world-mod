package com.lidao.moran.systems.trees;

import java.util.function.Consumer;

/**
 * 一个独立表达的性状——基因表达的片段单位。
 *
 * <p>设计出处:用户 2026-09-16 定稿(「基因分片段表达,重写物种基因为性状库,
 * 结合外因判断性状表达」)。品种不再是一个整体蓝图,而是<b>性状的集合</b>;
 * 每个性状 = 身份 × 表达门控 × 表达效果。
 *
 * <p>表达效果目前是「参数修饰/钩子注册」型:对品种 Builder 施加一组设定。
 * 粗粒度起步——一个性状描述一整块可辨识的生物学特征(如「顶腋侧腋着花」),
 * 过细的拆分等行为稳定后再做。
 */
public final class TreeTrait {

    private final String id;
    private final ExpressionGate gate;
    private final Consumer<TreeSpecies.Builder> effect;

    private TreeTrait(String id, ExpressionGate gate, Consumer<TreeSpecies.Builder> effect) {
        this.id = id;
        this.gate = gate;
        this.effect = effect;
    }

    /** 组成型性状(恒表达)——物种蓝图级特征的声明方式 */
    public static TreeTrait of(String id, Consumer<TreeSpecies.Builder> effect) {
        return new TreeTrait(id, ExpressionGate.CONSTITUTIVE, effect);
    }

    /** 带门控的性状(诱导型/发育期/剂量型——表达上下文接入后生效) */
    public static TreeTrait of(String id, ExpressionGate gate, Consumer<TreeSpecies.Builder> effect) {
        return new TreeTrait(id, gate, effect);
    }

    public String id() {
        return id;
    }

    public ExpressionGate gate() {
        return gate;
    }

    /** 表达本性状:门控通过才把效果施加到品种 Builder 上 */
    public void express(TreeSpecies.Builder builder) {
        if (gate.isExpressed()) {
            effect.accept(builder);
        }
    }

    @Override
    public String toString() {
        return gate + "/" + id;
    }
}
