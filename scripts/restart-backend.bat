@echo off
setlocal
chcp 65001 >nul
title Quiet Fleet Collection - Restart Backend
cd /d "%~dp0.."

echo Restarting Quiet Fleet Collection backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0restart-backend.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%EXIT_CODE%"=="0" (
  echo Backend restart failed. Exit code: %EXIT_CODE%
) else (
  echo Backend restart command finished.
)
echo Press any key to close this window.
pause >nul
exit /b %EXIT_CODE%
