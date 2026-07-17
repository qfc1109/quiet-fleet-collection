@echo off
setlocal
chcp 65001 >nul
title Quiet Fleet Collection - Setup Dev
cd /d "%~dp0.."

echo Setting up Quiet Fleet Collection development environment...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-dev.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%EXIT_CODE%"=="0" (
  echo Development environment setup failed. Exit code: %EXIT_CODE%
) else (
  echo Development environment setup completed successfully.
)
echo Press any key to close this window.
pause >nul
exit /b %EXIT_CODE%
