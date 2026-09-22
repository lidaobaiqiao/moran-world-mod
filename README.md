# 墨世界（Moran Mod）

Minecraft 1.20.1 Fabric 模组，围绕《桃花源记》制作桃花源维度、五种树木的生长系统、墨灵实体和竹筏传送。

## 项目结构与运行链路

- `fabric.mod.json` 声明通用入口 `MoranMod`、客户端入口 `MoranModClient` 和数据生成入口。
- `MoranMod` 依次初始化配置、方块、维度键、物品、实体、创造栏、命令及事件。客户端入口负责树枝模型、实体渲染、草色和落花粒子。
- 桃花源维度实际由 `src/main/resources/data/moran_mod/dimension/peach_blossom_dimension.json` 定义；它引用同目录的群系及 `worldgen/noise_settings/peach_blossom.json`。主世界不注入桃花源群系。
- `systems/trees` 管理五种树的基因、环境与生长；`systems/blocks/MoranBranchBlock` 和 `MoranFlowerBudBlock` 承接方块行为。内置基因在 `assets/moran_mod/genes`，首次运行时复制到 `config/moran_mod/genes`，之后优先读取可编辑配置。
- `MoranModDataGenerator` 为普通方块生成“掉落自身”的表到 `src/main/generated`；特殊掉落保存在 `src/main/resources/data/moran_mod/loot_tables/blocks`，两处不得有同路径文件。
- `systems/test/AiTestBridge` 只在 Fabric 开发环境启用；它会以服务器控制台权限执行 `ai_test/cmd.txt` 中的命令，不属于正式服功能。

## 开发与验证

使用 JDK 17、Gradle Wrapper 8.6。PowerShell 在项目根目录运行：

```powershell
.\gradlew.bat runDatagen
.\gradlew.bat build
.\gradlew.bat runClient
```

打包产物是 `build/libs/moran-mod-1.0.0.jar`。运行日志在 `run/logs/latest.log`。`simTrees` 可离线检查五种树形；`runServer` 可检查维度和数据包加载。详见 `COMMANDS.md`。

## 外部工具与依赖职责

- **Gradle Wrapper 8.6**：统一执行构建、运行、数据生成和辅助验证；不要直接依赖本机 Gradle 版本。
- **Fabric Loom**：把 Minecraft、Yarn 映射和 Fabric 开发环境接入 Gradle，并提供 `runClient`、`runServer`、`runDatagen`、jar 重映射等任务。
- **Fabric Loader / Fabric API**：Loader 负责入口点和模组生命周期；Fabric API 提供注册表、事件、维度传送、实体、客户端模型和数据生成接口。
- **Minecraft 1.20.1 + Yarn mappings**：游戏运行时和 Java 编译时的目标版本；JDK 固定为 17。
- **Cloth Config**：负责 `config/moran_mod.json` 的序列化和配置界面。目前部分配置字段还没有接入玩法逻辑。
- **`simTrees`**：离线树形模拟器，不进入游戏、不修改存档，用来批量检查基因参数和树冠形状。
- **`dumpBranchModels`**：开发期模型导出器，用来把动态树枝模型导出后和手作模型逐字段比对。
- **`AiTestBridge`**：仅开发环境注册的测试桥，从 `ai_test/cmd.txt` 读取命令并以服务器控制台权限执行；正式服不会启用。
- **GitHub Actions**：CI 中先跑数据生成并检查生成目录无脏改动，再执行完整构建。
- **VPN / Git**：只影响远端拉取和推送，不参与模组运行；`libs/` 中的旧 jar 也不在当前 Gradle 依赖链中。

运行时依赖 Fabric API 和 Cloth Config。TerraBlender、Architectury 与 NightConfig 不再是模组运行前置；`libs/` 中旧的本地 jar 保留作历史资料，构建不引用。注册 ID 的命名空间是 `moran_mod`，Fabric 模组 ID 则是 `moran-mod`，不要把两者混用。

## 当前边界

- 桃花源维度现在使用 `data/moran_mod/dimension_type/peach_blossom_type.json`；维度生成仍由 `dimension/peach_blossom_dimension.json` 与 `worldgen/noise_settings/peach_blossom.json` 驱动。
- `ModConfig` 中部分维度和地形选项尚未接入玩法逻辑。修改这些字段不等于改变世界生成。
- 仓库暂时没有 `src/test` 单元测试；构建、数据生成、树形模拟和游戏内场景验证是现有检查方式。
