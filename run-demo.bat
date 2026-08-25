@echo off
setlocal
cd /d "%~dp0"
chcp 65001 > nul
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dorg.slf4j.simpleLogger.defaultLogLevel=error"

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q -Dorg.slf4j.simpleLogger.defaultLogLevel=error test-compile 2>nul
if %ERRORLEVEL% NEQ 0 (
    call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q test-compile
    pause
    exit /b %ERRORLEVEL%
)

call "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" -q exec:java "-Dexec.mainClass=fastspider.Demo" "-Dexec.args=" "-Dexec.vmArgs=--enable-native-access=ALL-UNNAMED" 2>nul
pause
