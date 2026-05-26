package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class ParticleEditorGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    private Particle particleType = Particle.FLAME;
    private int count = 1;
    private double offsetX = 0.5;
    private double offsetY = 0.5;
    private double offsetZ = 0.5;
    private double speed = 0.1;
    private String data = "";

    public ParticleEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.particle_editor.title"));
        this.item = item;
        this.parent = parent;
        parseParticleString(item.getItemParticles());
    }

    private void parseParticleString(String particleStr) {
        if (particleStr == null || particleStr.isEmpty()) {
            return;
        }
        
        try {
            String[] parts = particleStr.split(":");
            if (parts.length >= 1) {
                particleType = Particle.valueOf(parts[0].toUpperCase());
            }
            if (parts.length >= 2) {
                count = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                offsetX = Double.parseDouble(parts[2]);
            }
            if (parts.length >= 4) {
                offsetY = Double.parseDouble(parts[3]);
            }
            if (parts.length >= 5) {
                offsetZ = Double.parseDouble(parts[4]);
            }
            if (parts.length >= 6) {
                speed = Double.parseDouble(parts[5]);
            }
            if (parts.length >= 7) {
                data = parts[6];
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
    }

    private String serializeParticle() {
        if (particleType == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(particleType.name()).append(":");
        sb.append(count).append(":");
        sb.append(offsetX).append(":");
        sb.append(offsetY).append(":");
        sb.append(offsetZ).append(":");
        sb.append(speed);
        if (data != null && !data.isEmpty()) {
            sb.append(":").append(data);
        }
        return sb.toString();
    }

    private void save() {
        item.setItemParticles(serializeParticle());
        item.save();
    }

    @Override
    public void initializeItems() {
        // Display Current Particle
        ItemBuilder displayBuilder = new ItemBuilder(Material.NETHER_STAR)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.item.name"));
        
        List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.particle_editor.item.lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MessageUtils.parse(line
                    .replace("%type%", particleType.name())
                    .replace("%count%", String.valueOf(count))
                    .replace("%offsetX%", String.valueOf(offsetX))
                    .replace("%offsetY%", String.valueOf(offsetY))
                    .replace("%offsetZ%", String.valueOf(offsetZ))
                    .replace("%speed%", String.valueOf(speed))
                    .replace("%data%", data)));
        }
        displayBuilder.lore(lore);
        inventory.setItem(4, displayBuilder.build());

        // Buttons
        inventory.setItem(10, new ItemBuilder(Material.PAPER).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.type")).build());
        inventory.setItem(11, new ItemBuilder(Material.REDSTONE).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.count")).build());
        inventory.setItem(12, new ItemBuilder(Material.COMPASS).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.offset")).build());
        inventory.setItem(13, new ItemBuilder(Material.FEATHER).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.speed")).build());
        inventory.setItem(14, new ItemBuilder(Material.WRITABLE_BOOK).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.data")).build());
        
        inventory.setItem(18, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.back")).build());
        inventory.setItem(26, new ItemBuilder(Material.LAVA_BUCKET).name(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.buttons.clear")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            parent.open();
        } else if (slot == 26) {
            item.setItemParticles(null);
            item.save();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.updated"));
            parent.open();
        } else if (slot == 10) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.prompt_type"));
            ChatInputListener.requestInput(player, (input) -> {
                try {
                    particleType = Particle.valueOf(input.toUpperCase());
                    save();
                    this.open();
                } catch (IllegalArgumentException e) {
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-particle")); // Reusing invalid message for now
                    this.open();
                }
            });
        } else if (slot == 11) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.prompt_count"));
            ChatInputListener.requestInput(player, (input) -> {
                try {
                    count = Integer.parseInt(input);
                    save();
                    this.open();
                } catch (NumberFormatException e) {
                    this.open();
                }
            });
        } else if (slot == 12) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.prompt_offset"));
            ChatInputListener.requestInput(player, (input) -> {
                try {
                    String[] parts = input.split(",");
                    if (parts.length >= 3) {
                        offsetX = Double.parseDouble(parts[0]);
                        offsetY = Double.parseDouble(parts[1]);
                        offsetZ = Double.parseDouble(parts[2]);
                        save();
                    }
                    this.open();
                } catch (NumberFormatException e) {
                    this.open();
                }
            });
        } else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.prompt_speed"));
            ChatInputListener.requestInput(player, (input) -> {
                try {
                    speed = Double.parseDouble(input);
                    save();
                    this.open();
                } catch (NumberFormatException e) {
                    this.open();
                }
            });
        } else if (slot == 14) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.particle_editor.messages.prompt_data"));
            ChatInputListener.requestInput(player, (input) -> {
                data = input;
                save();
                this.open();
            });
        }
    }
}
