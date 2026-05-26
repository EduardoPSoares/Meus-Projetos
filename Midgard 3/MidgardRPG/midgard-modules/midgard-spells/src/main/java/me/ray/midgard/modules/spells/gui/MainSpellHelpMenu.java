package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.gui.BaseHelpMenu;
import me.ray.midgard.modules.spells.SpellsModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Menu de ajuda interativo para o sistema principal de magias.
 * Fornece informações sobre modos de conjuração e dicas.
 */
public class MainSpellHelpMenu extends BaseHelpMenu {
    
    private final SpellsModule module;
    
    private static final String[] TOPICS = {
        "overview",
        "modes",
        "skillbar",
        "combo",
        "reset",
        "tips"
    };
    
    public MainSpellHelpMenu(Player player, SpellsModule module, BaseGui parentMenu) {
        super(player, 
              module.getMessage("main_gui.help.title"),
              TOPICS,
              0,
              28,
              parentMenu);
        this.module = module;
    }
    
    private MainSpellHelpMenu(Player player, SpellsModule module, BaseGui parentMenu, int page) {
        super(player,
              module.getMessage("main_gui.help.title"),
              TOPICS,
              page,
              28,
              parentMenu);
        this.module = module;
    }
    
    @Override
    protected Material getTopicMaterial(String topicId) {
        return switch (topicId) {
            case "overview" -> Material.BOOK;
            case "modes" -> Material.REPEATER;
            case "skillbar" -> Material.IRON_SWORD;
            case "combo" -> Material.BLAZE_ROD;
            case "reset" -> Material.BARRIER;
            case "tips" -> Material.LANTERN;
            default -> Material.PAPER;
        };
    }
    
    @Override
    protected String getTopicName(String topicId) {
        return module.getMessage("main_gui.help.topics." + topicId + ".name");
    }
    
    @Override
    protected List<String> getTopicLore(String topicId) {
        return module.getMessageList("main_gui.help.topics." + topicId + ".lore");
    }
    
    @Override
    protected BaseHelpMenu createNewPage(int page) {
        return new MainSpellHelpMenu(player, module, parentMenu, page);
    }
}
