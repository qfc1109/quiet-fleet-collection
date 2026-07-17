@echo off
setlocal
chcp 65001 >nul
title Quiet Fleet Collection - Restart Dev
cd /d "%~dp0.."

echo Restarting Quiet Fleet Collection dev services...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%EXIT_CODE%"=="0" (
  echo Dev services failed to restart. Exit code: %EXIT_CODE%
) else (
  echo Dev services restarted successfully.
)
echo Press any key to close this window.
pause >nul
exit /b %EXIT_CODE%
