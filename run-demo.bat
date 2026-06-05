@echo off
chcp 65001 > nul
    echo [ERROR] Build failed!
    pause 
    exit /b 
)
echo [RUN] Running Hero Demo...
call mvn exec:java -Dexec.mainClass=fastspider.Demo -q
pause
