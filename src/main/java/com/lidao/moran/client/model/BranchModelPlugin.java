package com.lidao.moran.client.model;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 树枝模型的运行时生成器——把 {@link BranchModelFactory} 接进模型加载流程。
 *
 * <h2>为什么要动态生成</h2>
 * 树枝模型是纯几何结构，组合数却是乘法级的：
 * <pre>
 *   单枝：6 朝向 x 8 档                    =   48
 *   分叉：6 朝向 x 7 档 x 11 种方向组合     =  462
 * </pre>
 * 全量落盘要 510 个 JSON（约 1MB）。而这些模型全都能由
 * 「走向 + 档位 + 分叉方向集合」三个参数算出来，没有必要提前枚举。
 * 故磁盘上零模型文件，每个组合在资源重载时现场算、现场烘焙。
 *
 * <h2>模型 id 命名</h2>
 * <pre>
 *   moran_mod:branch/&lt;facing&gt;_g&lt;growth&gt;                    单枝（无分叉）
 *   moran_mod:branch/&lt;facing&gt;_g&lt;growth&gt;__&lt;sub1&gt;[_&lt;sub2&gt;...]  分叉
 * </pre>
 * 例：{@code moran_mod:branch/north_g5__east_up} = 朝北的 g5 母枝，向东、向上各伸出一根子枝。
 * blockstate 里只要拼出这个 id 就能引用，无需任何文件。
 */
@Environment(EnvType.CLIENT)
public final class BranchModelPlugin implements ModelLoadingPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("moran-branch-model");

    /** 动态生成模型的命名空间前缀（放在 models/branch/ 下） */
    private static final String PATH_PREFIX = "branch/";

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.resolveModel().register(new Resolver());
        LOGGER.info("🌿 树枝模型运行时生成器已注册");
    }

    /** 识别 branch/ 前缀的模型 id，现场算出来 */
    private static final class Resolver implements ModelResolver {
        @Override
        @Nullable
        public UnbakedModel resolveModel(Context context) {
            Identifier id = context.id();
            if (!id.getNamespace().equals("moran_mod") || !id.getPath().startsWith(PATH_PREFIX)) {
                return null;
            }
            JsonObject json = parse(id.getPath().substring(PATH_PREFIX.length()));
            if (json == null) {
                LOGGER.warn("无法解析树枝模型 id: {}", id);
                return null;
            }
            // 走原版 JSON 解析管线：几何/uv/rotation 全部由成熟代码处理
            return JsonUnbakedModel.deserialize(json.toString());
        }
    }

    /**
     * 解析 {@code <facing>_g<growth>[__<sub>...]} 形式的 id。
     *
     * @return 模型 JSON；id 非法时返回 null
     */
    @Nullable
    private static JsonObject parse(String spec) {
        try {
            String facingPart;
            String subPart = null;
            int sep = spec.indexOf("__");
            if (sep >= 0) {
                facingPart = spec.substring(0, sep);
                subPart = spec.substring(sep + 2);
            } else {
                facingPart = spec;
            }

            // facingPart = "<facing>_g<growth>"
            int gi = facingPart.lastIndexOf("_g");
            if (gi < 0) {
                return null;
            }
            String facingName = facingPart.substring(0, gi);
            int growth = Integer.parseInt(facingPart.substring(gi + 2));

            net.minecraft.util.math.Direction facing = byName(facingName);
            if (facing == null) {
                return null;
            }

            Set<net.minecraft.util.math.Direction> subs = new LinkedHashSet<>();
            if (subPart != null && !subPart.isEmpty()) {
                for (String s : subPart.split("_")) {
                    net.minecraft.util.math.Direction d = byName(s);
                    if (d == null) {
                        return null;
                    }
                    subs.add(d);
                }
            }
            return BranchModelFactory.build(facing, growth, subs);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static net.minecraft.util.math.Direction byName(String name) {
        for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
            if (d.asString().equals(name)) {
                return d;
            }
        }
        return null;
    }
}
