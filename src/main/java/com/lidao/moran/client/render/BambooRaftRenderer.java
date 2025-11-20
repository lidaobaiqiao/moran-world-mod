package com.lidao.moran.client.render;

import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class BambooRaftRenderer extends BoatEntityRenderer { // 移除了泛型参数

    // 你的竹筏纹理标识符
    private static final Identifier TEXTURE = new Identifier("moran-mod", "textures/entity/bamboo_raft.png");

    public BambooRaftRenderer(EntityRendererFactory.Context context) {
        super(context, false); // false 表示这不是一艘运输船
    }

    @Override
    public Identifier getTexture(net.minecraft.entity.vehicle.BoatEntity entity) { // 注意参数类型变化
        return TEXTURE;
    }
}