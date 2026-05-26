@echo off
title MidgardRPG - Build & Deploy
echo ========================================
echo    MidgardRPG - Build ^& Deploy
echo ========================================
echo.

echo [1/2] Compilando midgard-loader...
call mvnw.cmd package -pl midgard-loader -am -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha na compilacao!
    pause
    exit /b 1
)

echo [2/2] Copiando JAR para o servidor...
copy /Y "midgard-loader\target\midgard-loader-1.0.0-SNAPSHOT.jar" "RPG\plugins\midgard-loader-1.0.0-SNAPSHOT.jar" >nul

if %ERRORLEVEL% == 0 (
    echo.
    echo ========================================
    echo    Deploy concluido com sucesso!
    echo ========================================
) else (
    echo.
    echo [ERRO] Falha ao copiar o JAR!
)

echo.
pause
