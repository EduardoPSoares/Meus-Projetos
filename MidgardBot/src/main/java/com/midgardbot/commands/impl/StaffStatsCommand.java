package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.StaffStats;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comando de Estatísticas da Staff.
 * Mostra quantos tickets/whitelists cada membro da staff atendeu.
 * Útil para monitorar produtividade.
 */
public class StaffStatsCommand implements ISlashCommand {

    public static final int ITEMS_PER_PAGE = 10;

    @Override
    public String getName() {
        return "staffstats";
    }

    @Override
    public String getDescription() {
        return "Gera o relatório de métricas e produtividade da equipe.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_STAFFSTATS";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_STAFFSTATS").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Restrito", "Requer permissão de Gerenciamento de Servidor.", event.getJDA().getSelfUser()).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Defer reply because filtering staff might take a moment
        event.deferReply().queue();

        // Force load members to ensure cache is populated
        event.getGuild().loadMembers().onSuccess(members -> {
            try {
                Map<String, StaffStats> stats = DataManager.getStaffStats();

                // Filter stats to only include current staff members
                List<Map.Entry<String, StaffStats>> filteredStats = filterActiveStaff(stats, event.getGuild());

                if (filteredStats.isEmpty()) {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createWarning("⚠️ Nenhum Staff Ativo", "Não há registros de atividade para membros da staff atuais.", event.getJDA().getSelfUser()).build())
                            .queue();
                    return;
                }

                MessageEmbed embed = generateEmbed(0, event.getJDA().getSelfUser(), filteredStats);
                int totalPages = (int) Math.ceil((double) filteredStats.size() / ITEMS_PER_PAGE);

                event.getHook().sendMessageEmbeds(embed)
                     .addActionRow(
                         Button.secondary("btn_stats_prev", "◀️ Anterior").asDisabled(),
                         Button.secondary("btn_stats_next", "Próximo ▶️").withDisabled(totalPages <= 1)
                     )
                     .queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Erro ao processar estatísticas: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        }).onError(error -> {
            event.getHook().sendMessage("Erro ao carregar lista de membros: " + error.getMessage()).queue();
        });
    }

    public static List<Map.Entry<String, StaffStats>> filterActiveStaff(Map<String, StaffStats> stats, Guild guild) {
        // 1. Get all staff role IDs from config
        Set<String> staffRoleIds = getAllStaffRoleIds();
        
        // 2. Find all members with these roles
        Set<String> staffMemberIds = new HashSet<>();
        
        // Iterar sobre todos os membros carregados no cache (agora garantido pelo loadMembers)
        for (Member member : guild.getMemberCache()) {
            boolean hasRole = false;
            for (net.dv8tion.jda.api.entities.Role role : member.getRoles()) {
                if (staffRoleIds.contains(role.getId())) {
                    hasRole = true;
                    break;
                }
            }
            
            if (hasRole || member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) || member.hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
                staffMemberIds.add(member.getId());
            }
        }
        
        // 3. Create a mutable copy of stats and add missing staff with 0 stats
        Map<String, StaffStats> finalStats = new HashMap<>(stats);
        for (String staffId : staffMemberIds) {
            if (!finalStats.containsKey(staffId)) {
                finalStats.put(staffId, new StaffStats());
            }
        }
        
        List<Map.Entry<String, StaffStats>> activeStaff = new ArrayList<>();
        
        for (Map.Entry<String, StaffStats> entry : finalStats.entrySet()) {
            String userId = entry.getKey();
            // Check if user is in our staff list (which means they are in the guild and have the role)
            if (staffMemberIds.contains(userId)) {
                activeStaff.add(entry);
            }
        }
        
        // Sort by total activity
        activeStaff.sort((e1, e2) -> Integer.compare(e2.getValue().getTotal(), e1.getValue().getTotal()));
        
        return activeStaff;
    }

    private static Set<String> getAllStaffRoleIds() {
        Set<String> roleIds = new HashSet<>();
        // List of keys that represent staff roles in config.env
        String[] keys = {
            "FUNDADOR", "ADMIN", "DEV", "MODERADOR", "CINEGRAFISTA", 
            "BUILDER", "LOREMAKER", "AJUDANTE", "INTERPRETE", "STAFF",
            "TICKET_SUPPORT_ROLES"
        };
        
        for (String key : keys) {
            String value = com.midgardbot.config.BotConfig.get(key);
            if (value != null && !value.isEmpty()) {
                for (String id : value.split(",")) {
                    roleIds.add(id.trim());
                }
            }
        }
        return roleIds;
    }

    public static MessageEmbed generateEmbed(int page, SelfUser selfUser, List<Map.Entry<String, StaffStats>> sortedStats) {
        int totalPages = (int) Math.ceil((double) sortedStats.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sortedStats.size());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📊 Painel de Performance Operacional");
        embed.setColor(EmbedUtils.COLOR_PRIMARY);
        
        StringBuilder sb = new StringBuilder();
        int rank = start + 1;
        
        for (int i = start; i < end; i++) {
            Map.Entry<String, StaffStats> entry = sortedStats.get(i);
            String staffId = entry.getKey();
            StaffStats stat = entry.getValue();
            
            String medal = "";
            if (rank == 1) medal = "🥇";
            else if (rank == 2) medal = "🥈";
            else if (rank == 3) medal = "🥉";
            else medal = String.format("`#%02d`", rank);

            sb.append(medal).append(" <@").append(staffId).append(">")
              .append("\n└─ 📁 **Whitelist:** `").append(stat.getTotal()).append("`")
              .append(" (✅ ").append(stat.approved).append(" | ❌ ").append(stat.rejected).append(")")
              .append("\n└─ 🎫 **Tickets:** `").append(stat.ticketsClaimed).append("` Assumidos | `").append(stat.ticketsClosed).append("` Fechados\n");
            
            rank++;
        }
        
        embed.setDescription("Métricas de análise de whitelist da equipe administrativa.\n*Classificação baseada no volume total de tickets processados.*\n\n" + sb.toString());
        embed.setFooter("Página " + (page + 1) + "/" + totalPages + " • Relatório gerado em tempo real • Midgard Analytics", selfUser != null ? selfUser.getEffectiveAvatarUrl() : null);
        
        return embed.build();
    }
}