package me.ray.midgard.modules.character.i18n;

import me.ray.midgard.core.i18n.MessageKey;
import me.ray.midgard.core.i18n.MessageRegistry;

public final class CharacterMessages {

    private CharacterMessages() {}

    // ============================================
    // COMMAND
    // ============================================

    public static final MessageKey COMMAND_DESCRIPTION = register(
            MessageKey.builder("character.command.description")
                    .module("character")
                    .fallback("Abre o menu de atributos do personagem")
                    .build()
    );

    public static final MessageKey COMMAND_USAGE = register(
            MessageKey.builder("character.command.usage")
                    .module("character")
                    .fallback("/rpg character")
                    .build()
    );

    // ============================================
    // ERRORS
    // ============================================

    public static final MessageKey ERROR_PLAYER_ONLY = register(
            MessageKey.builder("character.errors.player_only")
                    .module("character")
                    .fallback("<red>✖</red> <gray>ᴀᴘᴇɴᴀs ᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ.</gray>")
                    .build()
    );

    public static final MessageKey ERROR_MODULE_UNAVAILABLE = register(
            MessageKey.builder("character.errors.module_unavailable")
                    .module("character")
                    .fallback("<red>✖</red> <gray>ᴍᴏᴅᴜʟᴏ ᴅᴇ ᴘᴇʀsᴏɴᴀɢᴇᴍ ɴᴀᴏ ᴇsᴛᴀ ᴅɪsᴘᴏɴɪᴠᴇʟ.</gray>")
                    .build()
    );

    public static final MessageKey ERROR_MENU_OPEN_FAILED = register(
            MessageKey.builder("character.errors.menu_open_failed")
                    .module("character")
                    .fallback("<red>✖</red> <gray>ᴏᴄᴏʀʀᴇᴜ ᴜᴍ ᴇʀʀᴏ ᴀᴏ ᴀʙʀɪʀ ᴏ ᴍᴇɴᴜ.</gray>")
                    .build()
    );

    // ============================================
    // MENU
    // ============================================

    public static final MessageKey MENU_TITLE = register(
            MessageKey.builder("character.menu.title")
                    .module("character")
                    .fallback("Informações do Personagem")
                    .build()
    );

    public static final MessageKey MENU_NO_CLASS = register(
            MessageKey.builder("character.menu.no_class")
                    .module("character")
                    .fallback("Nenhuma")
                    .build()
    );

    public static final MessageKey ATTR_MENU_TITLE = register(
            MessageKey.builder("character.menu.attr_title")
                    .module("character")
                    .fallback("ᴀᴛʀɪʙᴜᴛᴏs")
                    .build()
    );

    public static final MessageKey MENU_NOT_ENOUGH_POINTS = register(
            MessageKey.builder("character.menu.not_enough_points")
                    .module("character")
                    .fallback("<red>✖</red> <gray>ᴘᴏɴᴛᴏs ɪɴsᴜꜰɪᴄɪᴇɴᴛᴇs ᴘᴀʀᴀ ᴇsᴛᴀ ᴀᴄᴀᴏ.</gray>")
                    .build()
    );

    public static final MessageKey MENU_ERROR = register(
            MessageKey.builder("character.menu.menu_error")
                    .module("character")
                    .fallback("<red>✖</red> <gray>ᴇʀʀᴏ ᴀᴏ ᴘʀᴏᴄᴇssᴀʀ ᴀᴄᴀᴏ ɴᴏ ᴍᴇɴᴜ.</gray>")
                    .build()
    );

    public static final MessageKey MENU_XP_MAX_LEVEL = register(
            MessageKey.builder("character.menu.xp_max_level")
                    .module("character")
                    .fallback("100%")
                    .build()
    );

    // ============================================
    // HOTBAR
    // ============================================

    public static final MessageKey HOTBAR_COMPASS_NAME = register(
            MessageKey.builder("character.hotbar.compass.name")
                    .module("character")
                    .fallback("<white>ᴘᴇʀsᴏɴᴀɢᴇᴍ</white>")
                    .build()
    );

    public static final MessageKey HOTBAR_COMPASS_RECEIVED = register(
            MessageKey.builder("character.hotbar.compass_received")
                    .module("character")
                    .fallback("<green>✔</green> <gray>ᴠᴏᴄᴇ ʀᴇᴄᴇʙᴇᴜ ᴀ ʙᴜssᴏʟᴀ ᴅᴇ ɴᴀᴠᴇɢᴀᴄᴀᴏ.</gray>")
                    .build()
    );

    // ============================================
    // MÉTODO AUXILIAR
    // ============================================

    private static MessageKey register(MessageKey key) {
        return MessageRegistry.getInstance().register(key);
    }

    public static void init() {
        // Força o carregamento de todas as constantes static
    }
}
