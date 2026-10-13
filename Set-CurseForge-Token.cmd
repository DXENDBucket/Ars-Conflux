@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\set-curseforge-token.ps1"
set "setup_result=%ERRORLEVEL%"
echo.
pause
exit /b %setup_result%
