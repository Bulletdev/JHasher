@echo off
setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

for %%f in ("%PROJECT_DIR%\build\libs\jhasher-*-all.jar") do set JAR=%%f

if not defined JAR (
    echo JHasher JAR not found. Run: gradlew shadowJar 1>&2
    exit /b 1
)

java -jar "%JAR%" %*
