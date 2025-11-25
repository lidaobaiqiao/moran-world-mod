@echo off
echo ========================================
echo 墨世界模组 - 技术迁移开始
echo ========================================
echo.

echo 1. 检查目标模组目录...
if not exist "E:\mozu" (
    echo [ERROR] 主模组目录不存在: E:\mozu
    echo 请先创建主模组目录
    pause
    exit /b 1
)

echo [OK] 找到主模组目录: E:\mozu

echo.
echo 2. 创建核心目录结构...
if not exist "E:\mozu\src\main\java\com\lidao\moran" mkdir "E:\mozu\src\main\java\com\lidao\moran"
if not exist "E:\mozu\src\main\resources\data\moran-mod\worldgen" mkdir "E:\mozu\src\main\resources\data\moran-mod\worldgen"
if not exist "E:\mozu\src\main\resources\data\moran-mod\dimension" mkdir "E:\mozu\src\main\resources\data\moran-mod\dimension"

echo [OK] 核心目录结构已创建

echo.
echo 3. 开始迁移核心文件...
echo 迁移主类...
if exist "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\MoranMod.java" (
    copy "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\MoranMod.java" "E:\mozu\src\main\java\com\lidao\moran\MoranMod.java"
    echo [OK] MoranMod.java 已迁移
) else (
    echo [WARNING] MoranMod.java 未找到
)

echo 迁移TerraBlender集成...
if exist "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\core\terrablender\TerrablenderIntegration.java" (
    copy "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\core\terrablender\TerrablenderIntegration.java" "E:\mozu\src\main\java\com\lidao\moran\core\terrablender\TerrablenderIntegration.java"
    echo [OK] TerrablenderIntegration.java 已迁移
) else (
    echo [WARNING] TerrablenderIntegration.java 未找到
)

echo 迁移维度注册器...
if exist "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\core\terrablender\TerraBlenderRegistry.java" (
    copy "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\core\terrablender\TerraBlenderRegistry.java" "E:\mozu\src\main\java\com\lidao\moran\core\terrablender\TerraBlenderRegistry.java"
    echo [OK] TerraBlenderRegistry.java 已迁移
) else (
    echo [WARNING] TerraBlenderRegistry.java 未找到
)

echo.
echo 4. 迁移维度系统...
xcopy "E:\test_mod-template-1.20.4\src\main\java\com\lidao\moran\dimensions\peach_blossom\*" "E:\mozu\src\main\java\com\lidao\moran\dimensions\peach_blossom\" /E /I /Y
echo [OK] 维度系统文件已迁移

echo.
echo 5. 迁移配置文件...
xcopy "E:\test_mod-template-1.20.4\src\main\resources\terrablender.json" "E:\mozu\src\main\resources\" /I /Y
echo [OK] terrablender.json 已迁移

xcopy "E:\test_mod-template-1.20.4\src\main\resources\data\moran-mod\worldgen\biome\*" "E:\mozu\src\main\resources\data\moran-mod\worldgen\biome\" /E /I /Y
echo [OK] 生物群系配置已迁移

xcopy "E:\test_mod-template-1.20.4\src\main\resources\data\moran-mod\dimension\*" "E:\mozu\src\main\resources\data\moran-mod\dimension\" /E /I /Y
echo [OK] 维度配置已迁移

echo.
echo ========================================
echo 迁移完成！
echo ========================================
echo.
echo 下一步:
echo 1. 检查 E:\mozu 目录下的文件
echo 2. 更新 build.gradle 依赖
echo 3. 更新 fabric.mod.json 元数据
echo 4. 运行 gradlew build 测试编译
echo 5. 运行 gradlew runClient 测试游戏
echo.
echo 技术迁移成功！桃花园维度系统已移至主模组！
echo.
pause