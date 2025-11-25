@echo off
echo ========================================
echo 🌸 妖灼华原生物群系完整性测试
echo ========================================
echo.

cd "E:\test_mod-template-1.20.4"

echo 1. 检查编译状态...
gradlew.bat build > build_test.log 2>&1
findstr "BUILD SUCCESSFUL" build_test.log >nul
if %errorlevel% == 0 (
    echo [✅] 编译测试：通过
) else (
    echo [❌] 编译测试：失败
    echo 查看build_test.log了解详情
    goto :end
)

echo.
echo 2. 检查关键配置文件...
set missing_files=0

if not exist "src\main\resources\data\mo-mod\worldgen\biome\yaozhuohua.json" (
    echo [❌] 缺失: yaozhuohua.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: yaozhuohua.json
)

if not exist "src\main\resources\data\mo-mod\worldgen\configured_feature\peach_tree_dense.json" (
    echo [❌] 缺失: peach_tree_dense.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: peach_tree_dense.json
)

if not exist "src\main\resources\data\mo-mod\worldgen\configured_feature\flowering_peach_tree.json" (
    echo [❌] 缺失: flowering_peach_tree.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: flowering_peach_tree.json
)

if not exist "src\main\resources\data\mo-mod\worldgen\configured_feature\immortal_ore.json" (
    echo [❌] 缺失: immortal_ore.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: immortal_ore.json
)

if not exist "src\main\resources\data\mo-mod\worldgen\feature\immortal_ore_vein.json" (
    echo [❌] 缺失: immortal_ore_vein.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: immortal_ore_vein.json
)

echo.
echo 3. 检查资源文件...
if not exist "src\main\resources\assets\mo-mod\blockstates\peach_log_horizontal.json" (
    echo [❌] 缺失: peach_log_horizontal.json
    set /a missing_files=%missing_files%+1
) else (
    echo [✅] 存在: peach_log_horizontal.json
)

echo.
echo 4. 统计功能实现...
set feature_count=0
if exist "src\main\resources\data\mo-mod\worldgen\configured_feature\peach_tree_dense.json" set /a feature_count=%feature_count%+1
if exist "src\main\resources\data\mo-mod\worldgen\configured_feature\flowering_peach_tree.json" set /a feature_count=%feature_count%+1
if exist "src\main\resources\data\mo-mod\worldgen\configured_feature\peach_tree_scattered.json" set /a feature_count=%feature_count%+1
if exist "src\main\resources\data\mo-mod\worldgen\configured_feature\immortal_ore.json" set /a feature_count=%feature_count%+1

echo [📊] 已实现功能: %feature_count% 个
echo.
echo ========================================
echo 🌸 妖灼华原测试完成
echo ========================================

if %missing_files% == 0 (
    echo [🎉] 所有关键文件都存在！
    echo [📊] 功能实现: %feature_count%/4 个主要特征
    echo [✨] 妖灼华原生物群系状态: 优秀
    echo.
    echo 🎯 下一步建议:
    echo 1. 测试游戏内效果
    echo 2. 创作缺失的纹理
    echo 3. 添加生物生成配置
    echo 4. 实现结构生成系统
) else (
    echo [⚠️] 发现 %missing_files% 个缺失文件
    echo [📊] 功能实现: %feature_count%/4 个主要特征
    echo [❌] 妖灼华原生物群系状态: 需要修复
    echo.
    echo 🔧 下一步建议:
    echo 1. 修复缺失的配置文件
    echo 2. 重新运行测试
    echo 3. 检查JSON语法
    echo 4. 确保编译通过
)

echo.
echo 📊 详细测试日志已保存到: build_test.log
echo 📋 完整需求清单: 妖灼华原完整需求清单.md
echo 🎨 美术创作指南: 妖灼华原美术创作指南.md
echo.

:end
echo 🌸 妖灼华原 - 东方玄幻生物群系
echo 🎭 让玩家在神秘东方世界中找到诗意与冒险！
echo.
pause