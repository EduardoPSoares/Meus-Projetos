# I18N Audit Report — midgard-item

**Module:** `midgard-modules/midgard-item`  
**Files Scanned:** 100 Java source files  
**Messages File:** `src/main/resources/modules/item/lang/messages.yml` (~400 lines)  
**Externalization Pattern:** `MidgardCore.getLanguageManager().getMessage("item.xxx")` / `.getRawMessage("item.xxx")`  

---

## Summary

| Priority | Files Affected | Hardcoded Strings | Category |
|----------|---------------|-------------------|----------|
| **P0** | 1 | ~20 | Player-facing GUI (UpgradeGui) |
| **P1** | 2 | ~97 | Player-facing data (ItemStat enum, LoreFormatter) |
| **P2** | 3 | ~80 | Admin GUIs (ForgeRecipeEditor, SmeltingRecipeEditor, CraftingTypeSelection) |
| **P3** | 4 | ~10 | Console logs, fallbacks, exception messages |
| **Total** | **10** | **~207** | |

**Files already properly externalized (no action needed):**
- `ItemCommand.java` — ✅ Fully uses `getLanguageManager().getMessage()`
- `StatRangeEditorGui.java` — ✅ Fully externalized
- `TypeBrowserGui.java` — ✅ Fully externalized
- `MaterialSelectionGui.java` — ✅ Fully externalized
- `RevisionConfigurationGui.java` — ✅ Fully externalized
- `NbtEditorGui.java` — ✅ Fully externalized
- `TierSelectionGui.java` — ✅ Mostly externalized (minor color formatting only)
- `EnchantmentSelectionGui.java` — ✅ Mostly externalized (color prefixes only)
- Most other GUIs, listeners, managers — ✅ already using getMessage()

---

## P0 — CRITICAL (Player-Facing GUI)

### 1. UpgradeGui.java — Item Refinement GUI

**Path:** `gui/UpgradeGui.java`  
**Impact:** Shown to ALL players during item upgrade. Fully hardcoded Portuguese.

| Line | Hardcoded String | Suggested YAML Key |
|------|-----------------|-------------------|
| 29 | `"Refinar Item"` (GUI title) | `gui.upgrade.title` |
| 140 | `"<yellow>Informações de Refino"` | `gui.upgrade.info_name` |
| 147 | `"<red>Nível Máximo Atingido!"` | `gui.upgrade.max_level_reached` |
| 151 | `"<gray>Nível Atual: <white>+"` | `gui.upgrade.current_level` |
| 152 | `"<gray>Próximo Nível: <gold>+"` | `gui.upgrade.next_level` |
| 154 | `"<gray>Chance de Sucesso: <green>"` | `gui.upgrade.success_chance` |
| 155 | `"<gray>Chance de Quebra: <dark_red>"` | `gui.upgrade.break_chance` |
| 156 | `"<gray>Chance de Regresso: <red>"` | `gui.upgrade.downgrade_chance` |
| 158 | `"<gray>Custo: <aqua>"` | `gui.upgrade.cost` |
| 172 | `"<green>✔ Materiais Suficientes"` | `gui.upgrade.materials_sufficient` |
| 174 | `"<red>✖ Materiais Insuficientes"` | `gui.upgrade.materials_insufficient` |
| 179 | `"<gray>Coloque um item para ver os detalhes."` | `gui.upgrade.place_item_hint` |
| 187 | `"<green>Confirmar Refino"` | `gui.upgrade.confirm_button` |
| 189 | `"<red>Aguardando Itens..."` | `gui.upgrade.waiting_items` |
| 226 | `"<green>Item refinado com sucesso!"` | `gui.upgrade.success_message` |
| 229 | `"<red>O refino falhou!"` | `gui.upgrade.fail_message` |
| 232 | `"<dark_red>O refino falhou e o item quebrou!"` | `gui.upgrade.break_message` |
| 236 | `"<red>O refino falhou e o item perdeu um nível!"` | `gui.upgrade.downgrade_message` |
| 239 | `"<red>Erro ao refinar item: "` | `gui.upgrade.error_message` |

**Total: 19 strings**

---

## P1 — HIGH (Player-Facing Data / Lore)

### 2. ItemStat.java — RPG Stat Display Names

