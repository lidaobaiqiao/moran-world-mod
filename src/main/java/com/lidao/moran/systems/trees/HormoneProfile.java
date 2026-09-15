package com.lidao.moran.systems.trees;

/**
 * 单棵树的激素档案 —— 「往哪长、爱不爱分叉、长多快」的生理学载体。
 *
 * <p>「奖励模型」的准确叫法是<b>激素模型</b>：植物偏好之所以不同，
 * 是因为激素含量不同。相似性的单位是<b>表型原型</b>（{@link Phenotype}）：
 * 原型 = 一套把拮抗关系配好的激素档案，同原型的树趋向一致、树形相似；
 * 个体差异只是原型内的小幅残差（{@link TreeSpecies#hormoneVariation()}）。
 * 独立的大幅随机抖动产生不了相似性——它只让每棵树各偏一点，
 * 得到的是互不相关的趋向，树与树之间没有共同原因。
 *
 * <p>三个分量都是围绕 1.0 的乘数（1.0 = 物种常态），引擎按生理学事实取用：
 * <ul>
 *   <li><b>生长素 auxin</b> —— 顶端优势。梢尖合成、向下运输、压制侧芽。
 *       高 → 向顶性更强、分叉门槛更高、侧芽与侧枝更稀；</li>
 *   <li><b>细胞分裂素 cytokinin</b> —— 促分裂、促侧芽萌出（根合成，与生长素拮抗）。
 *       高 → 侧芽更爱出、分叉概率更高；</li>
 *   <li><b>赤霉素 gibberellin</b> —— 节间伸长。
 *       高 → 长得快、枝条拉得长、停得晚。</li>
 * </ul>
 *
 * <p>激素<b>不占方块状态</b>：由树基坐标确定性推出（同一棵树的所有节算出同一份，
 * 重启后不变），零注册零存储 —— 方块状态空间动一格的教训见
 * {@link com.lidao.moran.systems.blocks.MoranBranchBlock} 里 IDLE 的类注。
 * 推导失效的唯一情形是链被打断后的孤儿枝：它按自己的位置另起一份档案，
 * 后果只是这根枝的偏好略微变化，可接受。
 */
public record HormoneProfile(float auxin, float cytokinin, float gibberellin) {
}
