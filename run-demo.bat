@echo off
setlocal
cd /d "%~dp0"
echo ===================================================
echo  Building FastSpider & Running Live Demo
echo ===================================================

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" test-compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [FastSpider] Running Demo...
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q exec:java "-Dexec.mainClass=fastspider.Demo"
pause
