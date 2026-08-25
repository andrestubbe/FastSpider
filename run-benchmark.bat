@echo off
setlocal
cd /d "%~dp0"
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dorg.slf4j.simpleLogger.defaultLogLevel=warn"
echo ===================================================
echo  Building FastSpider ^& Running Benchmark Suite
echo ===================================================

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -Dorg.slf4j.simpleLogger.defaultLogLevel=warn test-compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [FastSpider] Running Link Extraction & Network Benchmark...
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q exec:java "-Dexec.mainClass=fastspider.Benchmark" "-Dexec.args=" "-Dexec.vmArgs=--enable-native-access=ALL-UNNAMED"
pause
