@echo off
echo ========================================
echo   Warface Survivor Launcher
echo   Modo Desenvolvimento
echo ========================================
echo.

REM Verificar se dependencias estao instaladas
if not exist "node_modules" (
    echo Dependencias nao encontradas!
    echo.
    echo Instalando automaticamente...
    echo Isso pode demorar 2-5 minutos...
    echo.
    call npm install
    if errorlevel 1 (
        echo.
        echo ERRO ao instalar dependencias!
        echo.
        echo Execute manualmente:
        echo npm install
        echo.
        pause
        exit /b 1
    )
    echo.
    echo Dependencias instaladas com sucesso!
    echo.
)

echo Iniciando launcher...
echo.
echo DICA: Para fechar, pressione Ctrl+C aqui ou feche a janela do launcher
echo.

call npm start

if errorlevel 1 (
    echo.
    echo ERRO ao iniciar!
    echo.
    pause
)
