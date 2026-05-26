package de.maxhenkel.voicechat.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class Translations {

    public final ConfigEntry<String> forceVoicechatKickMessage;
    public final ConfigEntry<String> voicechatNotCompatibleMessage;
    public final ConfigEntry<String> voicechatNeededForCommandMessage;
    public final ConfigEntry<String> playerCommandMessage;

    public Translations(ConfigBuilder builder) {
        builder.header(
                "MidgardVoice - Traducoes",
                "Este arquivo contem todas as traducoes do lado do servidor para o mod MidgardVoice"
        );

        forceVoicechatKickMessage = builder.stringEntry(
                "force_voicechat_kick_message",
                "\u1D20\u1D0F\u1D04\u00EA \u1D18\u0280\u1D07\u1D04\u026As\u1D00 \u1D05\u1D0F %s %s \u1D18\u1D00\u0280\u1D00 \u1D0A\u1D0F\u0262\u1D00\u0280 \u0274\u1D07s\u1D1B\u1D07 s\u1D07\u0280\u1D20\u026A\u1D05\u1D0F\u0280",
                "The message a player gets when kicked for not having voice chat installed and the server has force_voicechat enabled",
                "The first parameter is the mod/plugin name and the second parameter is the mod/plugin version"
        );
        voicechatNotCompatibleMessage = builder.stringEntry(
                "voicechat_not_compatible_message",
                "s\u1D1C\u1D00 \u1D20\u1D07\u0280s\u00E3\u1D0F \u1D05\u1D0F \u1D04\u029C\u1D00\u1D1B \u1D05\u1D07 \u1D20\u1D0F\u1D22 \u0274\u00E3\u1D0F \u00E9 \u1D04\u1D0F\u1D0D\u1D18\u1D00\u1D1B\u00ED\u1D20\u1D07\u029F \u1D04\u1D0F\u1D0D \u1D00 \u1D20\u1D07\u0280s\u00E3\u1D0F \u1D05\u1D0F s\u1D07\u0280\u1D20\u026A\u1D05\u1D0F\u0280.\\n\u1D18\u1D0F\u0280 \uA730\u1D00\u1D20\u1D0F\u0280, \u026A\u0274s\u1D1B\u1D00\u029F\u1D07 \u1D00 \u1D20\u1D07\u0280s\u00E3\u1D0F %s \u1D05\u1D0F %s.",
                "The message a player gets when joining a server with an incompatible voice chat version",
                "The first parameter is the mod/plugin version and the second parameter is the mod/plugin name"
        );
        voicechatNeededForCommandMessage = builder.stringEntry(
                "voicechat_needed_for_command_message",
                "\u1D20\u1D0F\u1D04\u00EA \u1D18\u0280\u1D07\u1D04\u026As\u1D00 \u1D1B\u1D07\u0280 \u1D0F %s \u026A\u0274s\u1D1B\u1D00\u029F\u1D00\u1D05\u1D0F \u0274\u1D0F s\u1D07\u1D1C \u1D04\u029F\u026A\u1D07\u0274\u1D1B\u1D07 \u1D18\u1D00\u0280\u1D00 \u1D1C s\u1D00\u0280 \u1D07s\u1D1B\u1D07 \u1D04\u1D0F\u1D0D\u1D00\u0274\u1D05\u1D0F",
                "The message a player gets when trying to execute a command that requires the voice chat mod installed on the client side.",
                "The first parameter is the mod/plugin name"
        );
        playerCommandMessage = builder.stringEntry(
                "player_command_message",
                "\u1D07s\u1D1B\u1D07 \u1D04\u1D0F\u1D0D\u1D00\u0274\u1D05\u1D0F s\u00F3 \u1D18\u1D0F\u1D05\u1D07 s\u1D07\u0280 \u1D07x\u1D07\u1D04\u1D1C\u1D1B\u1D00\u1D05\u1D0F \u1D18\u1D0F\u0280 \u1D1C\u1D0D \u1D0A\u1D0F\u0262\u1D00\u1D05\u1D0F\u0280",
                "A mensagem que um jogador recebe ao tentar executar um comando que so pode ser executado como jogador"
        );
    }

}
