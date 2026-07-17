@echo off
setlocal
chcp 65001 >nul
title Quiet Fleet Collection - Stop Dev
cd /d "%~dp0.."

echo Stopping Quiet Fleet Collection development services...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-dev.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%EXIT_CODE%"=="0" (
  echo Development services failed to stop cleanly. Exit code: %EXIT_CODE%
) else (
  echo Development services stopped successfully.
)
echo Press any key to close this window.
pause >nul
exit /b %EXIT_CODE%
