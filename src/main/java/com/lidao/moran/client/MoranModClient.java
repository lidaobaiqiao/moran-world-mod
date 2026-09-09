package com.lidao.moran.client;

import com.lidao.moran.MoranMod;
import com.lidao.moran.client.render.MolingModel;
import com.lidao.moran.client.render.MolingRenderer;
import com.lidao.moran.systems.entities.EntitySystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors; // ✅ 导入存放静态方法的类
import net.minecraft.client.color.world.GrassColors; // ✅ 导入用于获取默认颜色的类
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 墨世界模组 - 客户端初始化类
 * 处理客户端特定的渲染和视觉效果，包括动态颜色注册。
 * 为玩家呈现诗意的水墨世界。
 */
public class MoranModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("mo-mod-client");

    /**
     * 客户端初始化的入口点
     */
    @Override
    public void onInitializeClient() {
        // 通过标识符获取我们的自定义方块对应的物品实例
        Item myGrassBlock = Registries.ITEM.get(new Identifier(MoranMod.MOD_ID, "peach_blossom_grass_block"));
        LOGGER.info("🔍 找到目标物品: {}", Registries.ITEM.getId(myGrassBlock));

        // 获取物品对应的方块实例
        Block myGrassBlockBlock = Block.getBlockFromItem(myGrassBlock);

        // === 核心修改：注册动态颜色提供器 ===
        registerBlockColorProvider(myGrassBlockBlock);
        registerItemColorProvider(myGrassBlock);

        // === 墨灵：实体渲染器 + 模型层 ===
        EntityModelLayerRegistry.registerModelLayer(MolingRenderer.MOLING_LAYER, MolingModel::getTexturedModelData);
        EntityRendererRegistry.register(EntitySystem.MOLING, MolingRenderer::new);
        LOGGER.info("👻 墨灵渲染器已注册");

        // 初始化其他客户端功能（保持你原有的结构）
        initializeClientRendering();
        initializeClientEvents();
        initializeParticleEffects();

        LOGGER.info("🖌️ 墨彩客户端渲染系统已就绪");
        LOGGER.info("🎮 客户端事件系统已激活");
        LOGGER.info("🌸 桃花特效系统已加载");
        LOGGER.info("🎭 玩家将体验完整的墨世界视觉效果！");
    }

    /**
     * 为【方块】注册动态颜色提供器
     * 这使得方块的颜色会根据其所在的生物群系而改变。
     * 完全复刻原版草方块的逻辑。
     *
     * @param block 要注册颜色的方块
     */
    private void registerBlockColorProvider(Block block) {
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            // 我们只关心 tintIndex 为 0 的层（在模型文件中定义）
            if (tintIndex == 0) {
                // 核心：完全复刻原版逻辑！
                // 如果 world 和 pos 存在，就获取群系颜色；否则返回默认颜色。
                return world != null && pos != null ?
                        BiomeColors.getGrassColor(world, pos) :
                        GrassColors.getDefaultColor();
            }
            // 对于其他 tintIndex，返回 -1 表示不进行着色
            return -1;
        }, block);

        LOGGER.info("✅ 桃花草方块【方块】已注册动态群系颜色！");
    }

    /**
     * 为【物品】注册颜色提供器
     *
     * @param item 要注册颜色的物品
     */
    private void registerItemColorProvider(Item item) {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // 使用和原版完全一样的默认草地颜色
                return GrassColors.getDefaultColor();
            }
            return -1;
        }, item);

        LOGGER.info("✅ 桃花草方块【物品】已注册固定颜色！");
    }


    /**
     * 初始化客户端渲染系统
     */
    private void initializeClientRendering() {
        LOGGER.info("🌈️ 初始化墨彩渲染系统...");
        // 这里将添加水墨风格的视觉效果
        // 桃花飘落效果
        // 墨染粒子系统
    }

    /**
     * 初始化客户端事件系统
     */
    private void initializeClientEvents() {
        LOGGER.info("⚡ 初始化客户端事件系统...");

        // ⭐️【修正后的核心代码】注册客户端世界刻事件，专门为桃花树叶生成落花 ⭐️
        // 注意：这里直接接收 ClientWorld 对象，无需 instanceof 判断
        ClientTickEvents.END_WORLD_TICK.register(clientWorld -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // 确保玩家已加载，避免在加载界面时出错
            if (client.player == null) return;

            Random random = clientWorld.getRandom();

            // 为了性能，我们不每刻都渲染，只在玩家附近随机选择位置
            // random.nextFloat() < 0.1f 表示每刻有10%的概率触发，可以调整这个值来改变落花密度
            if (random.nextFloat() < 0.1f) {
                BlockPos playerPos = client.player.getBlockPos();

                // 在玩家周围随机选择一个位置 (X和Z方向±16格，Y方向±4格)
                BlockPos randomPos = playerPos.add(
                        random.nextInt(32) - 16,
                        random.nextInt(8) - 4,
                        random.nextInt(32) - 16
                );

                // ⭐️【关键修改点】⭐️
                // 获取你的桃花树叶方块实例。请确保 "peach_blossom_leaves" 是你在方块注册时使用的ID
                Block peachLeavesBlock = Registries.BLOCK.get(new Identifier(MoranMod.MOD_ID, "peach_blossom_leaves"));

                // 检查这个位置的方块是不是你的桃花树叶
                if (clientWorld.getBlockState(randomPos).getBlock() == peachLeavesBlock) {
                    // 如果是，就在方块上方生成樱花粒子
                    // 使用 ParticleTypes.CHERRY_LEAVES 粒子，获得原版樱花树的落花效果
                    clientWorld.addParticle(
                            ParticleTypes.CHERRY_LEAVES, // 粒子类型
                            (double)randomPos.getX() + random.nextDouble(), // X坐标 (方块内随机)
                            (double)randomPos.getY() + random.nextDouble(), // Y坐标 (方块内随机)
                            (double)randomPos.getZ() + random.nextDouble(), // Z坐标 (方块内随机)
                            0.0D, // X方向速度
                            -0.05D, // Y方向速度 (轻微向下飘落)
                            0.0D  // Z方向速度
                    );
                }
            }
        });

        LOGGER.info("🌸 桃花树叶落花事件监听器已注册！");
    }

    /**
     * 初始化粒子效果系统
     */
    private void initializeParticleEffects() {
        LOGGER.info("🎐 初始化粒子效果系统...");
        // 桃花瓣飘落
        // 墨滴扩散
        // 水墨波纹
        // 彩霞流动
    }
}
