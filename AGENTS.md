# MoRan Mod - 代理工作指南

## 🎯 项目概述

**墨世界模组 (MoRan Mod)** 是一个基于 Fabric 的 Minecraft 1.20.4 模组，专注于实现一个以《桃花源记》为主题的多维度穿越系统。该项目采用模块化架构，包含多个维度的生物群系、地形生成、物品系统等。

### 关键信息
- **项目名称**: 墨世界模组 (MoRan Mod)
- **游戏版本**: Minecraft 1.20.1
- **模组加载器**: Fabric 0.15.11+
- **Java版本**: 17
- **构建工具**: Gradle 8+
- **作者**: Lidaobaiqiao!

## 📂 项目结构

### 根目录
```
├── build.gradle              # 主要构建配置
├── gradle.properties          # Gradle属性和项目常量
├── settings.gradle            # Gradle设置
├── LICENSE                    # CC0-1.0许可证
├── fabric.mod.json           # Fabric模组元数据
├── libs/                     # 本地依赖库
│   ├── TerraBlender-*.jar
│   ├── nightconfig-core-*.jar
│   └── nightconfig-toml-*.jar
├── src/                      # 源代码目录
│   ├── main/                 # 主要代码和资源
│   └── client/               # 客户端专用代码
└── 妖灼华原资源/               # 纹理资源文件
```

### 源代码组织
```
src/main/java/com/lidao/moran/
├── MoranMod.java             # 主模组类
├── core/                     # 核心系统
│   ├── DependencyManager.java
│   ├── config/              # 配置管理
│   ├── event/               # 事件处理
│   ├── terrablender/        # TerraBlender集成
│   └── resources/           # 资源管理
├── dimensions/              # 维度系统
│   ├── DimensionRegistry.java
│   ├── DimensionConfig.java
│   ├── base/                # 基础维度类
│   └── peach_blossom/       # 桃花源维度
├── systems/                 # 功能系统
│   ├── items/              # 物品系统
│   ├── blocks/             # 方块系统
│   ├── commands/           # 命令系统
│   ├── respawn/            # 重生系统
│   ├── worldgen/           # 世界生成
│   └── teleport/           # 传送系统
├── client/                  # 客户端代码
│   ├── MoranModClient.java
│   └── render/             # 渲染相关
├── mixin/                   # Mixin类
├── item/                    # 物品实现
└── tags/                    # 标签定义
```

## 🛠️ 开发环境设置

### 构建命令
```bash
# 构建项目
./gradlew build

# 清理构建
./gradlew clean

# 生成源代码JAR
./gradlew build sourcesJar

# 构建并发布到本地仓库
./gradlew build publishToMavenLocal

# 运行游戏进行测试
./gradlew runClient
```

### 调试配置
- **开发环境**: 使用 `./gradlew runClient` 启动测试
- **调试日志**: 通过 `ConfigManager.isDebugLoggingEnabled()` 控制
- **依赖检查**: `DependencyManager.checkDependencies()` 检查前置模组

## 🔧 前置模组依赖

### 必需依赖
1. **TerraBlender** (`terrablender`) - 生物群系管理
   - 版本要求: >=3.0.0
   - 本地文件: `libs/terrablender-fabric-1.20.1-3.0.1.10.jar`
   - 作用: 管理192个生物群系的注册和分布

2. **Cloth Config** (`cloth-config`) - 配置界面
   - 版本要求: >=11.1.118
   - Maven坐标: `me.shedaniel.cloth:cloth-config-fabric:11.1.118`
   - 作用: 提供用户友好的配置界面

3. **Architectury** (`architectury`) - 跨平台API
   - 版本要求: >=9.2.14
   - Maven坐标: `dev.architectury:architectury-fabric:9.2.14`
   - 作用: 提供跨平台兼容性

### 可选依赖
1. **Geckolib** (`geckolib`) - 高级地形控制
   - 版本要求: >=4.4.2
   - 作用: 复杂地形生成和动画系统

### 本地库处理
- NightConfig库文件必须包含在`libs/`目录中
- 必须使用`include`配置确保打包到最终JAR中
- 文件名必须与build.gradle中完全一致

## 📋 代码约定与模式

### 命名规范
- **包命名**: `com.lidao.moran.{system}.{subpackage}`
- **类命名**: PascalCase (例如: `PeachBlossomDimension`)
- **方法命名**: camelCase (例如: `registerDimension`)
- **常量命名**: UPPER_SNAKE_CASE (例如: `MOD_ID`)
- **资源ID**: 小写下划线 (例如: `peach_blossom`)

### 注册模式
使用静态注册模式，参考`ItemSystem`：
```java
public static final Item EXAMPLE_ITEM = register("example", new Item(new FabricItemSettings()));

private static Item register(String id, Item item) {
    ITEMS.put(id, item);
    return Registry.register(Registries.ITEM, new Identifier(MOD_ID, id), item);
}
```

### 日志规范
- 使用`MoranMod.LOGGER`进行日志记录
- 支持中文日志和emoji表情
- 错误日志包含上下文信息
- 调试日志可配置开关

