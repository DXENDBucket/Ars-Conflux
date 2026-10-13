@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\publish-curseforge.ps1" -Interactive
set "publish_result=%ERRORLEVEL%"
echo.
pause
exit /b %publish_result%