**Path:** `model/ItemStat.java`  
**Impact:** These names appear on item lore visible to ALL players via LoreFormatter. The enum has 90 entries, all with hardcoded Portuguese display names.

| Lines | Hardcoded String | Suggested YAML Key |
|-------|-----------------|-------------------|
| 4 | `"Dano"` | `stat.attack-damage` |
| 5 | `"Velocidade de Ataque"` | `stat.attack-speed` |
| 6 | `"Chance de Crítico"` | `stat.critical-strike-chance` |
| 7 | `"Dano Crítico"` | `stat.critical-strike-power` |
| 8 | `"Poder de Bloqueio"` | `stat.block-power` |
| 9 | `"Taxa de Bloqueio"` | `stat.block-rating` |
| 10 | `"Esquiva"` | `stat.dodge-rating` |
| 11 | `"Aparar"` | `stat.parry-rating` |
| 12 | `"Armadura"` | `stat.armor` |
| 13 | `"Resistência de Armadura"` | `stat.armor-toughness` |
| 14 | `"Vida Máxima"` | `stat.max-health` |
| 15 | `"Mana Máxima"` | `stat.max-mana` |
| 16 | `"Stamina Máxima"` | `stat.max-stamina` |
| 17 | `"Velocidade de Movimento"` | `stat.movement-speed` |
| 18 | `"Duas Mãos"` | `stat.two-handed` |
| 19 | `"Indestrutível"` | `stat.unbreakable` |
| 20 | `"Nível Necessário"` | `stat.required-level` |
| 21 | `"Chance de Crítico de Habilidade"` | `stat.skill-critical-strike-chance` |
| 22 | `"Dano Crítico de Habilidade"` | `stat.skill-critical-strike-power` |
| 23 | `"Redução de Cooldown de Bloqueio"` | `stat.block-cooldown-reduction` |
| 24 | `"Redução de Cooldown de Esquiva"` | `stat.dodge-cooldown-reduction` |
| 25 | `"Redução de Cooldown de Aparar"` | `stat.parry-cooldown-reduction` |
| 26 | `"Redução de Cooldown"` | `stat.cooldown-reduction` |
| 27 | `"Dano de Habilidade"` | `stat.skill-damage` |
| 28 | `"Dano de Projétil"` | `stat.projectile-damage` |
| 29 | `"Dano Mágico"` | `stat.magic-damage` |
| 30 | `"Dano contra Mortos-Vivos"` | `stat.undead-damage` |
| 31 | `"Redução de Dano"` | `stat.damage-reduction` |
| 32 | `"Defesa"` | `stat.defense` |
| 33 | `"Redução de Dano de Queda"` | `stat.fall-damage-reduction` |
| 34 | `"Redução de Dano de Projétil"` | `stat.projectile-damage-reduction` |
| 35 | `"Redução de Dano Físico"` | `stat.physical-damage-reduction` |
| 36 | `"Redução de Dano Mágico"` | `stat.magic-damage-reduction` |
| 37 | `"Redução de Dano de Fogo"` | `stat.fire-damage-reduction` |
| 38 | `"Roubo de Vida"` | `stat.lifesteal` |
| 39 | `"Redução de Dano PvE"` | `stat.pve-damage-reduction` |
| 40 | `"Redução de Dano PvP"` | `stat.pvp-damage-reduction` |
| 41 | `"Vampirismo de Feitiço"` | `stat.spell-vampirism` |
| 42 | `"Gravidade"` | `stat.gravity` |
| 43 | `"Resistência a Repulsão"` | `stat.knockback-resistance` |
| 44 | `"Regeneração de Mana Máxima"` | `stat.max-mana-regeneration` |
| 45 | `"Regeneração de Stamina"` | `stat.stamina-regeneration` |
| 46 | `"Regeneração de Stamina Máxima"` | `stat.max-stamina-regeneration` |
| 47 | `"Stellium Máximo"` | `stat.max-stellium` |
| 48 | `"Absorção Máxima"` | `stat.max-absorption` |
| 49 | `"Experiência Adicional"` | `stat.additional-experience` |
| 50 | `"Regeneração de Vida"` | `stat.health-regeneration` |
| 51 | `"Regeneração de Vida Máxima"` | `stat.max-health-regeneration` |
| 52 | `"Sorte"` | `stat.myluck` |
| 53 | `"Regeneração de Mana"` | `stat.mana-regeneration` |
| 54 | `"Taxa de Sucesso"` | `stat.success-rate` |
| 55 | `"Distância de Queda Segura"` | `stat.safe-fall-distance` |
| 56 | `"Escala"` | `stat.scale` |
| 57 | `"Altura do Degrau"` | `stat.step-height` |
| 58 | `"Tempo de Queima"` | `stat.burning-time` |
| 59 | `"Força do Pulo"` | `stat.jump-strength` |
| 60 | `"Resistência a Repulsão de Explosão"` | `stat.explosion-knockback-resistance` |
| 61 | `"Eficiência de Mineração"` | `stat.mining-efficiency` |
| 62 | `"Eficiência de Movimento"` | `stat.movement-efficiency` |
| 63 | `"Oxigênio Bônus"` | `stat.bonus-oxygen` |
| 64 | `"Velocidade Agachado"` | `stat.sneaking-speed` |
| 65 | `"Velocidade de Mineração Submersa"` | `stat.submerged-mining-speed` |
| 66 | `"Taxa de Dano de Varredura"` | `stat.sweeping-damage-ratio` |
| 67 | `"Eficiência de Movimento na Água"` | `stat.water-movement-efficiency` |
| 68 | `"Velocidade de Mineração"` | `stat.mining-speed` |
| 69 | `"Alcance de Interação com Blocos"` | `stat.block-interaction-range` |
| 70 | `"Alcance de Interação com Entidades"` | `stat.entity-interaction-range` |
| 71 | `"Multiplicador de Dano de Queda"` | `stat.fall-damage-multiplier` |
| 72 | `"Cooldown do Item"` | `stat.item-cooldown` |
| 74 | `"Dano de Fogo"` | `stat.fire-damage` |
| 75 | `"Dano de Gelo"` | `stat.ice-damage` |
| 76 | `"Dano de Luz"` | `stat.light-damage` |
| 77 | `"Dano de Escuridão"` | `stat.darkness-damage` |
| 78 | `"Dano Divino"` | `stat.divine-damage` |
| 80 | `"Força"` | `stat.strength` |
| 81 | `"Inteligência"` | `stat.intelligence` |
| 82 | `"Destreza"` | `stat.dexterity` |
| 84 | `"Precisão"` | `stat.accuracy` |
| 85 | `"Resistência Crítica"` | `stat.critical-resistance` |
| 86 | `"Espinhos"` | `stat.thorns` |
| 87 | `"Resistência Mágica"` | `stat.magic-resistance` |
| 89 | `"Penetração de Armadura"` | `stat.armor-penetration` |
| 90 | `"Penetração de Armadura (Flat)"` | `stat.armor-penetration-flat` |
| 91 | `"Penetração Mágica"` | `stat.magic-penetration` |
| 92 | `"Penetração Mágica (Flat)"` | `stat.magic-penetration-flat` |
| 94 | `"Redução de Dano de Gelo"` | `stat.ice-damage-reduction` |
| 95 | `"Redução de Dano de Luz"` | `stat.light-damage-reduction` |
| 96 | `"Redução de Dano de Escuridão"` | `stat.darkness-damage-reduction` |
| 97 | `"Redução de Dano Divino"` | `stat.divine-damage-reduction` |

