# UI Redesign - Módulo Races

## Resumo
Redesenho completo da interface de usuário do módulo de raças para um estilo minimalista, com foco em clareza e legibilidade.

## Mudanças Aplicadas

### 1. Remoção de Formatação Excessiva
- ❌ **Removido**: Todos os gradientes (`<gradient:...>`)
- ❌ **Removido**: Todos os textos em itálico (`<i>...</i>`)
- ❌ **Removido**: Emojis e símbolos decorativos excessivos (🔥, ⚡, ✦, ⚔)
- ✅ **Mantido**: Símbolos funcionais (✔, ✖, ⯈)

### 2. Simplificação de Cores
- **Antes**: `<gradient:#a855f7:#ec4899>⚔ Draconiano ⚔</gradient>`
- **Depois**: `<white>Draconiano</white>` ou `<aqua>Draconiano</aqua>`

### 3. Padronização de Listas
- **Antes**: `<dark_gray>▪</dark_gray>`, `<dark_gray>➤</dark_gray>`
- **Depois**: `<gray>•</gray>` (bullet point consistente)

### 4. Símbolos de Navegação
- **Antes**: `➡ CLIQUE PARA CONTINUAR`
- **Depois**: `⯈ Clique para continuar`

## Arquivos Modificados

### Java GUIs (9 arquivos)
1. ✅ `RaceSelectionGui.java` - Menu de seleção de raças
2. ✅ `RaceMainMenuGui.java` - Menu principal do sistema
3. ✅ `RacePreviewGui.java` - Preview detalhado de raça
4. ✅ `RaceConfirmationGui.java` - Confirmação de escolha
5. ✅ `RaceDetailGui.java` - Detalhes da raça atual
6. ✅ `RaceAbilitiesGui.java` - Lista de habilidades
7. ✅ `RaceProgressGui.java` - Progressão e XP
8. ✅ `RaceEvolutionGui.java` - Árvore de evolução
9. ✅ `RaceAdminGui.java` - Painel administrativo

### Arquivos YAML (2 arquivos)
1. ✅ `races.yml` - Removidos itálicos das descrições de raças
2. ✅ `messages.yml` - Simplificada mensagem de level up

## Padrões Estabelecidos

### Títulos de GUI
```yaml
Antes: <gradient:#a855f7:#ec4899>⚔ Sistema de Raças ⚔</gradient>
Depois: <white>Sistema de Raças</white>
```

### Nomes de Itens
```yaml
# Raça/Linhagem Principal
<aqua>Nome da Raça</aqua>

# Botões Positivos
<green>Botão de Confirmação</green>

# Botões Negativos
<red>Botão de Cancelamento</red>

# Informações
<white>Título da Informação</white>

# Progressão/Evoluções
<gold>Árvore de Evolução</gold>
```

### Lore/Descrições
```yaml
# Descrições simples
<gray>Texto descritivo sem itálico</gray>

# Listas com bullets
<gray>• Primeiro item</gray>
<gray>• Segundo item</gray>

# Status positivo
<green>✔ DESBLOQUEADA</green>

# Status negativo
<red>✖ BLOQUEADA</red>

# Ações
<yellow>⯈ Clique para continuar</yellow>
```

### Atributos
```yaml
# Formato padrão
<gray>Nome do Atributo:<cor>+valor</cor>

# Exemplos
<gray>Vida:<red>+10</red>
<gray>Força:<white>+5</white>
<gray>Experiência:<yellow>1000 XP</yellow>
```

## Motivação

O redesign foi solicitado para:
1. **Melhorar legibilidade**: Textos sem itálico são mais fáceis de ler
2. **Reduzir poluição visual**: Gradientes e símbolos excessivos distraem
3. **Manter clareza**: Informações importantes se destacam naturalmente
4. **Consistência**: Segue o padrão visual de servidores de Minecraft populares (Hypixel, Wynncraft)

## Resultado

- ✅ Todos os 9 GUIs redesenhados
- ✅ Todas as descrições de raças simplificadas
- ✅ Nenhum erro de compilação
- ✅ Build bem-sucedido
- ✅ 100% compatível com código existente

## Antes vs Depois

### RaceSelectionGui - Título
```diff
- <gradient:#a855f7:#ec4899>✦ Escolha sua Linhagem ✦</gradient>
+ <white>Escolha sua Linhagem</white>
```

### RaceMainMenuGui - Botão de Habilidades
```diff
- <gradient:#5555ff:#aa00ff>✦ Habilidades & Passivas</gradient>
+ <light_purple>Habilidades & Passivas</light_purple>
```

### RacePreviewGui - Botão de Confirmação
```diff
- <gradient:#00ff00:#00cc00><bold>⚡ CONFIRMAR SELEÇÃO ⚡</bold></gradient>
+ <green>Confirmar Seleção</green>
```

### RaceConfirmationGui - Título
```diff
- <gradient:#ff0000:#ff6b00>⚠ CONFIRMAÇÃO FINAL ⚠</gradient>
+ <red>⚠ CONFIRMAÇÃO FINAL ⚠</red>
```

### races.yml - Descrição
```diff
- <dark_gray><i>Descendentes dos dragões antigos.</i></dark_gray>
+ <dark_gray>Descendentes dos dragões antigos.</dark_gray>
```

---

**Data**: 31/01/2026  
**Versão**: MidgardRPG v1.0  
**Módulo**: midgard-races
