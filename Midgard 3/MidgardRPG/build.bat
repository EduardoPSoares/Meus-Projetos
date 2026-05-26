@echo off
title MidgardRPG - Build
echo ========================================
echo        MidgardRPG - Build
echo ========================================
echo.

call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% == 0 (
    echo.
    echo ========================================
    echo        Build concluido com sucesso!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo        ERRO no build!
    echo ========================================
)

echo.
pause
