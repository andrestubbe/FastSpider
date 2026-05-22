@echo off
chcp 65001 > nul
echo Building main project...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Running Benchmark...
cd examples\Benchmark
call mvn compile exec:java
cd ..\..
pause

