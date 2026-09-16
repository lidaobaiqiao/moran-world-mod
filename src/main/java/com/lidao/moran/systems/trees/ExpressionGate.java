package com.lidao.moran.systems.trees;

/**
 * 性状的表达门控——基因表达模型的核心开关。
 *
 * <p>基因型只回答「这棵树携带什么性状」;门控结合外因回答「此刻表不表达、表达几分」。
 * 四型对应真实基因调控机制(详见 基因报告.md):
 * <ul>
 *   <li><b>组成型</b>——管家基因,恒表达(物种蓝图级性状,本期唯一接入的门控);</li>
 *   <li><b>环境诱导型</b>——应激基因:重剪徒长、遮荫窜高(占位,待表达上下文接入);</li>
 *   <li><b>发育期门控</b>——时序基因:花芽分化(成龄+夏)、成龄结果(占位);</li>
 *   <li><b>剂量型</b>——表达强度随环境梯度连续变化:向光偏折、垂枝弧度(占位)。</li>
 * </ul>
 */
public enum ExpressionGate {

    CONSTITUTIVE,
    ENVIRONMENT_INDUCED,
    STAGED,
    DOSE_DEPENDENT;

    /**
     * 本性状在当前上下文是否表达。
     * 骨架期:组成型恒真,其余门控尚未接入表达上下文(环境快照/物候期),一律不表达。
     * 表达判定的重评时机=事件触发(种植/修剪/邻块变化),不在每拍轮询。
     */
    public boolean isExpressed() {
        return this == CONSTITUTIVE;
    }
}
