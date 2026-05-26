package me.ray.midgard.core.permission;

import org.bukkit.command.CommandSender;

/**
 * Gerenciador centralizado de permissões do MidgardRPG.
 * Define todas as permissões usadas no sistema de comandos.
 */
public final class PermissionManager {

    private PermissionManager() {
        // Classe utilitária
    }

    // Permissões principais
    public static final String ADMIN = "midgard.admin";
    public static final String MODERATOR = "midgard.moderator";
    public static final String PLAYER = "midgard.player";

    // Permissões de comandos administrativos
    public static final String ADMIN_RELOAD = "midgard.admin.reload";
    public static final String ADMIN_RESET = "midgard.admin.reset";
    public static final String ADMIN_SCAN = "midgard.admin.scan";
    public static final String ADMIN_PERFORMANCE = "midgard.admin.performance";

    // Permissões admin por módulo
    public static final String ADMIN_XP = "midgard.admin.xp";
    public static final String ADMIN_XP_ADD = "midgard.admin.xp.add";
    public static final String ADMIN_XP_SET = "midgard.admin.xp.set";
    public static final String ADMIN_XP_TAKE = "midgard.admin.xp.take";

    public static final String ADMIN_CLASS = "midgard.admin.class";
    public static final String ADMIN_CLASS_SET = "midgard.admin.class.set";
    public static final String ADMIN_CLASS_LIST = "midgard.admin.class.list";

    public static final String ADMIN_RACE = "midgard.admin.race";
    public static final String ADMIN_RACE_SET = "midgard.admin.race.set";
    public static final String ADMIN_RACE_RESET = "midgard.admin.race.reset";
    public static final String ADMIN_RACE_EXP = "midgard.admin.race.exp";
    public static final String ADMIN_RACE_RELOAD = "midgard.admin.race.reload";
    public static final String ADMIN_RACE_INFO = "midgard.admin.race.info";

    public static final String ADMIN_ITEM = "midgard.admin.item";
    public static final String ADMIN_ITEM_GIVE = "midgard.admin.item.give";
    public static final String ADMIN_ITEM_RELOAD = "midgard.admin.item.reload";
    public static final String ADMIN_ITEM_REFINE = "midgard.admin.item.refine";

    public static final String ADMIN_ECONOMY = "midgard.admin.economy";
    public static final String ADMIN_ECONOMY_GIVE = "midgard.admin.economy.give";
    public static final String ADMIN_ECONOMY_TAKE = "midgard.admin.economy.take";
    public static final String ADMIN_ECONOMY_SET = "midgard.admin.economy.set";
    public static final String ADMIN_ECONOMY_BALANCE = "midgard.admin.economy.balance";

    public static final String ADMIN_SPELL_LEARN = "midgard.admin.spell.learn";
    public static final String ADMIN_SPELL_UNLEARN = "midgard.admin.spell.unlearn";
    public static final String ADMIN_SPELL_SETLEVEL = "midgard.admin.spell.setlevel";
    public static final String ADMIN_SPELL_GIVESCROLL = "midgard.admin.spell.givescroll";

    public static final String ADMIN_COMMANDS = "midgard.admin.commands";

    // Permissões de essentials
    public static final String ESSENTIALS_GAMEMODE = "midgard.essentials.gamemode";
    public static final String ESSENTIALS_FLY = "midgard.essentials.fly";
    public static final String ESSENTIALS_HEAL = "midgard.essentials.heal";
    public static final String ESSENTIALS_FEED = "midgard.essentials.feed";
    public static final String ESSENTIALS_SPAWN = "midgard.essentials.spawn";
    public static final String ESSENTIALS_SETSPAWN = "midgard.essentials.setspawn";
    public static final String ESSENTIALS_WARP = "midgard.essentials.warp";
    public static final String ESSENTIALS_SETWARP = "midgard.essentials.setwarp";
    public static final String ESSENTIALS_DELWARP = "midgard.essentials.delwarp";
    public static final String ESSENTIALS_HOME = "midgard.essentials.home";
    public static final String ESSENTIALS_SETHOME = "midgard.essentials.sethome";
    public static final String ESSENTIALS_DELHOME = "midgard.essentials.delhome";
    public static final String ESSENTIALS_TPA = "midgard.essentials.tpa";
    public static final String ESSENTIALS_TPACCEPT = "midgard.essentials.tpaccept";
    public static final String ESSENTIALS_TPDENY = "midgard.essentials.tpdeny";
    public static final String ESSENTIALS_VANISH = "midgard.essentials.vanish";
    public static final String ESSENTIALS_BACK = "midgard.essentials.back";
    public static final String ESSENTIALS_TP = "midgard.essentials.tp";
    public static final String ESSENTIALS_TPHERE = "midgard.essentials.tphere";
    public static final String ESSENTIALS_TOP = "midgard.essentials.top";
    public static final String ESSENTIALS_SPEED = "midgard.essentials.speed";
    public static final String ESSENTIALS_INVSEE = "midgard.essentials.invsee";

    /**
     * Verifica se o sender tem permissão de administrador.
     */
    public static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission(ADMIN);
    }

    /**
     * Verifica se o sender tem permissão de moderador.
     */
    public static boolean isModerator(CommandSender sender) {
        return sender.hasPermission(MODERATOR) || isAdmin(sender);
    }

    /**
     * Verifica se o sender tem permissão básica de jogador.
     */
    public static boolean isPlayer(CommandSender sender) {
        return sender.hasPermission(PLAYER) || isModerator(sender);
    }

    /**
     * Verifica permissão com fallback hierárquico.
     */
    public static boolean hasPermission(CommandSender sender, String permission) {
        if (permission == null) {
            return true;
        }
        
        // Permissões administrativas incluem moderador e admin
        if (permission.startsWith("midgard.admin.")) {
            return sender.hasPermission(permission) || isAdmin(sender);
        }
        
        // Permissões de moderador incluem admin
        if (permission.startsWith("midgard.moderator.")) {
            return sender.hasPermission(permission) || isModerator(sender);
        }
        
        // Permissões normais
        return sender.hasPermission(permission);
    }

    /**
     * Obtém a permissão para uma categoria de comando.
     */
    public static String getPermissionForCategory(CommandCategory category) {
        switch (category) {
            case ADMIN: return ADMIN;
            case MODERATOR: return MODERATOR;
            case PLAYER: default: return null; // Sem permissão específica para jogadores
        }
    }

    /**
     * Valida se o sender pode usar comandos de uma categoria específica.
     */
    public static boolean canUseCategory(CommandSender sender, CommandCategory category) {
        switch (category) {
            case ADMIN: return isAdmin(sender);
            case MODERATOR: return isModerator(sender);
            case PLAYER: default: return true;
        }
    }
}