package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.listener;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryRegistry;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryBlueprintItem;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryGhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryGhostBlockSession;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelterySchematic;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

/**
 * Listener de eventos para construção de smelteries:
 * - Blueprint item (clique direito no chão) → inicia preview
 * - Shift + clique direito durante preview → confirma e inicia construção
 * - Colocação de blocos durante construção → valida contra schematic
 * - Proteção de blocos de smelteries contra quebra
 */
public class SmelteryBuildListener implements Listener {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private final SmelteryGhostBlockManager ghostBlockManager;
    private final SmelteryRegistry registry;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private BiConsumer<Player, SmelteryTier> onBlueprintValidation;

    public SmelteryBuildListener(SmelteryGhostBlockManager ghostBlockManager, SmelteryRegistry registry) {
        this.ghostBlockManager = ghostBlockManager;
        this.registry = registry;
    }

    /**
     * Interações com blueprint:
     * 1. Clique direito com blueprint no chão → inicia preview
     * 2. Shift + clique direito durante preview → confirma construção
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getHand() != EquipmentSlot.HAND) { return; }
        if (event.getClickedBlock() == null) { return; }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Caso 1: Jogador clica com item de blueprint
        if (SmelteryBlueprintItem.isBlueprint(item)) {
            event.setCancelled(true);

            SmelteryTier tier = SmelteryBlueprintItem.getTier(item);
            if (tier == null) { return; }

            SmelteryGhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
            if (session != null && session.isPreviewing()) {
                if (player.isSneaking()) {
                    ghostBlockManager.confirmBuild(player);
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.sendMessage(mm.deserialize(
                            msg("smeltery.ghost.preview_hint2")));
                }
                return;
            }

            if (session != null && session.isBuilding()) {
                player.sendMessage(mm.deserialize(msg("smeltery.build.already_building")));
                return;
            }

            if (onBlueprintValidation != null) {
                onBlueprintValidation.accept(player, tier);
                return;
            }

            // Fallback: iniciar preview direto
            Location anchor = event.getClickedBlock().getLocation().add(0, 1, 0);
            ghostBlockManager.startPreview(player, tier,
                    SmelterySchematic.createBasicSmeltery(), anchor);
            return;
        }

        // Caso 2: Shift + clique direito no chão durante preview (sem blueprint na mão)
        if (player.isSneaking()) {
            SmelteryGhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
            if (session != null && session.isPreviewing()) {
                event.setCancelled(true);
                ghostBlockManager.confirmBuild(player);
            }
        }
    }

    /**
     * Colocação de blocos durante construção.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!ghostBlockManager.hasSession(player.getUniqueId())) { return; }

        SmelteryGhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
        if (session == null || !session.isBuilding()) { return; }

        Location placedLoc = event.getBlock().getLocation();
        SmelteryGhostBlockSession.PlaceResult result = ghostBlockManager.onBlockPlaced(player, placedLoc);

        if (result == null) { return; }

        switch (result) {
            case WRONG_BLOCK -> event.setCancelled(true);
            case NOT_PART_OF_SCHEMATIC -> { /* permite colocação normal fora do schematic */ }
        }
    }

    /**
     * Proteção de smelteries contra quebra por não-donos.
     * Também rastreia quebras durante sessões de construção.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location brokenLoc = event.getBlock().getLocation();

        // Verificar se o bloco faz parte de uma smeltery ativa
        SmelteryStructure smeltery = registry.getAtLocation(brokenLoc);
        if (smeltery != null) {
            SmelteryBlockType blockType = smeltery.getBlockTypeAt(brokenLoc);
            if (blockType != null) {
                if (!smeltery.getOwnerUuid().equals(player.getUniqueId())
                        && !player.hasPermission("midgard.smeltery.admin.break")) {
                    event.setCancelled(true);
                    player.sendActionBar(mm.deserialize(msg("smeltery.build.cannot_destroy_other")));
                    return;
                }
            }
        }

        // Atualizar sessão de construção se o jogador está construindo
        if (ghostBlockManager.hasSession(player.getUniqueId())) {
            ghostBlockManager.onBlockBroken(player, brokenLoc);
        }
    }

    public void setOnBlueprintValidation(BiConsumer<Player, SmelteryTier> callback) {
        this.onBlueprintValidation = callback;
    }
}