**Total: 90 enum entries**

**Recommended approach:** Add a `getDisplayName()` method that looks up from messages.yml at runtime using the `path` field as key, falling back to the hardcoded name:

```java
public String getDisplayName() {
    String msg = MidgardCore.getLanguageManager().getRawMessage("item.stat." + path);
    return msg != null ? msg : name;
}
```

### 3. LoreFormatter.java — Item Lore Generation

**Path:** `utils/LoreFormatter.java`  
**Impact:** Generates the lore visible on items held by players.

| Line | Hardcoded String | Suggested YAML Key |
|------|-----------------|-------------------|
| 153 | `"<gray>■ {name}: <white>{value}"` (config fallback) | Already in config as `settings.lore-format.stats.default` — Portuguese default |
| 185 | `"<gray>■ {name}: <white>{value}"` (duplicate fallback) | Same as above |
| 235 | `"<green>◆ Engaste de {type} Vazio"` | Config key `settings.lore-format.sockets.empty` — Portuguese default |
| 280 | `"Arma"` | `stat.socket_type.weapon` |
| 281 | `"Armadura"` | `stat.socket_type.armor` |
| 282 | `"Acessório"` | `stat.socket_type.accessory` |
| 283 | `"Qualquer"` | `stat.socket_type.any` |

**Total: 7 strings** (4 unique in `translateSocketType()`, plus config fallbacks with Portuguese defaults)