### 依赖管理模式
所有前置模组检查通过`DependencyManager`进行：
```java
if (DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
    // 功能实现
} else {
    MoranMod.LOGGER.warn("⚠️ TerraBlender 未加载，相关功能不可用");
}
```

## 🌐 维度系统架构

### 当前维度状态
- ✅ **桃花源维度** (peach_blossom) - 已完成，6个诗意生物群系
- 🔄 **云端仙境** (sky_realm) - 下一开发目标
- ⏸️ **其他6个维度** - 规划中

### 生物群系设计模式
每个维度包含24个生物群系，使用以下设计原则：
- 诗意名称（基于中国古典文学）
- 多维参数系统（温度、湿度、大陆性等）
- 独特的生成特征
- 渐变过渡效果

### 数据驱动系统
使用Minecraft 1.20.4的数据驱动格式：
- 生物群系定义：JSON格式，位于`src/main/resources/data/moran-mod/worldgen/biome/`
- 地形配置：噪声设置、密度函数
- 特征生成：JSON配置，支持自定义特征

## 🎨 资源管理系统

### 纹理规范
- **方块纹理**: 16x16或64x64像素
- **物品纹理**: 推荐64x64像素（已配置显示缩放）
- **格式**: PNG，透明背景（除不透明方块外）
- **风格**: 保持统一艺术风格，桃花源以粉色、绿色、白色为主色调

### 模型文件
- 位于`src/main/resources/assets/moran-mod/models/`
- 支持方块状态变化
- 自定义光照和渲染设置

### 语言文件
- 支持中文诗意命名
- 位于`src/main/resources/assets/moran-mod/lang/`
- 支持多语言扩展

## 🚀 传送系统

### 竹筏传送机制
- 实现5秒传送延迟
- 基于玩家站立位置检测
- 配置开关控制
- 事件驱动的传送逻辑

### 传送代码位置
- 主要逻辑: `systems/teleport/RaftTeleportHandler.java`
- 事件监听: `MoranMod.java`中的`initializeTeleportSystem`方法

## ⚙️ 配置系统

### 配置层次结构
```java
ModConfig
├── dimensions      # 维度开关
├── biomes         # 生物群系配置
├── terrain        # 地形设置
├── teleport       # 传送设置
└── debug          # 调试选项
```

### 配置管理
- 使用`ConfigManager`进行配置访问
- 支持热重载和保存
- 依赖检查确保配置有效性
- 默认值处理机制

## 🧪 测试指南

### 单元测试
- 当前项目暂无正式单元测试
- 建议测试所有新增的注册项
- 测试维度传送逻辑

### 集成测试
- 使用`./gradlew runClient`进行游戏内测试
- 验证依赖加载状态
- 检查配置系统功能
- 测试维度生成和传送

### 常见测试场景
1. 启动游戏并检查日志，确保所有依赖正确加载
2. 进入桃花源维度，验证生物群系生成
3. 测试竹筏传送功能
4. 验证配置界面（如果Cloth Config可用）
5. 检查新增方块和物品的注册

## 🔄 构建与部署

### 构建流程
1. 使用`./gradlew build`构建完整项目
2. 输出文件位于`build/libs/`
3. 包含源代码和资源文件的完整JAR
4. 自动打包本地依赖库

### 发布配置
- CI/CD使用GitHub Actions
- 自动构建和测试
- 支持多平台构建
- 构件自动上传

## ⚠️ 常见问题与解决方案

### 依赖问题
- **问题**: Terrablender加载失败
- **解决**: 检查`libs/`目录中的文件名是否与build.gradle一致

- **问题**: NightConfig相关错误
- **解决**: 确保使用`include`配置打包本地JAR

### 维度问题
- **问题**: 维度注册失败
- **解决**: 检查`fabric.mod.json`中的依赖声明和版本要求

- **问题**: 生物群系不生成
- **解决**: 确认Terrablender正确加载并检查JSON配置语法

### 资源问题
- **问题**: 纹理显示为紫色/黑色方块
- **解决**: 检查文件名和路径，确保PNG格式正确

## 📈 性能考虑

### 当前优化
- 使用静态注册减少运行时开销
- 分模块加载避免不必要的初始化
- 依赖检查防止功能缺失

### 未来优化方向
- 实现LOD系统优化大规模渲染
- 添加缓存机制减少重复计算
- 优化生物群系过渡算法

## 🎯 开发优先级

### 当前状态
- ✅ 基础架构和依赖管理
- ✅ 桃花源维度和生物群系
- 🔄 云端仙境维度开发
- ⏸️ 其他6个维度实现

### 下一步行动
1. 完成云端仙境维度的生物群系设计
2. 实现更复杂的地形生成算法
3. 添加结构生成（村庄、神龛等）
4. 完善粒子效果和音效
5. 实现完整的配置界面

## 📚 重要文档

- `维度革新完成报告.md` - 桃花源维度实现细节
- `前置模组集成状态报告.md` - 依赖集成状态
- `桃花源维度革新-资源需求.md` - 纹理资源需求

---

**最后更新**: 2025-11-25  
**项目状态**: 基础架构完成，桃花源维度已实现，准备云端仙境开发  
**维护者**: Crush AI Assistant