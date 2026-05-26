# 🌐 Midgard-Professions — i18n Audit Report

> **Module:** `midgard-professions`  
> **Files Audited:** 90 Java files  
> **Total Hardcoded Strings Found:** ~350+  
> **Files with Hardcoded Strings:** 32  
> **Files Already Externalized:** 58 (clean or console-only)

---

## Priority Legend

| Priority | Meaning |
|----------|---------|
| 🔴 **P0** | Player GUI — massively visible, highest impact |
| 🟠 **P1** | Admin GUI — admin-facing, still user-visible |
| 🟡 **P2** | HUD/Display — BossBar, Scoreboard, Hologram, ActionBar |
| 🔵 **P3** | Enum display names — used throughout the system |
| 🟢 **P4** | Item lore/names — blueprint items, output items |
| ⚪ **P5** | Minor — single strings, descriptions, fallbacks |

---

## Summary by Category

| Category | Files | Strings | Priority |
|----------|-------|---------|----------|
| Player GUIs (Forge) | 6 | ~90 | 🔴 P0 |
| Player GUIs (Smeltery) | 3 | ~70 | 🔴 P0 |
| Admin GUIs (Forge) | 5 | ~80 | 🟠 P1 |
| Admin GUIs (Smeltery) | 5 | ~75 | 🟠 P1 |
| HUD/Display (Scoreboard, Hologram) | 2 | ~20 | 🟡 P2 |
| Minigame BossBar/ActionBar | 3 | ~25 | 🟡 P2 |
| Enum Display Names | 5 | ~49 | 🔵 P3 |
| Blueprint/Output Items | 3 | ~20 | 🟢 P4 |
| Workflow/Manager Strings | 2 | ~5 | ⚪ P5 |
| Command Descriptions | 3 | ~3 | ⚪ P5 |

---

## 🔴 P0 — Player GUIs (Forge)

### 1. `forge/gui/ForgeMainGui.java` — ~25 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Forja — " + forge.getTier().getDisplayName()` | `gui.forge.main.title` |
| fuel info | `"Nenhum"` (fuel fallback) | `gui.forge.main.fuel_none` |
| ItemBuilder | `"Combustível"` | `gui.forge.main.fuel` |
| ItemBuilder lore | `"Tipo:"`, `"Estoque:"`, `"Tempo restante:"` | `gui.forge.main.fuel_type`, `gui.forge.main.fuel_stock`, `gui.forge.main.fuel_time` |
| ItemBuilder | `"Livro de Receitas"` | `gui.forge.main.recipe_book` |
| ItemBuilder lore | `"Veja todas as receitas"`, `"disponíveis para seu nível."` | `gui.forge.main.recipe_book_lore` |
| ItemBuilder | `"Começar a Forjar"` | `gui.forge.main.start_forging` |
| ItemBuilder | `"Forja Indisponível"` | `gui.forge.main.forge_unavailable` |
| ItemBuilder lore | `"Clique para iniciar!"` | `gui.forge.main.click_to_start` |
| ItemBuilder lore | `"Nível insuficiente."` | `gui.forge.main.level_insufficient` |
| ItemBuilder | `"Reparar Item"` | `gui.forge.main.repair_item` |
| ItemBuilder | `"Profissão de Ferreiro"` | `gui.forge.main.profession_info` |
| ItemBuilder lore | `"Nível da forja:"`, `"Itens forjados:"`, `"Dono:"` | `gui.forge.main.forge_level`, `gui.forge.main.items_forged`, `gui.forge.main.owner` |
| ItemBuilder | `"Melhorias da Forja"` | `gui.forge.main.upgrades` |
| ItemBuilder | `"Configurações"` | `gui.forge.main.settings` |
| ItemBuilder | `"Fechar"` | `gui.forge.main.close` |
| ItemBuilder lore | `"Clique para abrir!"` | `gui.forge.main.click_to_open` |
| ItemBuilder lore | `"Dimensão:"` | `gui.forge.main.dimension` |

### 2. `forge/gui/ForgeRepairGui.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>🔧 Reparar Item"` | `gui.forge.repair.title` |
| ItemBuilder | `"Custo de Reparo"` | `gui.forge.repair.cost` |
| ItemBuilder lore | `"Coloque um item danificado"`, `"no slot à esquerda."` | `gui.forge.repair.place_item` |
| ItemBuilder | `"Sem item para reparar"` | `gui.forge.repair.no_item` |
| ItemBuilder | `"Item não está danificado"` | `gui.forge.repair.not_damaged` |
| ItemBuilder | `"Item não reparável"` | `gui.forge.repair.not_repairable` |
| ItemBuilder | `"Confirmar Reparo"` | `gui.forge.repair.confirm` |
| ItemBuilder | `"Materiais Insuficientes"` | `gui.forge.repair.insufficient_materials` |
| ItemBuilder lore | `"Material:"`, `"Necessário:"`, `"Disponível:"`, `"Durabilidade:"` | `gui.forge.repair.material`, `.required`, `.available`, `.durability` |
| ItemBuilder | `"← Voltar"` | `gui.common.back` |

