@echo off
chcp 65001 >nul
echo ========================================
echo 🌸 测试妖熙尘模组修复效果
echo ========================================
echo.

echo 1. 检查修复后的桃树特征文件...
if exist "src\main\resources\data\moran-mod\worldgen\configured_feature\yaozhuohua_peach_trees_placed.json" (
    echo [✅] 桃树特征文件存在
) else (
    echo [❌] 桃树特征文件缺失
    goto :end
)

echo.
echo 2. 验证修复内容...
findstr /C:"feature.*peach_tree" "src\main\resources\data\moran-mod\worldgen\configured_feature\yaozhuohua_peach_trees_placed.json" >nul
if %errorlevel% == 0 (
    echo [✅] 正确引用了 moran-mod:peach_tree
) else (
    echo [❌] 引用错误，仍引用了 yaozhuohua_peach_trees
    goto :end
)

echo.
echo 3. 检查生物群系引用...
findstr /C:"yaozhuohua_peach_trees_placed" "src\main\resources\data\moran-mod\worldgen\biome\yaozhuohua.json" >nul
if %errorlevel% == 0 (
    echo [✅] 生物群系正确引用了桃树特征
) else (
    echo [❌] 生物群系未引用桃树特征
    goto :end
)

echo.
echo ========================================
echo [🎉] 核心修复验证完成！
echo ========================================
echo.
echo 📊 修复总结:
echo  ✅ 解决了循环引用问题
echo  ✅ 桃树特征引用正确
echo  ✅ 生物群系配置完整
echo.
echo 🎯 下一步: 启动游戏测试
echo ========================================

:end
echo.
pause
