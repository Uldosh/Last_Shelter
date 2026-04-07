@echo off
setlocal

cd /d "%~dp0"
call gradlew.bat desktop:jar
if errorlevel 1 exit /b %errorlevel%

echo.
echo Built runnable JAR:
echo desktop\build\libs\LAST_SHELTER.jar
endlocal
