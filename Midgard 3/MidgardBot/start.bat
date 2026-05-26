@echo off
title MidgardBot
color 0A
cd /d "%~dp0"

echo.
echo  ========================================
echo       MidgardBot - Midgard RPG Network
echo  ========================================
echo.

:: Verifica se o Java esta instalado
java -version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo  [ERRO] Java nao encontrado!
    echo  Instale o Java 16+ para rodar o bot.
    echo.
    pause
    exit /b 1
)

:: Nome do JAR
set JAR_NAME=MidgardBot-1.0.0-SNAPSHOT.jar
set JAR_PATH=target\%JAR_NAME%

:: Verifica se o JAR existe, se nao compila
if not exist "%JAR_PATH%" (
    echo  [INFO] JAR nao encontrado. Compilando...
    echo.
    call "%~dp0mvnw.cmd" package -q -DskipTests
    if errorlevel 1 (
        color 0C
        echo.
        echo  [ERRO] Falha ao compilar o projeto!
        pause
        exit /b 1
    )
    echo  [OK] Compilado com sucesso!
    echo.
)

:: Verifica se o .env existe
if not exist ".env" (
    echo  [AVISO] Arquivo .env nao encontrado!
    echo  Criando .env de exemplo...
    (
        echo # MidgardBot - Configuracao
        echo BOT_TOKEN=seu_token_aqui
    ) > .env
    echo.
    echo  [INFO] Edite o arquivo .env e coloque seu token.
    echo.
    pause
    exit /b 0
)

:: Inicia o bot
echo  [INFO] Iniciando MidgardBot...
echo.

java -jar "%JAR_PATH%"

:: Se o bot parar
echo.
echo  [INFO] Bot encerrado.
pause