### 3. `forge/gui/ForgeUpgradeGui.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⬆ Melhorias da Forja"` | `gui.forge.upgrade.title` |
| ItemBuilder | `"Tier Atual"` | `gui.forge.upgrade.current_tier` |
| ItemBuilder | `"Próximo Tier"` | `gui.forge.upgrade.next_tier` |
| ItemBuilder | `"Tier Máximo!"` | `gui.forge.upgrade.max_tier` |
| ItemBuilder lore | `"Nível:"`, `"Tamanho:"`, `"Itens Forjados:"` | `gui.forge.upgrade.level`, `.size`, `.items_forged` |
| ItemBuilder lore | `"Nível Requerido:"` | `gui.forge.upgrade.required_level` |
| ItemBuilder lore | `"Para melhorar, construa uma forja maior..."` | `gui.forge.upgrade.build_larger` |
| ItemBuilder lore | `"← Tier Atual"`, `"Desbloqueado"` | `gui.forge.upgrade.current`, `.unlocked` |
| ItemBuilder | `"← Voltar"` | `gui.common.back` |

### 4. `forge/gui/RecipeBookGui.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>📖 Livro de Receitas"` | `gui.forge.recipes.title` |
| ItemBuilder | `"Especialização:"` | `gui.forge.recipes.specialization` |
| ItemBuilder | `"Todas"` | `gui.forge.recipes.filter_all` |
| ItemBuilder | `"Pesquisar"` | `gui.forge.recipes.search` |
| ItemBuilder | `"Apenas Fabricáveis"` / `"Mostrando Todas"` | `gui.forge.recipes.craftable_only`, `.showing_all` |
| recipe lore | `"Nível:"`, `"Forja:"`, `"Materiais:"`, `"Dificuldade:"`, `"XP Base:"`, `"Especialização:"` | `gui.forge.recipes.recipe_level`, `.forge`, `.materials`, `.difficulty`, `.base_xp`, `.spec` |
| recipe lore | `"Clique para selecionar!"` | `gui.forge.recipes.click_to_select` |
| recipe lore | `"Requisitos não atendidos."` | `gui.forge.recipes.requirements_not_met` |
| filter lore | `"Clique para trocar filtro."`, `"Clique para buscar por nome."`, `"Shift+Clique para limpar."` | `gui.forge.recipes.click_filter`, `.click_search`, `.shift_clear` |

### 5. `forge/gui/MaterialPrepGui.java` — ~10 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Preparação — "` | `gui.forge.prep.title` |
| ItemBuilder | `"Metal Primário"`, `"Material Secundário"` | `gui.forge.prep.primary_metal`, `.secondary_material` |
| ItemBuilder lore | `"Depositado"`, `"Pendente"` | `gui.forge.prep.deposited`, `.pending` |
| ItemBuilder | `"Confirmar e Forjar"` | `gui.forge.prep.confirm` |
| ItemBuilder lore | `"Deposite todos os materiais"`, `"antes de confirmar."` | `gui.forge.prep.deposit_all` |
| ItemBuilder | `"Cancelar"` | `gui.forge.prep.cancel` |
| ItemBuilder lore | `"Deposite o material aqui"` | `gui.forge.prep.deposit_here` |

### 6. `forge/gui/ForgeSettingsGui.java` — ~10 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚙ Configurações da Forja"` | `gui.forge.settings.title` |
| ItemBuilder | `"Forja Ativa"` / `"Forja Desativada"` | `gui.forge.settings.active`, `.inactive` |
| ItemBuilder lore | `"Quando desativada, a forja não pode ser utilizada."` | `gui.forge.settings.inactive_desc` |
| ItemBuilder lore | `"Clique para desativar"` / `"Clique para ativar"` | `gui.forge.settings.click_toggle` |
| ItemBuilder | `"Renomear Forja"` | `gui.forge.settings.rename` |
| ItemBuilder lore | `"Nome atual:"` | `gui.forge.settings.current_name` |
| ItemBuilder | `"Informações"` | `gui.forge.settings.info` |
| ItemBuilder | `"← Voltar"` | `gui.common.back` |

---

## 🔴 P0 — Player GUIs (Smeltery)

### 7. `forge/smeltery/gui/SmelteryGui.java` — ~40+ strings (LARGEST OFFENDER)

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚗ Fundição — " + ...` | `gui.smeltery.main.title` |
| ItemBuilder | `"Tanque Vazio"` / `"Tanque:"` | `gui.smeltery.main.tank_empty`, `.tank` |
| ItemBuilder | `"Temperatura"` | `gui.smeltery.main.temperature` |
| ItemBuilder lore | `"Aquecida"` / `"Fria"` | `gui.smeltery.main.heated`, `.cold` |
| ItemBuilder | `"Combustível"` | `gui.smeltery.main.fuel` |
| ItemBuilder lore | `"Ativo"` / `"Sem combustível!"` | `gui.smeltery.main.fuel_active`, `.fuel_empty` |
| ItemBuilder lore | `"Combustíveis aceitos:"` | `gui.smeltery.main.accepted_fuels` |
| ItemBuilder lore | `"Coloque no Fuel Input (barril)"` | `gui.smeltery.main.fuel_input_hint` |
| ItemBuilder | `"Fundindo:"` | `gui.smeltery.main.smelting` |
| ItemBuilder lore | `"Progresso:"`, `"Tempo restante:"` | `gui.smeltery.main.progress`, `.time_remaining` |
| ItemBuilder | `"Slot vazio"` / `"Jogue itens no Input"` | `gui.smeltery.main.slot_empty`, `.throw_items` |
| ItemBuilder | `"Inserir Material"` | `gui.smeltery.main.insert_material` |
| ItemBuilder lore | `"Materiais fundíveis:"` | `gui.smeltery.main.smeltable_materials` |
| ItemBuilder | `"Despejar Metal"` | `gui.smeltery.main.pour_metal` |
| ItemBuilder lore | `"Mesa (laje)"` / `"Bacia (caldeirão)"` | `gui.smeltery.main.table`, `.basin` |
| ItemBuilder lore | `"Tanque vazio!"` / `"Metal disponível:"` | `gui.smeltery.main.tank_empty_warn`, `.metal_available` |
| ItemBuilder | `"Livro de Ligas"` | `gui.smeltery.main.alloy_book` |
| ItemBuilder | `"Informações"` | `gui.smeltery.main.info` |
| ItemBuilder lore | `"Max Temperatura:"`, `"Drenos:"` | `gui.smeltery.main.max_temp`, `.drains` |
| ItemBuilder | `"Fechar"` | `gui.smeltery.main.close` |
| ItemBuilder lore | `"Volume:"`, `"Dureza:"` | `gui.smeltery.main.volume`, `.hardness` |
| ItemBuilder lore | `"Liga"` / `"Metal Base"` | `gui.smeltery.main.alloy`, `.base_metal` |
| ItemBuilder lore | `"Metais diferentes:"`, `"Itens fundidos:"`, `"Dono:"` | `gui.smeltery.main.metals`, `.items_smelted`, `.owner` |
| BossBar | `"Fuel: Xs restante"` | `gui.smeltery.main.fuel_remaining` |

