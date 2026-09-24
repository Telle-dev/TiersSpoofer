@echo off
setlocal
title TierSpoofer Build
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
set "ERR=%ERRORLEVEL%"

echo.
if not "%ERR%"=="0" (
    echo Build failed - see the messages above.
) else (
    echo Finished. The jar is in the "output" folder.
)
pause
exit /b %ERR%
