@echo off
setlocal

set "BASE_DIR=%~dp0"
set "WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper"
set "PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties"
set "JAR_FILE=%WRAPPER_DIR%\maven-wrapper.jar"

if not exist "%PROPS_FILE%" (
  echo Missing %PROPS_FILE% 1>&2
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b "wrapperUrl=" "%PROPS_FILE%"`) do (
  set "WRAPPER_URL=%%B"
)

if "%WRAPPER_URL%"=="" (
  set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
)

if not exist "%JAR_FILE%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%','%JAR_FILE%')" || exit /b 1
)

if not "%JAVA_HOME%"=="" (
  set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_CMD=java"
)

"%JAVA_CMD%" -classpath "%JAR_FILE%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal

