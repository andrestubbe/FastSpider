@echo off
echo âš¡ Building Main Project...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 ( pause & exit /b )
echo ðŸš€ Running Hero Demo...
cd examples
call mvn compile exec:java -Dexec.mainClass=fastspider.Demo
cd ..
pause