### 8. `forge/smeltery/gui/DrainSelectGui.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"Selecionar Metal"` | `gui.smeltery.drain.title` |
| ItemBuilder | `"← Voltar"` / `"Retorna à GUI da Smeltery"` | `gui.common.back`, `gui.smeltery.drain.back_desc` |
| error msg | `"Nenhuma mesa (laje) ao lado do dreno!"` | `gui.smeltery.drain.no_table` |
| error msg | `"Nenhuma bacia (caldeirão) ao lado do dreno!"` | `gui.smeltery.drain.no_basin` |
| ItemBuilder lore | `"Clique Esquerdo: Lingote"` / `"Clique Direito: Bloco"` | `gui.smeltery.drain.left_ingot`, `.right_block` |
| ItemBuilder lore | `"Insuficiente para lingote"` / `"Insuficiente para bloco"` | `gui.smeltery.drain.insufficient_ingot`, `.insufficient_block` |
| ItemBuilder lore | `"Volume:"`, `"Dureza:"` | `gui.smeltery.drain.volume`, `.hardness` |
| ItemBuilder lore | `"Liga"` / `"Metal Base"` | `gui.smeltery.drain.alloy`, `.base_metal` |
| pour output | `"Bloco"` / `"Lingote"` | `gui.smeltery.drain.block`, `.ingot` |

### 9. `forge/smeltery/gui/AlloyBookGui.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| createInventory | `"Livro de Ligas"` | `gui.smeltery.alloy.title` |
| ItemBuilder | `"Apenas Fabricáveis"` / `"Mostrando Todas"` | `gui.smeltery.alloy.craftable_only`, `.showing_all` |
| recipe lore | `"Ingredientes:"` | `gui.smeltery.alloy.ingredients` |
| recipe lore | `"Resultado:"` | `gui.smeltery.alloy.result` |
| recipe lore | `"Temp. mínima:"` | `gui.smeltery.alloy.min_temp` |
| recipe lore | `"Dureza:"` | `gui.smeltery.alloy.hardness` |
| recipe lore | `"Pode ser formada agora!"` / `"Ingredientes insuficientes"` | `gui.smeltery.alloy.can_form`, `.insufficient` |
| recipe lore | `"Ligas se formam automaticamente"` | `gui.smeltery.alloy.auto_form` |
| recipe lore | `"(Liga)"` | `gui.smeltery.alloy.alloy_tag` |
| recipe lore | `"Receita de Fundição"` | `gui.smeltery.alloy.recipe_title` |
| recipe lore | `"Metais necessários:"`, `"Falta:"`, `"Produz:"` | `gui.smeltery.alloy.metals_needed`, `.missing`, `.produces` |
| recipe lore | `"Temperatura insuficiente!"` | `gui.smeltery.alloy.temp_insufficient` |
| recipe lore | `"Pode ser fabricado agora!"` / `"Metais insuficientes"` | `gui.smeltery.alloy.can_craft`, `.metals_insufficient` |
| recipe lore | `"Clique para fabricar!"` | `gui.smeltery.alloy.click_craft` |
| ItemBuilder | `"← Voltar"` / `"Retorna à GUI da Smeltery"` | `gui.common.back`, `gui.smeltery.alloy.back_desc` |

---

## 🟠 P1 — Admin GUIs (Forge)

### 10. `forge/admin/ForgeAdminGui.java` — ~20 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Templates de Forja"` | `gui.admin.forge.list.title` |
| createItem lore | `"Ativo"` / `"Inativo"` | `gui.admin.common.active`, `.inactive` |
| createItem lore | `"Tipo:"`, `"Nível necessário:"`, `"Criado em:"`, `"Status:"` | `gui.admin.common.type`, `.required_level`, `.created_at`, `.status` |
| createItem lore | `"Dimensões:"`, `"Blocos:"` | `gui.admin.common.dimensions`, `.blocks` |
| createItem lore | `"Clique para editar"` | `gui.admin.common.click_edit` |
| addMenuBorder | `"⚒ Templates de Forja"` (title) | `gui.admin.forge.list.header` |
| addMenuBorder lore | `"Total:"`, `"Ativos:"`, `"Inativos:"` | `gui.admin.forge.list.total`, `.active_count`, `.inactive_count` |
| addMenuBorder lore | `"Templates são os modelos de forja"`, `"que os jogadores podem construir."` | `gui.admin.forge.list.desc` |
| addMenuBorder | `"+ Criar Novo Template"` | `gui.admin.forge.list.create_new` |
| addMenuBorder lore | `"Abre o painel de criação"`, `"de um novo modelo de forja."` | `gui.admin.forge.list.create_desc` |
| addMenuBorder lore | `"Clique para criar!"` | `gui.admin.forge.list.click_create` |
| addMenuBorder | `"↻ Atualizar Lista"` | `gui.admin.common.refresh` |
| addMenuBorder lore | `"Recarrega a lista de templates."` | `gui.admin.common.refresh_desc` |