---

## P2 — MEDIUM (Admin-Only GUIs)

### 4. ForgeRecipeEditorGui.java — Forge Recipe Editor

**Path:** `gui/editors/impl/ForgeRecipeEditorGui.java`  
**Impact:** Admin-only recipe editor. Portuguese throughout.

| Line | Hardcoded String | Suggested YAML Key |
|------|-----------------|-------------------|
| 56 | `"<dark_gray>⚒ Editor de Receita — Forja"` (GUI title) | `gui.crafting_gui.editor.forge.title` |
| 113 | `"<gold>⚔ Dificuldade"` | `gui.crafting_gui.editor.forge.difficulty.name` |
| 114-116 | `"<gray>Nível de dificuldade da forja."` / `"Afeta velocidade e precisão"` / `"necessária nos minigames."` | `gui.crafting_gui.editor.forge.difficulty.lore` |
| 118 | `"<white>Atual: <gold>" + val + "/10"` | `gui.crafting_gui.editor.forge.difficulty.current` |
| 120-121 | `"<yellow>Clique esquerdo: editar"` / `"<yellow>Clique direito: resetar"` | `gui.crafting_gui.editor.common.click_edit` / `.click_reset` |
| 128 | `"<green>📊 Nível Mínimo"` | `gui.crafting_gui.editor.forge.min_level.name` |
| 129-130 | `"<gray>Nível de profissão de"` / `"<gray>ferreiro necessário."` | `gui.crafting_gui.editor.forge.min_level.lore` |
| 132 | `"<white>Atual: <green>" + val` | `gui.crafting_gui.editor.forge.min_level.current` |
| 141 | `"<light_purple>⬆ Tier da Forja"` | `gui.crafting_gui.editor.forge.tier.name` |
| 142-143 | `"<gray>Tier mínimo da forja"` / `"para usar esta receita."` | `gui.crafting_gui.editor.forge.tier.lore` |
| 145 | `"<white>Atual: <light_purple>" + tier` | `gui.crafting_gui.editor.forge.tier.current` |
| 147 | `"<yellow>Clique para alterar"` | `gui.crafting_gui.editor.forge.tier.click` |
| 148 | `"<gray>BASIC → ADVANCED → EXPERT → MASTER"` | `gui.crafting_gui.editor.forge.tier.cycle` |
| 151 | `"nenhum"` (null placeholder) | `gui.crafting_gui.editor.common.none` |
| 154 | `"<aqua>🔗 Forge Recipe ID"` | `gui.crafting_gui.editor.forge.recipe_id.name` |
| 155-156 | `"<gray>ID da receita no sistema"` / `"de forja (ForgeRecipe)."` | `gui.crafting_gui.editor.forge.recipe_id.lore` |
| 158 | `"<white>Atual: <aqua>" + id` | `gui.crafting_gui.editor.forge.recipe_id.current` |
| 160 | `"<yellow>Clique para editar"` | `gui.crafting_gui.editor.common.click_edit_single` |
| 165 | `"<dark_gray>▬▬▬ Ingredientes ▬▬▬"` | `gui.crafting_gui.editor.forge.separator` |
| 174 | `"<gold>⛏ Metal Primário"` | `gui.crafting_gui.editor.forge.primary_metal` |
| 177-180 | `"<white>Material Secundário 1"` … `4` | `gui.crafting_gui.editor.forge.secondary_material` (with `%n%`) |
| 194, 205 | `"<gray>Item: <white>" + ingredient` | `gui.crafting_gui.editor.common.ingredient_current` |
| 196, 207 | `"<yellow>Clique esquerdo: trocar"` | `gui.crafting_gui.editor.common.click_swap` |
| 197, 208 | `"<red>Clique direito: remover"` | `gui.crafting_gui.editor.common.click_remove` |
| 212 | `" <red>(inválido)"` | `gui.crafting_gui.editor.common.invalid_suffix` |
| 213 | `"<red>Item não encontrado: "` | `gui.crafting_gui.editor.common.item_not_found` |
| 220 | `"<red>Obrigatório!"` / `"<gray>Opcional"` | `gui.crafting_gui.editor.forge.required` / `.optional` |
| 222 | `"<yellow>Clique para definir"` | `gui.crafting_gui.editor.common.click_set` |
| 257 | `"Digite a quantidade de output:"` | `gui.crafting_gui.editor.common.prompt.output_amount` |
| 279 | `"Digite a dificuldade (1-10):"` | `gui.crafting_gui.editor.forge.prompt.difficulty` |
| 294 | `"Digite o nível mínimo:"` | `gui.crafting_gui.editor.forge.prompt.min_level` |
| 316 | `"Digite o Forge Recipe ID:"` | `gui.crafting_gui.editor.forge.prompt.recipe_id` |

