package me.ray.midgard.modules.character.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.character.CharacterModule;
import me.ray.midgard.modules.character.gui.CharacterMenu;
import me.ray.midgard.modules.character.i18n.CharacterMessages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CharacterCommand extends MidgardCommand {

    public CharacterCommand() {
        super("character", null, true);
    }

    @Override
    public List<String> getAliases() {
        return List.of("char", "stats", "attributes");
    }

    @Override
    public String getDescription() {
        CharacterModule module = CharacterModule.getInstance();
        return module != null ? module.getMessage("command.description") : CharacterMessages.COMMAND_DESCRIPTION.getDefaultValue();
    }

    @Override
    public String getUsage() {
        CharacterModule module = CharacterModule.getInstance();
        return module != null ? module.getMessage("command.usage") : CharacterMessages.COMMAND_USAGE.getDefaultValue();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            CharacterModule module = CharacterModule.getInstance();
            String msg = (module != null) ? module.getMessage("errors.player_only") : CharacterMessages.ERROR_PLAYER_ONLY.getDefaultValue();
            MessageUtils.send(sender, msg);
            return;
        }
        
        Player player = (Player) sender;
        CharacterModule module = CharacterModule.getInstance();
        if (module == null) {
            MessageUtils.send(player, CharacterMessages.ERROR_MODULE_UNAVAILABLE);
            return;
        }
        new CharacterMenu(player).open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
