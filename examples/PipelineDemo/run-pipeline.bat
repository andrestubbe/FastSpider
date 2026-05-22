@echo off
echo ====================================================================
echo ⚡ FastJava Pipeline Orchestrator — Building Dependencies ⚡
echo ====================================================================

echo [1/3] Packaging FastSpider native library...
cd ..\..
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 ( 
    echo ❌ Failed to build FastSpider.
    pause 
    exit /b 
)
cd examples\PipelineDemo

echo [2/3] Packaging FastScrape native library...
cd ..\..\..\FastScrape
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 ( 
    echo ❌ Failed to build FastScrape.
    pause 
    exit /b 
)
cd ..\FastSpider\examples\PipelineDemo

echo [3/3] Running Combined Pipeline Demo...
call mvn compile exec:java -Dexec.mainClass=fastpipeline.PipelineDemo
if %ERRORLEVEL% NEQ 0 ( 
    echo ❌ Failed to execute PipelineDemo.
)
pause
