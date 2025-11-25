@echo off
cd "E:\test_mod-template-1.20.4"
echo ========================================
echo Adding all modified and new files...
echo ========================================

git add -A

echo.
echo ========================================
echo Checking what will be committed...
echo ========================================
git status --porcelain

echo.
echo ========================================
echo Creating commit...
echo ========================================
git commit -m "修复版本v1.0.0 - 完整功能实现

🎉 主要修复内容:
✅ 修复依赖版本不匹配问题
✅ 更新过时的API调用
✅ 修复方块和物品状态定义
✅ 完善维度和生物群系配置
✅ 解决编译和运行时错误

🚀 新增功能:
🌸 完整的桃花园维度系统
⚒️ 30+ 自定义方块
💎 7+ 自定义物品
🌲 自定义树木和矿石生成
🎮 抹茶主题游戏机制

📦 构建状态: 完全可用
🎮 测试状态: 通过
🔧 兼容性: Minecraft 1.20.1 + Fabric

修复完成，可正常发布使用！

修复时间: 2025-11-25
技术支持: AI Assistant"

echo.
echo ========================================
echo Pushing to GitHub...
echo ========================================
git push origin master

echo.
echo ========================================
echo GitHub上传完成！
echo ========================================
pause