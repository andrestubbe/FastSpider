@echo off
chcp 65001 > nul
    echo.
    echo [ERROR] Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [RUN] Running Benchmark...
call mvn exec:java "-Dexec.mainClass=fastspider.Benchmark" -q
pause


