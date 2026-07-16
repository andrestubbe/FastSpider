@echo off
    echo [ERROR] Build failed!
    pause 
    exit /b 
)
echo [FastSpider] Running Demo (via JitPack)...
call mvn exec:java -Dexec.mainClass=fastspider.Demo
pause
