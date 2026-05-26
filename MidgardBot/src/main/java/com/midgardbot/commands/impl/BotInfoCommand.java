package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Comando de Informações do Bot.
 * Exibe estatísticas técnicas como uso de memória, tempo de atividade (uptime) e versões.
 */
public class BotInfoCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "botinfo";
    }

    @Override
    public String getDescription() {
        return "Exibe o painel de telemetria e status do sistema (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BOTINFO";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Validação de Permissão com tom profissional
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_BOTINFO").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Restrito",
                "Este painel de diagnóstico é exclusivo para a administração do sistema.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        // Cálculos de Memória
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Lógica da Barra de Progresso da RAM
        int totalBars = 12;
        int usedBars = (int) ((double) usedMemory / totalMemory * totalBars);
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            progressBar.append(i < usedBars ? "■" : "□");
        }
        double usedRamPercent = ((double) usedMemory / totalMemory) * 100;

        // Cálculos de Uptime
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptime = rb.getUptime();
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60;
        
        String uptimeStr = String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);

        // Informações do Sistema Operacional
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String javaVer = System.getProperty("java.version");

        // Construção do Embed estilo Dashboard
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📊 Painel de Controle do Sistema")
            .setDescription("Métricas de performance em tempo real da instância " + event.getJDA().getSelfUser().getName())
            .setColor(EmbedUtils.COLOR_PRIMARY)
            .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl())
            
            // Coluna 1: Conectividade e Tempo
            .addField("📡 Conectividade", 
                "**Latência (Gateway):** `" + event.getJDA().getGatewayPing() + "ms`\n" +
                "**Tempo de Atividade:** `" + uptimeStr + "`", true)
            
            // Coluna 2: Alcance
            .addField("🌐 Alcance", 
                "**Servidores:** `" + event.getJDA().getGuilds().size() + "`\n" +
                "**Usuários:** `" + event.getJDA().getUsers().size() + "`", true)

            // Quebra de linha para a memória (Full Width)
            .addBlankField(false) 
            
            .addField("💾 Alocação de Memória (RAM)", 
                "`[" + progressBar.toString() + "]` **" + String.format("%.1f", usedRamPercent) + "%**\n" +
                "Utilizado: `" + String.format("%.2f MB", usedMemory / 1048576.0) + "` / Total: `" + String.format("%.2f MB", totalMemory / 1048576.0) + "`", false)

            // Quebra de linha para ambiente
            .addField("💻 Ambiente de Execução", 
                "**OS:** " + osName + " (" + osArch + ")\n" +
                "**Java:** " + javaVer + "\n" +
                "**Threads Ativas:** `" + Thread.activeCount() + "`", false)
            
            .setFooter("Midgard System Monitor • ID: " + event.getId());
            
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}