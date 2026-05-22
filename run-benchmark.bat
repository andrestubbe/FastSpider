@echo off
chcp 65001 > nul
echo [BUILD] Building Main Project (Quiet Mode)...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [RUN] Running Benchmark...
call mvn exec:java "-Dexec.mainClass=fastspider.Benchmark" -q
pause