**Total: ~35 strings**

### 5. SmeltingRecipeEditorGui.java — Smelting Recipe Editor

**Path:** `gui/editors/impl/SmeltingRecipeEditorGui.java`  
**Impact:** Admin-only smeltery recipe editor. Portuguese throughout including static METAL_NAMES map with 17 entries.

| Line | Hardcoded String | Suggested YAML Key |
|------|-----------------|-------------------|
| 65 | `"<white>Ferro Fundido"` | `gui.crafting_gui.editor.smelting.metal.IRON` |
| 66 | `"<gold>Ouro Fundido"` | `gui.crafting_gui.editor.smelting.metal.GOLD` |
| 67 | `"<#E87E04>Cobre Fundido"` | `gui.crafting_gui.editor.smelting.metal.COPPER` |
| 68 | `"<dark_red>Netherite Fundido"` | `gui.crafting_gui.editor.smelting.metal.NETHERITE_SCRAP` |
| 69 | `"<green>Esmeralda Fundida"` | `gui.crafting_gui.editor.smelting.metal.EMERALD` |
| 70 | `"<aqua>Diamante Fundido"` | `gui.crafting_gui.editor.smelting.metal.DIAMOND` |
| 71 | `"<light_purple>Ametista Fundida"` | `gui.crafting_gui.editor.smelting.metal.AMETHYST` |
| 72 | `"<white>Quartzo Fundido"` | `gui.crafting_gui.editor.smelting.metal.QUARTZ` |
| 73 | `"<blue>Lápis Fundido"` | `gui.crafting_gui.editor.smelting.metal.LAPIS` |
| 74 | `"<red>Redstone Fundida"` | `gui.crafting_gui.editor.smelting.metal.REDSTONE` |
| 75 | `"<#CD7F32>Bronze Fundido"` | `gui.crafting_gui.editor.smelting.metal.BRONZE` |
| 76 | `"<gray>Aço Fundido"` | `gui.crafting_gui.editor.smelting.metal.STEEL` |
| 77 | `"<dark_purple>Manyullyn Fundido"` | `gui.crafting_gui.editor.smelting.metal.MANYULLYN` |
| 78 | `"<dark_gray>Obsidiana Forjada"` | `gui.crafting_gui.editor.smelting.metal.OBSIDIAN_ALLOY` |
| 79 | `"<#F5A0C0>Ouro Rosa Fundido"` | `gui.crafting_gui.editor.smelting.metal.ROSE_GOLD` |
| 80 | `"<yellow>Electrum Fundido"` | `gui.crafting_gui.editor.smelting.metal.ELECTRUM` |
| 81 | `"<dark_aqua>Knightslime Fundido"` | `gui.crafting_gui.editor.smelting.metal.KNIGHTSLIME` |
| 100 | `"<dark_gray>🔥 Editor de Receita — Smelting"` (GUI title) | `gui.crafting_gui.editor.smelting.title` |
| 158 | `"<red>🌡 Temperatura Mínima"` | `gui.crafting_gui.editor.smelting.min_temp.name` |
| 159-160 | `"<gray>Temperatura mínima da"` / `"smeltery para esta receita."` | `gui.crafting_gui.editor.smelting.min_temp.lore` |
| 162 | `"<white>Atual: <red>" + minTemp + "°C"` | `gui.crafting_gui.editor.smelting.min_temp.current` |
| 164-165 | `"<yellow>Clique esquerdo: editar"` / `"<yellow>Clique direito: resetar (800°C)"` | `gui.crafting_gui.editor.common.click_edit` / `.click_reset_temp` |
| 170 | `"<green>+ Adicionar Metal"` | `gui.crafting_gui.editor.smelting.add_metal.name` |
| 171-172 | `"<gray>Adiciona um novo metal"` / `"fundido como ingrediente."` | `gui.crafting_gui.editor.smelting.add_metal.lore` |
| 174 | `"<yellow>Clique para abrir seleção"` | `gui.crafting_gui.editor.smelting.add_metal.click` |
| 179 | `"<dark_gray>▬▬▬ Metais Ingredientes ▬▬▬"` | `gui.crafting_gui.editor.smelting.separator` |
| 206 | `"<gray>Quantidade: <white>" + amount + "mb"` | `gui.crafting_gui.editor.smelting.metal_amount` |
| 208 | `"<yellow>Clique esquerdo: editar quantidade"` | `gui.crafting_gui.editor.smelting.click_edit_amount` |
| 209 | `"<red>Clique direito: remover"` | `gui.crafting_gui.editor.common.click_remove` |
| 214 | `"<dark_gray>Vazio"` | `gui.crafting_gui.editor.smelting.empty_slot` |
| 215 | `"<gray>Use o botão '+' para adicionar metais"` | `gui.crafting_gui.editor.smelting.empty_slot_hint` |
| 249 | `"Digite a quantidade de output:"` | `gui.crafting_gui.editor.common.prompt.output_amount` |
| 271 | `"Digite a temperatura mínima (°C):"` | `gui.crafting_gui.editor.smelting.prompt.min_temp` |
| 292 | `"<gold>Digite a quantidade em mb para <white>" + key` | `gui.crafting_gui.editor.smelting.prompt.metal_amount` |
| 316 | `"<gold>Digite a quantidade em mb para " + name` | same as above |
| 338 | `"<dark_gray>Selecionar Metal Fundido"` (inner GUI title) | `gui.crafting_gui.editor.smelting.metal_selection_title` |
| 362 | `"<yellow>Clique para selecionar"` | `gui.crafting_gui.editor.common.click_select` |
| 369 | `"<yellow>← Voltar"` | `gui.crafting_gui.editor.common.back_arrow` |

