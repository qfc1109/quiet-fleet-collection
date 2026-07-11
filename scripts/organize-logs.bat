@echo off
setlocal
chcp 65001 >nul
title Quiet Fleet Collection - Organize Logs
cd /d "%~dp0.."

echo Organizing Quiet Fleet Collection logs...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0organize-logs.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%EXIT_CODE%"=="0" (
  echo Log organization failed. Exit code: %EXIT_CODE%
) else (
  echo Log organization finished.
)
echo Press any key to close this window.
pause >nul
exit /b %EXIT_CODE%