### 11. `forge/admin/ForgeSetupGui.java` — ~50+ strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Criar Template de Forja"` | `gui.admin.forge.setup.title` |
| info header | `"⚒ Como Criar um Template"` | `gui.admin.forge.setup.how_to` |
| steps 1-5 | `"1. Defina o nome, tipo e nível"` ... `"5. Confirme a criação!"` | `gui.admin.forge.setup.step1` – `.step5` |
| ItemBuilder | `"✏ Nome da Forja"` | `gui.admin.forge.setup.name` |
| ItemBuilder lore | `"Atual:"`, `"Clique para alterar"`, `"Digite o nome no chat"` | `gui.admin.forge.setup.current`, `.click_change`, `.type_in_chat` |
| ItemBuilder | `"⭐ Tipo de Forja"` | `gui.admin.forge.setup.tier` |
| ItemBuilder lore | `"Clique esquerdo → Próximo"`, `"Clique direito → Anterior"` | `gui.admin.forge.setup.left_next`, `.right_prev` |
| ItemBuilder | `"📊 Nível da Profissão"` | `gui.admin.forge.setup.prof_level` |
| ItemBuilder lore | `"Nível necessário:"` | `gui.admin.forge.setup.required_level` |
| ItemBuilder lore | `"Clique esquerdo → +1"`, `"Clique direito → -1"`, `"Shift + Clique → ±10"` | `gui.admin.forge.setup.click_inc`, `.click_dec`, `.shift_inc` |
| pos1/pos2 | `"✔ Posição 1 Definida"` / `"① Definir Posição 1"` | `gui.admin.forge.setup.pos1_set`, `.pos1_define` |
| pos1/pos2 lore | `"Vá até um canto da forja"`, `"e clique aqui para definir."`, `"Clique para capturar"`, `"Clique para redefinir"` | `gui.admin.forge.setup.pos_go_corner`, `.click_define`, `.click_capture`, `.click_redefine` |
| pos2 lore | `"Vá até o canto oposto da forja"` | `gui.admin.forge.setup.pos_opposite` |
| scan | `"③ Escanear Área"`, `"✔ Área Escaneada"` | `gui.admin.forge.setup.scan`, `.scanned` |
| scan lore | `"Blocos detectados:"`, `"Clique para re-escanear"`, `"Clique para escanear!"`, `"Defina as duas posições primeiro."` | `gui.admin.forge.setup.blocks_detected`, `.rescan`, `.scan_now`, `.define_positions` |
| scan lore | `"Clique para escanear os"`, `"blocos entre as posições."` | `gui.admin.forge.setup.scan_desc` |
| blocks summary | `"📋 Blocos Detectados"` | `gui.admin.forge.setup.blocks_summary` |
| assign blocks | `"🔧 Configurar Blocos"` | `gui.admin.forge.setup.assign_blocks` |
| assign lore | `"Veja e configure os papéis"`, `"de cada bloco da forja."`, `"Clique para abrir!"` | `gui.admin.forge.setup.assign_desc`, `.click_open` |
| validation | `"✔ Validação OK"`, `"✘ Validação Incompleta"` | `gui.admin.forge.setup.valid_ok`, `.valid_incomplete` |
| validation lore | `"Blocos obrigatórios presentes."`, `"Pronto para criar!"`, `"Faltando:"` | `gui.admin.forge.setup.blocks_present`, `.ready`, `.missing` |
| validation lore | `"Clique para receber os blocos"` | `gui.admin.forge.setup.give_blocks` |
| confirm | `"✔ Confirmar Criação"` / `"✔ Confirmar (Incompleto)"` | `gui.admin.forge.setup.confirm`, `.confirm_incomplete` |
| confirm lore | `"Clique para criar o template!"` / `"Complete todos os passos."` | `gui.admin.forge.setup.click_create`, `.complete_all` |
| cancel | `"✘ Cancelar"`, `"Cancela a criação do template."` | `gui.admin.forge.setup.cancel`, `.cancel_desc` |
| close | `"Fechar"` | `gui.common.close` |
| formatLoc | `"não definida"` | `gui.admin.common.not_defined` |
| info lore | `"Blocos:"`, `"Blocos interativos:"`, `"Dimensões:"` | `gui.admin.common.blocks`, `.interactive_blocks`, `.dimensions` |
| **getTypeName()** | `"Fornalha"`, `"Bigorna"`, `"Caldeirão"`, `"Rebolo"`, `"Mesa de Ferreiro"`, `"Fogueira"`, `"Alto-Forno"`, `"Mesa de Encantamento"`, `"Zona de Combustível"`, `"Estrutural"`, `"Ar"` | `forge.block_type.furnace` – `.air` |

