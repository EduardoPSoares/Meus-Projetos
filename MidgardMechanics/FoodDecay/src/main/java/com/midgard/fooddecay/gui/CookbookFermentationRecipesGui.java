package com.midgard.fooddecay.gui;

import com.midgard.core.gui.PaginatedMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FermentationManager;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.midgard.core.utils.MessageUtils.sc;

public class CookbookFermentationRecipesGui extends PaginatedMenu {

    private final FoodDecayModule module;
    private final FoodDecayConfig config;

    public CookbookFermentationRecipesGui(FoodDecayModule module) {
        super(sc("&8Receitas - Fermentacao"), 6);
        this.module = module;
        this.config = module.getDecayConfig();
    }

    @Override
    public ItemStack getPreviousPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&ePagina anterior"))
                .build();
    }

    @Override
    public ItemStack getNextPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&eProxima pagina"))
                .build();
    }

    @Override
    public void setupDecoration(Player player) {
        fillBorder(ItemBuilder.placeholder(Material.BROWN_STAINED_GLASS_PANE));

        Collection<FoodDecayConfig.FermentRecipe> recipes = config.getFermentRecipes();

        clearPageItems();
        if (recipes.isEmpty()) {
            addPageItem(new ItemBuilder(Material.BARRIER)
                    .name(sc("&cNenhuma receita cadastrada"))
                    .lore(
                            sc("&7Nao ha bebidas configuradas"),
                            sc("&7para fermentacao.")
                    )
                    .build());
        } else {
            FermentationManager fermentManager = module.getFermentationManager();
            for (FoodDecayConfig.FermentRecipe recipe : recipes) {
                addPageItem(buildFermentationRecipe(recipe, fermentManager));
            }
        }

        setItem(4, new ItemBuilder(Material.BARREL)
                .name(sc("&5&lFermentacao"))
                .lore(
                        sc("&7Receitas de bebidas catalogadas."),
                        sc("&7Use um barril de fermentacao"),
                        sc("&7para preparar cada opcao."),
                        "",
                        sc("&7Receitas: &f" + recipes.size()),
                        sc("&7Pagina atual: &f" + (getCurrentPage() + 1) + "&8/&f" + Math.max(1, getTotalPages()))
                )
                .glow()
                .build());

        setItem(2, new ItemBuilder(Material.WATER_BUCKET)
                .name(sc("&e&lBase liquida"))
                .lore(
                        sc("&8| &7Processo: &fBarril de fermentacao"),
                        sc("&8| &7Tempo: &fCada bebida possui"),
                        sc("&8| &7sua propria duracao")
                )
                .build());

        setItem(6, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(sc("&e&lLeitura do caderno"))
                .lore(
                        sc("&7Cada bebida mostra o liquido base,"),
                        sc("&7o volume necessario e os efeitos."),
                        "",
                        sc("&7Aqui as receitas ficam todas visiveis,"),
                        sc("&7sem misturar com as maquinas.")
                )
                .build());

        setItem(49, new ItemBuilder(Material.ARROW)
                .name(sc("&fVoltar"))
                .lore(sc("&7Retornar para as categorias"))
                .build(), e -> new CookbookGui(module).open(player));

        setItem(50, new ItemBuilder(Material.BARRIER)
                .name(sc("&cFechar"))
                .build(), e -> player.closeInventory());
    }

    private ItemStack buildFermentationRecipe(FoodDecayConfig.FermentRecipe recipe,
                                              FermentationManager fermentManager) {
        String liquidName = fermentManager != null
                ? fermentManager.getLiquidDisplayName(recipe.inputLiquid())
                : recipe.inputLiquid();
        String displayName = displayNameForMenu(recipe.displayName(), recipe.resultMaterial());

        List<String> lore = new ArrayList<>();
        lore.add(sc("&8| &7Liquido base: &f" + liquidName));
        lore.add(sc("&8| &7Volume: &f" + recipe.requiredMb() + " mB"));
        lore.add(sc("&8| &7Tempo: &f" + recipe.timeMinutes() + " min"));

        if (!recipe.effects().isEmpty()) {
            lore.add("");
            lore.add(sc("&eEfeitos:"));
            for (FoodDecayConfig.DrinkEffect effect : recipe.effects()) {
                lore.add(sc("&8- &d" + formatEffect(effect.type(), effect.amplifier())
                        + " &7(" + (effect.durationTicks() / 20) + "s)"));
            }
        }

        lore.add("");
        lore.add(sc("&aReceita catalogada"));

        Material icon = recipe.resultMaterial() != null ? recipe.resultMaterial() : Material.POTION;

        return new ItemBuilder(icon)
                .name(sc("&f" + displayName))
                .lore(lore)
                .glow()
                .build();
    }

    private String formatEffect(PotionEffectType type, int amplifier) {
        String name = type == null ? "?" : type.getKey().getKey().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        if (amplifier <= 0) {
            return builder.toString();
        }

        return builder + " " + toRoman(amplifier + 1);
    }

    private String displayNameForMenu(String rawName, Material fallback) {
        if (rawName == null || rawName.isBlank()) {
            return fallback != null ? formatMaterial(fallback.name()) : "?";
        }

        String plain = rawName.replaceAll("(?i)&[0-9A-FK-OR]", "");
        plain = plain.replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
        if (!plain.isEmpty()) {
            return plain;
        }

        return fallback != null ? formatMaterial(fallback.name()) : "?";
    }

    private String formatMaterial(String materialName) {
        String[] parts = materialName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0))
                    .append(part.substring(1).toLowerCase());
        }
        return builder.toString();
    }

    private String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }
}
