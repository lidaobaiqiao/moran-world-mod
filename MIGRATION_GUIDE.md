# 墨世界模组 - 技术迁移指南

## 🎯 迁移目标
将测试模组中验证成功的维度系统迁移到主模组 "E:\mozu"

## ✅ 已验证的技术方案

### 1. 依赖系统
```gradle
dependencies {
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_version}"
    modImplementation "com.github.glitchfiend:TerraBlender-fabric-${minecraft_version}:${terrablender_version}"
}
```

### 2. 核心类结构
```java
// 维度注册
public class MoranMod implements ModInitializer {
    public static final String MOD_ID = "moran-mod";
    
    @Override
    public void onInitialize() {
        // TerraBlender集成
        TerraBlenderIntegration.register();
    }
}
```

### 3. TerraBlender集成
```java
public class TerrablenderIntegration {
    public static void register() {
        // 注册区域
        Regions.register(new PeachBlossomRegion(new ResourceLocation(MoranMod.MOD_ID, "peach_blossom")));
    }
}
```

### 4. 自定义维度系统
```java
public class PeachBlossomRegion extends Region {
    @Override
    public void addBiomes(Registry<Biome> registry) {
        // 添加6个自定义生物群系
    }
}
```

## 🔄 迁移步骤

### Step 1: 依赖配置更新
- 更新 `E:\mozu/build.gradle`
- 添加 TerraBlender 3.0.1.10
- 确保版本兼容性

### Step 2: 核心代码迁移
- 复制 `MoranMod.java` 核心类
- 迁移 `TerrablenderIntegration.java`
- 转移维度注册逻辑

### Step 3: 维度系统迁移
- 复制 `dimensions/peach_blossom/` 包
- 迁移 `PeachBlossomRegion.java`
- 更新包名和类引用

### Step 4: 生物群系配置
- 复制 `worldgen/biome/` JSON文件
- 更新资源路径引用
- 确保命名一致性

### Step 5: 资源文件迁移
- 复制 `assets/moran-mod/` 维度相关资源
- 更新 `fabric.mod.json`
- 迁移语言文件

### Step 6: 构建和测试
- 执行 `gradlew build`
- 运行 `gradlew runClient`
- 验证维度正常生成

## 📋 文件迁移清单

### 核心类文件
- [ ] `src/main/java/com/.../MoranMod.java`
- [ ] `src/main/java/.../TerrablenderIntegration.java`
- [ ] `src/main/java/.../dimensions/peach_blossom/`

### 资源文件
- [ ] `src/main/resources/data/moran-mod/dimension/`
- [ ] `src/main/resources/data/moran-mod/worldgen/biome/`
- [ ] `src/main/resources/terrablender.json`

### 配置文件
- [ ] `build.gradle` (依赖更新)
- [ ] `fabric.mod.json` (元数据)
- [ ] `gradle.properties` (版本)

## 🎮 验证检查点

### 编译验证
```bash
cd E:\mozu
gradlew build
# 应该无错误编译
```

### 运行验证
```bash
gradlew runClient
# 游戏应该启动成功
# 维度应该正常注册
```

### 功能验证
- [ ] 主菜单显示 "墨世界模组"
- [ ] 创造模式找到 "墨" 标签物品
- [ ] 命令系统能生成维度
- [ ] 桃花园维度正常进入
- [ ] 生物群系正确渲染

## 🔧 注意事项

### 1. 包名一致性
确保所有文件中的包引用更新为正确的主模组包名

### 2. 资源路径
检查所有资源文件路径指向正确的模组ID

### 3. 配置同步
确保主模组配置与测试模组保持一致

### 4. 测试覆盖
迁移后进行全面测试，确保功能完整

## 🎯 预期结果

迁移完成后，主模组 "E:\mozu" 将具备：
- ✅ 完整的桃花园自定义维度
- ✅ 6个独特生物群系
- ✅ 稳定的TerraBlender集成
- ✅ 可靠的生成系统
- ✅ 完整的Fabric兼容性

---

**迁移成功后，您的主模组将拥有完整的自定义维度系统！** 🌸