### 12. `forge/admin/ForgeEditGui.java` — ~20 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Editar: " + template.getName()` | `gui.admin.forge.edit.title` |
| ItemBuilder | `"✏ Renomear Template"` | `gui.admin.forge.edit.rename` |
| ItemBuilder lore | `"Nome atual:"`, `"Clique para renomear"`, `"Digite o novo nome no chat"` | `gui.admin.forge.edit.name_current`, `.click_rename`, `.type_chat` |
| ItemBuilder | `"⭐ Alterar Tipo"` | `gui.admin.forge.edit.change_tier` |
| ItemBuilder | `"📊 Alterar Nível"` | `gui.admin.forge.edit.change_level` |
| ItemBuilder | `"✔ Template Ativo"` / `"✘ Template Inativo"` | `gui.admin.forge.edit.template_active`, `.template_inactive` |
| ItemBuilder lore | `"Jogadores podem construir esta forja."` / `"Esta forja não está disponível."` | `gui.admin.forge.edit.active_desc`, `.inactive_desc` |
| ItemBuilder lore | `"Clique para desativar"` / `"Clique para ativar"` | `gui.admin.forge.edit.click_deactivate`, `.click_activate` |
| ItemBuilder | `"🗑 Excluir Template"` | `gui.admin.forge.edit.delete` |
| ItemBuilder lore | `"Remove permanentemente este template."`, `"⚠ Esta ação é irreversível!"`, `"Shift + Clique para excluir"` | `gui.admin.forge.edit.delete_desc`, `.irreversible`, `.shift_delete` |
| ItemBuilder | `"← Voltar"`, `"Voltar ao painel de templates"` | `gui.common.back`, `gui.admin.forge.edit.back_desc` |
| ItemBuilder | `"Fechar"` | `gui.common.close` |
| info lore | `"Tipo:"`, `"Nível necessário:"`, `"Criado em:"`, `"Status:"`, `"Ativo"` / `"Inativo"`, `"Dimensões:"`, `"Blocos:"` | reuse `gui.admin.common.*` keys |

### 13. `forge/admin/ForgeBlockTypeSelectGui.java` — ~5 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Selecionar Função"` | `gui.admin.forge.block_select.title` |
| ItemBuilder lore | `"Posição:"`, `"Função atual:"` | `gui.admin.common.position`, `.current_role` |
| ItemBuilder lore | `"Selecione a nova função abaixo:"` | `gui.admin.forge.block_select.select_new` |
| ItemBuilder lore | `"✔ Selecionado"` / `"Clique para selecionar"` | `gui.admin.common.selected`, `.click_select` |
| ItemBuilder | `"← Voltar"` | `gui.common.back` |

### 14. `forge/admin/ForgeBlockAssignGui.java` — ~8 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚒ Atribuir Blocos"` | `gui.admin.forge.block_assign.title` |
| createItem lore | `"Posição:"`, `"Função:"` | `gui.admin.common.position`, `.role` |
| createItem lore | `"✔ Atribuído"` / `"Sem função especial"` | `gui.admin.forge.block_assign.assigned`, `.no_role` |
| createItem lore | `"Clique para alterar função"` | `gui.admin.forge.block_assign.click_change` |
| ItemBuilder | `"← Voltar"`, `"Voltar ao menu principal"` | `gui.common.back`, `gui.admin.forge.block_assign.back_desc` |

---

## 🟠 P1 — Admin GUIs (Smeltery)

### 15. `forge/smeltery/admin/SmelteryAdminGui.java` — ~20 strings

Same pattern as ForgeAdminGui but with smeltery terms:

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚗ Templates de Fundição"` | `gui.admin.smeltery.list.title` |
| All identical labels | `"Tipo:"`, `"Nível necessário:"`, `"Status:"`, etc. | `gui.admin.smeltery.list.*` |
| addMenuBorder | `"⚗ Templates de Fundição"` (header) | `gui.admin.smeltery.list.header` |
| lore | `"Templates são os modelos de fundição"`, `"que os jogadores podem construir."` | `gui.admin.smeltery.list.desc` |
| Same action buttons | `"+ Criar Novo Template"`, `"↻ Atualizar Lista"` | reuse or `gui.admin.smeltery.list.*` |

### 16. `forge/smeltery/admin/SmelterySetupGui.java` — ~50+ strings

Same structure as ForgeSetupGui. Key unique strings:

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚗ Criar Template de Fundição"` | `gui.admin.smeltery.setup.title` |
| info | `"⚗ Como Criar um Template"` | `gui.admin.smeltery.setup.how_to` |
| step | `"2. Construa a fundição no mundo"` | `gui.admin.smeltery.setup.step2` |
| name | `"✏ Nome da Fundição"` | `gui.admin.smeltery.setup.name` |
| tier | `"⭐ Tipo de Fundição"` | `gui.admin.smeltery.setup.tier` |
| pos | `"Vá até um canto da fundição"` / `"canto oposto da fundição"` | `gui.admin.smeltery.setup.pos_hint` |
| formatLoc | `"não definida"` | `gui.admin.common.not_defined` |
| **getTypeName()** | `"Parede"`, `"Controlador"`, `"Dreno"`, `"Janela do Tanque"`, `"Entrada de Itens"`, `"Entrada de Combustível"`, `"Ar"`, `"Casting Table"`, `"Casting Basin"` | `smeltery.block_type.wall` – `.casting_basin` |
| All same structural strings as ForgeSetupGui | scan, validation, confirm, cancel... | `gui.admin.smeltery.setup.*` |

### 17. `forge/smeltery/admin/SmelteryEditGui.java` — ~20 strings

