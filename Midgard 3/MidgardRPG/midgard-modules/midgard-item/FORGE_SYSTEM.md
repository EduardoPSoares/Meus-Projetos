# ⚒️ Sistema de Forja Multibloco — Documento de Desenvolvimento

> **Módulo:** `midgard-item` (extensão) + futuro `midgard-professions`  
> **Versão do Documento:** 1.0  
> **Data:** 12/02/2026  
> **Status:** Planejamento  
> **Dependências:** midgard-core, midgard-item, midgard-economy, midgard-combat, midgard-classes, WorldEdit, Nexo, FancyHolograms

---

## 📋 Índice

1. [Visão Geral](#1-visão-geral)
2. [Sistema de Profissões — Base](#2-sistema-de-profissões--base)
3. [Profissão: Ferreiro (Blacksmith)](#3-profissão-ferreiro-blacksmith)
4. [Construção Multibloco — Schematic & Blocos Fantasma](#4-construção-multibloco--schematic--blocos-fantasma)
5. [A Forja — Estrutura e Tiers](#5-a-forja--estrutura-e-tiers)
6. [Processo de Forja Interativa](#6-processo-de-forja-interativa)
7. [Mini-Games de Forja](#7-mini-games-de-forja)
8. [Sistema de Qualidade](#8-sistema-de-qualidade)
9. [Materiais e Ligas](#9-materiais-e-ligas)
10. [Sistema de Níveis do Ferreiro](#10-sistema-de-níveis-do-ferreiro)
11. [Receitas de Forja](#11-receitas-de-forja)
12. [Integração com Sistemas Existentes](#12-integração-com-sistemas-existentes)
13. [Arquitetura Técnica](#13-arquitetura-técnica)
14. [Schema do Banco de Dados](#14-schema-do-banco-de-dados)
15. [Comandos e Permissões](#15-comandos-e-permissões)
16. [Configuração YAML](#16-configuração-yaml)
17. [Efeitos Visuais e Sonoros](#17-efeitos-visuais-e-sonoros)
18. [Fases de Implementação](#18-fases-de-implementação)
19. [Considerações de Performance](#19-considerações-de-performance)
20. [Referências e Inspiração](#20-referências-e-inspiração)

---

## 1. Visão Geral

O **Sistema de Forja Multibloco** é a primeira implementação concreta do sistema de profissões do MidgardRPG. Ele transforma a criação de armas e armaduras em uma experiência imersiva e interativa, forçando os jogadores a se especializarem como **Ferreiros** para produzir equipamentos.

### Princípios Fundamentais

- **Imersão acima de conveniência** — O jogador deve sentir que está realmente forjando
- **Skill-based** — O resultado depende da habilidade do jogador, não apenas de receitas
- **Progressão significativa** — Cada nível desbloqueado abre novas possibilidades reais
- **Interdependência social** — Ferreiros precisam de mineradores, encantadores precisam de ferreiros
- **Zero automação** — Nenhuma forja automática; cada item requer presença e ação ativa

### Fluxo Macro

```
[Jogador escolhe profissão Ferreiro]
        ↓
[Constrói Forja Multibloco com guia de blocos fantasma]
        ↓
[Seleciona receita na bigorna interativa]
        ↓
[Insere materiais no slot de input]
        ↓
[Aquece lingotes no forno da forja]
        ↓
[Mini-game: Martelamento na bigorna] → Determina forma
        ↓
[Mini-game: Têmpera no tanque de água] → Determina durabilidade
        ↓
[Mini-game: Afiação na pedra de amolar] → Determina dano/eficácia
        ↓
[Item forjado com qualidade baseada na performance]
        ↓
[Nome do ferreiro gravado no item]
```

---

## 2. Sistema de Profissões — Base

> Este sistema será implementado no módulo `midgard-professions` e serve de base para o Ferreiro e futuras profissões.

### 2.1 Estrutura da Profissão

```java
public interface Profession {
    String getId();                    // "blacksmith", "alchemist", etc.
    String getDisplayName();           // Nome com formatação MiniMessage
    String getIcon();                  // Nexo item ID para ícone
    int getMaxLevel();                 // Nível máximo (ex: 100)
    ProfessionType getType();          // CRAFTING, GATHERING, SERVICE
    List<ProfessionPerk> getPerks();   // Vantagens por nível
    List<ProfessionRecipe> getRecipes(); // Receitas desbloqueáveis
}
```

### 2.2 Regras de Profissão

| Regra | Valor | Descrição |
|-------|-------|-----------|
| Profissões simultâneas | 2 (configurável) | Jogador pode ter no máximo 2 profissões ativas |
| Troca de profissão | Permitida com penalidade | Perde 50% do nível ao trocar (configurável) |
| Profissão primária | 1 | Ganha 100% de XP |
| Profissão secundária | 1 | Ganha 60% de XP (configurável) |
| Nível mínimo para crafting | Variável | Cada receita exige um nível mínimo |
| Cooldown de troca | 7 dias reais | Impede troca constante de profissões |

### 2.3 Tipos de Profissão Planejados

| Profissão | Tipo | Produz | Depende de |
|-----------|------|--------|------------|
| **Ferreiro** | CRAFTING | Armas, Armaduras, Ferramentas | Minerador |
| Alquimista | CRAFTING | Poções, Venenos, Elixires | Herbalista |
| Encantador | CRAFTING | Encantamentos, Runas, Pergaminhos | Ferreiro, Alquimista |
| Joalheiro | CRAFTING | Anéis, Amuletos, Gemas lapidadas | Minerador |
| Coureiro | CRAFTING | Armaduras leves, Bolsas, Selas | Caçador |
| Carpinteiro | CRAFTING | Arcos, Cajados, Escudos, Mobília | Lenhador |
| Cozinheiro | CRAFTING | Comidas com buffs, Banquetes | Pescador, Fazendeiro |
| Minerador | GATHERING | Minérios, Gemas brutas, Ligas | — |
| Herbalista | GATHERING | Ervas, Flores, Raízes | — |
| Caçador | GATHERING | Couro, Ossos, Presas | — |
| Lenhador | GATHERING | Madeiras especiais, Resinas | — |
| Pescador | GATHERING | Peixes, Pérolas, Algas | — |
| Fazendeiro | GATHERING | Grãos, Vegetais, Especiarias | — |

### 2.4 `ProfessionData` (ModuleData)

```java
public class ProfessionData implements ModuleData {
    private String primaryProfession;    // ID da profissão primária
    private String secondaryProfession;  // ID da profissão secundária
    private Map<String, Integer> levels;        // professionId → nível
    private Map<String, Double> experience;     // professionId → XP atual
    private Map<String, Long> cooldowns;        // professionId → timestamp de cooldown
    private Map<String, Set<String>> unlockedRecipes; // professionId → receitas desbloqueadas
    private Map<String, ProfessionStats> stats; // Estatísticas por profissão
}
```

---

## 3. Profissão: Ferreiro (Blacksmith)

### 3.1 Identidade

- **ID:** `blacksmith`
- **Nome:** `<gradient:#FF6B00:#FFD700>Ferreiro</gradient>`
- **Ícone Nexo:** `profession_blacksmith_icon`
- **Tipo:** `CRAFTING`
- **Nível Máximo:** 100
- **Descrição:**
  > *"Mestre do fogo e do aço. O ferreiro transforma metal bruto em armas lendárias através de suor, precisão e anos de dedicação à bigorna."*

### 3.2 O que o Ferreiro Produz

| Categoria | Exemplos | Nível Mínimo |
|-----------|----------|--------------|
| Armas Simples | Adaga de ferro, Espada curta | 1 |
| Armadura Leve Metálica | Cota de malha, Elmo simples | 5 |
| Armas Marciais | Espada longa, Machado de batalha | 15 |
| Armadura Pesada | Peitoral de aço, Grevas | 25 |
| Ferramentas Especiais | Picareta reforçada, Machado de coleta | 10 |
| Armas Avançadas | Alabarda, Martelo de guerra | 40 |
| Armadura de Elite | Armadura completa articulada | 55 |
| Armas de Liga Especial | Espada de mithril, Lança de adamantina | 70 |
| Armas Lendárias | Itens únicos com habilidades | 90 |
| Armas Míticas | Requerem materiais de raid/boss | 100 |

### 3.3 Bônus Passivos por Nível

| Nível | Bônus |
|-------|-------|
| 1 | Acesso à forja básica |
| 10 | +5% chance de qualidade Superior |
| 20 | Desbloqueia Forja Intermediária |
| 25 | +5% eficiência de materiais (chance de não consumir) |
| 30 | Pode reparar itens na bigorna (mini-game simplificado) |
| 40 | Desbloqueia Forja Avançada |
| 50 | +10% chance de qualidade Superior, +5% Excepcional |
| 60 | Pode adicionar sockets de gema durante a forja |
| 70 | Desbloqueia Forja de Mestre |
| 75 | Pode forjar com ligas especiais (mithril, adamantina) |
| 80 | +15% chance de qualidade Superior, +10% Excepcional |
| 90 | Chance de forjar itens Lendários (qualidade perfeita) |
| 95 | Desbloqueia Forja Lendária |
| 100 | Título "Mestre Ferreiro", pode assinar itens com selo especial |

---

## 4. Construção Multibloco — Schematic & Blocos Fantasma

### 4.1 Conceito

A forja não é um bloco único — é uma **estrutura multibloco** que o jogador deve construir fisicamente no mundo. Para guiá-lo, o sistema usa **blocos fantasma** (packets de bloco enviados via ProtocolLib) que mostram exatamente onde cada bloco deve ser colocado.

### 4.2 Fluxo de Construção

```
1. Jogador usa item "Planta da Forja" (específico por tier)
        ↓
2. Sistema verifica espaço disponível (AABB check)
        ↓
3. Blocos fantasma aparecem mostrando a estrutura
   - Blocos corretos: Contorno VERDE (partículas)
   - Blocos faltando: Contorno VERMELHO (partículas)  
   - Blocos errados: Contorno AMARELO (partículas)
        ↓
4. Jogador coloca blocos reais nos locais indicados
        ↓
5. A cada bloco colocado, sistema valida em tempo real
        ↓
6. Quando completa: Efeito visual + sonoro de ativação
        ↓
7. Forja registrada como estrutura ativa (protegida)
```

### 4.3 Implementação dos Blocos Fantasma

```java
public class GhostBlockManager {
    
    // Armazena as schematics de cada tier de forja
    private final Map<ForgeTier, ForgeSchematic> schematics;
    
    // Jogadores visualizando blocos fantasma
    private final Map<UUID, GhostBlockSession> activeSessions;
    
    /**
     * Envia blocos fantasma para o jogador usando ProtocolLib.
     * Os blocos são packets do tipo BLOCK_CHANGE enviados apenas
     * para o jogador específico — não afetam o mundo real.
     */
    public void showGhostBlocks(Player player, Location anchor, ForgeTier tier, int rotation) {
        // 1. Carregar schematic do tier
        // 2. Rotacionar baseado na direção do jogador (0°, 90°, 180°, 270°)
        // 3. Para cada bloco da schematic:
        //    a. Verificar se o bloco real já está correto
        //    b. Se não: enviar BLOCK_CHANGE packet com o bloco esperado
        //    c. Adicionar partículas de contorno (task periódica)
        // 4. Registrar sessão ativa para cleanup
    }
    
    /**
     * Valida quando um bloco é colocado dentro da área da schematic.
     * Chamado via BlockPlaceEvent.
     */
    public void onBlockPlace(BlockPlaceEvent event) {
        // 1. Verificar se o bloco está dentro de uma sessão ativa
        // 2. Comparar com bloco esperado da schematic
        // 3. Atualizar estado (remover ghost block daquela posição)
        // 4. Verificar se a estrutura está completa
        // 5. Se completa: ativar forja
    }
}
```

### 4.4 Schematic da Forja

Cada tier de forja tem uma schematic definida em YAML e armazenada no banco:

```yaml
# Exemplo: Forja Básica (Tier 1) — 5x4x5 blocos
forge_basic:
  tier: 1
  name: "<gray>Forja Básica</gray>"
  dimensions:
    width: 5   # X
    height: 4  # Y
    depth: 5   # Z
  # Posição relativa à âncora (bloco central inferior)
  # Legenda: A=Ar, S=Stone Bricks, B=Blast Furnace, 
  #          N=Anvil, C=Cauldron(água), G=Grindstone
  #          F=Furnace, L=Lava(source), I=Iron Block,
  #          W=Smithing Table, P=Campfire
  layers:
    # Camada Y=0 (chão)
    - - [S, S, S, S, S]
      - [S, I, S, I, S]
      - [S, S, S, S, S]
      - [S, I, S, I, S]
      - [S, S, S, S, S]
    # Camada Y=1
    - - [A, A, A, A, A]
      - [A, F, A, C, A]
      - [A, A, N, A, A]
      - [A, W, A, G, A]
      - [A, A, P, A, A]
    # Camada Y=2
    - - [A, A, A, A, A]
      - [A, F, A, A, A]
      - [A, A, A, A, A]
      - [A, A, A, A, A]
      - [A, A, A, A, A]
    # Camada Y=3 (chaminé)
    - - [A, A, A, A, A]
      - [A, S, A, A, A]
      - [A, A, A, A, A]
      - [A, A, A, A, A]
      - [A, A, A, A, A]
  
  # Blocos interativos (posições relativas)
  interactive_blocks:
    furnace:       { x: 1, y: 1, z: 1 }   # Forno — aquecer metais
    anvil:         { x: 2, y: 1, z: 2 }   # Bigorna — martelar
    cauldron:      { x: 3, y: 1, z: 1 }   # Tanque — têmpera
    grindstone:    { x: 3, y: 1, z: 3 }   # Pedra de amolar — afiar
    smithing_table: { x: 1, y: 1, z: 3 }  # Mesa — selecionar receita
    campfire:      { x: 2, y: 1, z: 4 }   # Fogueira — aquecer carvão
  
  # Âncora: bloco que o jogador clica para iniciar construção
  anchor_offset: { x: 2, y: 0, z: 2 }
  
  # Requisitos
  requirements:
    profession_level: 1
    materials_cost:
      stone_bricks: 40
      iron_block: 4
      anvil: 1
      cauldron: 1
      grindstone: 1
      furnace: 2
      smithing_table: 1
      campfire: 1
```

### 4.5 Proteção da Estrutura

Uma vez construída e ativada, a forja ganha proteção:

- Blocos não podem ser quebrados por jogadores não-donos
- Proteção contra explosões (configurável)
- Se um bloco essencial for quebrado → Forja desativada até reparo
- Registro automático como região WorldGuard (opcional)
- Limite de forjas por jogador (configurável, padrão: 1 por tier)

### 4.6 Rotação e Posicionamento

```java
public enum ForgeRotation {
    NORTH(0),    // Padrão
    EAST(90),    // Rotacionado 90° horário
    SOUTH(180),  // Rotacionado 180°
    WEST(270);   // Rotacionado 270°
    
    // O jogador escolhe a rotação ao posicionar a planta
    // Shift+Scroll ou ciclo com botão direito
}
```

O sistema usa a direção que o jogador está olhando como rotação padrão, com opção de girar antes de confirmar.

---

## 5. A Forja — Estrutura e Tiers

### 5.1 Tiers de Forja

| Tier | Nome | Dimensão | Nível Req. | Capacidades Extras |
|------|------|----------|------------|-------------------|
| 1 | Forja Básica | 5×4×5 | 1 | Armas/armaduras simples |
| 2 | Forja Intermediária | 7×5×7 | 20 | +Ligas básicas, +Reparação |
| 3 | Forja Avançada | 9×6×7 | 40 | +Ligas avançadas, +Sockets de gema |
| 4 | Forja de Mestre | 9×7×9 | 70 | +Materiais raros, +Encantamento estrutural |
| 5 | Forja Lendária | 11×8×11 | 95 | +Itens lendários, +Efeitos visuais permanentes |

### 5.2 Componentes Interativos da Forja

Cada forja contém blocos que o jogador interage diretamente:

#### 🔥 Forno (Furnace/Blast Furnace)
- **Função:** Aquecer lingotes e ligas até a temperatura ideal
- **Mecânica:** O jogador insere combustível + metal e espera o aquecimento
- **Indicador:** Partículas mudam de cor (vermelho → laranja → branco) conforme temperatura
- **Tiers superiores:** Blast Furnace aquece mais rápido, permite ligas complexas

#### ⚒️ Bigorna (Anvil)
- **Função:** Dar forma ao metal aquecido (mini-game principal)
- **Mecânica:** Mini-game de timing — martelar no momento certo
- **Indicador:** O item sendo forjado fica visível sobre a bigorna (armor stand/display entity)
- **Cada batida altera o modelo visual do item (estágios de progresso)**

#### 💧 Tanque de Têmpera (Cauldron c/ água)
- **Função:** Resfriar o metal forjado para definir durabilidade
- **Mecânica:** Mini-game de timing — imergir no momento certo da temperatura
- **Indicador:** Partículas de vapor, som de chiado
- **Cedo demais:** Metal frágil (- durabilidade)
- **Tarde demais:** Metal perde o calor (refazer aquecimento)
- **Timing perfeito:** Bônus de durabilidade

#### 🔪 Pedra de Amolar (Grindstone)
- **Função:** Afiar lâminas e polir armaduras
- **Mecânica:** Mini-game de pressão — segurar na zona certa
- **Indicador:** Faíscas visuais, mudança no modelo do item
- **Excesso de pressão:** Desgaste do item
- **Pressão ideal:** Bônus de dano/proteção

#### 📋 Mesa de Trabalho (Smithing Table)
- **Função:** Interface principal — seleção de receitas e visão geral
- **Mecânica:** Abre GUI com receitas disponíveis, materiais necessários, preview
- **Funciona como o "hub" da forja

#### 🔥 Fogueira Auxiliar (Campfire) — Tier 2+
- **Função:** Preparar carvão especial, aquecer ferramentas auxiliares
- **Mecânica:** Slow-craft de combustíveis especiais

#### ✨ Pedestal de Encantamento (Enchanting Setup) — Tier 4+
- **Função:** Aplicar encantamentos estruturais durante a forja
- **Mecânica:** Integração com sistema de encantamentos

---

## 6. Processo de Forja Interativa

### 6.1 Visão Geral do Processo

O processo de forja é uma **sequência de etapas interativas** que o jogador deve completar presencialmente. Não há crafting instantâneo.

```
┌─────────────────────────────────────────────────────┐
│                  FLUXO DE FORJA                      │
├──────────┬──────────────────────────────────────────┤
│ Etapa 1  │ SELEÇÃO — Mesa de Trabalho               │
│          │ → Escolher receita                        │
│          │ → Ver materiais necessários               │
│          │ → Confirmar intenção de forjar             │
├──────────┼──────────────────────────────────────────┤
│ Etapa 2  │ PREPARAÇÃO DE MATERIAIS                   │
│          │ → Inserir materiais no input               │
│          │ → Sistema valida e consome                 │
│          │ → Qualidade dos materiais registrada       │
├──────────┼──────────────────────────────────────────┤
│ Etapa 3  │ AQUECIMENTO — Forno                       │
│          │ → Colocar lingotes no forno               │
│          │ → Adicionar combustível                    │
│          │ → Aguardar temperatura ideal               │
│          │ → Retirar na hora certa (janela de tempo) │
├──────────┼──────────────────────────────────────────┤
│ Etapa 4  │ MARTELAMENTO — Bigorna (Mini-Game)        │
│          │ → Mini-game de timing rítmico              │
│          │ → Determina: Forma e Stats base            │
│          │ → Duração: 15-45 segundos                  │
├──────────┼──────────────────────────────────────────┤
│ Etapa 5  │ TÊMPERA — Tanque de Água (Mini-Game)      │
│          │ → Mini-game de temperatura/timing          │
│          │ → Determina: Durabilidade e Resistência    │
│          │ → Duração: 5-15 segundos                   │
├──────────┼──────────────────────────────────────────┤
│ Etapa 6  │ AFIAÇÃO/POLIMENTO — Grindstone (Mini-Game)│
│          │ → Mini-game de pressão                     │
│          │ → Determina: Dano/Armadura e Acabamento    │
│          │ → Duração: 10-25 segundos                  │
├──────────┼──────────────────────────────────────────┤
│ Etapa 7  │ FINALIZAÇÃO                               │
│          │ → Cálculo final de qualidade               │
│          │ → Item gerado com stats e qualidade        │
│          │ → Nome do ferreiro gravado                 │
│          │ → XP de profissão concedido                │
│          │ → Efeitos visuais de conclusão             │
└──────────┴──────────────────────────────────────────┘
```

### 6.2 Session de Forja

Cada processo de forja é rastreado por uma `ForgeSession`:

```java
public class ForgeSession {
    private final UUID playerId;
    private final UUID forgeId;            // ID da forja multibloco
    private final ForgeRecipe recipe;       // Receita sendo forjada
    private final long startTime;           // Timestamp de início
    
    private ForgeStage currentStage;        // Etapa atual
    private MaterialQuality materialQuality; // Qualidade dos materiais
    private double heatingScore;            // Score do aquecimento (0.0 - 1.0)
    private double hammeringScore;          // Score do martelamento (0.0 - 1.0)
    private double quenchingScore;          // Score da têmpera (0.0 - 1.0)
    private double sharpeningScore;         // Score da afiação (0.0 - 1.0)
    
    private int hammerStrikes;              // Número de marteladas
    private int perfectStrikes;             // Marteladas perfeitas
    private boolean failed;                 // Se a forja falhou
    
    // Timeout: se o jogador abandonar, a sessão expira e materiais são perdidos
    private static final long SESSION_TIMEOUT = 10 * 60 * 1000; // 10 minutos
}

public enum ForgeStage {
    SELECTING,      // Escolhendo receita
    PREPARING,      // Inserindo materiais
    HEATING,        // Aquecendo no forno
    HAMMERING,      // Mini-game de martelamento
    QUENCHING,      // Mini-game de têmpera
    SHARPENING,     // Mini-game de afiação
    FINALIZING,     // Calculando resultado
    COMPLETED,      // Concluído
    FAILED,         // Falhou
    EXPIRED         // Expirou por timeout
}
```

### 6.3 Etapa de Aquecimento — Detalhes

O aquecimento é uma etapa passiva com interação periódica:

```
Temperatura do Metal:
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0°C    — Frio (Cinza)
[████░░░░░░░░░░░░░░░░░░░░░░░░░░] 200°C  — Morno (Amarelo escuro)
[████████░░░░░░░░░░░░░░░░░░░░░░] 500°C  — Quente (Laranja)
[████████████░░░░░░░░░░░░░░░░░░] 800°C  — Muito quente (Vermelho)
[████████████████░░░░░░░░░░░░░░] 1000°C — Ideal (Branco-amarelado) ← RETIRAR AQUI
[████████████████████░░░░░░░░░░] 1200°C — Superaquecido (Branco) ← PERIGOSO
[████████████████████████░░░░░░] 1500°C — DERRETENDO ← MATERIAL PERDIDO
```

- **Barra de temperatura** exibida no **BossBar** do jogador
- **Partículas** no forno mudam de cor conforme temperatura
- **Jogador deve retirar o metal** quando atingir a "zona ideal" (verde no BossBar)
- Cada tipo de metal tem uma temperatura ideal diferente:
  - Ferro: 900°C-1100°C
  - Aço: 1000°C-1200°C
  - Mithril: 1200°C-1400°C
  - Adamantina: 1400°C-1650°C

**Score de aquecimento:**
- Centro da zona ideal = 1.0
- Bordas da zona = 0.7
- Fora da zona (mas retirável) = 0.3-0.5
- Superaquecido = 0.1 (item danificado)
- Derretido = 0.0 (material perdido, sessão falha)

---

## 7. Mini-Games de Forja

### 7.1 Mini-Game: Martelamento (Anvil Strike)

O mini-game mais importante — determina a **forma base** do item.

#### Interface (GUI 6 rows = 54 slots)

```
┌──────────────────────────────────────────────┐
│  ⚒️ MARTELAMENTO — Espada Longa de Aço       │
│                                              │
│  Progresso: [████████░░░░░░░░] 53%           │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │         ZONA DE IMPACTO                │  │
│  │                                        │  │
│  │    [  ] [  ] [  ] [  ] [  ] [  ] [  ] │  │
│  │    [  ] [  ] [🟢] [🟢] [🟢] [  ] [  ] │  │
│  │    [  ] [  ] [  ] [🟡] [  ] [  ] [  ] │  │
│  │    [  ] [  ] [  ] [  ] [  ] [  ] [  ] │  │
│  │                   ↑                    │  │
│  │              Cursor móvel               │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Marteladas: 12/20  Perfeitas: 8  Boas: 3   │
│  ↑ Score: ████████████░░░ 82%                │
│                                              │
│  💡 Clique quando o cursor estiver na zona!  │
└──────────────────────────────────────────────┘
```

#### Mecânica Detalhada

1. **Grid** de 7×4 slots na GUI
2. Um **cursor** (item com enchant glow) se move automaticamente por uma rota predefinida
3. **Zonas alvo** aparecem em posições diferentes a cada ciclo:
   - 🟢 **Verde** (centro) = Perfeito (+3 progresso, +score alto)
   - 🟡 **Amarelo** (borda) = Bom (+2 progresso, +score médio)
   - 🔴 **Vermelho** (se errar) = Ruim (+1 progresso, -score)
4. O jogador clica quando o cursor está sobre uma zona verde/amarela
5. **Velocidade do cursor** aumenta conforme:
   - Tier da receita (itens melhores = mais rápido)
   - Progresso (últimas marteladas são mais rápidas)
6. **Quantidade de marteladas** varia por receita (10 a 30)
7. **Feedback visual:** A cada acerto, partículas + som de martelada no mundo real

#### Fórmula de Score

```
hammeringScore = (perfectStrikes × 3 + goodStrikes × 1.5) / (totalStrikes × 3)
```

Bônus:
- 5+ perfeitas consecutivas: multiplicador ×1.1
- Todas perfeitas: multiplicador ×1.25
- Nível de ferreiro: +0.5% por nível de bonus ao score

### 7.2 Mini-Game: Têmpera (Quench Timing)

Determina a **durabilidade** e **resistência** do item.

#### Interface (Action Bar + BossBar)

Este mini-game acontece **no mundo**, não em uma GUI:

```
BossBar: [██████████████████░░░░░░░░░░░░] Temperatura: 1050°C ← Caindo!

Action Bar: "⚡ Mergulhe o metal no tanque quando a temperatura estiver na zona azul!"

Mundo:
- O jogador segura o metal aquecido (item no cursor, efeito de partículas de fogo)
- O tanque de água (cauldron) tem partículas de vapor subindo
- Clicar no cauldron = mergulhar o metal
```

#### Mecânica Detalhada

1. A **temperatura** cai constantemente após sair do forno (mostrada no BossBar)
2. Existe uma **zona ideal de têmpera** (faixa azul no BossBar) que depende do metal
3. O jogador deve **clicar com botão direito no tanque** quando a temp estiver na zona
4. A barra cai em velocidade variável (com micro-oscilações para dificultar)

```
Temperatura:
[████████████████████████████░░] 1200°C — Muito quente (vermelho)
[██████████████████████░░░░░░░░] 1000°C — Quente (laranja)  
[████████████████░░░░░░░░░░░░░░] 800°C  — ZONA IDEAL (azul) ← CLICAR AQUI
[██████████████░░░░░░░░░░░░░░░░] 700°C  — Morno (amarelo)
[████████░░░░░░░░░░░░░░░░░░░░░░] 400°C  — Frio demais (cinza) ← FALHA
```

**Score:**
- Centro da zona azul = 1.0
- Bordas da zona = 0.7
- Fora da zona mas aceitável = 0.3-0.5
- Frio demais = 0.1 (item frágil)

### 7.3 Mini-Game: Afiação/Polimento (Grindstone Precision)

Determina o **dano final** (armas) ou **armadura final** (armaduras) e o **acabamento visual**.

#### Interface (GUI 3 rows)

```
┌──────────────────────────────────────────────┐
│  🔪 AFIAÇÃO — Espada Longa de Aço            │
│                                              │
│  Pressão: [░░░░░░░░░|█████|░░░░░░░░░]       │
│              Fraco   IDEAL   Forte           │
│                                              │
│  ┌──────────────────────────────────┐        │
│  │  Segure CLICK para aplicar       │        │
│  │  pressão na pedra de amolar.     │        │
│  │                                  │        │
│  │  [  ] [  ] [🗡️] [  ] [  ]       │        │
│  │         Progresso da Lâmina      │        │
│  └──────────────────────────────────┘        │
│                                              │
│  Passes: 3/5   Qualidade: ████████░░ 78%     │
│                                              │
│  💡 Mantenha a pressão na zona verde!        │
└──────────────────────────────────────────────┘
```

#### Mecânica Detalhada

1. O jogador deve **manter clicado** (segurando click) em um slot específico
2. Enquanto segura, uma **barra de pressão** oscila automaticamente
3. A pressão tende a subir quando segurando — o jogador deve **soltar e segurar** ritmicamente
4. A **zona ideal** (verde) é uma faixa estreita no centro da barra
5. Cada "passe" (ciclo completo) adiciona ao score
6. Quantos passes são necessários depende do tipo de item

**Implementação técnica:**
- Detectar "segurar click" via `InventoryClickEvent` (tipo HOLDING) ou intervalo de clicks
- Barra de pressão atualiza a cada tick (item na GUI muda: lã verde/amarela/vermelha)
- Score calculado pela % do tempo que ficou na zona verde

### 7.4 Mini-Game Bônus: Gravação de Runa (Tier 4+)

Para itens de alto nível, um mini-game extra opcional:

1. Um **padrão de runa** aparece na GUI (grid 5×5)
2. O jogador deve **replicar o padrão** clicando nos slots corretos
3. Memória + precisão = bônus de stats mágicos ao item
4. Errar não penaliza o item base, apenas não adiciona bônus

### 7.5 Configuração de Dificuldade dos Mini-Games

```yaml
minigames:
  hammering:
    cursor_speed_base: 3          # Ticks por movimento (menor = mais rápido)
    cursor_speed_scaling: 0.95    # Multiplicador por tier da receita
    target_zones_min: 2           # Zonas alvo mínimas por ciclo
    target_zones_max: 4           # Zonas alvo máximas
    perfect_zone_size: 1          # Slots de zona perfeita
    good_zone_size: 2             # Slots de zona boa (ao redor da perfeita)
    strikes_per_recipe_level: 2   # Marteladas extras por nível da receita
    base_strikes: 10              # Marteladas base
    
  quenching:
    temperature_drop_rate: 15.0   # Graus por tick
    ideal_zone_width: 150         # Largura da zona ideal em graus
    ideal_zone_width_scaling: 0.9 # Multiplicador por tier da receita
    oscillation_amplitude: 5.0    # Amplitude das micro-oscilações
    
  sharpening:
    pressure_drift_speed: 0.05    # Velocidade do drift de pressão
    ideal_zone_width: 0.2         # 20% da barra = zona ideal
    passes_base: 3                # Passes base
    passes_per_recipe_level: 1    # Passes extras por nível
    
  rune_engraving:
    grid_size: 5                  # 5x5
    pattern_complexity_min: 5     # Mínimo de blocos no padrão
    pattern_complexity_max: 15    # Máximo de blocos
    display_time: 3000            # MS para memorizar o padrão
    max_attempts: 2               # Tentativas para acertar
```

---

## 8. Sistema de Qualidade

### 8.1 Qualidade do Item Final

A qualidade de cada item forjado é uma combinação de **materiais** + **habilidade** + **nível** + **ferramentas**.

#### Fórmula de Qualidade

```
qualityScore = (
    materialQuality    × 0.25 +    // Qualidade dos materiais (0.0 - 1.0)
    heatingScore       × 0.15 +    // Precisão no aquecimento (0.0 - 1.0)
    hammeringScore     × 0.30 +    // Performance no martelamento (0.0 - 1.0)
    quenchingScore     × 0.15 +    // Precisão na têmpera (0.0 - 1.0)
    sharpeningScore    × 0.15      // Precisão na afiação (0.0 - 1.0)
) × forgeTierMultiplier × professionLevelBonus × toolQualityBonus
```

#### Tiers de Qualidade

| Tier | Nome | Score Range | Cor | Modificador de Stats | Efeito Visual |
|------|------|-------------|-----|---------------------|---------------|
| 0 | Defeituoso | 0.00 - 0.19 | `<dark_gray>` | ×0.5 | Textura rachada |
| 1 | Inferior | 0.20 - 0.39 | `<gray>` | ×0.75 | Textura gasta |
| 2 | Comum | 0.40 - 0.59 | `<white>` | ×1.0 | Padrão |
| 3 | Superior | 0.60 - 0.79 | `<green>` | ×1.15 | Leve brilho |
| 4 | Excepcional | 0.80 - 0.94 | `<blue>` | ×1.30 | Brilho forte |
| 5 | Obra-Prima | 0.95 - 1.00 | `<gold>` | ×1.50 | Partículas + brilho |
| 6 | Lendário | 1.00 (perfeito) | `<light_purple>` | ×1.75 | Aura + som especial |

> ⚠️ **Lendário** só é possível com materiais perfeitos + score perfeito + nível 90+ + forja tier 5.

#### Exemplo de Lore do Item

```
⚔ Espada Longa de Aço Superior
Qualidade: ████████░░ Superior

Dano Físico: 45-58
Velocidade de Ataque: 1.2
Durabilidade: 1240/1240

Forjado por: RayMaster
Selo: ⚒ Ferreiro Nível 47
Data: 12/02/2026

"Uma lâmina bem temperada, com fio
preciso e equilíbrio admirável."
```

### 8.2 Qualidade dos Materiais

Os materiais usados na forja também têm qualidade que afeta o resultado:

```java
public enum MaterialGrade {
    IMPURE(0.4, "<dark_gray>Impuro"),    // Minério mal processado
    CRUDE(0.6, "<gray>Bruto"),            // Processamento básico  
    REFINED(0.8, "<white>Refinado"),       // Processamento padrão
    PURE(0.95, "<aqua>Puro"),              // Alta purificação
    PRISTINE(1.0, "<gold>Prístino");       // Perfeição (raro)
    
    private final double qualityMultiplier;
    private final String displayName;
}
```

A qualidade do material é determinada pelo **Minerador** que o extraiu (futura profissão), ou por drops de mobs/loot de dungeon com qualidade aleatória.

### 8.3 Impacto da Qualidade nos Stats

Para cada stat do item, a qualidade modifica o valor base da receita:

```java
// Exemplo: Espada Longa de Aço
// Receita define: damage = StatRange(40, 55)

double baseStat = recipe.getDamage().roll(); // Ex: 48 (entre 40-55)
double qualityMod = qualityTier.getModifier(); // Ex: 1.15 (Superior)
double finalStat = baseStat * qualityMod; // 48 * 1.15 = 55.2

// Arredondado para int: 55 de dano
```

---

## 9. Materiais e Ligas

### 9.1 Metais Base

| Metal | Tier | Temperatura Ideal | Nível Req. | Obtido de |
|-------|------|------------------|------------|-----------|
| Cobre | 1 | 700°C-900°C | 1 | Minério de cobre |
| Ferro | 1 | 900°C-1100°C | 1 | Minério de ferro |
| Aço | 2 | 1000°C-1200°C | 15 | Liga: Ferro + Carvão |
| Bronze | 2 | 800°C-1000°C | 10 | Liga: Cobre + Estanho |
| Aço Temperado | 3 | 1050°C-1250°C | 30 | Liga: Aço + Carbite |
| Prata | 3 | 650°C-850°C | 25 | Minério raro |
| Electrum | 3 | 750°C-950°C | 35 | Liga: Ouro + Prata |
| Aço Negro | 4 | 1200°C-1400°C | 50 | Liga: Aço + Obsidiana triturada |
| Mithril | 4 | 1200°C-1400°C | 70 | Minério de dungeon |
| Adamantina | 5 | 1400°C-1650°C | 85 | Drop de boss / minério ultra-raro |
| Draconita | 5 | 1600°C-1900°C | 95 | Drop de dragão + ritual |

### 9.2 Sistema de Ligas

Ligas são criadas **misturando metais na forja** em proporções específicas:

```yaml
alloys:
  steel:
    name: "Aço"
    components:
      iron_ingot: 3    # 3 partes de ferro
      coal: 1           # 1 parte de carvão
    result: steel_ingot
    result_amount: 2
    heating_time: 30     # segundos
    required_level: 15
    required_forge_tier: 1
    
  dark_steel:
    name: "Aço Negro"
    components:
      steel_ingot: 4
      obsidian_dust: 2
      dark_essence: 1
    result: dark_steel_ingot
    result_amount: 3
    heating_time: 60
    required_level: 50
    required_forge_tier: 3
```

O processo de criar ligas é feito no **forno da forja**:
1. Inserir componentes nas proporções corretas
2. Aquecer até temperatura de fusão da liga
3. Aguardar tempo de fusão
4. Liga é produzida automaticamente

### 9.3 Materiais Secundários

| Material | Uso | Obtido de |
|----------|-----|-----------|
| Couro | Empunhaduras | Coureiro / Caçador |
| Madeira (tipos) | Cabos, empunhaduras | Carpinteiro / Lenhador |
| Gemas brutas | Socket inserts (Joalheiro) | Minerador |
| Tinta de enchanter | Gravação de runas | Alquimista |
| Fios metálicos | Cota de malha, detalhes | Produzido na forja |
| Rebites | Junções de armadura | Produzido na forja |
| Essências elementais | Imbue elemental | Drop de mobs elementais |

---

## 10. Sistema de Níveis do Ferreiro

### 10.1 Progressão de XP

A XP é ganha por cada item forjado, baseada em:

```
xpGained = baseRecipeXP × qualityMultiplier × firstCraftBonus × difficultyBonus

Onde:
- baseRecipeXP: XP base da receita (configurável, ex: 50 para adaga, 500 para espada longa)
- qualityMultiplier: 
  - Defeituoso: ×0.25
  - Inferior: ×0.5
  - Comum: ×1.0
  - Superior: ×1.5
  - Excepcional: ×2.0
  - Obra-Prima: ×3.0
  - Lendário: ×5.0
- firstCraftBonus: ×2.0 se é a primeira vez que o jogador forja aquele item
- difficultyBonus: multiplicador se o item está próximo ao nível máximo do jogador
```

### 10.2 Tabela de XP por Nível

Fórmula de XP necessária: `xpRequired = baseXP × (level ^ exponent)`

```yaml
leveling:
  base_xp: 100        # XP para nível 1→2
  exponent: 1.8        # Curva de crescimento
  max_level: 100
  
  # XP necessária (exemplos):
  # Nível  1→2:    100 XP
  # Nível  5→6:    1,438 XP
  # Nível 10→11:   6,310 XP
  # Nível 25→26:   31,623 XP
  # Nível 50→51:   125,893 XP
  # Nível 75→76:   354,813 XP
  # Nível 99→100:  630,957 XP
```

### 10.3 Desbloqueios por Nível

Além dos bônus passivos (seção 3.3), cada nível desbloqueia receitas específicas. As receitas são agrupadas em "capítulos" na GUI:

```
📖 Capítulo 1: Fundamentos (Nível 1-9)
├── Adaga de Cobre
├── Adaga de Ferro
├── Espada Curta de Ferro
├── Elmo Simples de Ferro
└── Escudo Pequeno de Ferro

📖 Capítulo 2: Artesão (Nível 10-19)
├── Espada Longa de Ferro
├── Machado de Batalha de Ferro
├── Cota de Malha de Ferro
├── Picareta Reforçada
└── Espada de Bronze

📖 Capítulo 3: Proficiente (Nível 20-34)
├── Espada Longa de Aço
├── Machado de Guerra de Aço
├── Peitoral de Aço
├── Grevas de Aço
├── Alabarda de Bronze
└── ... (mais receitas)

📖 Capítulo 4: Especialista (Nível 35-54)
📖 Capítulo 5: Artífice (Nível 55-74)
📖 Capítulo 6: Mestre (Nível 75-94)
📖 Capítulo 7: Grandmaster (Nível 95-100)
```

### 10.4 Especialização (Nível 50+)

Ao atingir nível 50, o ferreiro pode escolher uma **especialização**:

| Especialização | Bônus | Foco |
|----------------|-------|------|
| **Armeiro** | +15% qualidade em armas, -5% em armaduras | Armas de todos os tipos |
| **Blindador** | +15% qualidade em armaduras, -5% em armas | Armaduras e escudos |
| **Artífice** | +10% qualidade geral, desbloqueia itens decorativos | Itens artísticos e únicos |
| **Forjador de Ligas** | +20% eficiência em ligas, novas receitas de liga | Metais e ligas especiais |

A especialização pode ser trocada por um custo alto (gold + cooldown de 14 dias).

---

## 11. Receitas de Forja

### 11.1 Estrutura da Receita

```java
public class ForgeRecipe {
    private String id;                     // "iron_longsword"
    private String displayName;            // "<white>Espada Longa de Ferro"
    private String nexoModelId;            // ID do modelo Nexo (ou null)
    private String resultItemId;           // ID do MidgardItem resultante
    private int requiredLevel;             // Nível mínimo de ferreiro
    private ForgeTier requiredForgeTier;   // Tier mínimo da forja
    
    // Materiais necessários
    private Map<String, Integer> materials; // itemId → quantidade
    
    // Metal principal (determina temperatura de aquecimento)
    private String primaryMetal;           // "steel_ingot"
    private int primaryMetalAmount;        // Ex: 5
    
    // Materiais secundários
    private Map<String, Integer> secondaryMaterials; // "leather" → 2, etc.
    
    // Config dos mini-games para esta receita
    private int hammerStrikes;             // Número de marteladas
    private int sharpeningPasses;          // Passes de afiação
    private double difficultyMultiplier;   // Multiplicador de dificuldade
    
    // Stats base do resultado
    private Map<ItemStat, StatRange> baseStats;
    
    // XP concedido
    private int baseXP;
    
    // Especialização que ganha bônus
    private String specialization;         // "weaponsmith", "armorsmith", etc.
    
    // Capítulo no grimório
    private int chapter;
    
    // Tempo base de aquecimento (segundos)
    private int heatingTime;
    
    // Se permite encantamento estrutural (tier 4+)
    private boolean allowsRuneEngraving;
    
    // Se permite sockets de gema (nível 60+)
    private int maxGemSockets;
}
```

### 11.2 Exemplo de Receita YAML

```yaml
recipes:
  steel_longsword:
    display_name: "<white>Espada Longa de Aço"
    result_item: "midgard_steel_longsword"
    required_level: 20
    required_forge_tier: 2
    chapter: 3
    
    primary_metal: "steel_ingot"
    primary_metal_amount: 5
    
    secondary_materials:
      leather_strip: 2
      oak_wood_plank: 1
      iron_rivet: 3
    
    difficulty: 1.2
    hammer_strikes: 18
    sharpening_passes: 4
    heating_time: 25
    
    base_stats:
      PHYSICAL_DAMAGE: "40-55"
      ATTACK_SPEED: "1.1-1.3"
      DURABILITY: "800-1200"
      CRITICAL_CHANCE: "3-8"
    
    base_xp: 250
    specialization: "weaponsmith"
    max_gem_sockets: 0
    allows_rune_engraving: false
    
    lore:
      - "<gray>Uma espada versátil forjada em"
      - "<gray>aço temperado. Preferida por"
      - "<gray>guerreiros por seu equilíbrio"
      - "<gray>entre alcance e velocidade."
```

---

## 12. Integração com Sistemas Existentes

### 12.1 Integração com midgard-item

- Receitas de forja produzem `MidgardItem` existentes
- Stats são aplicados via `ItemStat` e `StatRange` do sistema de itens
- Tiers/categorias do `TierManager` e `CategoryManager` são reutilizados
- Gem sockets usam o sistema existente de `GemListener`
- Durabilidade usa `MidgardItemDurability`

### 12.2 Integração com midgard-economy

- Algumas receitas podem custar gold além de materiais
- Reparação de itens na forja custa gold
- Ferreiros podem vender serviços para outros jogadores (sistema de comissão)
- Trade system: jogador encomenda item → ferreiro produz → pagamento automático

### 12.3 Integração com midgard-combat

- Stats das armas/armaduras forjadas alimentam o `CombatAttributes`
- Modificadores de `AttributeModifier` aplicados via `EquipListener`
- XP de combate pode dar bônus ao ferreiro que criou o item (opcional)

### 12.4 Integração com midgard-classes

- Classe RPG pode dar buffs à profissão (ex: Guerreiro tem +5% qualidade ao forjar armas que pode usar)
- Requisitos de classe para usar itens forjados (`MidgardItemRequirements`)

### 12.5 Integração com Nexo

- Cada item forjado pode ter modelo 3D via Nexo
- Modelos diferentes para cada qualidade (textura diferente)
- Forja multibloco usa modelos customizados para blocos decorativos
- Itens durante a forja são renderizados como display entities com modelos Nexo

### 12.6 Integração com WorldEdit

- Schematics da forja podem ser exportadas/importadas via WorldEdit
- Editor de schematics para admins usa API do WorldEdit
- Clipboard system para posicionar forjas pré-construídas (modo criativo)

### 12.7 Integração com FancyHolograms

- Nome da forja flutua acima da estrutura
- Indicadores de temperatura são hologramas
- Item em progresso tem hologramas de status
- Efeitos de conclusão usam hologramas animados

### 12.8 Integração com MythicMobs

- Materiais raros podem ser drops de MythicMobs
- Receitas lendárias podem requerer drops de bosses específicos
- Mobs podem ter armas forjadas configuradas

---

## 13. Arquitetura Técnica

### 13.1 Estrutura de Pacotes

```
me.ray.midgard.modules.item.forge/
├── ForgeManager.java                // Gerente principal
├── ForgeRegistry.java               // Registro de forjas ativas no mundo
│
├── structure/
│   ├── ForgeStructure.java          // Representa uma forja construída
│   ├── ForgeTier.java               // Enum/classe dos tiers
│   ├── ForgeSchematic.java          // Dados da schematic
│   ├── ForgeBlock.java              // Bloco individual da schematic
│   ├── ForgeRotation.java           // Rotações suportadas
│   ├── ForgeValidator.java          // Valida estrutura multibloco
│   └── ForgeProtection.java         // Proteção dos blocos
│
├── ghost/
│   ├── GhostBlockManager.java       // Gerencia blocos fantasma
│   ├── GhostBlockSession.java       // Sessão de visualização
│   ├── GhostBlockRenderer.java      // Renderização via packets
│   └── GhostBlockParticles.java     // Partículas indicadoras
│
├── session/
│   ├── ForgeSession.java            // Sessão de forja ativa
│   ├── ForgeSessionManager.java     // Gerencia sessões ativas
│   ├── ForgeStage.java              // Estágios da forja
│   └── ForgeResult.java             // Resultado da forja
│
├── recipe/
│   ├── ForgeRecipe.java             // Definição de receita
│   ├── ForgeRecipeManager.java      // Carrega e gerencia receitas
│   ├── ForgeRecipeRegistry.java     // Registro e busca
│   └── ForgeRecipeRequirement.java  // Requisitos dinâmicos
│
├── minigame/
│   ├── ForgeMinigame.java           // Interface base
│   ├── HammeringMinigame.java       // Mini-game de martelamento
│   ├── QuenchingMinigame.java       // Mini-game de têmpera
│   ├── SharpeningMinigame.java      // Mini-game de afiação
│   ├── RuneEngravingMinigame.java   // Mini-game de runas
│   └── MinigameResult.java          // Resultado do mini-game
│
├── quality/
│   ├── QualityCalculator.java       // Calcula qualidade final
│   ├── QualityTier.java             // Tiers de qualidade
│   ├── MaterialGrade.java           // Grades de material
│   └── QualityModifier.java         // Modificadores de qualidade
│
├── alloy/
│   ├── AlloyRecipe.java             // Receita de liga
│   ├── AlloyManager.java            // Gerencia ligas
│   └── AlloyProcess.java            // Processo de fusão
│
├── gui/
│   ├── ForgeMainGui.java            // GUI principal (mesa de trabalho)
│   ├── RecipeBookGui.java           // Grimório de receitas
│   ├── RecipeDetailGui.java         // Detalhe de uma receita
│   ├── HammeringGui.java            // GUI do martelamento
│   ├── SharpeningGui.java           // GUI da afiação
│   ├── RuneEngravingGui.java        // GUI da gravação de runas
│   ├── AlloyGui.java                // GUI de ligas
│   └── ForgeUpgradeGui.java         // GUI de upgrade da forja
│
├── listener/
│   ├── ForgeInteractListener.java   // Interação com blocos da forja
│   ├── ForgeBuildListener.java      // Construção da forja
│   ├── ForgeBreakListener.java      // Proteção contra quebra
│   └── ForgeSessionListener.java    // Eventos durante sessão
│
├── effect/
│   ├── ForgeEffectManager.java      // Gerencia efeitos visuais
│   ├── ForgeParticles.java          // Partículas do processo
│   ├── ForgeSounds.java             // Sons do processo
│   └── ForgeHolograms.java          // Hologramas de status
│
├── data/
│   ├── ForgeData.java               // ModuleData do jogador
│   ├── ForgeRepository.java         // Persistência de forjas
│   └── ForgeStatistics.java         // Estatísticas do jogador
│
└── command/
    ├── ForgeCommand.java            // Comando principal
    ├── ForgeAdminCommand.java       // Comandos administrativos
    └── ForgeDebugCommand.java       // Debug e teste
```

### 13.2 Diagrama de Fluxo Técnico

```
                    ┌──────────────────┐
                    │  ForgeManager    │
                    │  (Singleton)     │
                    └────────┬─────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────▼──────┐ ┌──────▼───────┐ ┌─────▼──────────┐
    │ ForgeRegistry │ │ RecipeManager│ │ SessionManager │
    │              │ │              │ │                │
    │ Forjas ativas│ │ Receitas DB  │ │ Sessões ativas│
    │ no mundo     │ │ + cache      │ │ por jogador   │
    └───────┬──────┘ └──────┬───────┘ └────────┬───────┘
            │                │                  │
            │         ┌──────▼───────┐   ┌─────▼───────┐
            │         │ QualityCalc  │   │ Minigames   │
            │         │              │   │             │
            │         │ Material +   │   │ Hammering   │
            │         │ Skill score  │   │ Quenching   │
            │         │ → Quality    │   │ Sharpening  │
            │         └──────────────┘   │ RuneEngrave │
            │                            └─────────────┘
    ┌───────▼──────────┐
    │ GhostBlockManager│
    │                  │
    │ ProtocolLib      │
    │ packets +        │
    │ particles        │
    └──────────────────┘
```

### 13.3 Thread Safety e Folia Compatibility

Dado que o projeto usa **Folia API**, todas as manipulações de mundo devem ser region-aware:

```java
// CORRETO: Usar region scheduler do Folia
Bukkit.getRegionScheduler().execute(plugin, location, () -> {
    // Manipulação de blocos aqui
});

// Para GUIs e packets: usar entity scheduler
entity.getScheduler().execute(plugin, () -> {
    // Manipulação de GUI/packets
}, null, 0L);

// Tasks periódicas dos mini-games: usar async com sync callbacks
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
    // Calcular nova posição do cursor
    // Enviar update via region scheduler
    Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
        updateMinigameDisplay(player);
    });
}, 0, 50, TimeUnit.MILLISECONDS);
```

### 13.4 Caching Strategy

```java
// Forjas ativas: Caffeine cache com invalidação por região
private final Cache<ChunkKey, List<ForgeStructure>> forgesByChunk = 
    Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

// Receitas: Cache permanente (reload on command)
private final Map<String, ForgeRecipe> recipeCache = new ConcurrentHashMap<>();

// Sessões: Map direto (limpo quando sessão termina)
private final Map<UUID, ForgeSession> activeSessions = new ConcurrentHashMap<>();
```

---

## 14. Schema do Banco de Dados

### 14.1 Tabelas

```sql
-- Forjas construídas no mundo
CREATE TABLE midgard_forges (
    id VARCHAR(36) PRIMARY KEY,           -- UUID
    owner_uuid VARCHAR(36) NOT NULL,       -- UUID do dono
    world VARCHAR(64) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    tier INT NOT NULL DEFAULT 1,
    rotation INT NOT NULL DEFAULT 0,       -- 0, 90, 180, 270
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP,
    total_items_forged INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_owner (owner_uuid),
    INDEX idx_location (world, x, y, z)
);

-- Dados de profissão do jogador
CREATE TABLE midgard_profession_data (
    player_uuid VARCHAR(36) PRIMARY KEY,
    primary_profession VARCHAR(32),
    secondary_profession VARCHAR(32),
    data JSON NOT NULL,                     -- ProfessionData serializada
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Níveis e XP de profissão
CREATE TABLE midgard_profession_levels (
    player_uuid VARCHAR(36) NOT NULL,
    profession_id VARCHAR(32) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    experience DOUBLE NOT NULL DEFAULT 0,
    specialization VARCHAR(32),
    total_items_crafted INT DEFAULT 0,
    PRIMARY KEY (player_uuid, profession_id),
    INDEX idx_player (player_uuid),
    INDEX idx_profession (profession_id)
);

-- Receitas desbloqueadas
CREATE TABLE midgard_unlocked_recipes (
    player_uuid VARCHAR(36) NOT NULL,
    profession_id VARCHAR(32) NOT NULL,
    recipe_id VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    times_crafted INT DEFAULT 0,
    best_quality DOUBLE DEFAULT 0,
    PRIMARY KEY (player_uuid, profession_id, recipe_id),
    INDEX idx_player_prof (player_uuid, profession_id)
);

-- Histórico de itens forjados (para analytics e tracking)
CREATE TABLE midgard_forge_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    forge_id VARCHAR(36) NOT NULL,
    recipe_id VARCHAR(64) NOT NULL,
    quality_score DOUBLE NOT NULL,
    quality_tier INT NOT NULL,
    material_grade VARCHAR(16),
    heating_score DOUBLE,
    hammering_score DOUBLE,
    quenching_score DOUBLE,
    sharpening_score DOUBLE,
    xp_gained DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player (player_uuid),
    INDEX idx_recipe (recipe_id),
    INDEX idx_date (created_at)
);

-- Receitas de forja (definições do sistema)
CREATE TABLE midgard_forge_recipes (
    id VARCHAR(64) PRIMARY KEY,
    data JSON NOT NULL,                     -- ForgeRecipe serializada
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Schematics de forja (definições do sistema)
CREATE TABLE midgard_forge_schematics (
    tier INT PRIMARY KEY,
    data JSON NOT NULL,                     -- ForgeSchematic serializada
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Estatísticas do ferreiro
CREATE TABLE midgard_blacksmith_stats (
    player_uuid VARCHAR(36) PRIMARY KEY,
    total_forged INT DEFAULT 0,
    masterpieces_forged INT DEFAULT 0,
    legendary_forged INT DEFAULT 0,
    items_repaired INT DEFAULT 0,
    alloys_created INT DEFAULT 0,
    perfect_minigames INT DEFAULT 0,        -- Mini-games com score perfeito
    failed_forges INT DEFAULT 0,
    favorite_recipe VARCHAR(64),
    highest_quality DOUBLE DEFAULT 0,
    total_xp_earned DOUBLE DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 14.2 Sincronização Cross-Server (Redis)

```java
// Canal Redis para sincronização de forjas entre servidores
public class ForgeSyncManager {
    private static final String CHANNEL_FORGE_UPDATE = "midgard:forge:update";
    private static final String CHANNEL_FORGE_DELETE = "midgard:forge:delete";
    private static final String CHANNEL_RECIPE_RELOAD = "midgard:forge:recipe:reload";
    
    // Quando uma forja é construída/destruída, publicar no Redis
    // Outros servidores atualizam seu cache local
}
```

---

## 15. Comandos e Permissões

### 15.1 Comandos do Jogador

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/forge` | Abre informações da profissão | `midgard.forge.use` |
| `/forge recipes` | Lista receitas desbloqueadas | `midgard.forge.use` |
| `/forge stats` | Estatísticas do ferreiro | `midgard.forge.use` |
| `/forge specialize <spec>` | Escolhe especialização (nível 50+) | `midgard.forge.use` |
| `/forge locate` | Mostra localização das suas forjas | `midgard.forge.use` |

### 15.2 Comandos Administrativos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/forgeadmin reload` | Recarrega configurações | `midgard.forge.admin` |
| `/forgeadmin setlevel <player> <level>` | Define nível de ferreiro | `midgard.forge.admin` |
| `/forgeadmin addxp <player> <amount>` | Adiciona XP | `midgard.forge.admin` |
| `/forgeadmin give blueprint <player> <tier>` | Dá planta de forja | `midgard.forge.admin` |
| `/forgeadmin recipe create` | Abre editor de receitas | `midgard.forge.admin` |
| `/forgeadmin recipe list` | Lista todas as receitas | `midgard.forge.admin` |
| `/forgeadmin schematic edit <tier>` | Edita schematic de um tier | `midgard.forge.admin` |
| `/forgeadmin forges list [player]` | Lista forjas ativas | `midgard.forge.admin` |
| `/forgeadmin forges remove <id>` | Remove uma forja | `midgard.forge.admin` |
| `/forgeadmin simulate <recipe>` | Simula uma forja (debug) | `midgard.forge.admin` |
| `/forgeadmin quality <score>` | Testa resultado de qualidade | `midgard.forge.admin` |

### 15.3 Permissões

```yaml
permissions:
  midgard.forge.use:
    description: "Permite usar o sistema de forja"
    default: true
  midgard.forge.build:
    description: "Permite construir forjas"
    default: true
  midgard.forge.admin:
    description: "Acesso administrativo ao sistema de forja"
    default: op
  midgard.forge.bypass.cooldown:
    description: "Ignora cooldowns de forja"
    default: op
  midgard.forge.bypass.level:
    description: "Ignora requisitos de nível"
    default: op
```

---

## 16. Configuração YAML

### 16.1 Config Principal (`modules/forge/config.yml`)

```yaml
# ═══════════════════════════════════════════
#         MIDGARD FORGE SYSTEM CONFIG
# ═══════════════════════════════════════════

general:
  enabled: true
  max_forges_per_player: 3          # Máximo de forjas simultâneas
  max_forges_per_tier: 1            # Máximo de forjas por tier por jogador
  session_timeout: 600              # Timeout da sessão em segundos (10 min)
  require_profession: true          # Se true, precisa ser ferreiro para usar
  protect_forge_blocks: true        # Proteger blocos da forja
  explosion_protection: true        # Proteger contra explosões
  allow_shared_forges: false        # Se true, outros jogadores podem usar suas forjas

ghost_blocks:
  enabled: true
  particle_interval: 10             # Ticks entre partículas
  correct_block_color: "GREEN"      # Cor do contorno para blocos corretos
  missing_block_color: "RED"        # Cor para blocos faltando
  wrong_block_color: "YELLOW"       # Cor para blocos errados
  render_distance: 32               # Distância máxima de renderização

heating:
  base_heat_rate: 5.0               # Graus por tick base
  fuel_efficiency:
    coal: 1.0
    charcoal: 0.8
    lava_bucket: 2.0
    blaze_rod: 1.5
    special_coal: 3.0               # Carvão especial (craftado)
  overheat_penalty: 0.5             # Multiplicador de qualidade se superaquecer
  melt_threshold: 200               # Graus acima do máximo = derrete

quality:
  weights:
    material: 0.25
    heating: 0.15
    hammering: 0.30
    quenching: 0.15
    sharpening: 0.15
  
  tier_thresholds:
    defective: 0.0
    inferior: 0.20
    common: 0.40
    superior: 0.60
    exceptional: 0.80
    masterpiece: 0.95
    legendary: 1.00
  
  tier_stat_multipliers:
    defective: 0.50
    inferior: 0.75
    common: 1.00
    superior: 1.15
    exceptional: 1.30
    masterpiece: 1.50
    legendary: 1.75
  
  profession_level_bonus: 0.005     # +0.5% ao score por nível de ferreiro

leveling:
  base_xp: 100
  exponent: 1.8
  max_level: 100
  quality_xp_multipliers:
    defective: 0.25
    inferior: 0.50
    common: 1.00
    superior: 1.50
    exceptional: 2.00
    masterpiece: 3.00
    legendary: 5.00
  first_craft_bonus: 2.0

effects:
  particles: true
  sounds: true
  holograms: true
  camera_shake: false               # Shake na tela ao martelar (packets)
  
sounds:
  hammer_hit: "block.anvil.use"
  hammer_perfect: "entity.experience_orb.pickup"
  quench_sizzle: "block.lava.extinguish"
  forge_complete: "ui.toast.challenge_complete"
  forge_fail: "block.anvil.destroy"
  heating_ambient: "block.furnace.fire_crackle"
  sharpening: "entity.iron_golem.attack"

messages:
  prefix: "<gray>[<gradient:#FF6B00:#FFD700>⚒ Forja</gradient><gray>]</gray>"
  forge_activated: "<green>Sua forja foi ativada com sucesso!"
  forge_deactivated: "<red>Sua forja foi desativada. Repare os blocos danificados."
  session_started: "<yellow>Sessão de forja iniciada. Bom trabalho, ferreiro!"
  session_timeout: "<red>Sua sessão de forja expirou. Os materiais foram perdidos."
  not_blacksmith: "<red>Apenas ferreiros podem usar esta forja."
  level_too_low: "<red>Você precisa ser nível <level> para forjar este item."
  forge_tier_too_low: "<red>Esta forja não é avançada o suficiente para esta receita."
  quality_result: "<gray>Qualidade: <quality_color><quality_name> (<score>%)"
  xp_gained: "<yellow>+<xp> XP de Ferreiro"
  level_up: "<gold><bold>⚒ NÍVEL UP!</bold> <yellow>Ferreiro nível <level>!"
  new_recipe_unlocked: "<green>Nova receita desbloqueada: <white><recipe>"
```

---

## 17. Efeitos Visuais e Sonoros

### 17.1 Mapa de Efeitos por Etapa

| Etapa | Partículas | Sons | Hologramas | Outros |
|-------|-----------|------|------------|--------|
| Construção | Contorno de blocos fantasma | — | — | Ghost blocks via packets |
| Ativação | TOTEM, FIREWORK_SPARK | TOTEM_OF_UNDYING | Nome da forja aparece | Enchant glow nos blocos |
| Aquecimento | FLAME, LAVA (no forno) | FURNACE_CRACKLE (loop) | Temperatura | Bloco do forno aceso |
| Martelamento | CRIT (na bigorna) | ANVIL_USE (rítmico) | Progresso | Item visual sobre bigorna |
| Têmpera | CLOUD, DRIP_WATER | LAVA_EXTINGUISH | Temperatura caindo | Vapor subindo do caldeirão |
| Afiação | DUST (faíscas) | IRON_GOLEM_ATTACK | Pressão | — |
| Conclusão | TOTEM + ENCHANTMENT | CHALLENGE_COMPLETE | Qualidade | Item flutua e gira |
| Falha | SMOKE_LARGE | ANVIL_DESTROY | "FALHOU" (vermelho) | Metal rachado quebra |

### 17.2 Efeitos Ambientais da Forja

Quando uma forja está **ativa** (com sessão em progresso), ela emite efeitos ambientais:

```yaml
ambient_effects:
  # Fumaça saindo da chaminé
  chimney_smoke:
    particle: CAMPFIRE_COSY_SMOKE
    interval: 5      # ticks
    count: 3
    spread: 0.3
    
  # Calor distorcendo o ar (acima do forno)
  heat_shimmer:
    particle: DUST_COLOR_TRANSITION
    interval: 10
    count: 5
    color_start: "#FF4400"
    color_end: "#FFAA00"
    
  # Brasas no chão ao redor
  embers:
    particle: FALLING_LAVA
    interval: 20
    count: 1
    radius: 2.0
    
  # Som ambiente
  ambient_sound:
    sound: "block.furnace.fire_crackle"
    interval: 40     # ticks
    volume: 0.5
    pitch: 0.8
```

### 17.3 Efeitos de Qualidade na Conclusão

| Qualidade | Efeito Especial |
|-----------|----------------|
| Defeituoso | Item cai no chão, partículas de fumaça, som de metal rachando |
| Inferior | Item aparece sem efeito especial |
| Comum | Pequeno flash de luz |
| Superior | Flash de luz verde + partículas HAPPY_VILLAGER |
| Excepcional | Espiral de partículas azuis + som de encantamento |
| Obra-Prima | Explosão de partículas douradas + som de TOTEM + título temporário na tela |
| Lendário | Raio de luz do céu + TOTEM completo + som épico + anúncio no chat do servidor |

---

## 18. Fases de Implementação

### Fase 1 — Fundação (Sprint 1-2)
> **Foco:** Infraestrutura base e forja funcional mínima

- [ ] Criar pacote `forge` dentro de `midgard-item`
- [ ] Implementar `ForgeSchematic` e parser YAML
- [ ] Implementar `GhostBlockManager` com ProtocolLib
- [ ] Implementar `ForgeStructure` e validação multibloco
- [ ] Implementar `ForgeRegistry` com persistência em DB
- [ ] Implementar `ForgeBuildListener` (construção guiada)
- [ ] Implementar `ForgeBreakListener` (proteção)
- [ ] Criar schematic da Forja Básica (Tier 1)
- [ ] Testes unitários para validação de estrutura
- [ ] Testes de integração para ghost blocks

### Fase 2 — Processo de Forja (Sprint 3-4)
> **Foco:** Sessão de forja e mini-games core

- [ ] Implementar `ForgeSession` e `ForgeSessionManager`
- [ ] Implementar `ForgeRecipe` e `ForgeRecipeManager`
- [ ] Criar `ForgeMainGui` (mesa de trabalho)
- [ ] Criar `RecipeBookGui` com paginação
- [ ] Implementar sistema de aquecimento com BossBar
- [ ] Implementar `HammeringMinigame` (GUI interativa)
- [ ] Implementar `QuenchingMinigame` (mundo + BossBar)
- [ ] Implementar `SharpeningMinigame` (GUI interativa)
- [ ] Implementar `QualityCalculator`
- [ ] Implementar geração do item final com qualidade
- [ ] Testes unitários para mini-games e qualidade
- [ ] 10-15 receitas iniciais (ferro e bronze)

### Fase 3 — Profissão e Progressão (Sprint 5-6)
> **Foco:** Sistema de níveis, XP e desbloqueios

- [ ] Implementar `ProfessionData` como ModuleData
- [ ] Implementar sistema de nível e XP do ferreiro
- [ ] Implementar desbloqueio de receitas por nível
- [ ] Criar GUI de informações da profissão
- [ ] Implementar bônus passivos por nível
- [ ] Implementar sistema de especialização (nível 50+)
- [ ] Criar 30+ receitas (cobrindo todos os capítulos)
- [ ] Implementar estatísticas do ferreiro
- [ ] Persistência no banco de dados
- [ ] Sincronização Redis cross-server
- [ ] Comandos de jogador (`/forge`, `/forge recipes`, etc.)

### Fase 4 — Tiers Avançados e Ligas (Sprint 7-8)
> **Foco:** Forjas tier 2-5, sistema de ligas e materiais avançados

- [ ] Criar schematics para Forja Intermediária (Tier 2)
- [ ] Criar schematics para Forja Avançada (Tier 3)
- [ ] Criar schematics para Forja de Mestre (Tier 4)
- [ ] Criar schematic para Forja Lendária (Tier 5)
- [ ] Implementar `AlloyManager` e sistema de ligas
- [ ] Implementar MaterialGrade e qualidade de materiais
- [ ] Implementar `RuneEngravingMinigame`
- [ ] Implementar sockets de gema na forja
- [ ] 50+ receitas cobrindo todos os metais e ligas
- [ ] Balanceamento de dificuldade dos mini-games

### Fase 5 — Polimento e Integração (Sprint 9-10)
> **Foco:** Efeitos visuais, sons, hologramas e integração completa

- [ ] Implementar todos os efeitos visuais (partículas)
- [ ] Implementar todos os efeitos sonoros
- [ ] Integração com FancyHolograms (labels e indicadores)
- [ ] Integração com Nexo (modelos 3D)
- [ ] Display entities para itens em progresso
- [ ] Efeitos ambientais da forja ativa
- [ ] Efeitos de conclusão por qualidade
- [ ] Sistema de reparação de itens
- [ ] Comandos administrativos completos
- [ ] Editor de receitas in-game
- [ ] Editor de schematics in-game

### Fase 6 — Economia e Social (Sprint 11-12)
> **Foco:** Integração econômica e interdependência social

- [ ] Sistema de comissão (jogador encomenda → ferreiro produz)
- [ ] Integração com midgard-economy (custos, vendas)
- [ ] Sistema de reputação do ferreiro (rating por clientes)
- [ ] Placa de "Ferreiro Disponível" (FancyHolograms)
- [ ] Histórico de forja visível para outros jogadores
- [ ] Ranking de ferreiros no servidor
- [ ] Achievements/conquistas de ferreiro
- [ ] Selo do ferreiro em itens (metadata + lore)
- [ ] Testes de carga e performance
- [ ] Documentação final para admins

---

## 19. Considerações de Performance

### 19.1 Pontos Críticos

| Componente | Risco | Mitigação |
|------------|-------|-----------|
| Ghost Blocks | Packets por tick podem sobrecarregar | Limitar updates a 20 ticks, usar batch packets |
| Partículas | Muitas partículas simultâneas | Pool de partículas por forja, LOD por distância |
| Mini-game ticks | GUI updates a cada 2-5 ticks | Async calculation, sync GUI update mínimo |
| DB queries | Saves frequentes durante sessão | Buffer writes, save apenas no final da sessão |
| Validação multibloco | Verificar muitos blocos | Cache de estado da estrutura, validar incrementalmente |

### 19.2 Métricas Alvo

| Métrica | Alvo |
|---------|------|
| TPS impact (1 forja ativa) | < 0.1 ms/tick |
| TPS impact (10 forjas ativas) | < 0.8 ms/tick |
| Memória por forja registrada | < 2 KB |
| Memória por sessão ativa | < 50 KB |
| Tempo de validação multibloco | < 5 ms |
| Tempo de cálculo de qualidade | < 1 ms |
| Ghost block packet batch | < 2 ms para 200 blocos |

### 19.3 Profiling

Integrar com `MidgardProfiler` existente:

```java
profiler.start("forge.minigame.hammering.tick");
// ... lógica do tick
profiler.stop("forge.minigame.hammering.tick");
```

---

## 20. Referências e Inspiração

### Jogos de Referência
- **Skyrim** — Forja com materiais e melhoria em bancadas
- **New World** — Crafting interativo com mini-games de timing
- **Final Fantasy XIV** — Crafting complexo com rotação de habilidades
- **Valheim** — Estações de trabalho com upgrades incrementais
- **Runescape** — Smithing com barras e forja progressiva
- **Dark Souls** — Upgrade de armas com materiais raros + ferreiro NPC
- **Monster Hunter** — Árvore de crafting com materiais de monstros
- **Minecraft: Create Mod** — Multiblock structures e processamento multi-etapa

### Plugins de Referência
- **QualityCrafting** — Conceito de qualidade em items craftados
- **CustomCrafting** — Receitas customizadas
- **MMOItems Crafting** — Estações de crafting customizadas
- **SlimeFun** — Multiblock crafting (conceito básico)

### Design Decisions

| Decisão | Escolha | Razão |
|---------|---------|-------|
| Mini-games em GUI vs Mundo | **Híbrido** | Martelamento e afiação em GUI (mais controle); têmpera no mundo (mais imersivo) |
| Receitas em DB vs YAML | **DB com migração de YAML** | Consistência com padrão existente (DefinitionRepository) |
| Qualidade fixa vs range | **Range com pesos** | Mais dinâmico e recompensa habilidade |
| Profissão como módulo separado | **Iniciar em midgard-item, extrair depois** | Evita over-engineering prematuro; o sistema de forja é o MVP |
| Blocos fantasma via ProtocolLib | **Sim** | Não altera o mundo real, cada jogador vê independentemente |

---

> **Nota Final:** Este documento é um guia vivo de desenvolvimento. Cada fase deve ser revisada e ajustada com base no feedback de playtesting. O balanceamento de mini-games, qualidade e progressão de nível exigirá iteração significativa com jogadores reais.

---

*Documento criado para o projeto MidgardRPG — Sistema de Forja Multibloco v1.0*
