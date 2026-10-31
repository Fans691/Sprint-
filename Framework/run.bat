@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM  BUILD FRAMEWORK
REM ============================================================

pushd "%~dp0"

set "APP_NAME=Framework"
set "SRC_DIR=%CD%\src\java"
set "WEB_DIR=%CD%\src\webapps"
set "LIB=%CD%\lib"
set "BUILD_DIR=%CD%\build"
set "CLASSES_DIR=%BUILD_DIR%\WEB-INF\classes"
set "LIB_DIR=%BUILD_DIR%\WEB-INF\lib"
set "MAVEN_REPO=https://repo1.maven.org/maven2"
set "JAVA_MIN_VERSION=17"

echo.
echo ===================================
echo Recherche du JDK %JAVA_MIN_VERSION%+...
echo ===================================

set "JDK_HOME="
if defined JAVA_HOME call :check_jdk "%JAVA_HOME%"
for %%r in ("%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Java" "%ProgramFiles%\Microsoft") do (
    if not defined JDK_HOME (
        for /d %%j in ("%%~r\jdk-*") do (
            if not defined JDK_HOME call :check_jdk "%%~j"
        )
    )
)

if not defined JDK_HOME (
    echo ERREUR : aucun JDK %JAVA_MIN_VERSION%+ trouve ^(Spring 6 l'exige^).
    echo Installez un JDK 17 ou 21 ou definissez JAVA_HOME.
    goto :fail
)

set "JAVAC=%JDK_HOME%\bin\javac.exe"
set "JAR=%JDK_HOME%\bin\jar.exe"
echo JDK utilise : %JDK_HOME%

echo.
echo ===================================
echo Verification des dependances...
echo ===================================

if not exist "%LIB%" mkdir "%LIB%"
REM servlet-api : necessaire a la compilation uniquement (fourni par Tomcat 10+)
call :ensure_dep servlet-api.jar org/apache/tomcat/tomcat-servlet-api/10.0.16/tomcat-servlet-api-10.0.16.jar || goto :fail
call :ensure_dep spring-core-6.1.14.jar org/springframework/spring-core/6.1.14/spring-core-6.1.14.jar || goto :fail
call :ensure_dep spring-jcl-6.1.14.jar org/springframework/spring-jcl/6.1.14/spring-jcl-6.1.14.jar || goto :fail
call :ensure_dep spring-beans-6.1.14.jar org/springframework/spring-beans/6.1.14/spring-beans-6.1.14.jar || goto :fail
call :ensure_dep spring-aop-6.1.14.jar org/springframework/spring-aop/6.1.14/spring-aop-6.1.14.jar || goto :fail
call :ensure_dep spring-context-6.1.14.jar org/springframework/spring-context/6.1.14/spring-context-6.1.14.jar || goto :fail
call :ensure_dep spring-expression-6.1.14.jar org/springframework/spring-expression/6.1.14/spring-expression-6.1.14.jar || goto :fail
call :ensure_dep spring-web-6.1.14.jar org/springframework/spring-web/6.1.14/spring-web-6.1.14.jar || goto :fail
call :ensure_dep micrometer-observation-1.12.11.jar io/micrometer/micrometer-observation/1.12.11/micrometer-observation-1.12.11.jar || goto :fail
call :ensure_dep micrometer-commons-1.12.11.jar io/micrometer/micrometer-commons/1.12.11/micrometer-commons-1.12.11.jar || goto :fail
call :ensure_dep jackson-core-2.17.3.jar com/fasterxml/jackson/core/jackson-core/2.17.3/jackson-core-2.17.3.jar || goto :fail
call :ensure_dep jackson-databind-2.17.3.jar com/fasterxml/jackson/core/jackson-databind/2.17.3/jackson-databind-2.17.3.jar || goto :fail
call :ensure_dep jackson-annotations-2.17.3.jar com/fasterxml/jackson/core/jackson-annotations/2.17.3/jackson-annotations-2.17.3.jar || goto :fail
call :ensure_dep classgraph-4.8.179.jar io/github/classgraph/classgraph/4.8.179/classgraph-4.8.179.jar || goto :fail

echo.
echo ===================================
echo Nettoyage...
echo ===================================

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"
mkdir "%LIB_DIR%"

echo.
echo ===================================
echo Compilation Java...
echo ===================================

set "SOURCES=%TEMP%\framework-sources.txt"
if exist "%SOURCES%" del "%SOURCES%"
for /r "%SRC_DIR%" %%f in (*.java) do (
    set "FILE=%%~ff"
    echo "!FILE:\=/!">> "%SOURCES%"
)
if not exist "%SOURCES%" (
    echo ERREUR : Aucun fichier Java trouve dans %SRC_DIR%.
    goto :fail
)

"%JAVAC%" -parameters -encoding UTF-8 --release %JAVA_MIN_VERSION% -cp "%LIB%\*" -d "%CLASSES_DIR%" @"%SOURCES%"
set "RESULT=%ERRORLEVEL%"
del "%SOURCES%" 2>nul
if not "%RESULT%"=="0" (
    echo.
    echo ERREUR : Compilation echouee.
    goto :fail
)

echo.
echo ===================================
echo Copie des ressources Web...
echo ===================================

if exist "%WEB_DIR%" xcopy "%WEB_DIR%\*" "%BUILD_DIR%\" /E /I /Y /Q >nul

echo.
echo ===================================
echo Extraction des JARs...
echo ===================================

pushd "%CLASSES_DIR%"
for %%j in ("%LIB%\*.jar") do (
    set "NAME=%%~nxj"
    if /i not "!NAME:~0,11!"=="servlet-api" (
        echo Extraction de %%~nxj
        "%JAR%" xf "%%~fj"
    )
)
popd

REM web-fragment.xml de spring-web (metadata-complete="true") empecherait Tomcat
REM de lire les annotations du Framework.jar (@WebListener)
if exist "%CLASSES_DIR%\META-INF\web-fragment.xml" del "%CLASSES_DIR%\META-INF\web-fragment.xml"
if exist "%CLASSES_DIR%\module-info.class" del "%CLASSES_DIR%\module-info.class"

echo.
echo ===================================
echo Fusion des fichiers SPI Spring...
echo ===================================

call :merge_spring_file spring.handlers
call :merge_spring_file spring.schemas

echo.
echo ===================================
echo Creation du Framework.jar...
echo ===================================

"%JAR%" cf "%LIB_DIR%\%APP_NAME%.jar" -C "%CLASSES_DIR%" .
if errorlevel 1 (
    echo ERREUR : Creation du JAR impossible.
    goto :fail
)

echo.
echo JAR cree :
echo %LIB_DIR%\%APP_NAME%.jar

echo.
echo ===================================
echo BUILD TERMINE
echo ===================================

popd
pause
endlocal
exit /b 0

:fail
popd
pause
endlocal
exit /b 1

REM ------------------------------------------------------------
REM  %1 = dossier d'un JDK : le retient s'il est en version 17+
REM ------------------------------------------------------------
:check_jdk
if not exist "%~1\bin\javac.exe" exit /b 0
set "VERSION="
set "MAJOR=0"
for /f "tokens=2" %%v in ('"%~1\bin\javac.exe" -version 2^>^&1') do set "VERSION=%%v"
for /f "delims=." %%m in ("!VERSION!") do set "MAJOR=%%m"
if !MAJOR! geq %JAVA_MIN_VERSION% set "JDK_HOME=%~1"
exit /b 0

REM ------------------------------------------------------------
REM  %1 = nom du jar dans lib, %2 = chemin sur Maven Central
REM ------------------------------------------------------------
:ensure_dep
if exist "%LIB%\%~1" (
    echo OK          %~1
    exit /b 0
)
echo Telechargement de %~1
curl -fsSL -o "%LIB%\%~1" "%MAVEN_REPO%/%~2"
if errorlevel 1 (
    del "%LIB%\%~1" 2>nul
    echo ERREUR : telechargement impossible : %MAVEN_REPO%/%~2
    exit /b 1
)
exit /b 0

REM ------------------------------------------------------------
REM  %1 = fichier META-INF a fusionner depuis tous les jars Spring
REM ------------------------------------------------------------
:merge_spring_file
set "OUTPUT=%CLASSES_DIR%\META-INF\%~1"
set "MERGE_DIR=%TEMP%\framework-spring-merge"
set "FOUND="
if exist "%OUTPUT%" del "%OUTPUT%"
for %%j in ("%LIB%\*.jar") do (
    "%JAR%" tf "%%~fj" | findstr /x /c:"META-INF/%~1" >nul
    if not errorlevel 1 (
        if exist "%MERGE_DIR%" rmdir /s /q "%MERGE_DIR%"
        mkdir "%MERGE_DIR%"
        pushd "%MERGE_DIR%"
        "%JAR%" xf "%%~fj" "META-INF/%~1"
        popd
        type "%MERGE_DIR%\META-INF\%~1" >> "%OUTPUT%"
        echo.>> "%OUTPUT%"
        set "FOUND=1"
    )
)
if exist "%MERGE_DIR%" rmdir /s /q "%MERGE_DIR%"
if defined FOUND echo Fichier META-INF/%~1 fusionne.
exit /b 0
