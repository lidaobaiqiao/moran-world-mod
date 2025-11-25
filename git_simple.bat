@echo off
cd "E:\test_mod-template-1.20.4"
echo Adding files...
git add -A
echo Creating commit...
git commit -m "Fixed version v1.0.0 - Complete implementation

- Fixed all dependency version issues
- Updated outdated API calls  
- Fixed block and item state definitions
- Improved dimension and biome configurations
- Resolved compilation and runtime errors

Features:
- Complete peach blossom dimension system
- 30+ custom blocks
- 7+ custom items
- Custom trees and ore generation
- Matcha themed game mechanics

Status: Fully functional and ready for release"
echo Pushing to GitHub...
git push origin master
echo Done!
pause