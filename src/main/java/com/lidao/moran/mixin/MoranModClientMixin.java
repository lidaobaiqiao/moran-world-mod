package com.lidao.moran.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MoranModClientMixin {

    @Inject(method = "<init>", at = @At("TAIL"))  // 改为 TAIL
    private void onInit(CallbackInfo info) {
        System.out.println("🎨 墨世界客户端渲染启动 - 桃花源记，诗意境界");
        System.out.println("🌸 陶渊明的理想社会，在游戏世界呈现");
        System.out.println("📜 中华文化的人文情怀，在视觉世界传承");
    }
}