# Sistema de Importação de Pacotes MMOCore

O MidgardRPG possui um sistema de importação que permite converter pacotes criados para o MMOCore e plugins relacionados para o formato nativo do Midgard.

## Estrutura Suportada

O importador reconhece pacotes com a seguinte estrutura:

```
pacote/
├── MMOCore/
│   ├── classes/
│   │   └── *.yml           # Classes do jogo
│   ├── skills/
│   │   └── *.yml           # Definição de skills (nome, lore, stats)
│   └── gui/
│       └── *.yml           # (não importado ainda)
├── MythicLib/
│   └── skill/
│       └── *.yml           # Mapeamento para MythicMobs skills
├── MythicMobs/
│   ├── skills/
│   │   └── *.yml           # Skills do MythicMobs (mecânicas)
│   ├── mobs/
│   │   └── *.yml           # Mobs/VFX do MythicMobs
│   └── items/
│       └── *.yml           # (não importado diretamente)
├── MMOItems/
│   └── item/
│       └── *.yml           # Itens com abilities vinculadas
├── Nexo/
│   └── items/
│       └── *.yml           # (não importado ainda)
└── ModelEngine/
    └── blueprints/
        └── *.bbmodel        # (não importado - manual)
```

## Como Usar

### 1. Colocar o Pacote na Pasta de Imports

Copie a pasta do pacote para:
```
plugins/MidgardRPG/imports/<nome_do_pacote>/
```

Exemplo:
```
plugins/MidgardRPG/imports/archer_pack/
├── MMOCore/
├── MythicLib/
├── MythicMobs/
└── MMOItems/
```

### 2. Executar o Comando de Importação

```
/midgard import mmocore <nome_do_pacote>
```

Exemplo:
```
/midgard import mmocore archer_pack
```

### 3. Recarregar os Plugins

Após a importação:
```
/midgard reload
/mm reload
```

## O Que é Importado

### Classes (MMOCore/classes/)
- `display-name`: Nome da classe
- `icon`: Material e custom model data
- `lore`: Descrição
- `max-level`: Nível máximo
- `skills`: Lista de magias disponíveis para a classe

**Saída:** `modules/classes/classes/<classe>.yml`

### Spells (MMOCore/skills/ + MythicLib/skill/)
- `name`: Nome da magia
- `description`: Descrição (com placeholders)
- `icon`: Material e custom model data
- `mythic-skill`: ID da skill no MythicMobs
- `cooldown`: Base + per-level
- `mana`: Custo de mana
- `variables.damage`: Dano base + per-level

**Saída:** `modules/spells/spells/<spell>.yml`

### Itens (MMOItems/item/)
- `material`: Material base
- `model-data`: Custom model data
- `name`: Nome do item
- `description`: Descrição
- `mechanics.stats`: Attack damage, speed, etc.
- `mechanics.spell-bindings`: Magias vinculadas ao item

**Saída:** `modules/item/imported/<item>.yml`

### MythicMobs Skills
As skills do MythicMobs são **copiadas diretamente** para:
```
plugins/MythicMobs/Skills/
```

## Conversões Automáticas

### Cores
Códigos legados são convertidos para MiniMessage:
- `&a` → `<green>`
- `&c` → `<red>`
- `&l` → `<bold>`
- etc.

### Modos de Ability
Modos do MMOItems são convertidos para triggers do Midgard:
- `LEFT_CLICK` → `LEFT_CLICK`
- `RIGHT_CLICK` → `RIGHT_CLICK`
- `SHIFT_LEFT_CLICK` → `SHIFT_LEFT_CLICK`
- `TIMER` → `PASSIVE_TIMER`

## Limitações

1. **ModelEngine**: Blueprints `.bbmodel` devem ser copiados manualmente para `plugins/ModelEngine/blueprints/`

2. **Nexo Items**: Itens do Nexo ainda não são importados automaticamente. Configure manualmente se necessário.

3. **Resource Packs**: Texturas e modelos custom devem ser configurados separadamente no Nexo ou server resource pack.

4. **Sounds**: Sons custom devem ser adicionados ao resource pack e configurados manualmente.

## Exemplo de Pacote Importado

### Antes (MMOCore)
```yaml
# MMOCore/classes/archer.yml
display:
    name: '&2&lArcher Class'
    item: STONE_SWORD:4577
skills:
    ARCHER_QUICKSHOT:
        level: 1
        max-level: 30
```

### Depois (MidgardRPG)
```yaml
# modules/classes/classes/archer.yml
display-name: '<dark_green><bold>Archer Class</bold></dark_green>'
icon: STONE_SWORD
model-data: 4577
spells:
  - id: archer_quickshot
    unlock-level: 1
    max-level: 30
```

## Troubleshooting

### "Skill não encontrada no MythicMobs"
Certifique-se de que:
1. Os arquivos de skills foram copiados para `plugins/MythicMobs/Skills/`
2. Execute `/mm reload` após a importação
3. O ID da skill no MythicLib corresponde ao nome no MythicMobs

### "Mapeamento não encontrado"
Se um skill do MMOCore não tem mapeamento no MythicLib, o importador usa o ID da skill como fallback. Crie a skill correspondente no MythicMobs.

### "Item não aparece"
Verifique se:
1. O custom model data está configurado corretamente
2. O resource pack está aplicado no cliente
3. O Nexo está configurado (se usando itens Nexo)
