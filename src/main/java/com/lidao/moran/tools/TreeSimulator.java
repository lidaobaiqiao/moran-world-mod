package com.lidao.moran.tools;

import com.lidao.moran.systems.trees.HormoneProfile;
import com.lidao.moran.systems.trees.Phenotype;
import com.lidao.moran.systems.trees.TreeSpecies;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 离线树形模拟渲染器 —— 不进游戏批量验证树形，为「完美树基准」筛种子。
 *
 * <p>动机：树形调参每次都靠 runClient 肉眼看，反馈太慢。本工具把生长引擎的
 * 决策逻辑复刻成纯 Java 模拟（世界是 {@code Map<BlockPos, SimBlock>}），
 * 批量跑几十个种子，渲染成侧视 PNG 供一眼筛树：
 * <pre>
 *   gradlew simTrees                                 # 48 棵总览图 → .workbuddy/sim_out/
 *   gradlew simTrees -PsimArgs="--seeds 96"          # 加大量
 *   gradlew simTrees -PsimArgs="--pick 123"          # 单棵放大 + 打印可粘贴的基准参数
 *   gradlew simTrees -PsimArgs="--env average"       # 环境评分降档（target 变矮）
 * </pre>
 *
 * <h2>找完美树的流程</h2>
 * 总览图里看中哪棵 → {@code --pick seed} 放大并打印它的激素档案 →
 * 把打印出的 {@code .phenotype(...)} 行粘进 PeachSpecies 作为基准原型 →
 * 此后真实林子以它为常态，激素原型系统在基准附近给出「相似」的变体。
 *
 * <h2>同步责任（重要）</h2>
 * 生长决策是 {@link com.lidao.moran.systems.blocks.MoranBranchBlock} 与
 * {@link com.lidao.moran.systems.trees.PeachSpecies} 的<b>复刻</b>（方法名一一对应，
 * 见各方法注释的出处），结构参数全部实时读 {@link TreeSpecies} 档案实例——
 * 改引擎逻辑时必须同步本文件。<b>激素推导不走复刻</b>：直接调
 * {@link TreeSpecies#hormoneProfileForSeed(long)}，与游戏共用同一条代码路径，零漂移。
 *
 * <p>环境替身：模拟的是野生树（NATURAL 模式、条件过即长），
 * 环境只通过目标高度评分表达（--env ideal/average/poor）。
 */
public final class TreeSimulator {

    public static void main(String[] args) throws Exception {
        int seeds = 48;
        long pick = Long.MIN_VALUE;
        String out = ".workbuddy/sim_out";
        String webOut = null;
        int envScore = 4;
        boolean best = false;
        int bestCount = 300;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seeds" -> seeds = Integer.parseInt(args[++i]);
                case "--pick" -> pick = Long.parseLong(args[++i]);
                case "--out" -> out = args[++i];
                case "--web" -> webOut = args[++i];
                case "--env" -> envScore = switch (args[++i]) {
                    case "poor" -> 2;
                    case "average" -> 3;
                    default -> 4;
                };
                case "--best" -> best = true;
                case "--count" -> bestCount = Integer.parseInt(args[++i]);
                default -> { }
            }
        }

        TreeSpecies species = com.lidao.moran.systems.trees.PeachSpecies.peach();
        Files.createDirectories(Path.of(out));

        if (pick != Long.MIN_VALUE) {
            SimWorld w = growTree(species, pick, envScore);
            String label = String.format(Locale.ROOT,
                    "seed=%d  %s  aux=%.2f cyt=%.2f gib=%.2f  target=%d",
                    pick, w.phenotype.id(), w.hormones.auxin(), w.hormones.cytokinin(),
                    w.hormones.gibberellin(), w.max);
            TreeRenderer.renderTreePng(w, Path.of(out, "tree_" + pick + ".png"), label);
            printBaseline(species, pick, w);
            return;
        }

        if (best) {
            runBest(species, bestCount, envScore, out);
            return;
        }

        List<SimWorld> trees = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        System.out.println("seed | 原型  | aux/cyt/gib      | target | 高  | 宽 | 枝/叶/叉");
        for (int i = 0; i < seeds; i++) {
            long seed = 1000L + i;
            SimWorld w = growTree(species, seed, envScore);
            trees.add(w);
            labels.add(String.format(Locale.ROOT, "#%d %s", seed, w.phenotype.id()));
            SimStats s = stats(w);
            System.out.printf(Locale.ROOT, "%d | %-5s | %.2f/%.2f/%.2f | %-6d | %-3d | %-2d | %d/%d/%d%n",
                    seed, w.phenotype.id(), w.hormones.auxin(), w.hormones.cytokinin(),
                    w.hormones.gibberellin(), w.max, s.height, s.width,
                    s.branches, s.leaves, s.forks);
        }

        if (webOut != null) {
            writeWeb(Path.of(webOut), trees);
            System.out.println("3D 查看器: " + Path.of(webOut, "trees.html").toAbsolutePath()
                    + "（浏览器直接打开，可旋转缩放）");
            return;
        }

        Path sheet = Path.of(out, "sheet.png");
        TreeRenderer.renderSheetPng(trees, labels, 8, sheet,
                "moran tree sim  (pick: gradlew simTrees -PsimArgs=\"--pick <seed>\")");
        System.out.println("总览图: " + sheet.toAbsolutePath());
        // 按枝叶量排个序，方便从数字角度挑候选
        trees.sort((a, b) -> Integer.compare(stats(b).branches + stats(b).leaves,
                stats(a).branches + stats(a).leaves));
        System.out.print("枝叶最密 Top5: ");
        for (int i = 0; i < Math.min(5, trees.size()); i++) {
            System.out.print("#" + trees.get(i).seed + " ");
        }
        System.out.println();
    }

    /** 打印「完美树基准」：把抽到的激素档案写成可直接粘进 PeachSpecies 的原型声明 */
    private static void printBaseline(TreeSpecies species, long seed, SimWorld w) {
        HormoneProfile h = w.hormones;
        System.out.println("===== 完美树基准（seed=" + seed + "）=====");
        System.out.printf(Locale.ROOT, "原型: %s   auxin=%.3f  cytokinin=%.3f  gibberellin=%.3f  target=%d%n",
                w.phenotype.id(), h.auxin(), h.cytokinin(), h.gibberellin(), w.max);
        System.out.println("粘进 PeachSpecies.peach()（作为基准原型，其他树在其附近变体）：");
        System.out.printf(Locale.ROOT,
                ".phenotype(\"golden\", 1F, %.2fF, %.2fF, %.2fF)%n",
                h.auxin(), h.cytokinin(), h.gibberellin());
        System.out.println("注意：这是「个体档案」。要成为基准应取整到两位小数并去掉个体残差的意义——");
        System.out.println("      基准定义的是原型常态，残差留给 hormoneVariation。");
    }

    // ============================================================
    //  3D 查看器导出：体素数据 + 单文件网页（Three.js 相对引用，离线可开）
    // ============================================================

    /**
     * 导出 trees_data.js（全部树的体素数组）+ trees.html（查看器页面）。
     *
     * <p>渲染轮子不自己造：页面用 Three.js（GitHub 最主流的 WebGL 库）画体素，
     * 相对路径引用同目录的 three.min.js（离线可用；文件缺失时页面会提示改用 CDN）。
     * 体素格式：[x, y, z, type, growth]；type 0=枝干（边长=growth/8，忠实档位粗细）
     * 1=花苞 2=树叶。
     */
    private static void writeWeb(Path outDir, List<SimWorld> trees) throws Exception {
        Files.createDirectories(outDir);

        StringBuilder js = new StringBuilder("// 由 TreeSimulator 生成：体素数据树\nwindow.TREES = [\n");
        for (SimWorld w : trees) {
            SimStats st = stats(w);
            js.append("{\"seed\":").append(w.seed)
                    .append(",\"ph\":\"").append(w.phenotype.id()).append('"')
                    .append(",\"hormone\":\"").append(String.format(Locale.ROOT,
                            "aux %.2f / cyt %.2f / gib %.2f", w.hormones.auxin(),
                            w.hormones.cytokinin(), w.hormones.gibberellin()))
                    .append('"')
                    .append(",\"max\":").append(w.max)
                    .append(",\"stat\":\"枝 ").append(st.branches)
                    .append(" · 叶 ").append(st.leaves)
                    .append(" · 叉 ").append(st.forks)
                    .append(" · 高 ").append(st.height).append('"')
                    .append(",\"voxels\":[");
            boolean first = true;
            for (Map.Entry<BlockPos, SimBlock> e : w.blocks.entrySet()) {
                if (!first) {
                    js.append(',');
                }
                first = false;
                BlockPos p = e.getKey();
                SimBlock s = e.getValue();
                int type = s.type == SimType.BRANCH ? 0 : s.type == SimType.BUD ? 1 : 2;
                js.append('[').append(p.getX()).append(',').append(p.getY()).append(',')
                        .append(p.getZ()).append(',').append(type).append(',').append(s.growth).append(']');
            }
            js.append("]},\n");
        }
        js.append("];\n");
        Files.writeString(outDir.resolve("trees_data.js"), js.toString());

        Files.writeString(outDir.resolve("trees.html"), WEB_PAGE);
    }

    /** 查看器页面：左侧树列表 + 右侧 Three.js 体素场景（左键旋转 / 滚轮缩放） */
    private static final String WEB_PAGE = """
            <!DOCTYPE html>
            <html lang="zh">
            <head>
            <meta charset="utf-8">
            <title>墨世界 · 树形查看器</title>
            <style>
              html,body{margin:0;height:100%;overflow:hidden;font-family:"Microsoft YaHei",sans-serif}
              #side{position:absolute;left:0;top:0;bottom:0;width:230px;background:#faf8f4;
                    border-right:1px solid #ddd;overflow-y:auto;padding:10px;box-sizing:border-box}
              #side h3{margin:4px 0 8px;font-size:14px;color:#555}
              .item{padding:5px 8px;border-radius:6px;cursor:pointer;font-size:12px;color:#333}
              .item:hover{background:#efe9df}
              .item.sel{background:#4a7c59;color:#fff}
              .item .sub{color:#999;font-size:11px}
              .item.sel .sub{color:#dfe8e0}
              #info{margin-top:10px;padding:8px;background:#f0ece4;border-radius:6px;
                    font-size:12px;color:#444;line-height:1.7;display:none}
              #view{position:absolute;left:230px;right:0;top:0;bottom:0}
              #tip{position:absolute;right:12px;bottom:10px;color:#999;font-size:12px}
              #warn{position:absolute;left:250px;top:10px;color:#a33;font-size:13px;display:none}
            </style>
            </head>
            <body>
            <div id="side">
              <h3>模拟树列表（点击切换）</h3>
              <div id="list"></div>
              <div id="info"></div>
            </div>
            <div id="view"></div>
            <div id="tip">左键拖拽旋转 · 滚轮缩放 · 右键平移</div>
            <div id="warn">未找到 three.min.js —— 请把 three.min.js 放到本目录，或联网后将
              &lt;script src="./three.min.js"&gt; 改为 CDN 地址。</div>
            <script src="./three.min.js"></script>
            <script src="./trees_data.js"></script>
            <script>
            if (typeof THREE === 'undefined') {
              document.getElementById('warn').style.display = 'block';
            } else {
              const view = document.getElementById('view');
              const scene = new THREE.Scene();
              scene.background = new THREE.Color(0xf2efe9);
              const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 500);
              const renderer = new THREE.WebGLRenderer({antialias: true});
              view.appendChild(renderer.domElement);

              scene.add(new THREE.AmbientLight(0xffffff, 0.6));
              const sun = new THREE.DirectionalLight(0xfff3e0, 0.85);
              sun.position.set(18, 30, 12);
              scene.add(sun);
              const grid = new THREE.GridHelper(40, 40, 0xd8d2c6, 0xe7e2d8);
              scene.add(grid);

              // —— 简易轨道控制：左键旋转 / 滚轮缩放 / 右键平移 ——
              const ctrl = {theta: 0.8, phi: 1.05, radius: 26, target: new THREE.Vector3(0, 6, 0)};
              function applyCam() {
                ctrl.phi = Math.max(0.05, Math.min(Math.PI - 0.05, ctrl.phi));
                ctrl.radius = Math.max(3, Math.min(200, ctrl.radius));
                camera.position.set(
                  ctrl.target.x + ctrl.radius * Math.sin(ctrl.phi) * Math.sin(ctrl.theta),
                  ctrl.target.y + ctrl.radius * Math.cos(ctrl.phi),
                  ctrl.target.z + ctrl.radius * Math.sin(ctrl.phi) * Math.cos(ctrl.theta));
                camera.lookAt(ctrl.target);
              }
              let drag = null;
              renderer.domElement.addEventListener('mousedown', e => drag = {x: e.clientX, y: e.clientY, b: e.button});
              window.addEventListener('mouseup', () => drag = null);
              window.addEventListener('mousemove', e => {
                if (!drag) return;
                const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
                drag = {x: e.clientX, y: e.clientY, b: drag.b};
                if (drag.b === 2) {
                  const pan = new THREE.Vector3().crossVectors(camera.up,
                    new THREE.Vector3().subVectors(ctrl.target, camera.position)).normalize();
                  ctrl.target.addScaledVector(pan, dx * ctrl.radius * 0.0016);
                  ctrl.target.y += dy * ctrl.radius * 0.0016;
                } else {
                  // 跟手约定：向右拖 = 树向右转（相机相对树向左公转，theta 取负增量）
                  ctrl.theta -= dx * 0.006;
                  ctrl.phi -= dy * 0.006;
                }
                applyCam();
              });
              renderer.domElement.addEventListener('wheel', e => {
                e.preventDefault();
                ctrl.radius *= e.deltaY > 0 ? 1.1 : 0.9;
                applyCam();
              }, {passive: false});
              renderer.domElement.addEventListener('contextmenu', e => e.preventDefault());

              // —— 体素 → 场景 ——
              const matBark = new THREE.MeshLambertMaterial({color: 0x6b4a2f});
              const matLeaf = new THREE.MeshLambertMaterial({color: 0x6fae5f, transparent: true, opacity: 0.88});
              const matBud  = new THREE.MeshLambertMaterial({color: 0xe08aa0});
              const geoCache = {};
              function boxGeo(s) {
                if (!geoCache[s]) geoCache[s] = new THREE.BoxGeometry(s, s, s);
                return geoCache[s];
              }
              let current = null;
              function showTree(i) {
                const t = window.TREES[i];
                if (current) { scene.remove(current); }
                const g = new THREE.Group();
                for (const v of t.voxels) {
                  const [x, y, z, type, growth] = v;
                  const size = type === 0 ? Math.max(0.125, growth / 8) : (type === 1 ? 0.5 : 1.0);
                  const m = new THREE.Mesh(boxGeo(size),
                    type === 0 ? matBark : (type === 1 ? matBud : matLeaf));
                  m.position.set(x, y + 0.5, z);
                  g.add(m);
                }
                // 居中到原点上方（y 不动，落在地面网格上）
                let minX = 1e9, maxX = -1e9, minZ = 1e9, maxZ = -1e9, maxY = 1;
                for (const v of t.voxels) {
                  minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
                  minZ = Math.min(minZ, v[2]); maxZ = Math.max(maxZ, v[2]);
                  maxY = Math.max(maxY, v[1]);
                }
                g.position.set(-(minX + maxX) / 2, 0, -(minZ + maxZ) / 2);
                scene.add(g);
                current = g;
                ctrl.target.set(0, maxY * 0.45, 0);
                ctrl.radius = Math.max(10, maxY * 2.2);
                applyCam();
                document.getElementById('info').style.display = 'block';
                document.getElementById('info').innerHTML =
                  '<b>seed ' + t.seed + '</b> · ' + t.ph +
                  '<br>激素: ' + t.hormone +
                  '<br>max=' + t.max + '<br>' + t.stat;
                document.querySelectorAll('.item').forEach((el, j) =>
                  el.classList.toggle('sel', j === i));
              }

              // —— 列表 ——
              const list = document.getElementById('list');
              window.TREES.forEach((t, i) => {
                const div = document.createElement('div');
                div.className = 'item';
                div.innerHTML = '<b>#' + t.seed + '</b> ' + t.ph +
                  '<div class="sub">' + t.stat + '</div>';
                div.onclick = () => showTree(i);
                list.appendChild(div);
              });

              function resize() {
                const w = view.clientWidth, h = view.clientHeight;
                renderer.setSize(w, h);
                camera.aspect = w / h;
                camera.updateProjectionMatrix();
              }
              window.addEventListener('resize', resize);
              resize();
              showTree(0);

              (function loop() {
                requestAnimationFrame(loop);
                renderer.render(scene, camera);
              })();
            }
            </script>
            </body>
            </html>
            """;


    // ============================================================
    //  世界替身
    // ============================================================

    enum SimType { BRANCH, BUD, LEAF }

    /** 方块替身：字段与 MoranBranchBlock / MoranFlowerBudBlock 的属性一一对应 */
    static final class SimBlock {
        final SimType type;
        int growth;          // BRANCH: 1..max；BUD: stage 1..3
        Direction facing;
        boolean trunk;
        boolean dormant;
        boolean topped;
        int forkMask;        // FORK_SET 的位掩码（模拟器内部用 int，无哈希熵问题）

        SimBlock(SimType type, int growth, Direction facing) {
            this.type = type;
            this.growth = growth;
            this.facing = facing;
        }
    }

    /** 一棵模拟树：世界 + 随机流 + 激素档案 + 根块 */
    static final class SimWorld {
        final TreeMap<BlockPos, SimBlock> blocks = new TreeMap<>();
        final Random random;
        final TreeSpecies species;
        final HormoneProfile hormones;
        final Phenotype phenotype;
        final long seed;
        final int max;          // 本树成熟档位（树基种子推导，与游戏同路径）
        SimBlock root;
        /** 复刻 IDLE Map：连续无产出计数（不入档，terminate 写回 topped/dormant） */
        private final Map<BlockPos, Integer> idleCounts = new HashMap<>();

        SimWorld(TreeSpecies species, long seed, int envScore) {
            this.species = species;
            this.seed = seed;
            this.random = Random.create(seed);
            // 激素走与游戏相同的纯函数路径（真实运行时种子 = 树基坐标 asLong）
            TreeSpecies.SeededHormones sh = species.seededHormones(seed);
            this.phenotype = sh.phenotype();
            this.hormones = sh.hormones();
            // 本树成熟档位：同样走游戏的纯函数路径。
            // 模拟树基固定在原点，故用 seed 代入（真实运行时是树基坐标 asLong），
            // 否则每棵模拟树会算出同一个 max，个体差异全丢。
            this.max = species.treeMaxGrowth(seed);
            this.root = new SimBlock(SimType.BRANCH, 1, Direction.UP);
            root.trunk = true;
            blocks.put(new BlockPos(0, 0, 0), root);
        }

        int bumpIdle(BlockPos p) {
            return idleCounts.merge(p, 1, Integer::sum);
        }

        void clearIdle(BlockPos p) {
            idleCounts.remove(p);
        }

        SimBlock get(BlockPos p) {
            return blocks.get(p);
        }

        boolean isBranch(BlockPos p) {
            SimBlock s = blocks.get(p);
            return s != null && s.type == SimType.BRANCH;
        }

        boolean air(BlockPos p) {
            return !blocks.containsKey(p);
        }

        void put(BlockPos p, SimBlock s) {
            blocks.put(p, s);
        }
    }

    record SimStats(int height, int width, int branches, int leaves, int buds, int forks) {
    }

    static SimStats stats(SimWorld w) {
        int maxY = 0, minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        int branches = 0, leaves = 0, buds = 0, forks = 0;
        for (Map.Entry<BlockPos, SimBlock> e : w.blocks.entrySet()) {
            BlockPos p = e.getKey();
            SimBlock s = e.getValue();
            maxY = Math.max(maxY, p.getY());
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
            switch (s.type) {
                case BRANCH -> {
                    branches++;
                    // 主干的 forkMask 是定干主枝位(渲染填充用),不是分叉;叉只统计侧枝
                    if (!s.trunk) {
                        forks += Integer.bitCount(s.forkMask);
                    }
                }
                case LEAF -> leaves++;
                case BUD -> buds++;
            }
        }
        return new SimStats(maxY, Math.max(maxX - minX, maxZ - minZ), branches, leaves, buds, forks);
    }

    // ============================================================
    //  生长主循环（复刻 scheduledTick 的 NATURAL 路径）
    // ============================================================

    /** 轮数兜底：正常树几百轮内收尾，超限视为异常（打印警告） */
    private static final int MAX_ROUNDS = 3000;

    static SimWorld growTree(TreeSpecies species, long seed, int envScore) {
        SimWorld w = new SimWorld(species, seed, envScore);
        for (int round = 0; round < MAX_ROUNDS; round++) {
            boolean anyActive = false;
            // TreeMap 按坐标排序，顺序稳定可复现
            for (Map.Entry<BlockPos, SimBlock> e : new ArrayList<>(w.blocks.entrySet())) {
                BlockPos pos = e.getKey();
                SimBlock s = e.getValue();
                if (s.type == SimType.BRANCH) {
                    if (s.dormant) {
                        continue;   // 复刻 scheduledTick：DORMANT 不参与（被唤醒后恢复）
                    }
                    if (isFullyGrown(w, pos, s)) {
                        continue;   // 复刻：长满永久静默
                    }
                    anyActive = true;
                    tickBranch(w, pos, s);
                } else if (s.type == SimType.BUD) {
                    anyActive = true;
                    tickBud(w, pos, s);
                }
            }
            if (!anyActive) {
                return w;
            }
        }
        System.out.println("警告: seed=" + seed + " 达到轮数上限，可能存在逻辑死循环");
        return w;
    }

    /** 复刻 MoranBranchBlock.scheduledTick（NATURAL 分支：环境过即长） */
    private static void tickBranch(SimWorld w, BlockPos pos, SimBlock s) {
        boolean progressed = growPart(w, pos, s);
        if (isFullyGrown(w, pos, s)) {
            return;   // 静默，防 tick 风暴
        }
        // 生命周期终止：连续 IDLE_LIMIT 次请求无产出 → terminate
        // （侧枝够开花年龄则开花收场——树冠的花叶全靠这条退出路径，同步引擎）
        if (progressed) {
            w.clearIdle(pos);
        } else if (w.bumpIdle(pos) >= IDLE_LIMIT) {
            if (s.trunk) {
                s.topped = true;
            } else {
                s.dormant = true;
                if (s.growth >= (w.max + 1) / 2) {
                    onBranchStop(w, pos, s.facing);
                }
            }
            w.clearIdle(pos);
        }
    }

    private static final int IDLE_LIMIT = 10;

    private static boolean growPart(SimWorld w, BlockPos pos, SimBlock s) {
        Direction facing = s.facing;
        int growth = s.growth;
        // 复刻 growPart：档位相对本树 max（treeMaxGrowth=TARGET-4）；侧枝至 min(max-1, 营养)
        int max = w.max;
        int cap = s.trunk ? max
                : Math.min(max - 1, nutritionAt(w, pos, facing));
        boolean progressed = false;
        if (growth < cap) {
            s.growth = ++growth;
            progressed = true;
        }
        progressed |= s.trunk ? growTrunk(w, pos, s, growth) : growBranch(w, pos, s, growth, facing);
        return progressed;
    }

    /** 复刻 MoranBranchBlock.growTrunk */
    private static boolean growTrunk(SimWorld w, BlockPos pos, SimBlock s, int growth) {
        boolean progressed = false;
        BlockPos above = pos.up();
        boolean top = !w.isBranch(above);
        int height = heightBelow(w, pos) + 1;

        // 封顶决策：主干只长 max/2 格（桃 3-4 格），整树 6-8 格
        if (top && !s.topped) {
            boolean canExtend = w.air(above);
            if (height >= w.species.trunkHalfHeight(w.max) || !canExtend) {
                s.topped = true;
                progressed = true;
            }
        }

        if (s.topped) {
            wakeDormantBuds(w, pos);
            // 定干分叉（开心形）：主干尽头四水平向逐拍抽主枝（等粗 max-1），
            // 无顶花苞；幂等判据 = 邻居本身，每拍一根
            // 等干长满（growth 到 max）再定干 —— 四根主枝档位统一 max-1，冠层齐整
            if (top && growth >= w.max && Integer.bitCount(s.forkMask) < w.species.limbCount()) {
                for (Direction d : HORIZONTALS) {
                    BlockPos p = pos.offset(d);
                    if (w.air(p) && !isCrowded(w, p)) {
                        SimBlock limb = new SimBlock(SimType.BRANCH, Math.max(1, w.max - 2), d);
                        limb.trunk = false;
                        w.put(p, limb);
                        // 同步引擎：主干记 FORK_SET 位（渲染侧向填充用，模拟内保持状态一致）
                        s.forkMask |= 1 << slotOf(s.facing, d);
                        progressed = true;
                        break;   // 每拍一根
                    }
                }
            }
        } else if (top && w.air(above)) {
            // 抽高期：自身 >= 2 才能向上生新节（新节恒为 1）
            if (growth >= 2) {
                SimBlock next = new SimBlock(SimType.BRANCH, 1, Direction.UP);
                next.trunk = true;
                w.put(above, next);
                progressed = true;
            }
        }

        // 侧芽萌发：桃范式 isBudPosition 恒 false（侧枝完全由定干承担），
        // 引擎的逐节侧芽机制保留给其他树种 —— 此处按当前档案行为省略该分支。
        return progressed;
    }

    private static final int MAX_BRANCH_CHAIN = 6;
    private static final float BRANCH_EXTEND_CHANCE = 0.75F;

    /** 复刻 MoranBranchBlock.growBranch */
    private static boolean growBranch(SimWorld w, BlockPos pos, SimBlock s, int growth, Direction facing) {
        int chainPos = chainPosition(w, pos, facing);
        BlockPos tip = pos.offset(facing);
        boolean tipAir = w.air(tip);

        // 末端延伸：链上限按养分递减；赤霉素促伸长
        int chainLimit = Math.min(MAX_BRANCH_CHAIN, Math.max(2, nutritionAt(w, pos, facing) - w.max / 4));
        if (growth >= 2 && chainPos < chainLimit && tipAir
                && w.random.nextFloat() < BRANCH_EXTEND_CHANCE * w.hormones.gibberellin()) {
            SimBlock next = new SimBlock(SimType.BRANCH, 1, facing);
            next.trunk = false;
            w.put(tip, next);
            return true;
        }

        // 分叉：额度（营养）+ 衰减 + 空间冷却 + 激素（cytokinin 促 / auxin 抑）
        if (growth >= w.max - 1 && chainPos >= w.species.forkMinChainPos()) {
            int mask = s.forkMask;
            int have = Math.max(Integer.bitCount(mask), neighborForkCount(w, pos, facing));
            int cap = Math.min(4, w.species.forkCapacity(nutritionAt(w, pos, facing),
                    w.species.treeMaxGrowth(w.seed)));
            if (have < cap && !forkedWithin(w, pos, facing, w.species.forkSpacing())) {
                float p = w.species.subBranchChance() * (float) Math.pow(w.species.forkDecay(), have)
                        * w.hormones.cytokinin() * (2F - w.hormones.auxin());
                if (w.random.nextFloat() < p) {
                    // 候选按收益加权随机；门槛随生长素抬升
                    List<Direction> free = new ArrayList<>(4);
                    int[] gains = new int[4];
                    int totalGain = 0;
                    int bestGain = Integer.MIN_VALUE;
                    for (Direction side : perpendiculars(facing)) {
                        int slot = slotOf(facing, side);
                        if (slot < 0 || (mask & (1 << slot)) != 0) {
                            continue;
                        }
                        if (!w.air(pos.offset(side))) {
                            continue;
                        }
                        int g = branchForkGain(w, pos, side);
                        if (g > bestGain) {
                            bestGain = g;
                        }
                        if (g > 0) {
                            gains[slot] = g;
                            free.add(side);
                            totalGain += g;
                        }
                    }
                    int minGain = Math.round(w.species.forkMinGain() * w.hormones.auxin());
                    if (bestGain >= minGain && !free.isEmpty()) {
                        int roll = w.random.nextInt(totalGain);
                        Direction side = free.get(free.size() - 1);
                        for (Direction candidate : free) {
                            roll -= gains[slotOf(facing, candidate)];
                            if (roll < 0) {
                                side = candidate;
                                break;
                            }
                        }
                        // 子枝档位：新枝一律从 1 档长起（同上，与引擎一致）
                        SimBlock sub = new SimBlock(SimType.BRANCH, 1, side);
                        sub.trunk = false;
                        w.put(pos.offset(side), sub);
                        s.forkMask = mask | (1 << slotOf(facing, side));
                        return true;
                    }
                }
            }
        }

        // 主动停止开花（养分封顶前的提前成花）：与引擎同步——只对未到自身 cap 的节生效，
        // 到 cap 的节走分叉窗口 + 终止开花（见 tickBranch）；开花即休眠（停止即终末）
        int cap = Math.min(w.max - 1, nutritionAt(w, pos, facing));
        if (growth < cap && growth >= (w.max + 1) / 2
                && w.random.nextFloat() < w.species.branchStopChance() * chainPos / (float) chainLimit) {
            onBranchStop(w, pos, facing);
            s.dormant = true;
            return true;
        }
        return false;
    }

    /** 复刻 PeachSpecies.onBranchStop：末端 + 四周 + 链上侧腋开花 */
    private static void onBranchStop(SimWorld w, BlockPos pos, Direction facing) {
        placeBud(w, pos.offset(facing), facing);
        Direction[] around = facing.getAxis() == Direction.Axis.Y
                ? new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}
                : new Direction[]{facing.rotateYCounterclockwise(), facing.rotateYClockwise()};
        for (Direction d : around) {
            placeBud(w, pos.offset(d), d);
        }
        if (facing.getAxis() == Direction.Axis.Y) {
            return;
        }
        BlockPos p = pos.offset(facing.getOpposite());
        for (int i = 0; i < AXILLARY_DEPTH; i++) {
            SimBlock s = w.get(p);
            if (s == null || s.type != SimType.BRANCH || s.facing != facing) {
                break;
            }
            if (w.random.nextFloat() < AXILLARY_BUD_CHANCE) {
                placeBud(w, p.up(), Direction.UP);
            }
            if (w.random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction l = facing.rotateYCounterclockwise();
                placeBud(w, p.offset(l), l);
            }
            if (w.random.nextFloat() < AXILLARY_BUD_CHANCE) {
                Direction r = facing.rotateYClockwise();
                placeBud(w, p.offset(r), r);
            }
            p = p.offset(facing.getOpposite());
        }
    }

    private static final float AXILLARY_BUD_CHANCE = 0.6F;
    private static final int AXILLARY_DEPTH = 3;

    private static void placeBud(SimWorld w, BlockPos pos, Direction facing) {
        if (w.air(pos)) {
            w.put(pos, new SimBlock(SimType.BUD, 1, facing));
        }
    }

    /** 复刻 MoranFlowerBudBlock.scheduledTick（NATURAL）：stage 满 → 成熟 */
    private static void tickBud(SimWorld w, BlockPos pos, SimBlock s) {
        if (s.growth >= 3) {
            onBudMature(w, pos, s.facing);
        } else {
            s.growth++;
        }
    }

    /** 复刻 PeachSpecies.onBudMature：先花后叶；顶花苞生成小树冠（四向必放 + 四角半数）。
     *  【盛花 prototype，待与引擎同步】侧向花苞成熟时不再只长单块叶——长成小花团
     *  （水平 8 邻大半 + 上方强化 + 下方弱化），冠层连片成「盛花体量」，
     *  对标目标图的中国风满树繁花；否则等轴测下只是架子挂点、冠层空心。 */
    private static void onBudMature(SimWorld w, BlockPos pos, Direction facing) {
        w.put(pos, new SimBlock(SimType.LEAF, 1, facing));
        if (facing == Direction.UP) {
            for (Direction d : HORIZONTALS) {
                placeLeaf(w, pos.offset(d));
            }
            for (Direction d : HORIZONTALS) {
                if (w.random.nextBoolean()) {
                    placeLeaf(w, pos.offset(d).up());
                }
            }
        } else {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos p = pos.add(dx, 0, dz);
                    if (w.random.nextFloat() < 0.75F) {
                        placeLeaf(w, p);
                    }
                    if (w.random.nextFloat() < 0.40F) {
                        placeLeaf(w, p.up());
                    }
                }
            }
            if (w.random.nextFloat() < 0.85F) {
                placeLeaf(w, pos.up());
            }
            if (w.random.nextFloat() < 0.25F) {
                placeLeaf(w, pos.down());
            }
        }
    }

    private static void placeLeaf(SimWorld w, BlockPos pos) {
        if (w.air(pos)) {
            w.put(pos, new SimBlock(SimType.LEAF, 1, Direction.UP));
        }
    }

    // ============================================================
    //  结构查询（逐方法复刻 MoranBranchBlock 的私有助手）
    // ============================================================

    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** 复刻 nutritionAt：沿链回溯到主干，同链每节 -chainDecay、换向 -branchDecay */
    private static int nutritionAt(SimWorld w, BlockPos pos, Direction facing) {
        int decay = 0;
        Direction dir = facing;
        BlockPos p = pos;
        for (int i = 0; i < 24; i++) {
            BlockPos back = p.offset(dir.getOpposite());
            SimBlock s = w.get(back);
            if (s == null || s.type != SimType.BRANCH) {
                return Math.max(1, w.species.trunkMaxGrowth() - decay);
            }
            if (s.trunk) {
                return Math.max(1, s.growth - decay);
            }
            decay += (s.facing == dir) ? w.species.chainNutritionDecay() : w.species.branchNutritionDecay();
            p = back;
            dir = s.facing;
        }
        return 1;
    }

    /** 复刻 chainPosition：距主干节数（1 = 贴干首节） */
    private static int chainPosition(SimWorld w, BlockPos pos, Direction facing) {
        int dist = 1;
        BlockPos p = pos.offset(facing.getOpposite());
        while (dist < 16) {
            SimBlock s = w.get(p);
            if (s == null || s.type != SimType.BRANCH || s.facing != facing) {
                return dist;
            }
            dist++;
            p = p.offset(facing.getOpposite());
        }
        return dist;
    }

    /** 复刻 heightBelow */
    private static int heightBelow(SimWorld w, BlockPos pos) {
        int height = 0;
        BlockPos p = pos.down();
        while (height < w.species.biologicalTopMax()) {
            SimBlock s = w.get(p);
            if (s == null || s.type != SimType.BRANCH || !s.trunk) {
                break;
            }
            height++;
            p = p.down();
        }
        return height;
    }

    /** 复刻 wakeDormantBuds：主干到顶，唤醒整列休眠侧芽（幂等） */
    private static void wakeDormantBuds(SimWorld w, BlockPos trunkPos) {
        BlockPos p = trunkPos;
        while (w.isBranch(p.down())) {
            p = p.down();
        }
        BlockPos cur = p;
        while (true) {
            for (Direction d : HORIZONTALS) {
                SimBlock s = w.get(cur.offset(d));
                if (s != null && s.type == SimType.BRANCH && s.dormant) {
                    s.dormant = false;
                }
            }
            SimBlock above = w.get(cur.up());
            if (above != null && above.type == SimType.BRANCH && above.trunk) {
                cur = cur.up();
            } else {
                return;
            }
        }
    }

    /** 复刻 treeTopped：沿主干向上读到顶端 TOPPED */
    private static boolean treeTopped(SimWorld w, BlockPos pos) {
        BlockPos p = pos;
        while (true) {
            SimBlock above = w.get(p.up());
            if (above != null && above.type == SimType.BRANCH && above.trunk) {
                p = p.up();
            } else {
                break;
            }
        }
        SimBlock top = w.get(p);
        return top != null && top.topped;
    }

    /** 复刻 isCrowded：六向实心 ≥5 压抑萌芽 */
    private static boolean isCrowded(SimWorld w, BlockPos target) {
        int solid = 0;
        for (Direction d : Direction.values()) {
            if (!w.air(target.offset(d))) {
                solid++;
            }
        }
        return solid >= 5;
    }

    /** 复刻 opennessAround：候选方向 2 格内空气 + 斜上开口 */
    private static int opennessAround(SimWorld w, BlockPos pos, Direction dir) {
        int open = 0;
        for (int step = 1; step <= 2; step++) {
            if (w.air(pos.offset(dir, step))) {
                open++;
            }
        }
        if (w.air(pos.offset(dir).up())) {
            open++;
        }
        return open;
    }

    /** 复刻 TreeSpecies.branchForkGain 默认实现：开口度×2 + 生长素调制的向顶 bias */
    private static int branchForkGain(SimWorld w, BlockPos pos, Direction candidate) {
        int gain = opennessAround(w, pos, candidate) * 2;
        int upBias = Math.round(5F * w.hormones.auxin());   // 向上发散（同步 TreeSpecies 默认）
        if (candidate == Direction.UP) {
            gain += upBias;
        } else if (candidate == Direction.DOWN) {
            gain -= upBias + 1;
        }
        return gain;
    }

    /** 复刻 forkedWithin：链上前 spacing-1 节内是否已分叉 */
    private static boolean forkedWithin(SimWorld w, BlockPos pos, Direction facing, int spacing) {
        BlockPos p = pos;
        for (int i = 1; i < spacing; i++) {
            p = p.offset(facing.getOpposite());
            SimBlock s = w.get(p);
            if (s == null || s.type != SimType.BRANCH || s.facing != facing) {
                return false;
            }
            if (s.forkMask != 0) {
                return true;
            }
        }
        return false;
    }

    /** 复刻 neighborForkCount：mask 主判据 + 邻居扫描兜底（排除同向延伸节） */
    private static int neighborForkCount(SimWorld w, BlockPos pos, Direction selfFacing) {
        SimBlock self = w.get(pos);
        if (self != null && self.forkMask != 0) {
            return Integer.bitCount(self.forkMask);
        }
        int n = 0;
        for (Direction d : Direction.values()) {
            if (d == selfFacing) {
                continue;
            }
            SimBlock s = w.get(pos.offset(d));
            if (s != null && s.type == SimType.BRANCH && !s.trunk && s.facing == d) {
                n++;
            }
        }
        return n;
    }

    /** 复刻 isFullyGrown：结构上长满 → 永久静默（档位相对本树 max；定干未完成不算满） */
    private static boolean isFullyGrown(SimWorld w, BlockPos pos, SimBlock s) {
        if (s.type != SimType.BRANCH) {
            return false;
        }
        int max = w.max;
        if (s.trunk) {
            if (s.growth < max) {
                return false;
            }
            BlockPos above = pos.up();
            boolean top = !w.isBranch(above);
            if (top) {
                // 定干分叉未完成（主枝数未达 limbCount 且还有能落的空位）→ 还需 tick
                if (Integer.bitCount(s.forkMask) < w.species.limbCount()) {
                    for (Direction d : HORIZONTALS) {
                        if (w.air(pos.offset(d)) && !isCrowded(w, pos.offset(d))) {
                            return false;
                        }
                    }
                }
            }
            return treeTopped(w, pos);
        }
        // 侧枝没有「结构长满即静默」出口（同步引擎）：终局是开花写入的 dormant，
        // 到 cap 未开花的节留在循环里走分叉窗口，由终止逻辑开花收场
        return false;
    }

    // ===== 分叉槽位（复刻 MoranBranchBlock.perpendiculars/slotOf，与 blockstate 生成器一致） =====

    private static Direction[] perpendiculars(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        }
        return new Direction[]{facing.rotateYClockwise(), facing.rotateYCounterclockwise(),
                Direction.UP, Direction.DOWN};
    }

    private static int slotOf(Direction facing, Direction side) {
        Direction[] slots = perpendiculars(facing);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == side) {
                return i;
            }
        }
        return -1;
    }

    // ============================================================
    //  完美基因自动搜索：扫大批量种子，多维度 Top-K 合并成候选池，
    //  批量渲染 + 打印每棵的激素档案，交人工眼挑定稿。
    // ============================================================

    /**
     * 候选池筛选：每个维度各取 Top-K，合并去重 —— 覆盖「最茂密/最高/最宽/最对称/分叉最多」等各型好树，
     * 避免单一指标筛出的候选高度雷同，给眼挑留足多样性。
     *
     * <p>维度向「中国风盛花桃树」目标图校准（2026-09-15）：伞形开张 = 冠幅宽于树高、
     * 末梢深分叉、花簇挂外围。故废维「苞」（终态恒 0，排序退化随机）替换为
     * <b>横展半径</b>（冠层到树基轴的最大水平距离，量全方位外扩含对角）与
     * <b>宽高比</b>（伞形核心，矮桩不成型不计分）。
     */
    private static void runBest(TreeSpecies species, int count, int envScore, String outBase) throws Exception {
        Path dir = Path.of(outBase, "best");
        Files.createDirectories(dir);

        List<SimWorld> all = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            all.add(growTree(species, 1000L + i, envScore));
        }

        int n = all.size();
        double[] dLeaves = new double[n], dBranch = new double[n], dFork = new double[n],
                dHeight = new double[n], dWidth = new double[n],
                dLush = new double[n], dSym = new double[n],
                dSpread = new double[n], dRatio = new double[n];
        for (int i = 0; i < n; i++) {
            SimWorld w = all.get(i);
            SimStats s = stats(w);
            double sp = spread(w);
            dLeaves[i] = s.leaves; dBranch[i] = s.branches; dFork[i] = s.forks;
            dHeight[i] = s.height; dWidth[i] = s.width;
            dLush[i] = s.branches + s.leaves + s.buds;
            dSym[i] = symmetry(w);
            dSpread[i] = sp;
            dRatio[i] = s.height >= 5 ? sp / s.height : 0;
        }

        int topK = 3;
        LinkedHashSet<Long> candSeeds = new LinkedHashSet<>();
        topN(dLeaves, topK, candSeeds, all);
        topN(dBranch, topK, candSeeds, all);
        topN(dFork, topK, candSeeds, all);
        topN(dHeight, topK, candSeeds, all);
        topN(dWidth, topK, candSeeds, all);
        topN(dSpread, topK, candSeeds, all);
        topN(dRatio, topK, candSeeds, all);
        topN(dLush, topK, candSeeds, all);
        topN(dSym, topK, candSeeds, all);

        Map<Long, SimWorld> bySeed = new HashMap<>();
        for (SimWorld w : all) bySeed.put(w.seed, w);

        List<SimWorld> cands = new ArrayList<>();
        for (Long sd : candSeeds) cands.add(bySeed.get(sd));

        // 单棵大图（细节）
        for (SimWorld w : cands) {
            HormoneProfile h = w.hormones;
            String label = String.format(Locale.ROOT,
                    "seed=%d %s aux=%.2f cyt=%.2f gib=%.2f tgt=%d",
                    w.seed, w.phenotype.id(), h.auxin(), h.cytokinin(), h.gibberellin(), w.max);
            TreeRenderer.renderTreePng(w, dir.resolve("tree_" + w.seed + ".png"), label);
        }
        // 总览拼图（一眼扫候选）
        List<String> labels = new ArrayList<>();
        for (SimWorld w : cands) labels.add(String.format(Locale.ROOT, "#%d %s", w.seed, w.phenotype.id()));
        TreeRenderer.renderSheetPng(cands, labels, 6, dir.resolve("sheet.png"),
                "moran perfect-gene candidates  (scanned " + count + ", pool " + cands.size() + ")");

        // 控制台：候选档案表
        System.out.println("===== 完美基因候选池（扫描 " + count + " 棵，各维度 Top" + topK
                + " 合并，共 " + cands.size() + " 棵）=====");
        for (SimWorld w : cands) {
            SimStats s = stats(w);
            HormoneProfile h = w.hormones;
            System.out.printf(Locale.ROOT,
                    "#%d %-5s aux=%.3f cyt=%.3f gib=%.3f tgt=%d | 高%d 宽%d 展%.1f 比%.2f 枝%d 叶%d 叉%d 对称%.2f%n",
                    w.seed, w.phenotype.id(), h.auxin(), h.cytokinin(), h.gibberellin(), w.max,
                    s.height, s.width, spread(w), spread(w) / Math.max(1, s.height),
                    s.branches, s.leaves, s.forks, symmetry(w));
        }
        System.out.println("粘进 PeachSpecies.peach()（取你挑中的一棵，作为基准原型；残差取整到两位小数）：");
        for (SimWorld w : cands) {
            HormoneProfile h = w.hormones;
            System.out.printf(Locale.ROOT, "  .phenotype(\"golden\", 1F, %.2fF, %.2fF, %.2fF)  // #%d %s%n",
                    h.auxin(), h.cytokinin(), h.gibberellin(), w.seed, w.phenotype.id());
        }
        System.out.println("总览图: " + dir.resolve("sheet.png").toAbsolutePath());

        // 纯 ASCII 候选档案（避开终端中文编码乱码，供画廊脚本稳健解析）
        StringBuilder tsv = new StringBuilder("seed\tpheno\taux\tcyt\tgib\ttgt\th\tw\tsp\tratio\tb\tl\tf\tsym\n");
        for (SimWorld w : cands) {
            SimStats s = stats(w);
            HormoneProfile h = w.hormones;
            tsv.append(String.format(Locale.ROOT, "%d\t%s\t%.3f\t%.3f\t%.3f\t%d\t%d\t%d\t%.2f\t%.2f\t%d\t%d\t%d\t%.2f\n",
                    w.seed, w.phenotype.id(), h.auxin(), h.cytokinin(), h.gibberellin(),
                    w.max, s.height, s.width, spread(w), spread(w) / Math.max(1, s.height),
                    s.branches, s.leaves, s.forks, symmetry(w)));
        }
        Files.writeString(dir.resolve("candidates.tsv"), tsv.toString());
        System.out.println("候选档案(TSV): " + dir.resolve("candidates.tsv").toAbsolutePath());

        // 3D 体素查看器：眼挑应在三维里转着看（与 --seeds 路径同一条 writeWeb 出口）
        writeWeb(dir, cands);
        System.out.println("3D 查看器: " + dir.resolve("trees.html").toAbsolutePath()
                + "（浏览器直接打开，左键旋转 / 滚轮缩放 / 右键平移）");
    }

    /** 树冠重心偏离主干轴(0,0)的程度：越居中越对称（1/(1+偏移)） */
    private static double symmetry(SimWorld w) {
        int sx = 0, sz = 0, n = 0;
        for (Map.Entry<BlockPos, SimBlock> e : w.blocks.entrySet()) {
            if (e.getValue().trunk) continue;
            BlockPos p = e.getKey();
            sx += p.getX(); sz += p.getZ(); n++;
        }
        if (n == 0) return 0;
        return 1.0 / (1.0 + Math.hypot((double) sx / n, (double) sz / n));
    }

    /**
     * 冠层横展半径：非主干方块到树基竖轴的最大水平距离。
     * 与 stats.width（单轴跨度）互补——伞形树四向斜伸时对角距离更大，此值才量得全。
     */
    private static double spread(SimWorld w) {
        double r = 0;
        for (Map.Entry<BlockPos, SimBlock> e : w.blocks.entrySet()) {
            if (e.getValue().trunk) continue;
            BlockPos p = e.getKey();
            r = Math.max(r, Math.hypot(p.getX(), p.getZ()));
        }
        return r;
    }

    /** 按分数降序取 Top-K 的 seed 并入候选集（LinkedHashSet 保序去重） */
    private static void topN(double[] score, int k, LinkedHashSet<Long> out, List<SimWorld> all) {
        Integer[] idx = new Integer[score.length];
        for (int i = 0; i < score.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(score[b], score[a]));
        for (int i = 0; i < Math.min(k, idx.length); i++) out.add(all.get(idx[i]).seed);
    }
}
