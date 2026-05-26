# ✅ Checklist de Testes - Redesign de Menus de Raças

## 🎯 Pré-requisitos
- [  ] Plugin compilado sem erros
- [  ] Arquivo `gui_messages.yml` copiado para `plugins/MidgardRPG/modules/races/messages/`
- [  ] Servidor iniciado sem erros
- [  ] Pelo menos 1 raça configurada em `races.yml`

---

## 📋 Testes Funcionais

### 1. Menu Principal (RaceMainMenuGui)

#### 1.1 Jogador SEM Raça
- [  ] Comando `/race` abre o menu
- [  ] Título mostra gradiente corretamente
- [  ] Perfil do jogador exibe "Sem Linhagem"
- [  ] Botão "ESCOLHER LINHAGEM" aparece e brilha (glow)
- [  ] Botão "Fechar" funciona
- [  ] Som de clique é reproduzido

#### 1.2 Jogador COM Raça
- [  ] Perfil mostra nome da raça com gradiente
- [  ] Nível e XP aparecem corretamente
- [  ] Barra de progresso (percentual) calculada
- [  ] 5 botões de navegação aparecem:
  - [  ] Raça (ícone da raça + glow)
  - [  ] Habilidades (livro encantado)
  - [  ] Progressão (garrafa de XP)
  - [  ] Evolução (estrela do nether)
  - [  ] Ajuda (livro)
- [  ] Botão Admin aparece apenas para admins

---

### 2. Seleção de Raças (RaceSelectionGui)

#### 2.1 Layout e Visual
- [  ] Menu abre ao clicar em "Escolher Linhagem"
- [  ] Raças aparecem em grade (até 45 itens)
- [  ] Cada raça mostra:
  - [  ] Nome com gradiente
  - [  ] Descrição curta (máx 2 linhas)
  - [  ] Separador visual (`━━━━━━`)
  - [  ] Lista de atributos com cores (verde/vermelho)
  - [  ] Contagem de habilidades
  - [  ] Hint "Clique para ver detalhes"
- [  ] Raça atual tem badge verde "✓ ATUAL" e glow

#### 2.2 Navegação
- [  ] Botão "Voltar" retorna ao menu principal
- [  ] Info de página mostra "Página X/Y"
- [  ] Setas de página anterior/próxima funcionam
- [  ] Paginação automática funciona com muitas raças
- [  ] Botão "Fechar" fecha o inventário

#### 2.3 Interação
- [  ] Clicar em uma raça abre RacePreviewGui
- [  ] Som diferente ao mudar de página

---

### 3. Preview de Raça (RacePreviewGui)

#### 3.1 Informações Exibidas
- [  ] Ícone central da raça (slot 4) com glow
- [  ] 📜 Descrição completa da raça
- [  ] ⚡ Atributos:
  - [  ] Base (com cores)
  - [  ] Por Nível (se houver)
- [  ] ✦ Habilidades:
  - [  ] Lista com níveis requeridos
  - [  ] "Mais..." se exceder 8
  - [  ] Total de habilidades
- [  ] ⬆ Evoluções:
  - [  ] Lista de evoluções
  - [  ] "Sem evoluções" se não houver

#### 3.2 Botões
- [  ] Confirmar (verde com glow) para raças novas
- [  ] "Linhagem Atual" (verde sem ação) se já for a raça
- [  ] Voltar retorna para seleção
- [  ] Fechar fecha tudo

#### 3.3 Interação
- [  ] Clicar em "Confirmar" abre RaceConfirmationGui
- [  ] Raça atual não permite clique em confirmar

---

### 4. Confirmação (RaceConfirmationGui)

#### 4.1 Layout Modal (3 linhas)
- [  ] Título "⚠ Confirmar" em amarelo
- [  ] Ícone da raça no centro
- [  ] Aviso de permanência
- [  ] Botão CONFIRMAR (verde, glow)
- [  ] Botão CANCELAR (vermelho)

#### 4.2 Funcionalidade
- [  ] Confirmar aplica a raça:
  - [  ] RaceData atualizado
  - [  ] Perfil salvo
  - [  ] Mensagem de sucesso
  - [  ] Sons: levelup + toast
  - [  ] Evento PlayerChangeRaceEvent disparado
- [  ] Cancelar volta para preview
- [  ] Validações:
  - [  ] Não permite se já tiver raça
  - [  ] Não permite se raça não existir

---

### 5. Detalhes da Raça (RaceDetailGui)

#### 5.1 Informações
- [  ] Ícone central com nível/XP
- [  ] 📊 Estatísticas:
  - [  ] Nível atual
  - [  ] XP atual/necessário
  - [  ] Progresso em %
  - [  ] Habilidades desbloqueadas/total
- [  ] ⚡ Atributos Ativos:
  - [  ] Calculados com bônus de nível
- [  ] 📜 História da raça