Same structure as ForgeEditGui:

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| constructor | `"<dark_gray>⚗ Editar: " + template.getName()` | `gui.admin.smeltery.edit.title` |
| toggle | `"Jogadores podem construir esta fundição."` / `"Esta fundição não está disponível."` | `gui.admin.smeltery.edit.active_desc`, `.inactive_desc` |
| All same labels | rename, alter tier/level, delete, back, close | `gui.admin.smeltery.edit.*` |

### 18. `forge/smeltery/admin/SmelteryBlockTypeSelectGui.java` — ~5 strings

| Hardcoded String | Suggested Key |
|-----------------|---------------|
| `"<dark_gray>⚗ Selecionar Função"` | `gui.admin.smeltery.block_select.title` |
| Same pattern as ForgeBlockTypeSelectGui | `gui.admin.smeltery.block_select.*` |

### 19. `forge/smeltery/admin/SmelteryBlockAssignGui.java` — ~8 strings

| Hardcoded String | Suggested Key |
|-----------------|---------------|
| `"<dark_gray>⚗ Atribuir Blocos"` | `gui.admin.smeltery.block_assign.title` |
| Same pattern as ForgeBlockAssignGui | `gui.admin.smeltery.block_assign.*` |

---

## 🟡 P2 — HUD/Display

### 20. `forge/display/ForgeScoreboard.java` — ~15 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| board title | `"⚒ Forja"` | `display.scoreboard.title` |
| score line | `"Receita:"` | `display.scoreboard.recipe` |
| score line | `"Etapa:"` | `display.scoreboard.stage` |
| section header | `"Pontuações"` | `display.scoreboard.scores` |
| score line | `"Aquecimento:"` | `display.scoreboard.heating` |
| score line | `"Martelamento:"` | `display.scoreboard.hammering` |
| score line | `"Têmpera:"` | `display.scoreboard.quenching` |
| score line | `"Afiação:"` | `display.scoreboard.sharpening` |
| section header | `"Qualidade"` | `display.scoreboard.quality` |
| score line | `"Estimativa:"` | `display.scoreboard.estimate` |

### 21. `forge/display/ForgeHologram.java` — ~5 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| getStageText(HEATING) | `"Aqueça o metal"` | `display.hologram.heat_metal` |
| getStageText(HAMMERING) | `"Martele na zona verde"` | `display.hologram.hammer_green` |
| getStageText(QUENCHING) | `"Mergulhe no momento certo"` | `display.hologram.quench_timing` |
| getStageText(SHARPENING) | `"Mantenha a pressão"` | `display.hologram.keep_pressure` |
| getStageText(FINALIZING) | `"Finalizando..."` | `display.hologram.finalizing` |

---

## 🟡 P2 — Minigame BossBar/ActionBar

### 22. `forge/minigame/QuenchingMinigame.java` — ~15 strings (ENTIRELY UNHARDCODED)

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| BossBar | `"Temperatura: MÁXIMA"` | `minigame.quenching.bossbar_max_temp` |
| BossBar | `"Clique direito no caldeirão no momento certo!"` | `minigame.quenching.instruction` |
| ActionBar | `"PERFEITO! Temperatura ideal! Pontuação: "` | `minigame.quenching.perfect` |
| ActionBar | `"Bom mergulho! Pontuação: "` | `minigame.quenching.good` |
| ActionBar | `"Temperatura muito alta!"` / `"Temperatura muito baixa!"` | `minigame.quenching.too_hot`, `.too_cold` |
| ActionBar | `"Metal reaquecido! Mergulhos restantes: "` | `minigame.quenching.reheated` |
| ActionBar | `"Têmpera concluída! Pontuação final: "` | `minigame.quenching.complete` |
| BossBar zone | `"ZONA PERFEITA! Mergulhe agora!"` | `minigame.quenching.perfect_zone` |
| BossBar zone | `"Zona Ideal — Bom momento para mergulhar"` | `minigame.quenching.ideal_zone` |
| BossBar zone | `"Muito quente! Espere esfriar..."` | `minigame.quenching.too_hot_wait` |
| BossBar zone | `"Esfriando... Temperatura baixa"` | `minigame.quenching.cooling` |

### 23. `forge/minigame/HammeringMinigame.java` — ~5 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| buildBarText | `"PERFEITO"` | `minigame.hammering.perfect` |
| buildBarText | `"BOM"` | `minigame.hammering.good` |
| buildBarText | `"Martelar!"` | `minigame.hammering.strike` |
| buildBarText | `"Martelamento"` | `minigame.hammering.title` |

### 24. `forge/minigame/SharpeningMinigame.java` — ~5 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| buildBarText | `"EM ZONA"` | `minigame.sharpening.in_zone` |
| buildBarText | `"Fora"` | `minigame.sharpening.out` |
| buildBarText | `"Afiação"` | `minigame.sharpening.title` |
| buildBarText | `"Passe"` | `minigame.sharpening.pass` |
| buildBarText | `"Média:"` | `minigame.sharpening.average` |

---

## 🔵 P3 — Enum Display Names

### 25. `forge/quality/QualityTier.java` — 7 strings

| Enum Value | Hardcoded `displayName` | Suggested Key |
|-----------|------------------------|---------------|
| DEFECTIVE | `"Defeituoso"` | `quality.defective` |
| INFERIOR | `"Inferior"` | `quality.inferior` |
| COMMON | `"Comum"` | `quality.common` |
| SUPERIOR | `"Superior"` | `quality.superior` |
| EXCEPTIONAL | `"Excepcional"` | `quality.exceptional` |
| MASTERWORK | `"Obra-Prima"` | `quality.masterwork` |
| LEGENDARY | `"Lendário"` | `quality.legendary` |

