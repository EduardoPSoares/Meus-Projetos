Write-Host "========================================"
Write-Host "   MidgardRPG - Build & Deploy"
Write-Host "========================================"
Write-Host ""

Write-Host "[1/2] Compilando midgard-loader..."
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" package -pl midgard-loader -am -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERRO] Falha na compilacao!" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "[2/2] Copiando JAR para o servidor..."
Copy-Item "midgard-loader\target\midgard-loader-1.0.0-SNAPSHOT.jar" "RPG\plugins\midgard-loader-1.0.0-SNAPSHOT.jar" -Force

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   Deploy concluido com sucesso!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
