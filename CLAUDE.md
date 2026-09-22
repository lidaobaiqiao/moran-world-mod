# 项目开发备忘

## 网络 / 构建环境约定（重要）

- 这台电脑**不会一直开启 VPN**，所有下载必须优先使用国内镜像：
  - Gradle 发行版：`https://mirrors.cloud.tencent.com/gradle/`（腾讯镜像，gradle-wrapper.properties 已配置）
  - Maven 中央仓库：`https://maven.aliyun.com/repository/public`（阿里云，build.gradle repositories 里已排在最前）
  - Gradle 插件：`https://maven.aliyun.com/repository/gradle-plugin`（settings.gradle pluginManagement 已配置）
  - Fabric 专用仓库 `maven.fabricmc.net` 保留原地址
- 本机 JDK：建议使用 `C:\Users\15537\.jdks\temurin-17`。项目的 `gradle.properties` 没有固定 `org.gradle.java.home`，运行 Wrapper 的 Java 仍需是 17。
- `.vscode/settings.json` 已把 VS Code 的 Gradle 同步和 Java 语言服务器指到 temurin-17
- Gradle 8.14 与 fabric-loom 1.6 不兼容，必须用 Gradle 8.6（wrapper 已锁 8.6）

## 项目概况

- Minecraft 1.20.1 Fabric 模组「墨世界」（moran-mod），主题为《桃花源记》
- 核心内容：由数据包 JSON 定义的桃花源维度、妖灼华原群系、五树生长系统、竹筏静止 5 秒传送等
- 详见对话记录或源码 `src/main/java/com/lidao/moran/`

## 内容生产约定

- **datagen**：`gradlew runDatagen` 生成资源到 `src/main/generated`（已配置 loom.runs.datagen，入口 `MoranModDataGenerator`）。
  目前为普通方块生成掉落自身的战利品表。新方块加进 BlockSystem 后跑一次即可补表；特殊掉落需加入生成器的 `HAND_WRITTEN_LOOT` 集合并手写表。
  生成文件与 `.cache` 一起提交进 git。手工资源与生成资源禁止同路径重复。
- **语言文件**：位于 `assets/moran_mod/lang/zh_cn.json` 和 `en_us.json`（2026-09-09 修正：之前放在 assets 根目录导致翻译从未生效，勿移回）。
- 手写 JSON 与 datagen 的分工：现有手写资源保持手写；**新增内容优先走 datagen**，避免 147 个群系时代的维护灾难。

## 版本控制

- 本仓库在嵌套目录 `moran-world-mod-master/moran-world-mod-master` 内，分支 master
- 远程 origin = https://github.com/lidaobaiqiao/moran-world-mod（国内直连不通，开 VPN 后：
  `git fetch origin && git reset origin/master && git add -A && git commit -m "同步本地进度" && git push`）
- 仓库级 git 身份：lidaobaiqiao / lidaobaiqiao@users.noreply.github.com