### 26. `forge/quality/MaterialGrade.java` — 5 strings

| Enum Value | Hardcoded `displayName` | Suggested Key |
|-----------|------------------------|---------------|
| IMPURE | `"Impuro"` | `material_grade.impure` |
| RAW | `"Bruto"` | `material_grade.raw` |
| REFINED | `"Refinado"` | `material_grade.refined` |
| PURE | `"Puro"` | `material_grade.pure` |
| PRISTINE | `"Prístino"` | `material_grade.pristine` |

### 27. `forge/ForgeTier.java` — 10 strings (5 name + 5 displayName)

| Enum Value | Hardcoded Strings | Suggested Keys |
|-----------|-------------------|----------------|
| BASIC | `"Forja Básica"` / display variant | `forge_tier.basic.name`, `.display` |
| INTERMEDIATE | `"Forja Intermediária"` | `forge_tier.intermediate.name`, `.display` |
| ADVANCED | `"Forja Avançada"` | `forge_tier.advanced.name`, `.display` |
| MASTER | `"Forja de Mestre"` | `forge_tier.master.name`, `.display` |
| LEGENDARY | `"Forja Lendária"` | `forge_tier.legendary.name`, `.display` |

### 28. `forge/ForgeStage.java` — 10 strings

| Enum Value | Hardcoded `displayName` | Suggested Key |
|-----------|------------------------|---------------|
| SELECTING_RECIPE | `"Selecionando Receita"` | `forge_stage.selecting` |
| PREPARING_MATERIALS | `"Preparando Materiais"` | `forge_stage.preparing` |
| HEATING | `"Aquecendo Metal"` | `forge_stage.heating` |
| HAMMERING | `"Martelando"` | `forge_stage.hammering` |
| QUENCHING | `"Temperando"` | `forge_stage.quenching` |
| SHARPENING | `"Afiando"` | `forge_stage.sharpening` |
| FINALIZING | `"Finalizando"` | `forge_stage.finalizing` |
| COMPLETED | `"Concluído"` | `forge_stage.completed` |
| FAILED | `"Falhou"` | `forge_stage.failed` |
| EXPIRED | `"Expirado"` | `forge_stage.expired` |

### 29. `forge/smeltery/SmelteryTier.java` — 10 strings (5 name + 5 formattedName)

| Enum Value | Hardcoded Strings | Suggested Keys |
|-----------|-------------------|----------------|
| SMALL | `"Fundição Pequena"` | `smeltery_tier.small.name`, `.display` |
| MEDIUM | `"Fundição Média"` | `smeltery_tier.medium.name`, `.display` |
| LARGE | `"Fundição Grande"` | `smeltery_tier.large.name`, `.display` |
| MASTER | `"Fundição de Mestre"` | `smeltery_tier.master.name`, `.display` |
| LEGENDARY | `"Fundição Lendária"` | `smeltery_tier.legendary.name`, `.display` |

### 30. `forge/smeltery/MoltenMetal.java` — 34 strings (17 displayName + 17 formattedName)

| Enum Value | Hardcoded `displayName` | Suggested Key |
|-----------|------------------------|---------------|
| IRON | `"Ferro Fundido"` | `molten_metal.iron` |
| GOLD | `"Ouro Fundido"` | `molten_metal.gold` |
| COPPER | `"Cobre Fundido"` | `molten_metal.copper` |
| NETHERITE | `"Netherite Fundido"` | `molten_metal.netherite` |
| EMERALD | `"Esmeralda Fundida"` | `molten_metal.emerald` |
| DIAMOND | `"Diamante Fundido"` | `molten_metal.diamond` |
| AMETHYST | `"Ametista Fundida"` | `molten_metal.amethyst` |
| QUARTZ | `"Quartzo Fundido"` | `molten_metal.quartz` |
| LAPIS | `"Lápis Fundido"` | `molten_metal.lapis` |
| REDSTONE | `"Redstone Fundida"` | `molten_metal.redstone` |
| BRONZE | `"Bronze Fundido"` | `molten_metal.bronze` |
| STEEL | `"Aço Fundido"` | `molten_metal.steel` |
| MANYULLYN | `"Manyullyn Fundido"` | `molten_metal.manyullyn` |
| OBSIDIAN | `"Obsidiana Forjada"` | `molten_metal.obsidian` |
| ROSE_GOLD | `"Ouro Rosa Fundido"` | `molten_metal.rose_gold` |
| ELECTRUM | `"Electrum Fundido"` | `molten_metal.electrum` |
| KNIGHTSLIME | `"Knightslime Fundido"` | `molten_metal.knightslime` |

---

## 🟢 P4 — Item Names/Lore

### 31. `forge/ghost/ForgeBlueprintItem.java` — ~7 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| addLore | `"Projeto de construção de forja."` | `item.blueprint.forge.desc1` |
| addLore | `"Coloque no chão para iniciar a"` | `item.blueprint.forge.desc2` |
| addLore | `"sessão de construção."` | `item.blueprint.forge.desc3` |
| addLore | `"Nível necessário:"` | `item.blueprint.forge.required_level` |
| addLore | `"Tamanho:"` | `item.blueprint.forge.size` |
| addLore | `"▶ Clique direito no chão para construir"` | `item.blueprint.forge.click_build` |

