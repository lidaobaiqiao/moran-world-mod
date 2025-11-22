package com.lidao.moran.item;

import com.lidao.moran.systems.items.ItemSystem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class MoranToolMaterials {
    public static final ToolMaterial INK = new ToolMaterial() {
        @Override
        public int getDurability() {
            return 1561;
        }

        @Override
        public float getMiningSpeedMultiplier() {
            return 8.0F;
        }

        @Override
        public float getAttackDamage() {
            return 3.0F;
        }

        @Override
        public int getMiningLevel() {
            return 3;
        }

        @Override
        public int getEnchantability() {
            return 10;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(ItemSystem.MORAN);
        }
    };
}
