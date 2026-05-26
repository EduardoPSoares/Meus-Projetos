package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.backup.TicketBackupManager;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.File;
import java.util.List;

public class BackupCommand implements ISlashCommand {

    private final TicketBackupManager backupManager;

    public BackupCommand(TicketBackupManager backupManager) {
        this.backupManager = backupManager;
    }

    @Override
    public String getName() {
        return "backup";
    }

    @Override
    public String getDescription() {
        return "Gerencia backups de tickets";
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
            new SubcommandData("create", "Cria um backup manual de todos os tickets"),
            new SubcommandData("list", "Lista os backups disponiveis"),
            new SubcommandData("rollback", "Restaura um backup")
                .addOption(OptionType.STRING, "backup_id", "ID do backup", true)
                .addOption(OptionType.STRING, "channel_id", "ID do canal alvo (opcional)", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BACKUP";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Subcomando invalido.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "create":
                event.deferReply(true).queue();
                String id = backupManager.backupAllTickets();
                if ("jda_unavailable".equals(id)) {
                    event.getHook()
                        .sendMessage("O backup nao pode ser executado agora porque o bot ainda nao esta conectado ao Discord.")
                        .queue();
                    return;
                }
                event.getHook().sendMessage("Backup concluido. ID: `" + id + "`").queue();
                break;

            case "list":
                File dir = new File("backups/tickets");
                File[] files = dir.listFiles(File::isDirectory);
                if (!dir.exists() || files == null || files.length == 0) {
                    event.reply("Nenhum backup encontrado.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder("**Backups Disponiveis:**\n");
                for (File file : files) {
                    sb.append("- `").append(file.getName()).append("`\n");
                }
                event.reply(sb.toString()).setEphemeral(true).queue();
                break;

            case "rollback":
                if (!(event.getChannel() instanceof TextChannel textChannel)) {
                    event.reply("Este comando so pode ser usado em canais de texto.").setEphemeral(true).queue();
                    return;
                }

                String backupId = event.getOption("backup_id").getAsString();
                String targetChannelId = event.getOption("channel_id") != null
                    ? event.getOption("channel_id").getAsString()
                    : textChannel.getId();

                event.deferReply(true).queue();
                backupManager.restoreTicket(backupId, targetChannelId, textChannel);
                event.getHook().sendMessage("Processo de restauracao iniciado.").queue();
                break;

            default:
                event.reply("Subcomando invalido.").setEphemeral(true).queue();
                break;
        }
    }
}
