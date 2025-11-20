package com.lidao.moran.item;

import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class MoranSwordItem extends SwordItem {
    // 自定义工具材质（类似铁剑属性）
    public static final ToolMaterial MORAN_TOOL_MATERIAL = new ToolMaterial() {
        @Override
        public int getDurability() { return 4096; }           // 耐久度
        @Override
        public float getMiningSpeedMultiplier() { return 6.0f; }  // 挖掘速度
        @Override
        public float getAttackDamage() { return 12.0f; }      // 攻击伤害（3点=1.5颗心）
        @Override
        public int getMiningLevel() { return 2; }            // 挖掘等级（2=铁级）
        @Override
        public int getEnchantability() { return 30; }        // 附魔能力
        @Override
        public Ingredient getRepairIngredient() {
            // 修复：使用物品系统获取墨石物品
            return Ingredient.ofItems(com.lidao.moran.systems.items.ItemSystem.MORAN);
        }
    };

    public MoranSwordItem() {
        super(MORAN_TOOL_MATERIAL, 3, -2.4f, new Item.Settings());
    }
}