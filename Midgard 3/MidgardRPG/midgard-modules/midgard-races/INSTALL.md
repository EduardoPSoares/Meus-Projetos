# 🚀 Instruções de Instalação - Redesign de Menus

## 📦 Passo a Passo

### 1. Compilar o Projeto
```bash
cd c:\MidgardRPG
.\gradlew clean build -x test
```

### 2. Parar o Servidor
```bash
# No console do servidor:
stop
```

### 3. Copiar JARs
```bash
# Deletar versões antigas
Remove-Item "RPG\plugins\midgard-*.jar" -Force

# Copiar novos JARs
Copy-Item "midgard-core\build\libs\*.jar" "RPG\plugins\"
Copy-Item "midgard-loader\build\libs\*.jar" "RPG\plugins\"
Copy-Item "midgard-modules\build\libs\*.jar" "RPG\plugins\"
```

### 4. Verificar Arquivo de Mensagens
O arquivo `gui_messages.yml` deve ser criado automaticamente em:
```
RPG/plugins/MidgardRPG/modules/races/messages/gui_messages.yml
```

Se não for criado automaticamente, copie manualmente:
```bash
Copy-Item "midgard-modules\midgard-races\src\main\resources\modules\races\messages\gui_messages.yml" "RPG\plugins\MidgardRPG\modules\races\messages\"
```

### 5. Iniciar o Servidor
```bash
cd RPG
.\start.bat
```

### 6. Verificar Logs
Procure por:
```
[MidgardRPG] Habilitando Midgard-Races...
[MidgardRPG] Midgard-Races habilitado com sucesso!
```

---

## 🎮 Testando os Menus

### Comandos Básicos
```
/race                    # Abre menu principal
/race select             # Abre seleção de raças
/race info               # Info da sua raça
```

### Comandos Admin (requer permissão)
```
/race set <player> <id>  # Define raça
/race reset <player>     # Remove raça
/race exp <player> <qtd> # Dá XP
/race reload             # Recarrega configs
```

---

## 🐛 Troubleshooting

### Problema: Menu não abre
**Solução:**
1. Verifique permissão: `midgard.races.use`
2. Verifique logs para erros
3. Confira se o arquivo `gui_messages.yml` existe

### Problema: Gradientes não aparecem
**Solução:**
1. Verifique versão do servidor (requer Paper 1.21+)
2. Confirme MiniMessage está ativo no core
3. Verifique logs de parse de mensagens

### Problema: Raças não aparecem
**Solução:**
1. Verifique `races.yml` está configurado
2. Confirme MaterialBuilder reconhece os ícones
3. Reload com `/race reload`

### Problema: Erros ao clicar
**Solução:**
1. Verifique logs completos
2. Confirme profile do jogador existe
3. Verifique permissões de interação

---

## 📋 Arquivos Importantes

### Configurações
- `RPG/plugins/MidgardRPG/modules/races/config.yml`
- `RPG/plugins/MidgardRPG/modules/races/races.yml`
- `RPG/plugins/MidgardRPG/modules/races/messages/messages.yml`
- `RPG/plugins/MidgardRPG/modules/races/messages/gui_messages.yml` ⭐ **NOVO**

### Código-Fonte (Java)
- `RaceMainMenuGui.java` ✅ Reformulado
- `RaceSelectionGui.java` ✅ Reformulado
- `RacePreviewGui.java` ✅ Novo
- `RaceConfirmationGui.java` ✅ Novo
- `RaceDetailGui.java` ✅ Novo
- `RaceAbilitiesGui.java` ✅ Novo
- `RaceProgressGui.java` ✅ Novo
- `RaceEvolutionGui.java` ✅ Novo
- `RaceAdminGui.java` ✅ Novo
- `ItemBuilder.java` ✅ Método `setLoreMultiline()` adicionado
- `RacesModule.java` ✅ Métodos `getGuiMessage()` adicionados

---

## ✅ Checklist de Instalação

- [  ] Projeto compilado sem erros
- [  ] JARs copiados para `RPG/plugins/`
- [  ] Arquivo `gui_messages.yml` existe
- [  ] Servidor iniciado sem erros
- [  ] Menu `/race` abre corretamente
- [  ] Gradientes aparecem corretamente
- [  ] Navegação funciona
- [  ] Seleção de raça funciona
- [  ] Confirmação aplica raça
- [  ] Nenhum erro no console

---

## 🎨 Preview Rápido

### Antes (Antigo)
```
[GUI Simples]
- Bordas de vidro
- Cores básicas (&a, &c)
- Lore simples
- Layout 5 linhas
```

### Depois (Novo)
```
[GUI Premium]
- Sem bordas
- Gradientes vibrantes
- Lore hierárquica com separadores
- Layout 6 linhas
- Estilo Wynncraft/Hypixel
```

---

## 📞 Suporte

Se encontrar problemas:
1. Verifique `TEST_CHECKLIST.md` para testes detalhados
2. Confira `REDESIGN_COMPLETE.md` para documentação completa
3. Analise logs em `RPG/logs/latest.log`
4. Verifique erros no console ao abrir menus

---

**Desenvolvido por**: Copilot Agent (Shark Mode)  
**Data**: 01/02/2026  
**Versão**: 5.0 (Beast Mode)
