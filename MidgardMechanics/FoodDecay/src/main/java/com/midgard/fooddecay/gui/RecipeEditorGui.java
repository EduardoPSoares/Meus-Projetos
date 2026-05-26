package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import static com.midgard.fooddecay.gui.RecipeEditorText.*;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.FoodTrait;
import com.midgard.fooddecay.NutritionManager;
import com.midgard.fooddecay.multiblock.MMOCoreHook;
import com.midgard.fooddecay.multiblock.ItemsAdderHook;
import com.midgard.fooddecay.multiblock.MMOItemsHook;
import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.MultiblockType;
import com.midgard.fooddecay.multiblock.RecipeIngredient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Admin recipe editor with a fixed MMOItems-inspired slot layout.
 */
public class RecipeEditorGui extends GuiMenu {

    private static final int PAGE_COUNT = RecipeEditorPage.count();
    private static final ItemStack FILL_ITEM = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .hideFlags()
            .build();
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final FoodDecayModule module;
    private final boolean isNew;
    private int currentPage = 0;

    private final String recipeId;
    private final MultiblockType machineType;
    private Material inputMaterial;
    private String inputMmoType;
    private String inputMmoId;
    private String inputItemsAdderId;
    private int inputCustomModelData;
    private Material outputMaterial;
    private String outputMmoType;
    private String outputMmoId;
    private String outputItemsAdderId;
    private String outputName;
    private List<String> outputLore;
    private int outputCustomModelData;
    private int spoiledCustomModelData;
    private String spoiledName;
    private int timeMinutes;
    private FoodTrait trait;
    private List<RecipeIngredient> extraIngredients;
    private FoodTrait requiresTrait;
    private String requiresRecipe;
    private List<String> nutritionGroups;
    private String profession;
    private int professionLevel;
    private String experienceProfession;
    private double experienceReward;

    public RecipeEditorGui(FoodDecayModule module, MultiblockRecipe recipe, boolean isNew) {
        super(sc("&8Edicao de Receita: &f" + recipe.getId().toUpperCase(Locale.ROOT).replace('-', '_')), 6);
        this.module = module;
        this.isNew = isNew;

        this.recipeId = recipe.getId();
        this.machineType = recipe.getMachineType();
        this.inputMaterial = recipe.getInputMaterial();
        this.inputMmoType = recipe.getInputMmoType();
        this.inputMmoId = recipe.getInputMmoId();
        this.inputItemsAdderId = recipe.getInputItemsAdderId();
        this.inputCustomModelData = recipe.getInputCustomModelData();
        this.outputMaterial = recipe.getOutputMaterial();
        this.outputMmoType = recipe.getOutputMmoType();
        this.outputMmoId = recipe.getOutputMmoId();
        this.outputItemsAdderId = recipe.getOutputItemsAdderId();
        this.outputName = blankToNull(recipe.getOutputName());
        this.outputLore = recipe.getOutputLore() != null ? new ArrayList<>(recipe.getOutputLore()) : new ArrayList<>();
        this.outputCustomModelData = recipe.getOutputCustomModelData();
        this.spoiledCustomModelData = recipe.getSpoiledCustomModelData();
        this.spoiledName = blankToNull(recipe.getSpoiledName());
        this.timeMinutes = Math.max(1, recipe.getTimeMinutes());
        this.trait = recipe.getTrait();
        this.extraIngredients = recipe.getExtraIngredients() != null
                ? new ArrayList<>(recipe.getExtraIngredients())
                : new ArrayList<>();
        this.requiresTrait = recipe.getRequiresTrait();
        this.requiresRecipe = blankToNull(recipe.getRequiresRecipe());
        this.nutritionGroups = normalizeNutritionGroups(recipe.getNutritionGroups());
        this.profession = blankToNull(recipe.getProfession());
        this.professionLevel = Math.max(0, recipe.getProfessionLevel());
        this.experienceProfession = blankToNull(recipe.getExperienceProfession());
        this.experienceReward = Math.max(0D, recipe.getExperienceReward());
    }

    @Override
    public void setup(Player player) {
        fill(FILL_ITEM);
        setupTopBar(player);
        setupNavigation(player);
        switch (currentEditorPage()) {
            case RECIPE -> setupRecipePage(player);
            case APPEARANCE -> setupAppearancePage(player);
            case REQUIREMENTS -> setupRequirementsPage(player);
            case MMOCORE -> setupMmocorePage(player);
        }
        setupFooter(player);
    }

    private void setupTopBar(Player player) {
        setItem(2, buildMachineSlot());
        setItem(4, buildDisplaySlot());

        setItem(6, buildExitSlot(), e -> {
            if (e.isShiftClick() && !isNew) {
                module.getDecayConfig().deleteRecipe(machineType, recipeId);
                player.sendMessage(MessageUtils.toComponent(sc("&cReceita &e" + recipeId + " &cremovida.")));
            }
            new RecipeListGui(module, machineType).open(player);
        });
    }

    private void setupNavigation(Player player) {
        setItem(13, buildStageOverviewSlot());
    }

    private void setupRecipePage(Player player) {
        setupInputSlot(player);
        setupOutputSlot(player);
        setupTimeSlot(player);
        setupExtraIngredientsSlot(player);
        setupInputModelDataSlot(player);
        setupTraitSlot(player);
    }

    private void setupAppearancePage(Player player) {
        setupNameSlot(player);
        setupLoreSlot(player);
        setupOutputModelDataSlot(player);
        setupSpoiledModelSlot(player);
        setupSpoiledNameSlot(player);
    }

    private void setupRequirementsPage(Player player) {
        setupRequiresTraitSlot(player);
        setupRequiresRecipeSlot(player);
        setupNutritionGroupsSlot(player);
    }

    private void setupMmocorePage(Player player) {
        setupProfessionSlot(player);
        setupExperienceSlot(player);
    }

    private void setupFooter(Player player) {
        setItem(45, buildPreviousStageSlot(), e -> {
            if (currentPage == 0) {
                new RecipeListGui(module, machineType).open(player);
                return;
            }
            currentPage--;
            open(player);
        });

        setItem(49, buildStageProgressSlot());

        if (currentPage >= PAGE_COUNT - 1) {
            setItem(53, buildSaveSlot(), e -> saveAndReturn(player));
            return;
        }

        setItem(53, buildNextStageSlot(), e -> {
            if (currentPage == 0 && (!hasInputConfigured() || !hasOutputConfigured())) {
                player.sendMessage(MessageUtils.toComponent(sc("&cDefina input e output antes de ir para a proxima etapa.")));
                return;
            }
            currentPage++;
            open(player);
        });
    }

