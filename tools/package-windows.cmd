@echo off
setlocal EnableExtensions
rem ---------------------------------------------------------------------------
rem  Pentagon: Das Aschensiegel - Windows-Paket
rem
rem  Erzeugt aus target\aschensiegel-1.0.0.jar einen Ordner mit Aschensiegel.exe
rem  und eigener Java-Laufzeit (jpackage "app-image") und packt ihn als ZIP.
rem  Der Spieler braucht kein installiertes Java.
rem
rem    tools\package-windows.cmd             Ordner + ZIP  (Standard)
rem    tools\package-windows.cmd installer   zusaetzlich Aschensiegel-1.0.0.exe
rem                                          (Setup; braucht WiX Toolset 3.x im PATH)
rem
rem  Voraussetzung: JDK 17 oder neuer mit jpackage. JAVA_HOME gewinnt vor PATH.
rem  Ergebnis:      target\native\Aschensiegel\Aschensiegel.exe
rem                 target\native\Aschensiegel-1.0.0-windows-x64.zip
rem ---------------------------------------------------------------------------
cd /d "%~dp0\.."
set "VERSION=1.0.0"
set "NAME=Aschensiegel"
set "JAR=target\aschensiegel-%VERSION%.jar"

set "JP=jpackage"
if defined JAVA_HOME set "JP=%JAVA_HOME%\bin\jpackage.exe"
"%JP%" --version >nul 2>&1
if errorlevel 1 (
  echo jpackage nicht gefunden. JAVA_HOME auf ein JDK 17+ setzen, z. B.
  echo   set "JAVA_HOME=C:\Program Files\Java\jdk-23-temurin"
  exit /b 1
)

if not exist "%JAR%" (
  echo %JAR% fehlt - baue mit Maven ...
  call mvn -B -DskipTests package
  if errorlevel 1 exit /b 1
)

if exist target\package-input rmdir /s /q target\package-input
mkdir target\package-input
copy /y "%JAR%" target\package-input\ >nul
if not exist target\native mkdir target\native
if exist "target\native\%NAME%" rmdir /s /q "target\native\%NAME%"

rem Nur die Module, die die JAR wirklich braucht (jdeps --print-module-deps, siehe
rem docs\technik\RELEASE.md). Ohne --add-modules packt jpackage die komplette Java-SE-Laufzeit.
set "MODULES=java.base,java.compiler,java.desktop,java.management,java.prefs,java.sql,jdk.unsupported"

"%JP%" --type app-image ^
  --name %NAME% --app-version %VERSION% --vendor PentagonQuest ^
  --description "Pentagon: Das Aschensiegel" ^
  --input target\package-input --dest target\native ^
  --main-jar aschensiegel-%VERSION%.jar --main-class de.pentagon.core.Main ^
  --icon tools\icon\aschensiegel.ico ^
  --add-modules %MODULES% ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --java-options "-Xms256m" ^
  --java-options "-Xmx2g"
if errorlevel 1 exit /b 1

set "ZIP=target\native\%NAME%-%VERSION%-windows-x64.zip"
if exist "%ZIP%" del /q "%ZIP%"
powershell -NoProfile -Command "Compress-Archive -Path 'target\native\%NAME%' -DestinationPath '%ZIP%' -CompressionLevel Optimal"
if errorlevel 1 exit /b 1

echo.
echo Ordner: %CD%\target\native\%NAME%\%NAME%.exe
echo ZIP:    %CD%\%ZIP%

if /i not "%~1"=="installer" goto :done
where candle.exe >nul 2>&1
if errorlevel 1 (
  echo.
  echo Setup uebersprungen: WiX Toolset 3.x ^(candle.exe/light.exe^) nicht im PATH.
  echo Installieren, z. B. "winget install WiXToolset.WiXToolset", dann erneut aufrufen.
  exit /b 2
)
"%JP%" --type exe ^
  --name %NAME% --app-version %VERSION% --vendor PentagonQuest ^
  --description "Pentagon: Das Aschensiegel" ^
  --input target\package-input --dest target\native ^
  --main-jar aschensiegel-%VERSION%.jar --main-class de.pentagon.core.Main ^
  --icon tools\icon\aschensiegel.ico ^
  --add-modules %MODULES% ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --java-options "-Xms256m" ^
  --java-options "-Xmx2g" ^
  --win-shortcut --win-menu --win-menu-group PentagonQuest --win-dir-chooser --win-per-user-install
if errorlevel 1 exit /b 1
echo Setup:  %CD%\target\native\%NAME%-%VERSION%.exe

:done
endlocal
