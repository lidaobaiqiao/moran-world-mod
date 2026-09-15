package com.lidao.moran.systems.trees;

/**
 * 表型原型 —— 同种树的「亚型」，<b>相似性的单位</b>。
 *
 * <p>独立的随机抖动产生不了相似性：三份激素各自乱抖，树与树之间没有共同原因，
 * 结果只是每棵树各有一些互不相关的趋向。文献里的结构不是这样的——
 * 垂枝桃 F2 代的直立:垂枝分离比 3:1，树形是<b>离散的遗传性状</b>；
 * 激素的「上下比值趋向」（IAA/CTK 高低、GA 上下分布）是跟着原型走的，
 * 不是跟着单棵树散点走的。
 *
 * <p>所以：大差异归原型，小差异归个体。同原型的树共享同一份激素档案，
 * 趋向一致、树形相似；原型之间的配比差异（彼此拮抗、方向相反）就是文献测到的趋向；
 * 个体只在原型内做小幅残差（{@link TreeSpecies#hormoneVariation()}）。
 *
 * <p>比例终将落到形状上：垂枝型的 {@code branchChildDirection} 覆写等
 * 形状层差异以后也挂在原型上（id 留给调试与这些覆写用）。
 */
public record Phenotype(String id, float weight, HormoneProfile hormones) {
}
