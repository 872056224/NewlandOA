@echo off
rem ============================================
rem  OA Management System - One-Click Start
rem  Args pass-through, e.g.:
rem    start.bat -Rebuild        force rebuild backend
rem    start.bat -ReinitDb       re-import SQL (data loss!)
rem    start.bat -SkipModels     skip ollama model pull
rem    start.bat -CheckOnly      environment check only
rem ============================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" %*
echo.
pause
