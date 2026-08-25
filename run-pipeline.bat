@echo off
setlocal
cd /d "%~dp0"
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dorg.slf4j.simpleLogger.defaultLogLevel=warn"
echo ====================================================================
echo  FastJava Pipeline: FastSpider (WinHTTP) + FastScrape (SIMD)
echo ====================================================================

echo [1/3] Building FastSpider...
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build FastSpider.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Building FastScrape...
cd ..\FastScrape
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build FastScrape.
    pause
    exit /b %ERRORLEVEL%
)
cd ..\FastSpider

echo [3/3] Running Pipeline Demo...
cd examples\PipelineDemo
call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q test-compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to compile PipelineDemo.
    pause
    exit /b %ERRORLEVEL%
)

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q exec:java "-Dexec.mainClass=fastpipeline.PipelineDemo" "-Dexec.args=" "-Dexec.vmArgs=--enable-native-access=ALL-UNNAMED"
pause
