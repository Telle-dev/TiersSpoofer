@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
if errorlevel 1 (echo Build failed.) else (echo Done, the jar is in the output folder.)
pause
