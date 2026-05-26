# ⚔️ Midgard-Item Module Documentation

## 📋 Visão Geral
O módulo **Midgard-Item** é responsável pelo gerenciamento de itens customizados no MidgardRPG. Ele permite a criação de itens com atributos, mecânicas, crafting e comportamentos específicos que vão além das capacidades padrão do Minecraft.

O sistema é altamente modular, dividindo as propriedades do item em componentes (Display, Mechanics, Crafting, etc.) e suporta um editor in-game completo.

---

## 🏗️ Arquitetura

### Classes Principais

*   **`ItemModule`**: Classe principal do módulo. Inicializa gerenciadores (`ItemManager`, `CategoryManager`) e registra listeners e tarefas.
*   **`ItemManager`**: Responsável por carregar itens dos arquivos YAML, mantê-los em memória e fornecer métodos de acesso (`getItem`, `registerItem`).
*   **`MidgardItem` (Interface) / `MidgardItemImpl` (Implementação)**: Representa um item customizado. Em vez de uma classe monolítica, ele delega funcionalidades para subcomponentes:
    *   `MidgardItemGeneral`: Propriedades básicas (Material, Nome, Revisão).
    *   `MidgardItemDisplay`: Aparência (ModelData, Lore, Cores).
    *   `MidgardItemMechanics`: Atributos de RPG (Dano, Defesa, Stats), Gemas, etc.
    *   `MidgardItemCrafting`: Receitas de criação.
    *   `MidgardItemRequirements`: Requisitos de uso (Nível, Classe).
    *   `MidgardItemRestrictions`: Restrições (Não dropável, Não trocável).
    *   `MidgardItemDurability`: Sistema de durabilidade customizada.
    *   `MidgardItemUpdaterOptions`: Opções de atualização automática de itens antigos.

### Fluxo de Dados
1.  **Carregamento**: Ao iniciar, o `ItemManager` varre a pasta `resources/modules/item/` e subpastas.
2.  **Identificação**: Cada arquivo YAML define um item. O nome do arquivo (sem extensão) ou uma chave interna define o ID do item.
3.  **Construção**: Quando um item é solicitado (`build()`), o `MidgardItemBuilder` cria um `ItemStack` do Bukkit e aplica NBT tags persistentes (PDC) para armazenar o ID do item e dados dinâmicos (stats, durabilidade).

---

## ⚙️ Configuração (YAML)

Os itens são definidos em arquivos YAML. A estrutura é flexível, mas geralmente segue este padrão:

```yaml
example_sword:
  base:
    material: DIAMOND_SWORD
    name: '<red>Espada de Fogo' # Suporte a MiniMessage
    type: SWORD
    revision-id: 1
    
    # Componente de Exibição
    display:
      lore:
        - '<gray>Uma espada lendária.'
      custom-model-data: 1001

    # Componente de Mecânicas (Stats de RPG)
    mechanics:
      stats:
        attack_damage: 10-15
        critical_chance: 5
      
      # Slots para gemas
      gem_sockets:
        - 'RED'
        - 'BLUE'

    # Componente de Crafting
    crafting:
      enabled: true
      recipe:
        - ' D '
        - ' D '
        - ' S '
      ingredients:
        D: DIAMOND
        S: STICK

    # Requisitos
    requirements:
      level: 10
      class: 'WARRIOR'
```

### Categorias
Os itens são organizados em categorias (pastas) que definem comportamentos padrão ou stats base. Exemplos: `ARMOR`, `WEAPON`, `CONSUMABLE`, `GEM_STONE`.

As categorias são definidas em `item-types.yml`. Cada categoria pode ter um ícone e um `display-item`.
*   **Display Item**: Quando um novo item é criado em uma categoria, ele se torna automaticamente o "Display Item" dessa categoria no navegador de tipos, garantindo que o ícone da categoria sempre reflita o item mais recente ou relevante.

---

## ✨ Funcionalidades Principais

### 1. Editor In-Game
O módulo possui um sistema robusto de GUIs para criar e editar itens dentro do jogo.
*   **Comando**: `/midgard item` para abrir o navegador.
*   Permite editar lore, stats, flags, receitas e muito mais sem tocar nos arquivos de configuração.

### 2. Integração com Nexo
O módulo utiliza a API do **Nexo** para gerenciamento de recursos visuais (CustomModelData, texturas).
*   Substitui o antigo suporte ao ItemsAdder.
*   Permite carregar itens baseados em IDs do Nexo.

### 3. Stats Customizados
O sistema suporta atributos arbitrários (`ItemStat`).
*   Stats podem ser faixas de valores (`10-15`) ou valores fixos.
*   Ao gerar o item, os valores são rolados (RNG) e salvos no NBT do item.

### 4. Atualização de Equipamentos (`EquipmentUpdateTask`)
Uma tarefa roda periodicamente (`EquipmentUpdateTask`) para verificar os itens equipados pelos jogadores.
*   Ela aplica atributos ao jogador (via `AttributeModifier` do Core) baseados nos itens que ele está usando.
*   Gerencia sets de itens e bônus de conjunto.

### 5. Sistema de Gemas (Sockets)
Itens podem ter "sockets" onde "Gems" (outro tipo de item) podem ser inseridas para adicionar stats.

---

## 🛠️ Comandos

*   `/midgard item`: Abre o navegador de itens (GUI principal).
*   `/midgard item give <player> <id> [amount]`: Dá um item a um jogador.
*   `/midgard item reload`: Recarrega as configurações de itens e categorias.

---

## 📦 Dependências

*   **Midgard-Core**: Para utilitários, sistema de atributos e GUI base.
*   **Nexo**: Para recursos visuais e custom items.
*   **Midgard-Classes** (Opcional): Para requisitos de classe.
*   **Vault** (Opcional): Para custos em economia.
