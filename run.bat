@echo off
cd /d "%~dp0"
if not exist target\aschensiegel-1.0.0.jar call mvn -Dmaven.repo.local=.cache/maven package
if errorlevel 1 exit /b 1
java --enable-native-access=ALL-UNNAMED -Xms256m -Xmx2g -jar target\aschensiegel-1.0.0.jar %*