### 32. `forge/smeltery/ghost/SmelteryBlueprintItem.java` — ~7 strings (x2 methods)

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| addLore | `"Projeto de construção de fundição."` | `item.blueprint.smeltery.desc1` |
| addLore | `"Coloque no chão para iniciar a"` | `item.blueprint.smeltery.desc2` |
| addLore | `"sessão de construção."` | `item.blueprint.smeltery.desc3` |
| addLore | `"Nível necessário:"` | `item.blueprint.smeltery.required_level` |
| addLore | `"Tamanho:"` | `item.blueprint.smeltery.size` |
| addLore | `"▶ Clique direito no chão para construir"` | `item.blueprint.smeltery.click_build` |

### 33. `forge/smeltery/SmelteryOutputItem.java` — ~6 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| create | `"Lingote de "` | `item.smeltery.ingot_prefix` |
| create | `"Bloco de "` | `item.smeltery.block_prefix` |
| create lore | `"Dureza:"` | `item.smeltery.hardness` |
| create lore | `"Liga da Fundição"` | `item.smeltery.alloy_tag` |
| create lore | `"Use na Forja para criar itens"` | `item.smeltery.use_in_forge` |
| create | `"Bloco de Liga"` | `item.smeltery.alloy_block` |
| internal | `.replace(" Fundido", "").replace(" Fundida", "")` | Needs refactoring with enum key lookup |

### 34. `forge/quality/ForgeQualityApplier.java` — 1 string

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| rebuildLore | `"Forjado — Qualidade: "` | `item.forge.quality_lore` |

---

## ⚪ P5 — Minor Strings

### 35. `forge/ForgeWorkflowService.java` — 1 hardcoded string

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| startForging | `"Nenhum"` (fuel fallback in started_fuel msg) | `forge.fuel.none` |

### 36. `forge/ghost/GhostBlockSession.java` — ~3 strings

| Line(s) | Hardcoded String | Suggested Key |
|---------|-----------------|---------------|
| getHudText (PREVIEWING) | `"Confirme a posição (Shift+Clique direito)"` | `display.ghost.confirm_position` |
| getHudText (BUILDING) | `"Construindo "`, `" blocos | Camada "` | `display.ghost.building_hud` |

### 37–39. Command Descriptions

| File | Hardcoded String | Suggested Key |
|------|-----------------|---------------|
| `ForgeCommand.java` | `"Comandos do sistema de forja"` | `command.forge.description` |
| `ForgeAdminCommand.java` | `"Comandos administrativos do sistema de forja"` | `command.forge_admin.description` |
| `SmelteryAdminCommand.java` | `"Comandos administrativos do sistema de fundição"` | `command.smeltery_admin.description` |

---

## Files Confirmed Clean (✅ Use `msg()` or no player strings)

These files properly use the `msg()` externalization pattern or have no player-facing strings:

- `ForgeCommand.java` — all subcommand messages externalized
- `ForgeAdminCommand.java` — all subcommand messages externalized
- `SmelteryAdminCommand.java` — all subcommand messages externalized
- `ForgeManager.java` — all player messages externalized
- `SmelteryManager.java` — all player messages externalized
- `ForgeWorkflowService.java` — all player messages externalized (except 1 fallback)
- `GhostBlockManager.java` — all player messages externalized
- `ForgeBuildListener.java` — all player messages externalized
- `ForgeInteractListener.java` — no player messages
- `ForgePlayerListener.java` — no player messages
- `SmelteryListener.java` — no player messages (dispatches to SmelteryManager)
- `SmelteryBlockType.java` — enum with no display names (technical only)
- All data/repository/session/event/structure/recipe/effect/schematic classes — no player-facing strings

---

## Recommended Implementation Order

1. **Enums first** (P3) — These are referenced everywhere. Create the keys, then make enums load display names from `messages.yml` via a static helper. All GUIs that call `getDisplayName()` / `getFormattedName()` automatically benefit.

2. **Common shared strings** — `"← Voltar"`, `"Fechar"`, `"Clique para..."` patterns appear in 20+ GUIs. Define once under `gui.common.*`.

3. **Player GUIs** (P0) — Start with `SmelteryGui.java` (largest offender ~40+), then `ForgeMainGui.java`, then rest.

4. **Admin GUIs** (P1) — `ForgeSetupGui` and `SmelterySetupGui` are the biggest (~50+ each). Many strings can be shared via `gui.admin.common.*`.

5. **HUD/Minigames** (P2) — `QuenchingMinigame.java` is entirely unhardcoded and needs full treatment. Others need only BossBar text extracted.

6. **Item text** (P4) — Blueprint items and `SmelteryOutputItem` lore.

7. **Minor** (P5) — Command descriptions, single fallbacks.

---

## Block Type Names (Shared Constants)

The `getTypeName()` methods in `ForgeSetupGui` and `SmelterySetupGui` define block type display names used across multiple admin GUIs. These should be centralized into `messages.yml`:

**Forge block types** (`ForgeSetupGui.getTypeName()`):
`Fornalha`, `Bigorna`, `Caldeirão`, `Rebolo`, `Mesa de Ferreiro`, `Fogueira`, `Alto-Forno`, `Mesa de Encantamento`, `Zona de Combustível`, `Estrutural`, `Ar`

**Smeltery block types** (`SmelterySetupGui.getTypeName()`):
`Parede`, `Controlador`, `Dreno`, `Janela do Tanque`, `Entrada de Itens`, `Entrada de Combustível`, `Ar`, `Casting Table`, `Casting Basin`

---

*Report generated from full audit of 90 Java files in `midgard-modules/midgard-professions/src/main/java/`*
