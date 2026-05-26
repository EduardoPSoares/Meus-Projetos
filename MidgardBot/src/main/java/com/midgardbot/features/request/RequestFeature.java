package com.midgardbot.features.request;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

public class RequestFeature extends ListenerAdapter implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestFeature.class);

    @Override
    public String getName() {
        return "requisicao";
    }

    @Override
    public String getDescription() {
        return "Envia uma requisição para os desenvolvedores.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "conteudo", "O conteúdo da requisição", true),
            new OptionData(OptionType.STRING, "prioridade", "A prioridade da requisição", true)
                .addChoice("Baixa", "Baixa")
                .addChoice("Média", "Média")
                .addChoice("Alta", "Alta")
                .addChoice("Urgente", "Urgente"),
            new OptionData(OptionType.STRING, "tipo", "O tipo da requisição", true)
                .addChoice("Bug", "Bug")
                .addChoice("Feature", "Feature")
                .addChoice("Configuração", "Configuração")
                .addChoice("Outro", "Outro"),
            new OptionData(OptionType.ATTACHMENT, "imagem", "Imagem ou print para auxiliar (opcional)", false)
        );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permission Check: Only Team Members (Developer, Founder or Staff) can use this command
        String devRoleId = BotConfig.getDeveloperRoleId();
        String founderRoleId = BotConfig.getFounderRoleId();
        String staffRoleId = BotConfig.getStaffRoleId();
        boolean isTeam = false;
        
        if (devRoleId != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(devRoleId))) isTeam = true;
        if (founderRoleId != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(founderRoleId))) isTeam = true;
        if (staffRoleId != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(staffRoleId))) isTeam = true;
        
        if (!isTeam) {
            event.replyEmbeds(EmbedUtils.createError("Sem Permissão", "Este comando é restrito à equipe (Staff).", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        String content = event.getOption("conteudo").getAsString();
        String priority = event.getOption("prioridade").getAsString();
        String type = event.getOption("tipo") != null ? event.getOption("tipo").getAsString() : "Outro";
        net.dv8tion.jda.api.entities.Message.Attachment attachment = event.getOption("imagem") != null ? event.getOption("imagem").getAsAttachment() : null;
        String requester = event.getUser().getAsMention();

        String channelId = BotConfig.getRequestChannelId();
        if (channelId == null || channelId.isEmpty()) {
            event.replyEmbeds(EmbedUtils.createError("Erro", "Canal de requisições não configurado.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getJDA().getTextChannelById(channelId);
        if (channel == null) {
            event.replyEmbeds(EmbedUtils.createError("Erro", "Canal de requisições não encontrado.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Nova Requisição: " + type);
        embed.addField("Requisitor", requester, true);
        embed.addField("Data", "<t:" + Instant.now().getEpochSecond() + ":f>", true);
        embed.addField("Tipo", type, true);
        embed.addField("Prioridade", priority, true);
        embed.addField("Requisição", content, false);
        embed.addField("Status", "Aguardando Aprovação", false);
        
        // Color coding based on Type
        switch (type) {
            case "Bug": embed.setColor(Color.RED); break;
            case "Feature": embed.setColor(Color.GREEN); break;
            case "Configuração": embed.setColor(Color.BLUE); break;
            default: embed.setColor(Color.YELLOW); break;
        }
        
        if (attachment != null) {
            embed.setImage(attachment.getUrl());
        }
        
        embed.setFooter("Midgard Request System");

        channel.sendMessageEmbeds(embed.build())
            .setActionRow(
                Button.success("req_approve", "Aprovar"),
                Button.danger("req_deny", "Negar"),
                Button.primary("req_founder", "Solicitar Aprovação Fundador")
            )
            .queue(msg -> {
                event.replyEmbeds(EmbedUtils.createSuccess("Sucesso", "Requisição enviada com sucesso!", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                
                // Log initial request
                logStatus(event.getJDA(), event.getUser(), embed.build(), "Aguardando Aprovação", null, logId -> {
                    // Update Request Message with Log ID in footer
                    EmbedBuilder updatedRequest = new EmbedBuilder(embed.build());
                    updatedRequest.setFooter("Midgard Request System | LogID: " + logId);
                    msg.editMessageEmbeds(updatedRequest.build()).queue();
                });
            });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("req_")) return;

        // Verificação de permissão para ações de desenvolvedor
        if (id.equals("req_approve") || id.equals("req_deny") || id.equals("req_founder") || id.equals("req_implemented") || id.equals("req_update") || id.equals("req_implementing")) {
            // Se for req_deny e estiver aguardando fundador, a lógica específica de fundador será tratada dentro do bloco req_deny
            // Mas para a aprovação inicial e solicitação de fundador, deve ser Dev.
            
            boolean isDevAction = id.equals("req_approve") || id.equals("req_founder") || id.equals("req_implemented") || id.equals("req_update") || id.equals("req_implementing");
            // Para req_deny, precisamos verificar se NÃO é o caso do fundador antes de exigir Dev (ou se Dev também pode negar no lugar do fundador? O prompt diz "apenas o fundador pode decretar")
            // O prompt diz: "apos utilizar essa funcao apenas o fundador pode decretar se vai ser aprovado ou nao" -> Isso é para quando já foi solicitado ao fundador.
            // Antes disso (botões iniciais), o usuário pediu "somente o dev conseguir aprovar ou negar".
            
            if (isDevAction || (id.equals("req_deny"))) {
                 // Exceção: Se for req_deny e estiver em "Aguardando Aprovação do Fundador", a verificação é feita dentro do bloco do req_deny
                 // Vamos deixar o bloco req_deny lidar com sua própria lógica complexa, mas para os outros, exigimos Dev.
                 
                 if (!id.equals("req_deny")) {
                     String devRoleId = BotConfig.getDeveloperRoleId();
                     if (devRoleId != null && !event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(devRoleId))) {
                         event.reply("Apenas desenvolvedores podem realizar esta ação.").setEphemeral(true).queue();
                         return;
                     }
                 }
            }
        }

        MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
        EmbedBuilder newEmbed = new EmbedBuilder(originalEmbed);
        
        // Extract Log ID from footer
        String footer = originalEmbed.getFooter() != null ? originalEmbed.getFooter().getText() : "";
        String logId = (footer != null && footer.contains("LogID: ")) ? footer.split("LogID: ")[1] : null;

        if (id.equals("req_approve")) {
             updateStatus(newEmbed, originalEmbed, "Pendente de Implementação");
             newEmbed.setColor(EmbedUtils.COLOR_INFO);
             
             event.editMessageEmbeds(newEmbed.build())
                 .setActionRow(
                     Button.primary("req_implementing", "Começar Implementação"),
                     Button.secondary("req_update", "Atualizar Requisitor")
                 ).queue();
             
             notifyRequester(originalEmbed, "Requisição Aprovada", "Sua requisição foi aprovada e está na fila de implementação.", EmbedUtils.COLOR_INFO, event.getJDA());
             logStatus(event.getJDA(), event.getUser(), originalEmbed, "Pendente de Implementação", logId, null);
                 
        } else if (id.equals("req_deny")) {
            // Check if status is "Aguardando Aprovação do Fundador"
            boolean founderOnly = false;
            for (MessageEmbed.Field field : originalEmbed.getFields()) {
                if (field.getName().equals("Status") && "Aguardando Aprovação do Fundador".equals(field.getValue())) {
                     founderOnly = true;
                     break;
                }
            }
            
            if (founderOnly) {
                String founderRoleId = BotConfig.getFounderRoleId();
                if (founderRoleId != null && !event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(founderRoleId))) {
                    event.reply("Apenas o fundador pode negar esta requisição neste estágio.").setEphemeral(true).queue();
                    return;
                }
            } else {
                // Se não for estágio de fundador, exige Developer
                String devRoleId = BotConfig.getDeveloperRoleId();
                if (devRoleId != null && !event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(devRoleId))) {
                    event.reply("Apenas desenvolvedores podem negar requisições.").setEphemeral(true).queue();
                    return;
                }
            }

            TextInput reason = TextInput.create("reason", "Motivo", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Motivo da negação")
                .setRequired(true)
                .build();
            Modal modal = Modal.create("req_deny_modal", "Negar Requisição")
                .addActionRow(reason)
                .build();
            event.replyModal(modal).queue();
        } else if (id.equals("req_founder")) {
             updateStatus(newEmbed, originalEmbed, "Aguardando Aprovação do Fundador");
             newEmbed.setColor(EmbedUtils.COLOR_GOLD);
             event.editMessageEmbeds(newEmbed.build())
                 .setActionRow(
                     Button.success("req_approve_founder", "Aprovar (Fundador)"),
                     Button.danger("req_deny", "Negar")
                 ).queue();
             
             notifyRequester(originalEmbed, "Aguardando Aprovação Superior", "Sua requisição foi encaminhada para aprovação da fundação.", EmbedUtils.COLOR_GOLD, event.getJDA());
             logStatus(event.getJDA(), event.getUser(), originalEmbed, "Aguardando Aprovação do Fundador", logId, null);
        } else if (id.equals("req_approve_founder")) {
            String founderRoleId = BotConfig.getFounderRoleId();
            if (founderRoleId != null && !event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(founderRoleId))) {
                event.reply("Apenas o fundador pode aprovar esta requisição.").setEphemeral(true).queue();
                return;
            }
            
             updateStatus(newEmbed, originalEmbed, "Pendente de Implementação");
             newEmbed.setColor(EmbedUtils.COLOR_INFO);
             event.editMessageEmbeds(newEmbed.build())
                 .setActionRow(
                     Button.primary("req_implementing", "Começar Implementação"),
                     Button.secondary("req_update", "Atualizar Requisitor")
                 ).queue();
             
             notifyRequester(originalEmbed, "Aprovado pela Fundação", "Sua requisição foi aprovada pela fundação e está na fila de implementação.", EmbedUtils.COLOR_INFO, event.getJDA());
             logStatus(event.getJDA(), event.getUser(), originalEmbed, "Pendente de Implementação", logId, null);
        } else if (id.equals("req_implementing")) {
             // Custom update logic to include Developer field
             newEmbed.clearFields();
             boolean devFound = false;
             for (MessageEmbed.Field field : originalEmbed.getFields()) {
                 if (field.getName().equals("Status")) {
                     newEmbed.addField("Status", "Implementando", false);
                 } else if (field.getName().equals("Desenvolvedor")) {
                     newEmbed.addField("Desenvolvedor", event.getUser().getAsMention(), true);
                     devFound = true;
                 } else {
                     newEmbed.addField(field);
                 }
             }
             
             if (!devFound) {
                 newEmbed.addField("Desenvolvedor", event.getUser().getAsMention(), true);
             }
             
             newEmbed.setColor(EmbedUtils.COLOR_WARNING);
             MessageEmbed finalEmbed = newEmbed.build();
             
             event.editMessageEmbeds(finalEmbed)
                 .setActionRow(
                     Button.success("req_implemented", "Concluir Implementação"),
                     Button.secondary("req_update", "Atualizar Requisitor")
                 ).queue();
             
             notifyRequester(originalEmbed, "Implementação Iniciada", "Sua requisição começou a ser implementada por " + event.getUser().getAsMention(), EmbedUtils.COLOR_WARNING, event.getJDA());
             logStatus(event.getJDA(), event.getUser(), finalEmbed, "Implementando", logId, null);
        } else if (id.equals("req_implemented")) {
             updateStatus(newEmbed, originalEmbed, "Implementado");
             newEmbed.setColor(EmbedUtils.COLOR_SUCCESS);
             event.editMessageEmbeds(newEmbed.build()).setComponents().queue();
             
             notifyRequester(originalEmbed, "Requisição Implementada", "Sua requisição foi marcada como implementada pela equipe de desenvolvimento.", EmbedUtils.COLOR_SUCCESS, event.getJDA());
             logStatus(event.getJDA(), event.getUser(), originalEmbed, "Implementado", logId, null);
        } else if (id.equals("req_update")) {
            TextInput updateMsg = TextInput.create("message", "Mensagem", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Mensagem para o requisitor")
                .setRequired(true)
                .build();
            Modal modal = Modal.create("req_update_modal", "Atualizar Requisitor")
                .addActionRow(updateMsg)
                .build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("req_deny_modal")) {
            String reason = event.getValue("reason").getAsString();
            if (event.getMessage() != null) {
                MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
                EmbedBuilder newEmbed = new EmbedBuilder(originalEmbed);
                updateStatus(newEmbed, originalEmbed, "Negado: " + reason);
                newEmbed.setColor(EmbedUtils.COLOR_ERROR);
                event.editMessageEmbeds(newEmbed.build()).setComponents().queue();
                 
                notifyRequester(originalEmbed, "Requisição Negada", "Sua requisição foi negada.\n\n**Motivo:** " + reason, EmbedUtils.COLOR_ERROR, event.getJDA());
                
                String footer = originalEmbed.getFooter() != null ? originalEmbed.getFooter().getText() : "";
                String logId = (footer != null && footer.contains("LogID: ")) ? footer.split("LogID: ")[1] : null;
                logStatus(event.getJDA(), event.getUser(), originalEmbed, "Negado", logId, null);
            }
        } else if (event.getModalId().equals("req_update_modal")) {
            String message = event.getValue("message").getAsString();
            if (event.getMessage() != null) {
                 MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
                 notifyRequester(originalEmbed, "Atualização de Requisição", message, EmbedUtils.COLOR_INFO, event.getJDA());
                 event.reply("Requisitor atualizado.").setEphemeral(true).queue();
            }
        }
    }
    
    private void updateStatus(EmbedBuilder newEmbed, MessageEmbed originalEmbed, String status) {
        newEmbed.clearFields();
        for (MessageEmbed.Field field : originalEmbed.getFields()) {
            if (field.getName().equals("Status")) {
                newEmbed.addField("Status", status, false);
            } else {
                newEmbed.addField(field);
            }
        }
    }
    
    private void notifyRequester(MessageEmbed originalEmbed, String title, String description, Color color, net.dv8tion.jda.api.JDA jda) {
        for (MessageEmbed.Field field : originalEmbed.getFields()) {
            if (field.getName().equals("Requisitor")) {
                String mention = field.getValue();
                String userId = mention.replaceAll("[^0-9]", "");
                jda.retrieveUserById(userId).queue(user -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(title);
                    embed.setDescription(description);
                    embed.setColor(color);
                    embed.setTimestamp(Instant.now());
                    embed.setFooter("Midgard Request System", jda.getSelfUser().getAvatarUrl());
                    
                    // Add original request content for context
                    for (MessageEmbed.Field f : originalEmbed.getFields()) {
                        if (f.getName().equals("Requisição")) {
                            embed.addField("Sua Requisição", f.getValue(), false);
                            break;
                        }
                    }

                    user.openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(embed.build()).queue());
                }, e -> {});
                break;
            }
        }
    }

    private void logStatus(net.dv8tion.jda.api.JDA jda, net.dv8tion.jda.api.entities.User user, MessageEmbed originalEmbed, String newStatus, String logId, java.util.function.Consumer<String> onLogSent) {
        String logChannelId = BotConfig.getRequestLogChannelId();
        if (logChannelId == null || logChannelId.isEmpty()) {
            LOGGER.debug("Log Channel ID is null or empty");
            return;
        }
        
        TextChannel logChannel = jda.getTextChannelById(logChannelId);
        if (logChannel == null) {
            LOGGER.debug("Log Channel not found: {}", logChannelId);
            return;
        }

        EmbedBuilder logEmbed = new EmbedBuilder(originalEmbed);
        logEmbed.setTitle("🔄 Atualização de Requisição");
        
        // Update Status Field
        // Use originalEmbed.getFields() to avoid reference issues when clearing fields
        logEmbed.clearFields();
        for (MessageEmbed.Field field : originalEmbed.getFields()) {
            if (field.getName().equals("Status")) {
                logEmbed.addField("Status", newStatus, false);
            } else {
                logEmbed.addField(field);
            }
        }
        
        // Add Staff info
        logEmbed.addField("Atualizado por", user.getAsMention(), true);

        // Set Color
        if (newStatus.contains("Implementado")) {
            logEmbed.setColor(EmbedUtils.COLOR_SUCCESS);
        } else if (newStatus.contains("Negado")) {
            logEmbed.setColor(EmbedUtils.COLOR_ERROR);
        } else if (newStatus.contains("Pendente")) {
            logEmbed.setColor(EmbedUtils.COLOR_INFO);
        } else if (newStatus.contains("Implementando")) {
            logEmbed.setColor(EmbedUtils.COLOR_WARNING);
        } else if (newStatus.contains("Aguardando")) {
            logEmbed.setColor(EmbedUtils.COLOR_GOLD);
        } else {
            logEmbed.setColor(EmbedUtils.COLOR_PRIMARY);
        }
        
        logEmbed.setTimestamp(Instant.now());
        logEmbed.setFooter("Log de Requisições • MidgardBOT", jda.getSelfUser().getAvatarUrl());

        if (logId != null) {
            logChannel.retrieveMessageById(logId).queue(
                msg -> msg.editMessageEmbeds(logEmbed.build()).queue(
                    success -> {
                        LOGGER.debug("Log edited successfully");
                        
                        // Manage Thread based on status
                        net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel thread = success.getStartedThread();
                        
                        if (newStatus.contains("Pendente de Implementação") && thread == null) {
                            String type = "Outro";
                            String content = "Discussão";
                            for (MessageEmbed.Field field : originalEmbed.getFields()) {
                                if (field.getName().equals("Tipo")) type = field.getValue();
                                if (field.getName().equals("Requisição")) content = field.getValue();
                            }
                            String threadName = type + ": " + (content.length() > 50 ? content.substring(0, 47) + "..." : content);
                            success.createThreadChannel(threadName).queue();
                        } else if (thread != null) {
                            if (newStatus.contains("Negado")) {
                                thread.delete().queue(
                                    v -> LOGGER.debug("Thread deleted for denied request"),
                                    e -> LOGGER.error("Failed to delete thread: {}", e.getMessage())
                                );
                            } else if (newStatus.contains("Implementado")) {
                                thread.getManager().setLocked(true).setArchived(true).queue(
                                    v -> LOGGER.debug("Thread locked and archived for implemented request"),
                                    e -> LOGGER.error("Failed to lock thread: {}", e.getMessage())
                                );
                            }
                        }
                    },
                    error -> {
                        LOGGER.error("Failed to edit log: {}", error.getMessage());
                        // Fallback to sending new message if edit fails (e.g. message deleted)
                        logChannel.sendMessageEmbeds(logEmbed.build()).queue();
                    }
                ),
                error -> {
                    LOGGER.error("Log message not found: {}", error.getMessage());
                    logChannel.sendMessageEmbeds(logEmbed.build()).queue();
                }
            );
        } else {
            logChannel.sendMessageEmbeds(logEmbed.build()).queue(
                success -> {
                    LOGGER.debug("Log sent successfully to {}", logChannel.getName());
                    if (onLogSent != null) {
                        onLogSent.accept(success.getId());
                    }
                },
                error -> LOGGER.error("Failed to send log: {}", error.getMessage())
            );
        }
    }
}
