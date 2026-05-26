@echo off
title AdminPanel
cd /d "%~dp0"
..\Servidor\NodeJs\node.exe server.js
pause
