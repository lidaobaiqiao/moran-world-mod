@echo off
cd "E:\test_mod-template-1.20.4"
echo Final Git Status:
git log --oneline -3
echo.
echo Remote Status:
git remote -v
echo.
echo Done!
pause