@echo off
setlocal
if not defined INST_MC_DIR set "INST_MC_DIR=%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%INST_MC_DIR%\quantumcore-prism-bootstrap.ps1"
set "BUILD_EXIT=%ERRORLEVEL%"
if not "%BUILD_EXIT%"=="0" (
  echo.
  echo QUANTUM UNKNOWN could not build automatically. Check the Prism console output and .minecraft\quantumcore-source\build\reports\tests\test\index.html if it exists.
  exit /b %BUILD_EXIT%
)
exit /b 0