**Total: ~40 strings**

### 6. CraftingTypeSelectionGui.java — Recipe Type Selection (partial)

**Path:** `gui/CraftingTypeSelectionGui.java`  
**Impact:** Admin-only. Most strings externalized, but Forge and Smeltery recipe types are hardcoded.

| Line | Hardcoded String | Suggested YAML Key |
|------|-----------------|-------------------|
| 98 | `"<gold>⚒ Forja"` | `gui.crafting_gui.type_selection.forge.name` |
| 104 | `"<red>🔥 Fundição (Smeltery)"` | `gui.crafting_gui.type_selection.smelting.name` |
| 105 | `"<gray>Receita de crafting na smeltery."` | `gui.crafting_gui.type_selection.smelting.lore` (list) |
| 106 | `"<gray>Configura metais fundidos,"` | (part of lore list) |
| 107 | `"<gray>quantidades e temperatura."` | (part of lore list) |
| 109 | `"<yellow>Clique para criar receita"` | `gui.crafting_gui.type_selection.smelting.click` |

**Total: 6 strings**

---

## P3 — LOW (Console Logs, Fallbacks, Internal)

### 7. ItemModule.java — Module Lifecycle (console-only)

| Line | Hardcoded String | Context |
|------|-----------------|---------|
| ~41 | `"Habilitando Midgard-Item..."` | Console log on enable |
| ~142 | `"Desabilitando Midgard-Item..."` | Console log on disable |

### 8. MidgardItemGeneral.java — Default Item Name

