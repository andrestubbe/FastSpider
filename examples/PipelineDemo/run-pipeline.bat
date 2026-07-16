@echo off
chcp 65001 > nul
echo ====================================================================
echo [PIPELINE] FastJava Pipeline Orchestrator - Building Dependencies
echo ====================================================================

echo [1/3] Packaging FastSpider native library (Quiet)...
cd ..\..
    echo [ERROR] Failed to build FastSpider.
    pause 
    exit /b 
)
cd examples\PipelineDemo

echo [2/3] Packaging FastScrape native library (Quiet)...
cd ..\..\..\FastScrape
    echo [ERROR] Failed to build FastScrape.
    pause 
    exit /b 
)
cd ..\FastSpider\examples\PipelineDemo

echo [3/3] Running Combined Pipeline Demo...
call mvn compile -q
if %ERRORLEVEL% NEQ 0 ( 
    echo [ERROR] Failed to compile PipelineDemo.
    pause
    exit /b
)
java -cp "target\classes;..\..\target\fastspider-0.1.0.jar;..\..\..\FastScrape\target\fastscrape-0.1.0.jar" --enable-native-access=ALL-UNNAMED fastpipeline.PipelineDemo
if %ERRORLEVEL% NEQ 0 ( 
    echo [ERROR] Failed to execute PipelineDemo.
)
pause
