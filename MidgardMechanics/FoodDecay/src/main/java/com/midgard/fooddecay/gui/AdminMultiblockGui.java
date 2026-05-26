package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Admin GUI for multiblock system settings, QTE, proximity and per-machine access.
 */
public class AdminMultiblockGui extends AdminBaseGui {

    public AdminMultiblockGui(FoodDecayModule module) {
        super("&8\u2699 Máquinas & Multiblocks", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminMultiblockGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.SMITHING_TABLE)
                .name(sc("&3&lSistema de Multiblocks"))
                .lore(sc("&7Configurações globais de máquinas,"),
                      sc("&7QTE, proximidade e sessão."))
                .build());

        // ── Row 1: Global toggles & basics ──
        setItem(10, toggle(Material.SMITHING_TABLE, "&3&lMultiblock Ativado",
                config.isMultiblockEnabled(),
                "&7Ativa/desativa todo o sistema", "&7de máquinas multiblock."),
                e -> { config.saveValue("multiblock.enabled", !config.isMultiblockEnabled()); reopen.run(); });

        setItem(12, val(Material.BELL, "&e&lRaio de Notificação",
                config.getNotificationRadius() + " blocos",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o raio de notificação (blocos):",
                        "multiblock.notification-radius", 1, reopen));

        setItem(14, val(Material.CLOCK, "&c&lAbandono (min)",
                config.getAbandonmentMinutes() + " min",
                "&eClique para editar (0 = desativado)"),
                e -> editInt(player, "&eDigite os minutos de abandono (0 = desativado):",
                        "multiblock.abandonment-minutes", 0, reopen));

        setItem(16, val(Material.COMPASS, "&6&lTimeout Sessão",
                config.getSessionTimeoutMinutes() + " min",
                "&eClique para editar (0 = sem timeout)"),
                e -> editInt(player, "&eDigite o timeout da sessão (minutos):",
                        "multiblock.session-timeout-minutes", 0, reopen));

        // ── Row 2: Proximity & QTE ──
        setItem(19, val(Material.ENDER_EYE, "&d&lRaio de Proximidade",
                config.getProximityRadius() + " blocos",
                "&eClique para editar (0 = desativado)"),
                e -> editInt(player, "&eDigite o raio de proximidade (blocos):",
                        "multiblock.proximity-radius", 0, reopen));

        setItem(21, val(Material.EXPERIENCE_BOTTLE, "&a&lBônus Proximidade/Tick",
                config.getProximityBonusPerTick(),
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o bônus por tick:",
                        "multiblock.proximity-bonus-per-tick", 0.0, reopen));

        setItem(23, val(Material.REDSTONE, "&c&lPenalidade QTE Miss",
                config.getQteMissPenalty(),
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite a penalidade por QTE perdido:",
                        "multiblock.qte-miss-penalty", 0.0, reopen));

        // QTE header — click to edit bonus per event
        setItem(25, new ItemBuilder(Material.LIGHTNING_ROD)
                .name(sc("&e&l\u26A1 Sistema QTE"))
                .lore("", sc("&7Intervalo: &f" + config.getQteIntervalSeconds() + "s"),
                      sc("&7Duração: &f" + config.getQteDurationSeconds() + "s"),
                      sc("&7Chance: &f" + (int) (config.getQteChance() * 100) + "%"),
                      sc("&7Max/Ciclo: &f" + config.getQteMaxPerCycle()),
                      sc("&7Bônus/Evento: &f" + config.getQteBonusPerEvent()),
                      "", sc("&eClique para editar o bônus"))
                .glow().build(),
                e -> editDouble(player, "&eDigite o bônus por evento QTE resolvido:",
                        "multiblock.qte.bonus-per-event", 0.0, reopen));

        // ── Row 3: QTE details ──
        setItem(28, val(Material.CLOCK, "&e&lIntervalo QTE",
                config.getQteIntervalSeconds() + "s",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o intervalo entre QTEs (segundos):",
                        "multiblock.qte.interval-seconds", 1, reopen));

        setItem(30, val(Material.CLOCK, "&e&lDuração QTE",
                config.getQteDurationSeconds() + "s",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite a duração do QTE (segundos):",
                        "multiblock.qte.duration-seconds", 1, reopen));

        setItem(32, val(Material.RABBIT_FOOT, "&e&lChance QTE",
                (int) (config.getQteChance() * 100) + "%",
                "&eClique para editar (0.0 - 1.0)"),
                e -> editDouble(player, "&eDigite a chance (ex: 0.5 = 50%):",
                        "multiblock.qte.chance", 0.0, reopen));

        setItem(34, val(Material.REPEATER, "&e&lMax QTE/Ciclo",
                config.getQteMaxPerCycle(),
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o máximo de QTEs por ciclo:",
                        "multiblock.qte.max-per-cycle", 0, reopen));

        // ── Row 4: Per-machine buttons ──
        MultiblockType[] types = MultiblockType.values();
        int[] machineSlots = centerSlots(37, 43, types.length);
        for (int i = 0; i < types.length; i++) {
            MultiblockType type = types[i];
            boolean on = config.isMultiblockTypeEnabled(type);
            String displayName = config.getMultiblockDisplayName(type);
            int procMin = config.getMultiblockProcessingMinutes(type);

            setItem(machineSlots[i], new ItemBuilder(on ? type.getIcon() : Material.GRAY_DYE)
                    .name(sc((on ? "&a" : "&c") + "&l" + displayName))
                    .lore("", sc("&7Tempo: &f" + procMin + " min"),
                          sc("&7Receitas: &f" + (config.hasRecipes(type) ? config.getRecipes(type).size() : 0)),
                          "", sc(on ? "&a\u2714 Ativado" : "&c\u2718 Desativado"),
                          "", sc("&eClique para gerenciar"))
                    .build(),
                    e -> new AdminMachineGui(module, type).open(player));
        }

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }

    /**
     * Centers N items within a slot range [start..end] of the same row.
     */
    private static int[] centerSlots(int start, int end, int count) {
        int range = end - start + 1;
        count = Math.min(count, range);
        int offset = (range - count) / 2;
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = start + offset + i;
        }
        return slots;
    }
}
