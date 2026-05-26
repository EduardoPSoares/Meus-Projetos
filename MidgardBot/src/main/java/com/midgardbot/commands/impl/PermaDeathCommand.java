package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.PermaDeathHandler;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comando /permadeath - Painel administrativo completo para controle do sistema PermaDeath.
 * Permite listar mortos, ressuscitar, deletar registros, ver detalhes, toggle e reload via Discord.
 */
public class PermaDeathCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermaDeathCommand.class);

    /** messageId -> discordId do admin que abriu o painel */
    public static final ConcurrentHashMap<String, String> sessionOwners = new ConcurrentHashMap<>();
    /** messageId -> página atual da lista */
    public static final ConcurrentHashMap<String, Integer> sessionPages = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "permadeath";
    }

    @Override
    public String getDescription() {
        return "Painel administrativo do sistema PermaDeath (Admin).";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_PERMADEATH";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Acesso Negado",
                "Apenas **administradores** podem usar este painel.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        EmbedBuilder embed = buildMainPanel(event);

        List<ActionRow> actionRows = List.of(
            ActionRow.of(
                Button.primary("pd_list", "📋 Listar Mortos"),
                Button.primary("pd_search", "🔍 Buscar Jogador"),
                Button.success("pd_revive", "♻️ Ressuscitar"),
                Button.danger("pd_delete", "🗑️ Deletar Registro")
            ),
            ActionRow.of(
                Button.secondary("pd_toggle", "⚙️ Toggle PermaDeath"),
                Button.secondary("pd_reload", "🔄 Reload Plugin"),
                Button.primary("pd_stats", "📊 Estatísticas"),
                Button.danger("pd_close", "❌ Fechar")
            )
        );

        event.getHook().sendMessageEmbeds(embed.build()).setComponents(actionRows).queue(msg -> {
            sessionOwners.put(msg.getId(), event.getUser().getId());
            sessionPages.put(msg.getId(), 0);
        });

        LOGGER.info("Painel PermaDeath aberto por {}", event.getUser().getName());
    }

    /**
     * Constrói o embed principal do painel PermaDeath.
     */
    public static EmbedBuilder buildMainPanel(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("☠️ Painel PermaDeath — Controle Admin");
        embed.setColor(EmbedUtils.COLOR_ERROR);
        embed.setThumbnail("https://mc-heads.net/avatar/MHF_Skeleton/128");
        embed.setTimestamp(Instant.now());

        // Estatísticas rápidas do banco
        int totalDead = PermaDeathHandler.countDeadPlayers();
        String toggleStatus = PermaDeathHandler.getPermaDeathStatus();

        embed.setDescription(
            "Controle completo do sistema **PermaDeath** direto pelo Discord.\n" +
            "Use os botões abaixo para gerenciar mortes, ressuscitar jogadores e mais.\n\n" +
            EmbedUtils.SEPARATOR
        );

        embed.addField("💀 Jogadores Mortos", "**" + totalDead + "**", true);
        embed.addField("⚙️ PermaDeath", toggleStatus, true);
        embed.addField("\u200B", "\u200B", true); // spacer

        embed.addField("📋 Ações Disponíveis",
            "**📋 Listar Mortos** — Ver todos os jogadores mortos com detalhes\n" +
            "**🔍 Buscar Jogador** — Buscar informações de morte de um jogador\n" +
            "**♻️ Ressuscitar** — Trazer um jogador de volta à vida\n" +
            "**🗑️ Deletar Registro** — Remover registro de morte e resetar dados\n" +
            "**⚙️ Toggle** — Ativar/Desativar o sistema PermaDeath\n" +
            "**🔄 Reload** — Recarregar configurações do plugin\n" +
            "**📊 Estatísticas** — Informações detalhadas do sistema",
            false
        );

        embed.setFooter("MidgardBOT • PermaDeath Admin Panel", event != null ? event.getJDA().getSelfUser().getAvatarUrl() : null);
        return embed;
    }
}
