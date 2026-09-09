package com.lidao.moran.client.render;

import com.lidao.moran.MoranMod;
import com.lidao.moran.systems.entities.MolingEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MolingRenderer extends MobEntityRenderer<MolingEntity, MolingModel> {
    public static final EntityModelLayer MOLING_LAYER =
            new EntityModelLayer(new Identifier(MoranMod.MOD_ID, "moling"), "main");

    private static final Identifier TEXTURE =
            new Identifier(MoranMod.MOD_ID, "textures/entity/moling.png");

    public MolingRenderer(EntityRendererFactory.Context context) {
        super(context, new MolingModel(context.getPart(MOLING_LAYER)), 0.4F);
    }

    @Override
    public Identifier getTexture(MolingEntity entity) {
        return TEXTURE;
    }
}