#### 5.2 Navegação
- [  ] Voltar retorna ao menu principal
- [  ] Fechar funciona

---

### 6. Habilidades (RaceAbilitiesGui)

#### 6.1 Layout
- [  ] Info card no topo (total, desbloqueadas, bloqueadas)
- [  ] Lista de habilidades com paginação
- [  ] Cada habilidade mostra:
  - [  ] Status (✓ verde ou ✖ cinza)
  - [  ] Nome (branco se desbloqueada, cinza se não)
  - [  ] Tipo da trait
  - [  ] Nível necessário (se bloqueada)
  - [  ] Glow se desbloqueada

#### 6.2 Paginação
- [  ] Máx 45 por página
- [  ] Navegação funciona
- [  ] Info de página correta

---

### 7. Progressão (RaceProgressGui)

#### 7.1 Informações
- [  ] Perfil da raça (ícone + glow)
- [  ] Nível e XP com %
- [  ] Barra de progresso:
  - [  ] XP atual
  - [  ] XP necessário
  - [  ] Faltando
- [  ] Próximos desbloqueios:
  - [  ] Lista de habilidades do próximo nível
  - [  ] "Nenhum" se não houver

---

### 8. Evolução (RaceEvolutionGui)

#### 8.1 Árvore de Evoluções
- [  ] Raça atual no topo
- [  ] Evoluções em grid (slots 19, 21, 23, 25, etc)
- [  ] Evoluções DISPONÍVEIS:
  - [  ] Verde com glow
  - [  ] Info completa
  - [  ] Hint "Clique para evoluir"
- [  ] Evoluções BLOQUEADAS:
  - [  ] Cinza, sem glow
  - [  ] Nível necessário
  - [  ] Descrição reduzida

#### 8.2 Sem Evoluções
- [  ] Mensagem "Sem evoluções" se não houver

---

### 9. Admin (RaceAdminGui)

#### 9.1 Acesso
- [  ] Apenas jogadores com `midgard.admin.race` veem botão
- [  ] Menu abre corretamente

#### 9.2 Opções
- [  ] 👥 Gerenciar Jogadores:
  - [  ] Mostra comandos disponíveis
- [  ] ⟳ Recarregar:
  - [  ] Recarrega configs
  - [  ] Mensagem de sucesso
  - [  ] Som de sucesso
- [  ] 📊 Estatísticas:
  - [  ] Total de raças
  - [  ] Raças base vs sub-raças
  - [  ] Jogadores online
  - [  ] Jogadores com raça

---

## 🎨 Testes Visuais

### Gradientes
- [  ] Títulos usam gradientes corretos
- [  ] Nomes de raças com gradiente
- [  ] Botões de ação com gradiente
- [  ] Cores consistentes em todos os menus

### Separadores
- [  ] `━━━━━━` aparece corretamente (não bugado)
- [  ] Espaçamento adequado antes/depois

### Lore
- [  ] Sem itálico (exceto quando desejado)
- [  ] Quebras de linha funcionam (`\n`)
- [  ] Linhas vazias preservadas
- [  ] Hierarquia visual clara

### Ícones
- [  ] Glow aplicado onde devido
- [  ] Materiais corretos
- [  ] SkullOwner funciona (se aplicável)

---

## 🐛 Testes de Erros

### Validações
- [  ] Jogador sem perfil não causa erro
- [  ] Raça inexistente não quebra menu
- [  ] Paginação com 0 itens não quebra
- [  ] Clique fora do inventário não causa erro

### Logs
- [  ] Erros logados com contexto completo
- [  ] Sem spam no console
- [  ] MidgardLogger usado corretamente

### Performance
- [  ] Menus abrem instantaneamente
- [  ] Sem lag ao paginar
- [  ] Sem memory leak ao abrir/fechar múltiplas vezes

---

## 📊 Resultados

| Categoria | Total | Passou | Falhou | Taxa |
|-----------|-------|--------|--------|------|
| Menu Principal | 0 | 0 | 0 | 0% |
| Seleção | 0 | 0 | 0 | 0% |
| Preview | 0 | 0 | 0 | 0% |
| Confirmação | 0 | 0 | 0 | 0% |
| Detalhes | 0 | 0 | 0 | 0% |
| Habilidades | 0 | 0 | 0 | 0% |
| Progressão | 0 | 0 | 0 | 0% |
| Evolução | 0 | 0 | 0 | 0% |
| Admin | 0 | 0 | 0 | 0% |
| Visuais | 0 | 0 | 0 | 0% |
| Erros | 0 | 0 | 0 | 0% |
| **TOTAL** | **0** | **0** | **0** | **0%** |

---

## 📝 Notas de Teste

### Bugs Encontrados


### Melhorias Sugeridas


### Observações


---

**Testador**: _______________  
**Data**: ___/___/2026  
**Versão**: 5.0