| Line | Hardcoded String | Context |
|------|-----------------|---------|
| 40 | `"<red>ɪᴛᴇᴍ ɪɴᴠᴀʟɪᴅᴏ</red>"` | Fallback for missing config |

**Suggested key:** `common.invalid_item_name`

### 9. ItemEditionGui.java — Fallback Strings

| Line | Hardcoded String | Context |
|------|-----------------|---------|
| 113 | `"<green>Get Item"` | Fallback when config missing (English) |
| 140 | `"<red>Close"` | Fallback when config missing (English) |
| 208, 371, 382 | `"<green>✔</green> <gray>ᴍᴀᴛᴇʀɪᴀʟ ᴀᴛᴜᴀʟɪᴢᴀᴅᴏ ᴠɪᴀ ɪɴᴠᴇɴᴛᴀʀɪᴏ!</gray>"` | Fallback for `getSafeMessage()` |

> These are fallbacks triggered only if messages.yml key is missing. Primary path already uses externalized messages.

### 10. Other Minor Occurrences

| File | Line | Hardcoded String | Context |
|------|------|-----------------|---------|
| `MidgardItemBuilder.java` | 69, 294, 435 | Console warning messages in Portuguese | Logger-only |
| `BiomeSelectionGui.java` | 102 | `"Biome inválido"` | Exception message |
| `ItemMigrationTool.java` | 42 | `"Banco de dados vazio. Iniciando migração..."` | Console migration log |
| `RecipeManager.java` | 150 | `"Ingrediente inválido na receita: "` | Console warning |

---

## Recommended messages.yml Additions