    private void setupInputSlot(Player player) {
        setItem(20, new ItemBuilder(buildInputSlotItem())
                .name(sc("&aEntrada"))
                .lore(buildFieldLore(
                        "Escolhe qual item sera aceito como materia-prima desta receita.|Voce pode usar um item vanilla da mao ou abrir o seletor para navegar por MMOItems, ItemsAdder e materiais comuns.",
                        currentValueLine("Entrada", getInputDescription(), hasInputConfigured()),
                        requiredAction(),
                        leftAction("copiar o item que esta na sua mao."),
                        rightAction("abrir o seletor completo de itens."),
                        shiftAction("remover a entrada configurada."),
                        noteAction("se usar MMOItems ou ItemsAdder, a referencia externa tambem sera salva.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                inputMaterial = null;
                inputMmoType = null;
                inputMmoId = null;
                inputItemsAdderId = null;
                inputCustomModelData = 0;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                new ItemSelectorGui(module, "Entrada", (material, mmoType, mmoId, itemsAdderId) -> {
                    inputMaterial = material;
                    inputMmoType = mmoType;
                    inputMmoId = mmoId;
                    inputItemsAdderId = itemsAdderId;
                    if ((mmoType != null && mmoId != null) || itemsAdderId != null) {
                        inputCustomModelData = 0;
                    }
                    open(player);
                }, () -> open(player)).open(player);
                return;
            }

            if (!hasHandItem(player)) {
                player.sendMessage(MessageUtils.toComponent(sc("&cSegure um item na mao primeiro.")));
                return;
            }

            captureInputFromHand(player);
            open(player);
        });
    }

    private void setupOutputSlot(Player player) {
        setItem(22, new ItemBuilder(buildOutputSlotItem())
                .name(sc("&aSaida"))
                .lore(buildFieldLore(
                        "Escolhe qual item a maquina vai entregar ao fim do processo.|A saida pode ser vanilla, MMOItems ou ItemsAdder e ainda receber nome, lore e model data nas etapas seguintes.",
                        currentValueLine("Saida", getOutputDescription(), hasOutputConfigured()),
                        requiredAction(),
                        leftAction("copiar o item que esta na sua mao."),
                        rightAction("abrir o seletor completo de itens."),
                        shiftAction("limpar a saida e os overrides visuais."),
                        noteAction("o preview do topo sempre mostra o resultado atual.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                outputMaterial = null;
                outputMmoType = null;
                outputMmoId = null;
                outputItemsAdderId = null;
                outputName = null;
                outputLore.clear();
                outputCustomModelData = 0;
                spoiledCustomModelData = 0;
                spoiledName = null;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                new ItemSelectorGui(module, "Saida", (material, mmoType, mmoId, itemsAdderId) -> {
                    outputMaterial = material;
                    outputMmoType = mmoType;
                    outputMmoId = mmoId;
                    outputItemsAdderId = itemsAdderId;
                    open(player);
                }, () -> open(player)).open(player);
                return;
            }

            if (!hasHandItem(player)) {
                player.sendMessage(MessageUtils.toComponent(sc("&cSegure um item na mao primeiro.")));
                return;
            }

            captureOutputFromHand(player);
            open(player);
        });
    }

    private void setupTimeSlot(Player player) {
        setItem(24, new ItemBuilder(Material.CLOCK)
                .name(sc("&aTempo de processamento"))
                .lore(buildFieldLore(
                        "Define quanto tempo o processamento vai levar dentro da maquina.|Esse valor e salvo em minutos e influencia diretamente o ritmo da receita no jogo.",
                        currentValueLine("Tempo", timeMinutes + " min", true),
                        requiredAction(),
                        leftAction("digitar um novo tempo no chat."),
                        shiftAction("restaurar o tempo padrao desta maquina."),
                        noteAction("use valores baixos para receitas simples e altos para processos raros.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                timeMinutes = machineType.getDefaultProcessingMinutes();
                open(player);
                return;
            }

            requestInt(player, "&eDigite o tempo em minutos:", value -> timeMinutes = Math.max(1, value));
        });
    }

    private void setupExtraIngredientsSlot(Player player) {
        setItem(28, new ItemBuilder(Material.BOWL)
                .name(sc("&aIngredientes extras"))
                .lore(buildFieldLore(
                        "Define ingredientes adicionais consumidos do inventario do jogador quando o preparo comeca.|Isso permite receitas compostas de verdade, como sanduiches, sopas, pratos mistos e refeicoes completas.",
                        currentValueLine("Extras", getExtraIngredientsDescription(), !extraIngredients.isEmpty()),
                        optionalAction(),
                        leftAction("editar a lista no chat usando | para separar ingredientes."),
                        shiftAction("remover todos os ingredientes extras."),
                        noteAction("exemplos: BREAD | COOKED_BEEF | CARROT, COOKED_BEEF*2, mmo:CONSUMABLE:jerky, ia:pack:item.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                extraIngredients = new ArrayList<>();
                open(player);
                return;
            }

            requestText(player, "&eDigite os ingredientes extras separados por |:", value -> {
                List<RecipeIngredient> parsed = parseExtraIngredients(value);
                if (parsed == null) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc("&cFormato invalido. Use exemplos como &fBREAD | COOKED_BEEF*2 | ia:pack:item&c.")));
                    return;
                }
                extraIngredients = parsed;
            });
        });
    }

    private void setupTraitSlot(Player player) {
        FoodDecayConfig config = module.getDecayConfig();
        Material icon = trait != null && trait.getDefaultIngredient() != null
                ? trait.getDefaultIngredient()
                : Material.HONEYCOMB;

        setItem(32, new ItemBuilder(icon)
                .name(sc("&aTraco aplicado"))
                .lore(buildFieldLore(
                        "Adiciona um traco de conservacao ao item produzido por esta receita.|Esse traco sera usado no resultado final para indicar o metodo de preservacao.",
                        currentValueLine("Traco", getTraitName(trait), trait != null),
                        requiredAction(),
                        leftAction("alternar entre os tracos disponiveis."),
                        rightAction("usar automaticamente o traco padrao da maquina."),
                        shiftAction("remover qualquer traco extra desta receita."),
                        noteAction("se ficar vazio, a receita nao adiciona nenhum traco por conta propria.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                trait = null;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                trait = machineType.getResultTrait();
                open(player);
                return;
            }

            trait = cycleEnum(FoodTrait.class, trait);
            if (trait != null) {
                player.sendMessage(MessageUtils.toComponent(sc("&aTraco atual: &f" + config.getTraitDisplayName(trait))));
            }
            open(player);
        });
    }

    private void setupNameSlot(Player player) {
        setItem(20, new ItemBuilder(Material.NAME_TAG)
                .name(sc("&aNome customizado"))
                .lore(buildFieldLore(
                        "Sobrescreve o nome exibido no item final da receita.|Use este campo quando quiser um nome especial, com cores, estilo ou identidade propria para o item produzido.",
                        currentValueLine("Nome", getDisplayValue(outputName, "Sem override"), outputName != null),
                        optionalAction(),
                        leftAction("digitar um novo nome no chat."),
                        shiftAction("remover o nome customizado."),
                        noteAction("voce pode usar codigos de cor com &.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                outputName = null;
                open(player);
                return;
            }

            requestText(player, "&eDigite o nome customizado da saida:", value -> outputName = blankToNull(value));
        });
    }

    private void setupLoreSlot(Player player) {
        setItem(22, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(sc("&aLore da saida"))
                .lore(buildFieldLore(
                        "Define as linhas de lore que vao aparecer no item final.|Essa descricao ajuda a explicar o item para o jogador e pode trazer estilo, contexto ou informacoes extras.",
                        currentValueLine("Lore", outputLore.isEmpty() ? "Sem linhas" : outputLore.size() + " linhas", !outputLore.isEmpty()),
                        optionalAction(),
                        leftAction("editar todas as linhas pelo chat."),
                        shiftAction("apagar a lore atual."),
                        noteAction("separe cada linha usando o caractere |.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                outputLore.clear();
                open(player);
                return;
            }

            requestText(player, "&eDigite a lore usando | para separar linhas:", value -> {
                outputLore = new ArrayList<>();
                for (String part : value.split("\\|")) {
                    String clean = part.trim();
                    if (!clean.isEmpty()) {
                        outputLore.add(clean);
                    }
                }
            });
        });
    }

    private void setupInputModelDataSlot(Player player) {
        setItem(30, new ItemBuilder(Material.ITEM_FRAME)
                .name(sc("&aModelo do input"))
                .lore(buildFieldLore(
                        "Restringe a entrada usando o custom model data do item base.|Isso permite diferenciar itens visivelmente iguais, mas que usam modelos diferentes no resource pack.",
                        currentValueLine("Modelo", inputCustomModelData > 0 ? String.valueOf(inputCustomModelData) : "Qualquer modelo", inputCustomModelData > 0),
                        optionalAction(),
                        leftAction("copiar o model data do item na mao."),
                        rightAction("digitar manualmente o valor no chat."),
                        shiftAction("aceitar qualquer model data."),
                        noteAction("use 0 ou vazio para nao filtrar por modelo.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                inputCustomModelData = 0;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                requestInt(player,
                        "&eDigite o custom model data do input (0 para limpar):",
                        value -> inputCustomModelData = Math.max(0, value));
                return;
            }

            if (!hasHandItem(player)) {
                player.sendMessage(MessageUtils.toComponent(sc("&cSegure um item com model data na mao.")));
                return;
            }

            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) {
                player.sendMessage(MessageUtils.toComponent(sc("&cO item da mao nao possui custom model data.")));
                return;
            }

            inputCustomModelData = meta.getCustomModelData();
            open(player);
        });
    }

    private void setupOutputModelDataSlot(Player player) {
        setItem(24, new ItemBuilder(Material.ANVIL)
                .name(sc("&aModelo da saida"))
                .lore(buildFieldLore(
                        "Aplica um custom model data especifico na saida final.|Isso muda a aparencia do resultado sem precisar trocar o tipo base do item.",
                        currentValueLine("Modelo", outputCustomModelData > 0 ? String.valueOf(outputCustomModelData) : "Sem override", outputCustomModelData > 0),
                        optionalAction(),
                        leftAction("copiar o model data do item na mao."),
                        rightAction("digitar manualmente o valor no chat."),
                        shiftAction("remover o override visual da saida."),
                        noteAction("combine com nome e lore para criar um resultado unico.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                outputCustomModelData = 0;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                requestInt(player,
                        "&eDigite o custom model data da saida (0 para limpar):",
                        value -> outputCustomModelData = Math.max(0, value));
                return;
            }

            if (!hasHandItem(player)) {
                player.sendMessage(MessageUtils.toComponent(sc("&cSegure um item com model data na mao.")));
                return;
            }

            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) {
                player.sendMessage(MessageUtils.toComponent(sc("&cO item da mao nao possui custom model data.")));
                return;
            }

            outputCustomModelData = meta.getCustomModelData();
            open(player);
        });
    }

    private void setupRequiresTraitSlot(Player player) {
        Material icon = requiresTrait != null && requiresTrait.getDefaultIngredient() != null
                ? requiresTrait.getDefaultIngredient()
                : Material.PAPER;

        setItem(21, new ItemBuilder(icon)
                .name(sc("&aTraco requerido"))
                .lore(buildFieldLore(
                        "Exige que o item de entrada ja tenha um traco especifico antes do processamento.|Isso permite criar receitas em cadeia, onde um metodo de conservacao prepara o alimento para outro.",
                        currentValueLine("Traco", getTraitName(requiresTrait), requiresTrait != null),
                        optionalAction(),
                        leftAction("alternar entre os tracos disponiveis."),
                        shiftAction("remover a exigencia de traco."),
                        noteAction("se ficar vazio, qualquer item compativel pode entrar.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                requiresTrait = null;
            } else {
                requiresTrait = cycleEnum(FoodTrait.class, requiresTrait);
            }
            open(player);
        });
    }

    private void setupRequiresRecipeSlot(Player player) {
        setItem(23, new ItemBuilder(Material.PAPER)
                .name(sc("&aReceita requerida"))
                .lore(buildFieldLore(
                        "Exige que o alimento tenha passado por uma receita anterior especifica.|Isso ajuda a montar progressao entre maquinas, etapas artesanais ou transformacoes em cadeia.",
                        currentValueLine("Receita", getDisplayValue(requiresRecipe, "Nenhuma"), requiresRecipe != null),
                        optionalAction(),
                        leftAction("digitar o ID da receita anterior no chat."),
                        shiftAction("remover a dependencia de receita."),
                        noteAction("informe apenas o ID interno salvo no recipes.yml.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                requiresRecipe = null;
                open(player);
                return;
            }

            requestText(player, "&eDigite o ID da receita requerida:", value -> requiresRecipe = blankToNull(value));
        });
    }

    private void setupNutritionGroupsSlot(Player player) {
        setItem(25, new ItemBuilder(Material.SUSPICIOUS_STEW)
                .name(sc("&aGrupos de nutricao"))
                .lore(buildFieldLore(
                        "Define quais grupos de nutricao esta receita vai entregar no item final.|Use este campo para comidas complexas, como pratos completos, ensopados, lanches mistos ou refeicoes compostas.",
                        currentValueLine("Grupos", getNutritionGroupsDescription(), !nutritionGroups.isEmpty()),
                        optionalAction(),
                        leftAction("digitar os IDs dos grupos, separados por virgula."),
                        shiftAction("voltar para a nutricao automatica pelo material."),
                        noteAction("exemplo: GRAIN, PROTEIN, VEGETABLE.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                nutritionGroups = new ArrayList<>();
                open(player);
                return;
            }

            requestText(player, "&eDigite os grupos de nutricao separados por virgula:", value -> {
                List<String> parsedGroups = normalizeNutritionGroups(List.of(value.split(",")));
                if (parsedGroups.isEmpty() && !value.isBlank()) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc("&cNenhum grupo valido encontrado. Use IDs como &fGRAIN, PROTEIN, VEGETABLE&c.")));
                    return;
                }
                nutritionGroups = parsedGroups;
            });
        });
    }

    private void setupSpoiledModelSlot(Player player) {
        setItem(32, new ItemBuilder(Material.ROTTEN_FLESH)
                .name(sc("&aModelo estragado"))
                .lore(buildFieldLore(
                        "Troca o modelo visual quando o alimento entrar no estado estragado.|Serve para mostrar de forma clara que o item venceu e mudou de estado.",
                        currentValueLine("Modelo", spoiledCustomModelData > 0 ? String.valueOf(spoiledCustomModelData) : "Sem override", spoiledCustomModelData > 0),
                        optionalAction(),
                        leftAction("digitar o model data do estado estragado."),
                        rightAction("copiar o model data atual da saida."),
                        shiftAction("desativar o modelo estragado."),
                        noteAction("se ficar vazio, o item estragado usa o mesmo modelo da saida.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                spoiledCustomModelData = 0;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                spoiledCustomModelData = outputCustomModelData;
                open(player);
                return;
            }

            requestInt(player,
                    "&eDigite o custom model data do item estragado (0 para limpar):",
                    value -> spoiledCustomModelData = Math.max(0, value));
        });
    }

    private void setupSpoiledNameSlot(Player player) {
        setItem(30, new ItemBuilder(Material.NAME_TAG)
                .name(sc("&aNome estragado"))
                .lore(buildFieldLore(
                        "Define um nome especial para o item quando ele estragar.|E util para deixar o estado vencido mais claro e visualmente coerente com a proposta do plugin.",
                        currentValueLine("Nome", getDisplayValue(spoiledName, "Sem override"), spoiledName != null),
                        optionalAction(),
                        leftAction("digitar o nome do estado estragado."),
                        shiftAction("remover o nome estragado."),
                        noteAction("tambem aceita cores usando &.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                spoiledName = null;
                open(player);
                return;
            }

            requestText(player, "&eDigite o nome do item estragado:", value -> spoiledName = blankToNull(value));
        });
    }

    private void setupProfessionSlot(Player player) {
        if (!MMOCoreHook.isAvailable()) {
            setItem(21, new ItemBuilder(Material.GRAY_DYE)
                    .name(sc("&cMMOCore indisponivel"))
                    .lore(buildFieldLore(
                            "Os campos desta etapa dependem do plugin MMOCore instalado e ativo no servidor.|Sem ele, profissao e experiencia ficam desativadas neste editor.",
                            currentValueLine("Status", "Plugin nao encontrado", false),
                            noteAction("instale ou ative o MMOCore para liberar esta configuracao.")
                    ))
                    .build());
            return;
        }

        setItem(21, new ItemBuilder(Material.IRON_SWORD)
                .name(sc("&aProfissao requerida"))
                .lore(buildFieldLore(
                        "Define uma profissao obrigatoria para usar esta receita.|Quando configurado, so jogadores com a profissao e o nivel minimo informados poderao processar esse item.",
                        currentValueLine("Profissao", getProfessionDescription(), profession != null),
                        leftAction("alternar entre as profissoes disponiveis."),
                        rightAction("editar o nivel minimo exigido."),
                        shiftAction("remover a restricao de profissao."),
                        noteAction("se nenhuma profissao for definida, qualquer jogador pode usar.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                profession = null;
                professionLevel = 0;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                if (profession == null) {
                    player.sendMessage(MessageUtils.toComponent(sc("&cEscolha uma profissao primeiro.")));
                    return;
                }

                requestInt(player,
                        "&eDigite o nivel minimo da profissao:",
                        value -> professionLevel = Math.max(1, value));
                return;
            }

            List<String> professions = MMOCoreHook.getProfessionIds();
            if (professions.isEmpty()) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNenhuma profissao foi encontrada no MMOCore.")));
                return;
            }

            profession = cycleString(professions, profession);
            professionLevel = profession != null ? Math.max(1, professionLevel) : 0;
            open(player);
        });
    }

    private void setupExperienceSlot(Player player) {
        if (!MMOCoreHook.isAvailable()) {
            setItem(23, new ItemBuilder(Material.GRAY_DYE)
                    .name(sc("&cExperiencia indisponivel"))
                    .lore(buildFieldLore(
                            "A recompensa de experiencia tambem depende do MMOCore ativo no servidor.|Sem essa integracao, a receita nao pode premiar experiencia de profissao.",
                            currentValueLine("Status", "Plugin nao encontrado", false),
                            noteAction("instale ou ative o MMOCore para liberar esta configuracao.")
                    ))
                    .build());
            return;
        }

        setItem(23, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(sc("&aExperiencia concedida"))
                .lore(buildFieldLore(
                        "Premia o jogador com experiencia ao concluir esta receita.|Voce pode escolher a profissao que recebe a recompensa e depois definir a quantidade no chat.",
                        currentValueLine("Experiencia", getExperienceDescription(), experienceProfession != null && experienceReward > 0D),
                        leftAction("editar a quantidade de experiencia."),
                        rightAction("alternar a profissao que recebe a recompensa."),
                        shiftAction("remover a recompensa de experiencia."),
                        noteAction("a profissao precisa estar definida para o valor ser salvo corretamente.")
                ))
                .build(), e -> {
            if (e.isShiftClick()) {
                experienceProfession = null;
                experienceReward = 0D;
                open(player);
                return;
            }

            if (e.isRightClick()) {
                List<String> professions = MMOCoreHook.getProfessionIds();
                if (professions.isEmpty()) {
                    player.sendMessage(MessageUtils.toComponent(sc("&cNenhuma profissao foi encontrada no MMOCore.")));
                    return;
                }

                experienceProfession = cycleString(professions, experienceProfession);
                open(player);
                return;
            }

            requestDouble(player,
                    "&eDigite a quantidade de experiencia:",
                    value -> experienceReward = Math.max(0D, value));
        });
    }

    private void saveAndReturn(Player player) {
        RecipeEditorSaveValidator.ValidationResult validation = currentSaveValidation();
        if (validation.hasMissingRequirements()) {
            player.sendMessage(MessageUtils.toComponent(
                    sc("&cAinda faltam campos obrigatorios para salvar: &f"
                            + String.join(", ", validation.missingRequirements()))));
            currentPage = validation.firstMissingPage().index();
            open(player);
            return;
        }

        List<String> cleanLore = new ArrayList<>();
        for (String line : outputLore) {
            String clean = blankToNull(line);
            if (clean != null) {
                cleanLore.add(clean);
            }
        }

        MultiblockRecipe recipe = new MultiblockRecipe(
                recipeId,
                machineType,
                inputMaterial,
                inputMmoType,
                inputMmoId,
                inputItemsAdderId,
                Math.max(0, inputCustomModelData),
                outputMaterial,
                outputMmoType,
                outputMmoId,
                outputItemsAdderId,
                blankToNull(outputName),
                cleanLore.isEmpty() ? null : cleanLore,
                Math.max(0, outputCustomModelData),
                Math.max(0, spoiledCustomModelData),
                blankToNull(spoiledName),
                Math.max(1, timeMinutes),
                trait,
                requiresTrait,
                blankToNull(requiresRecipe),
                extraIngredients.isEmpty() ? List.of() : new ArrayList<>(extraIngredients),
                nutritionGroups.isEmpty() ? null : new ArrayList<>(nutritionGroups)
        );

        recipe.setProfession(blankToNull(profession));
        recipe.setProfessionLevel(recipe.getProfession() != null ? Math.max(1, professionLevel) : 0);
        recipe.setExperienceProfession(blankToNull(experienceProfession));
        recipe.setExperienceReward(recipe.getExperienceProfession() != null ? Math.max(0D, experienceReward) : 0D);

        module.getDecayConfig().saveRecipe(recipe);
        player.sendMessage(MessageUtils.toComponent(sc("&aReceita &f" + recipeId + " &asalva com sucesso.")));
        new RecipeListGui(module, machineType).open(player);
    }

    private ItemStack buildDisplaySlot() {
        ItemStack preview = null;
        if (outputItemsAdderId != null) {
            preview = ItemsAdderHook.createItem(outputItemsAdderId);
        } else if (outputMmoType != null && outputMmoId != null) {
            preview = MMOItemsHook.createItem(outputMmoType, outputMmoId);
        } else if (outputMaterial != null) {
            preview = new ItemStack(outputMaterial);
        }

        if (preview == null) {
            preview = new ItemStack(Material.STONE);
        }

        ItemStack item = preview.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (outputCustomModelData > 0) {
                meta.setCustomModelData(outputCustomModelData);
            }
            if (outputName != null) {
                meta.displayName(SERIALIZER.deserialize(outputName));
            } else if (!meta.hasDisplayName()) {
                meta.displayName(SERIALIZER.deserialize("&aPreview da saida"));
            }

            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            if (!outputLore.isEmpty()) {
                lore = new ArrayList<>();
                for (String line : outputLore) {
                    lore.add(SERIALIZER.deserialize(line));
                }
            }
            lore.add(Component.empty());
            lore.add(SERIALIZER.deserialize("&8Preview da Receita"));
            lore.add(SERIALIZER.deserialize("&7Este item representa o resultado atual"));
            lore.add(SERIALIZER.deserialize("&7com todas as configuracoes visuais aplicadas."));
            if (!nutritionGroups.isEmpty()) {
                lore.add(Component.empty());
                lore.add(SERIALIZER.deserialize("&8Nutricao"));
                lore.add(SERIALIZER.deserialize("&7" + getNutritionGroupsDescription()));
            }
            if (!extraIngredients.isEmpty()) {
                lore.add(Component.empty());
                lore.add(SERIALIZER.deserialize("&8Ingredientes extras"));
                lore.add(SERIALIZER.deserialize("&7" + getExtraIngredientsDescription()));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack buildExitSlot() {
        List<String> lore = new ArrayList<>(buildFieldLore(
                "Fecha a edicao atual e retorna para a lista de receitas da maquina.|Use este botao quando quiser sair da configuracao ou revisar outras receitas.",
                currentValueLine("Acao", "Sair sem salvar", true),
                leftAction("voltar para a lista de receitas.")
        ));
        if (!isNew) {
            lore.add(sc(shiftAction("excluir permanentemente esta receita.")));
        }

        return new ItemBuilder(Material.BARRIER)
                .name(sc("&cVoltar"))
                .lore(lore)
                .build();
    }

    private ItemStack buildMachineSlot() {
        FoodDecayConfig config = module.getDecayConfig();

        return new ItemBuilder(machineType.getIcon())
                .name(sc("&aMaquina"))
                .lore(buildTextLore(
                        "&8Contexto",
                        "&7Voce esta editando uma receita da maquina:",
                        "&f" + config.getMultiblockDisplayName(machineType),
                        "",
                        "&fReceita: &a" + recipeId,
                        "&fTempo base: &b" + machineType.getDefaultProcessingMinutes() + " min",
                        "&fTraco padrao: &d" + config.getTraitDisplayName(machineType.getResultTrait())
                ))
                .build();
    }

    private ItemStack buildStageOverviewSlot() {
        return switch (currentEditorPage()) {
            case RECIPE -> new ItemBuilder(Material.CRAFTING_TABLE)
                    .name(sc("&aEtapa 1: Base da receita"))
                    .lore(buildTextLore(
                            "&8Objetivo",
                            "&7Nesta primeira parte voce define o nucleo da receita.",
                            "&7Sem input e output, a receita nao pode avancar.",
                            "",
                            "&fEntrada: " + (hasInputConfigured() ? "&a" + getInputDescription() : "&cPendente"),
                            "&fSaida: " + (hasOutputConfigured() ? "&a" + getOutputDescription() : "&cPendente"),
                            "&fTempo: &b" + timeMinutes + " min",
                            "&fExtras: " + (!extraIngredients.isEmpty() ? "&e" + getExtraIngredientsDescription() : "&7Nenhum"),
                            "&fTraco: " + (trait != null ? "&d" + getTraitName(trait) : "&cPendente")
                    ))
                    .build();
            case APPEARANCE -> new ItemBuilder(Material.WRITABLE_BOOK)
                    .name(sc("&aEtapa 2: Aparencia"))
                    .lore(buildTextLore(
                            "&8Objetivo",
                            "&7Aqui voce ajusta como o item vai aparecer ao jogador.",
                            "&7Nome, lore e modelos visuais ficam todos nesta etapa.",
                            "",
                            "&fNome: " + (outputName != null ? "&aCustomizado" : "&7Padrao"),
                            "&fLore: " + (!outputLore.isEmpty() ? "&a" + outputLore.size() + " linhas" : "&7Nenhuma"),
                            "&fModelo saida: " + (outputCustomModelData > 0 ? "&b" + outputCustomModelData : "&7Padrao"),
                            "&fEstado estragado: " + (spoiledName != null || spoiledCustomModelData > 0 ? "&6Configurado" : "&7Nao usado")
                    ))
                    .build();
            case REQUIREMENTS -> new ItemBuilder(Material.PAPER)
                    .name(sc("&aEtapa 3: Requisitos"))
                    .lore(buildTextLore(
                            "&8Objetivo",
                            "&7Esta etapa controla quem pode entrar na receita.",
                            "&7Use apenas se quiser criar dependencia entre processos.",
                            "",
                            "&fTraco requerido: " + (requiresTrait != null ? "&d" + getTraitName(requiresTrait) : "&7Nenhum"),
                            "&fReceita requerida: " + (requiresRecipe != null ? "&a" + requiresRecipe : "&7Nenhuma"),
                            "&fNutricao final: " + (!nutritionGroups.isEmpty() ? "&a" + getNutritionGroupsDescription() : "&7Automatica"),
                            "",
                            "&7Se nada for definido, qualquer item",
                            "&7compativel podera usar a receita."
                    ))
                    .build();
            case MMOCORE -> new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                    .name(sc("&aEtapa 4: MMOCore"))
                    .lore(buildTextLore(
                            "&8Objetivo",
                            "&7Aqui ficam os extras opcionais ligados ao MMOCore.",
                            "&7Profissao e experiencia nao sao obrigatorios para salvar.",
                            "",
                            "&fProfissao: " + (profession != null ? "&a" + getProfessionDescription() : "&7Nenhuma"),
                            "&fExperiencia: " + (experienceProfession != null && experienceReward > 0D ? "&b" + getExperienceDescription() : "&7Nenhuma"),
                            "",
                            MMOCoreHook.isAvailable()
                                    ? "&aMMOCore detectado."
                                    : "&cMMOCore nao encontrado."
                    ))
                    .build();
        };
    }

    private ItemStack buildPreviousStageSlot() {
        if (currentPage == 0) {
            return new ItemBuilder(Material.ARROW)
                    .name(sc("&fVoltar para receitas"))
                    .lore(buildTextLore(
                            "&8Navegacao",
                            "&7Sai do editor atual e retorna",
                            "&7para a lista de receitas desta maquina."
                    ))
                    .build();
        }

        return new ItemBuilder(Material.ARROW)
                .name(sc("&fEtapa anterior"))
                .lore(buildTextLore(
                        "&8Navegacao",
                        "&7Volta para a parte anterior da configuracao.",
                        "&7Use quando quiser revisar ou corrigir algo.",
                        "",
                        "&fAtual: &e" + currentEditorPage().title(),
                        "&fDestino: &a" + pageFor(currentPage - 1).title()
                ))
                .build();
    }

    private ItemStack buildStageProgressSlot() {
        return new ItemBuilder(Material.MAP)
                .name(sc("&aFluxo da receita"))
                .lore(buildTextLore(
                        "&8Progresso",
                        "&fEtapa atual: &a" + (currentPage + 1) + "/" + PAGE_COUNT,
                        "&fNome: &e" + currentEditorPage().title(),
                        "",
                        "&8Etapas",
                        currentPage == RecipeEditorPage.RECIPE.index() ? "&a1. Base" : "&71. Base",
                        currentPage == RecipeEditorPage.APPEARANCE.index() ? "&a2. Aparencia" : "&72. Aparencia",
                        currentPage == RecipeEditorPage.REQUIREMENTS.index() ? "&a3. Requisitos" : "&73. Requisitos",
                        currentPage == RecipeEditorPage.MMOCORE.index() ? "&a4. MMOCore" : "&74. MMOCore"
                ))
                .build();
    }

    private ItemStack buildNextStageSlot() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&aProxima etapa"))
                .lore(buildTextLore(
                        "&8Navegacao",
                        "&7Avanca para a proxima parte da configuracao.",
                        "&7Cada etapa mostra apenas os campos daquele assunto.",
                        "",
                        "&fAtual: &e" + currentEditorPage().title(),
                        "&fDestino: &a" + pageFor(currentPage + 1).title()
                ))
                .build();
    }

    private ItemStack buildSaveSlot() {
        RecipeEditorSaveValidator.ValidationResult validation = currentSaveValidation();
        boolean ready = !validation.hasMissingRequirements();
        List<String> lore = new ArrayList<>();
        lore.add(sc("&8Informacoes"));
        lore.add(sc("&7Valida os campos obrigatorios"));
        lore.add(sc("&7e salva a receita."));

        lore.add("");
        lore.add(sc("&8Estado Atual"));
        lore.add(sc(currentValueLine("Pronto", ready ? "Sim" : "Ainda faltam campos", ready)));

        lore.add("");
        lore.add(sc("&8Campos Obrigatorios"));
        lore.add(sc(requirementStatusLine("Entrada", hasInputConfigured())));
        lore.add(sc(requirementStatusLine("Saida", hasOutputConfigured())));
        lore.add(sc(requirementStatusLine("Tempo", timeMinutes > 0)));
        lore.add(sc(requirementStatusLine("Traco", trait != null)));

        if (validation.hasMissingRequirements()) {
            lore.add(sc("&cFalta: &7" + String.join(", ", validation.missingRequirements())));
        }

        lore.add("");
        lore.add(sc("&8Acoes"));
        lore.add(sc(leftAction("salvar a receita e voltar para a lista.")));

        ItemBuilder builder = new ItemBuilder(Material.SPRUCE_SIGN)
                .name(sc("&aSalvar"))
                .lore(lore);

        if (ready) {
            builder.glow();
        }

        return builder.build();
    }

    private void captureInputFromHand(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            return;
        }

        ItemsAdderHook.ItemsAdderItemReference itemsAdderReference = ItemsAdderHook.identifyItem(handItem);
        if (itemsAdderReference != null) {
            inputMaterial = itemsAdderReference.material();
            inputMmoType = null;
            inputMmoId = null;
            inputItemsAdderId = itemsAdderReference.namespacedId();
            inputCustomModelData = 0;
            return;
        }

        MMOItemsHook.MMOItemReference mmoReference = MMOItemsHook.identifyItem(handItem);
        if (mmoReference != null) {
            inputMaterial = mmoReference.material();
            inputMmoType = mmoReference.type();
            inputMmoId = mmoReference.id();
            inputItemsAdderId = null;
            inputCustomModelData = 0;
            return;
        }

        inputMaterial = handItem.getType();
        inputMmoType = null;
        inputMmoId = null;
        inputItemsAdderId = null;

        ItemMeta meta = handItem.getItemMeta();
        inputCustomModelData = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
    }

    private void captureOutputFromHand(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            return;
        }

        ItemsAdderHook.ItemsAdderItemReference itemsAdderReference = ItemsAdderHook.identifyItem(handItem);
        MMOItemsHook.MMOItemReference mmoReference = MMOItemsHook.identifyItem(handItem);
        outputMaterial = handItem.getType();
        outputMmoType = itemsAdderReference == null && mmoReference != null ? mmoReference.type() : null;
        outputMmoId = itemsAdderReference == null && mmoReference != null ? mmoReference.id() : null;
        outputItemsAdderId = itemsAdderReference != null ? itemsAdderReference.namespacedId() : null;

        ItemMeta meta = handItem.getItemMeta();
        if (meta != null) {
            outputCustomModelData = outputItemsAdderId == null && outputMmoType == null
                    ? (meta.hasCustomModelData() ? meta.getCustomModelData() : 0)
                    : 0;
            outputName = meta.hasDisplayName() ? SERIALIZER.serialize(meta.displayName()) : null;

            outputLore = new ArrayList<>();
            if (meta.lore() != null) {
                for (Component line : meta.lore()) {
                    outputLore.add(SERIALIZER.serialize(line));
                }
            }
        } else {
            outputCustomModelData = 0;
            outputName = null;
            outputLore = new ArrayList<>();
        }

        nutritionGroups = detectCustomNutritionGroups(handItem);
    }

    private boolean hasHandItem(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        return handItem != null && !handItem.getType().isAir();
    }

    private String getInputDescription() {
        if (inputItemsAdderId != null) {
            return ItemsAdderHook.getItemReferenceLabel(inputItemsAdderId);
        }
        if (inputMmoType != null && inputMmoId != null) {
            return MMOItemsHook.getItemReferenceLabel(inputMmoType, inputMmoId);
        }
        if (inputMaterial != null) {
            String base = formatMaterial(inputMaterial);
            if (inputCustomModelData > 0) {
                return base + " (CMD " + inputCustomModelData + ")";
            }
            return base;
        }
        return "Nao definida";
    }

    private String getOutputDescription() {
        String baseDisplayName = getOutputBaseDisplayName();
        String baseReferenceLabel = getOutputBaseReferenceLabel();

        if (outputName != null) {
            String name = stripLegacy(outputName);
            if (baseReferenceLabel != null && (baseDisplayName == null || !name.equalsIgnoreCase(baseDisplayName))) {
                return name + " (base: " + baseReferenceLabel + ")";
            }
            return name;
        }

        if (baseReferenceLabel != null) {
            return baseReferenceLabel;
        }
        return "Nao definida";
    }

    private ItemStack buildInputSlotItem() {
        if (inputItemsAdderId != null) {
            ItemStack preview = ItemsAdderHook.createItem(inputItemsAdderId);
            if (preview != null) {
                return preview;
            }
        }
        if (inputMmoType != null && inputMmoId != null) {
            ItemStack preview = MMOItemsHook.createItem(inputMmoType, inputMmoId);
            if (preview != null) {
                return preview;
            }
        }
        return new ItemStack(inputMaterial != null ? inputMaterial : Material.CHEST);
    }

    private ItemStack buildOutputSlotItem() {
        if (outputItemsAdderId != null) {
            ItemStack preview = ItemsAdderHook.createItem(outputItemsAdderId);
            if (preview != null) {
                return preview;
            }
        }
        if (outputMmoType != null && outputMmoId != null) {
            ItemStack preview = MMOItemsHook.createItem(outputMmoType, outputMmoId);
            if (preview != null) {
                return preview;
            }
        }
        return new ItemStack(outputMaterial != null ? outputMaterial : Material.CHEST);
    }

    private String getOutputBaseDisplayName() {
        if (outputItemsAdderId != null) {
            return ItemsAdderHook.getItemDisplayName(outputItemsAdderId);
        }
        if (outputMmoType != null && outputMmoId != null) {
            return MMOItemsHook.getItemDisplayName(outputMmoType, outputMmoId);
        }
        if (outputMaterial != null) {
            return formatMaterial(outputMaterial);
        }
        return null;
    }

    private String getOutputBaseReferenceLabel() {
        if (outputItemsAdderId != null) {
            return ItemsAdderHook.getItemReferenceLabel(outputItemsAdderId);
        }
        if (outputMmoType != null && outputMmoId != null) {
            return MMOItemsHook.getItemReferenceLabel(outputMmoType, outputMmoId);
        }
        if (outputMaterial != null) {
            return formatMaterial(outputMaterial);
        }
        return null;
    }

    private String getTraitName(FoodTrait selectedTrait) {
        if (selectedTrait == null) {
            return "Nenhum";
        }
        return module.getDecayConfig().getTraitDisplayName(selectedTrait);
    }

    private String getProfessionDescription() {
        if (profession == null) {
            return "Nao definida";
        }
        return profession + " Lv." + Math.max(1, professionLevel);
    }

    private String getExperienceDescription() {
        if (experienceProfession == null || experienceReward <= 0D) {
            return "Nao configurada";
        }
        return experienceProfession + " +" + formatDouble(experienceReward) + " exp";
    }

    private String getExtraIngredientsDescription() {
        if (extraIngredients == null || extraIngredients.isEmpty()) {
            return "Nenhum";
        }

        List<String> labels = new ArrayList<>();
        for (RecipeIngredient ingredient : extraIngredients) {
            labels.add(ingredient.getReferenceLabel());
            if (labels.size() >= 3) {
                break;
            }
        }

        if (extraIngredients.size() > labels.size()) {
            labels.add("+" + (extraIngredients.size() - labels.size()) + " item(ns)");
        }
        return String.join(", ", labels);
    }

    private String getNutritionGroupsDescription() {
        if (nutritionGroups == null || nutritionGroups.isEmpty()) {
            return "Automatico pelo material";
        }

        List<String> labels = new ArrayList<>();
        for (String groupName : nutritionGroups) {
            try {
                NutritionManager.FoodGroup group = NutritionManager.FoodGroup.valueOf(groupName);
                labels.add(module.getNutritionManager().getGroupDisplayName(group));
            } catch (IllegalArgumentException ignored) {
                labels.add(groupName);
            }
        }
        return String.join(", ", labels);
    }

    private RecipeEditorPage currentEditorPage() {
        return RecipeEditorPage.fromIndex(currentPage);
    }

    private RecipeEditorPage pageFor(int page) {
        return RecipeEditorPage.fromIndex(page);
    }

    private RecipeEditorSaveValidator.ValidationResult currentSaveValidation() {
        return RecipeEditorSaveValidator.validate(
                hasInputConfigured(),
                hasOutputConfigured(),
                timeMinutes,
                trait,
                profession,
                professionLevel,
                experienceProfession,
                experienceReward
        );
    }

    private boolean hasInputConfigured() {
        return inputMaterial != null
                || (inputMmoType != null && inputMmoId != null)
                || inputItemsAdderId != null;
    }

    private boolean hasOutputConfigured() {
        return outputMaterial != null
                || (outputMmoType != null && outputMmoId != null)
                || outputItemsAdderId != null;
    }

    private List<String> normalizeNutritionGroups(List<String> rawGroups) {
        List<String> normalized = new ArrayList<>();
        var parsed = module.getNutritionManager().parseFoodGroups(rawGroups);
        for (NutritionManager.FoodGroup group : NutritionManager.FoodGroup.values()) {
            if (parsed.contains(group)) {
                normalized.add(group.name());
            }
        }
        return normalized;
    }

    private List<RecipeIngredient> parseExtraIngredients(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new ArrayList<>();
        }

        List<RecipeIngredient> parsed = new ArrayList<>();
        for (String token : rawValue.split("\\|")) {
            String clean = token.trim();
            if (clean.isEmpty()) {
                continue;
            }

            RecipeIngredient ingredient = RecipeIngredient.parseShorthand(clean);
            if (ingredient == null) {
                return null;
            }
            parsed.add(ingredient);
        }
        return parsed;
    }

    private List<String> detectCustomNutritionGroups(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return new ArrayList<>();
        }

        var nutrition = module.getNutritionManager();
        var effectiveGroups = nutrition.getFoodGroups(item);
        if (effectiveGroups.isEmpty() || effectiveGroups.equals(nutrition.getFoodGroups(item.getType()))) {
            return new ArrayList<>();
        }

        List<String> detected = new ArrayList<>();
        for (NutritionManager.FoodGroup group : NutritionManager.FoodGroup.values()) {
            if (effectiveGroups.contains(group)) {
                detected.add(group.name());
            }
        }
        return detected;
    }

    private void requestText(Player player, String prompt, Consumer<String> callback) {
        ChatInput.request(player, sc(prompt), value -> {
            callback.accept(value.trim());
            open(player);
        }, () -> open(player));
    }

    private void requestInt(Player player, String prompt, IntConsumer callback) {
        ChatInput.request(player, sc(prompt), value -> {
            try {
                callback.accept(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNumero invalido.")));
            }
            open(player);
        }, () -> open(player));
    }

    private void requestDouble(Player player, String prompt, DoubleConsumer callback) {
        ChatInput.request(player, sc(prompt), value -> {
            try {
                callback.accept(Double.parseDouble(value.trim().replace(',', '.')));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNumero invalido.")));
            }
            open(player);
        }, () -> open(player));
    }

    private static String cycleString(List<String> values, String current) {
        if (values.isEmpty()) {
            return null;
        }
        if (current == null) {
            return values.getFirst();
        }
        int index = values.indexOf(current);
        if (index < 0 || index + 1 >= values.size()) {
            return null;
        }
        return values.get(index + 1);
    }

    private static <E extends Enum<E>> E cycleEnum(Class<E> enumClass, E current) {
        E[] values = enumClass.getEnumConstants();
        if (values.length == 0) {
            return null;
        }
        if (current == null) {
            return values[0];
        }
        int nextIndex = current.ordinal() + 1;
        return nextIndex >= values.length ? null : values[nextIndex];
    }

    private static String stripLegacy(String value) {
        return value.replaceAll("(?i)&[0-9A-FK-OR]", "").trim();
    }
}
