package com.lidao.moran.client.render;

import com.lidao.moran.systems.entities.MolingEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * 墨灵模型：7×7×7 悬浮墨团 + 下方 2×2×2 墨滴。
 * 贴图 32×32（UV 展开图，实体贴图规则，不受方块 16×16 铁律约束）。
 */
public class MolingModel extends SinglePartEntityModel<MolingEntity> {
    private static final float BASE_PIVOT_Y = 19.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart drop;

    public MolingModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.drop = root.getChild("drop");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild(EntityModelPartNames.BODY,
                ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-3.5F, -3.5F, -3.5F, 7.0F, 7.0F, 7.0F),
                ModelTransform.pivot(0.0F, BASE_PIVOT_Y, 0.0F));
        root.addChild("drop",
                ModelPartBuilder.create().uv(0, 14)
                        .cuboid(-1.0F, 3.5F, -1.0F, 2.0F, 2.0F, 2.0F),
                ModelTransform.pivot(0.0F, BASE_PIVOT_Y, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(MolingEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // 悬浮呼吸感：整体缓慢上下浮动 + 轻微侧摆
        float bob = MathHelper.sin(animationProgress * 0.13F) * 0.6F;
        this.body.pivotY = BASE_PIVOT_Y + bob;
        this.drop.pivotY = BASE_PIVOT_Y + bob
                + MathHelper.sin(animationProgress * 0.26F) * 0.4F;
        this.body.roll = MathHelper.sin(animationProgress * 0.09F) * 0.08F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