```yaml
# ── Upgrade GUI (P0) ──
gui:
  upgrade:
    title: "Refinar Item"
    info_name: "<yellow>Informações de Refino"
    max_level_reached: "<red>Nível Máximo Atingido!"
    current_level: "<gray>Nível Atual: <white>+%level%"
    next_level: "<gray>Próximo Nível: <gold>+%level%"
    success_chance: "<gray>Chance de Sucesso: <green>%chance%%"
    break_chance: "<gray>Chance de Quebra: <dark_red>%chance%%"
    downgrade_chance: "<gray>Chance de Regresso: <red>%chance%%"
    cost: "<gray>Custo: <aqua>%amount%x %material%"
    materials_sufficient: "<green>✔ Materiais Suficientes"
    materials_insufficient: "<red>✖ Materiais Insuficientes"
    place_item_hint: "<gray>Coloque um item para ver os detalhes."
    confirm_button: "<green>Confirmar Refino"
    waiting_items: "<red>Aguardando Itens..."
    success_message: "<green>Item refinado com sucesso!"
    fail_message: "<red>O refino falhou!"
    break_message: "<dark_red>O refino falhou e o item quebrou!"
    downgrade_message: "<red>O refino falhou e o item perdeu um nível!"
    error_message: "<red>Erro ao refinar item: %error%"

# ── Stat Display Names (P1) ──
stat:
  attack-damage: "Dano"
  attack-speed: "Velocidade de Ataque"
  critical-strike-chance: "Chance de Crítico"
  critical-strike-power: "Dano Crítico"
  block-power: "Poder de Bloqueio"
  block-rating: "Taxa de Bloqueio"
  dodge-rating: "Esquiva"
  parry-rating: "Aparar"
  armor: "Armadura"
  armor-toughness: "Resistência de Armadura"
  max-health: "Vida Máxima"
  max-mana: "Mana Máxima"
  max-stamina: "Stamina Máxima"
  movement-speed: "Velocidade de Movimento"
  two-handed: "Duas Mãos"
  unbreakable: "Indestrutível"
  required-level: "Nível Necessário"
  skill-critical-strike-chance: "Chance de Crítico de Habilidade"
  skill-critical-strike-power: "Dano Crítico de Habilidade"
  block-cooldown-reduction: "Redução de Cooldown de Bloqueio"
  dodge-cooldown-reduction: "Redução de Cooldown de Esquiva"
  parry-cooldown-reduction: "Redução de Cooldown de Aparar"
  cooldown-reduction: "Redução de Cooldown"
  skill-damage: "Dano de Habilidade"
  projectile-damage: "Dano de Projétil"
  magic-damage: "Dano Mágico"
  undead-damage: "Dano contra Mortos-Vivos"
  damage-reduction: "Redução de Dano"
  defense: "Defesa"
  fall-damage-reduction: "Redução de Dano de Queda"
  projectile-damage-reduction: "Redução de Dano de Projétil"
  physical-damage-reduction: "Redução de Dano Físico"
  magic-damage-reduction: "Redução de Dano Mágico"
  fire-damage-reduction: "Redução de Dano de Fogo"
  lifesteal: "Roubo de Vida"
  pve-damage-reduction: "Redução de Dano PvE"
  pvp-damage-reduction: "Redução de Dano PvP"
  spell-vampirism: "Vampirismo de Feitiço"
  gravity: "Gravidade"
  knockback-resistance: "Resistência a Repulsão"
  max-mana-regeneration: "Regeneração de Mana Máxima"
  stamina-regeneration: "Regeneração de Stamina"
  max-stamina-regeneration: "Regeneração de Stamina Máxima"
  max-stellium: "Stellium Máximo"
  max-absorption: "Absorção Máxima"
  additional-experience: "Experiência Adicional"
  health-regeneration: "Regeneração de Vida"
  max-health-regeneration: "Regeneração de Vida Máxima"
  myluck: "Sorte"
  mana-regeneration: "Regeneração de Mana"
  success-rate: "Taxa de Sucesso"
  safe-fall-distance: "Distância de Queda Segura"
  scale: "Escala"
  step-height: "Altura do Degrau"
  burning-time: "Tempo de Queima"
  jump-strength: "Força do Pulo"
  explosion-knockback-resistance: "Resistência a Repulsão de Explosão"
  mining-efficiency: "Eficiência de Mineração"
  movement-efficiency: "Eficiência de Movimento"
  bonus-oxygen: "Oxigênio Bônus"
  sneaking-speed: "Velocidade Agachado"
  submerged-mining-speed: "Velocidade de Mineração Submersa"
  sweeping-damage-ratio: "Taxa de Dano de Varredura"
  water-movement-efficiency: "Eficiência de Movimento na Água"
  mining-speed: "Velocidade de Mineração"
  block-interaction-range: "Alcance de Interação com Blocos"
  entity-interaction-range: "Alcance de Interação com Entidades"
  fall-damage-multiplier: "Multiplicador de Dano de Queda"
  item-cooldown: "Cooldown do Item"
  fire-damage: "Dano de Fogo"
  ice-damage: "Dano de Gelo"
  light-damage: "Dano de Luz"
  darkness-damage: "Dano de Escuridão"
  divine-damage: "Dano Divino"
  strength: "Força"
  intelligence: "Inteligência"
  dexterity: "Destreza"
  accuracy: "Precisão"
  critical-resistance: "Resistência Crítica"
  thorns: "Espinhos"
  magic-resistance: "Resistência Mágica"
  armor-penetration: "Penetração de Armadura"
  armor-penetration-flat: "Penetração de Armadura (Flat)"
  magic-penetration: "Penetração Mágica"
  magic-penetration-flat: "Penetração Mágica (Flat)"
  ice-damage-reduction: "Redução de Dano de Gelo"
  light-damage-reduction: "Redução de Dano de Luz"
  darkness-damage-reduction: "Redução de Dano de Escuridão"
  divine-damage-reduction: "Redução de Dano Divino"
  socket_type:
    weapon: "Arma"
    armor: "Armadura"
    accessory: "Acessório"
    any: "Qualquer"
```

---

## Implementation Priority

1. **Phase 1 (P0):** Externalize `UpgradeGui.java` — highest player impact, 19 strings
2. **Phase 2 (P1):** Refactor `ItemStat.java` to load display names from messages.yml — 90 entries, affects all item lore
3. **Phase 3 (P1):** Fix `LoreFormatter.translateSocketType()` — 4 strings + config fallbacks
4. **Phase 4 (P2):** Externalize `ForgeRecipeEditorGui.java` and `SmeltingRecipeEditorGui.java` — admin-only but large
5. **Phase 5 (P2):** Fix remaining `CraftingTypeSelectionGui.java` entries — 6 strings
6. **Phase 6 (P3):** Console logs, fallbacks, exception messages — low priority
