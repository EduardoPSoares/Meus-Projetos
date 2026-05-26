@echo off
:inicio
echo Iniciando o servidor de Midgard...

if exist "C:\Projetos\Midgard 3\MidgardRPG\RPG\plugins\MidgardRPG" (
    echo Excluindo pasta MidgardRPG...
    rmdir /s /q "C:\Projetos\Midgard 3\MidgardRPG\RPG\plugins\MidgardRPG"
)

java -Xmx2G -jar server.jar --nogui

echo Servidor fechado. Reiniciando...
goto inicio