package com.lidao.moran.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoranModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("moran-mod-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("墨纬度模组客户端初始化开始...");

        // ⚠️ 移除自定义竹筏渲染器注册
        // EntityRendererRegistry.register(MoranMod.BAMBOO_RAFT_ENTITY, BambooRaftRenderer::new);

        LOGGER.info("墨纬度模组客户端初始化完成！");
    }
}