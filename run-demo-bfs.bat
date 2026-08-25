@echo off
setlocal
cd /d "%~dp0"
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dorg.slf4j.simpleLogger.defaultLogLevel=warn"
echo =========================================================================
echo  FastSpider BFS Tree Crawler - 100+ Concurrent Language and SIMD Scan
echo =========================================================================

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -Dorg.slf4j.simpleLogger.defaultLogLevel=warn test-compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [FastSpider] Running BFS Tree Crawler...
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q exec:java "-Dexec.mainClass=fastspider.DemoBFS" "-Dexec.args=" "-Dexec.vmArgs=--enable-native-access=ALL-UNNAMED"
pause
