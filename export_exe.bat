@echo off
setlocal

cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b %errorlevel%

if not exist dist mkdir dist

if "%LAUNCH4J_HOME%"=="" (
    echo LAUNCH4J_HOME is not set.
    echo Install Launch4j, then set LAUNCH4J_HOME to its folder.
    echo Example: set LAUNCH4J_HOME=C:\Tools\launch4j
    exit /b 1
)

if exist "%LAUNCH4J_HOME%\launch4jc.exe" (
    "%LAUNCH4J_HOME%\launch4jc.exe" launch4j.xml
) else if exist "%LAUNCH4J_HOME%\launch4j.exe" (
    "%LAUNCH4J_HOME%\launch4j.exe" launch4j.xml
) else (
    echo Launch4j executable not found in LAUNCH4J_HOME.
    exit /b 1
)

if errorlevel 1 exit /b %errorlevel%

echo.
echo Exported EXE:
echo dist\LAST_SHELTER.exe
endlocal
