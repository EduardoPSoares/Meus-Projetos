package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.core.command.BaseCommand;
import me.ray.midgard.bot.core.command.CommandCategory;
import me.ray.midgard.bot.core.command.CommandContext;
import me.ray.midgard.bot.core.command.CommandOption;
import me.ray.midgard.bot.core.command.SlashCommand;
import me.ray.midgard.bot.core.command.SubCommand;
import me.ray.midgard.bot.core.embed.EmbedFactory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.List;
import java.util.Optional;

@SlashCommand(
        name = "whitelist",
        description = "Gerenciar o sistema de whitelist",
        category = CommandCategory.ADMINISTRATION,
        permissions = {Permission.ADMINISTRATOR}
)
public class WhitelistCommand extends BaseCommand {

    private final WhitelistConfig config;
    private final WhitelistRepository repository;

    public WhitelistCommand(WhitelistConfig config, WhitelistRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    private WhitelistReviewListener reviewListener;
    private WhitelistRedisSync redisSync;

    public void setReviewListener(WhitelistReviewListener reviewListener) {
        this.reviewListener = reviewListener;
    }

    public void setRedisSync(WhitelistRedisSync redisSync) {
        this.redisSync = redisSync;
    }

    @Override
    public void execute(CommandContext ctx) {
        // Root command - show help
        ctx.replyEphemeral(EmbedFactory.info("Whitelist",
                "Use os subcomandos:\n" +
                "`/whitelist setup` - Envia o embed de registro no canal\n" +
                "`/whitelist setup-review` - Envia o painel de análise no canal\n" +
                "`/whitelist status` - Ver estatísticas\n" +
                "`/whitelist check` - Verificar inscrição de um usuário\n" +
                "`/whitelist nick` - Trocar o nick Minecraft de um usuário\n" +
                "`/whitelist force` - Forçar whitelist de um usuário\n" +
                "`/whitelist reset` - Resetar inscrição de um usuário\n" +
                "`/whitelist reload` - Recarregar configurações"));
    }

    // ==================== Setup ====================

    @SubCommand(name = "setup", description = "Envia o embed de registro no canal atual")
    public void setup(CommandContext ctx) {
        MessageChannel channel = ctx.getChannel();

        channel.sendMessageEmbeds(WhitelistEmbeds.registration(config))
                .addComponents(ActionRow.of(
                        Button.of(ButtonStyle.PRIMARY, WhitelistListener.BUTTON_START, config.getButtonStartText())
                ))
                .queue(success -> {
                    ctx.replyEphemeral(EmbedFactory.success("Setup Completo",
                            "Embed de whitelist enviado com sucesso neste canal!"));
                }, error -> {
                    ctx.replyEphemeral(EmbedFactory.error("Erro",
                            "Não foi possível enviar o embed: " + error.getMessage()));
                });
    }

    // ==================== Setup Review Panel ====================

    @SubCommand(name = "setup-review", description = "Envia o painel de análise de whitelists no canal atual")
    public void setupReview(CommandContext ctx) {
        if (reviewListener == null) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "O sistema de análise não está configurado."));
            return;
        }

        MessageChannel channel = ctx.getChannel();

        channel.sendMessageEmbeds(reviewListener.buildPanelEmbed())
                .addComponents(reviewListener.buildPanelButton())
                .queue(message -> {
                    reviewListener.getReviewManager().setPanelMessage(
                            channel.getId(), message.getId());
                    ctx.replyEphemeral(EmbedFactory.success("Setup Completo",
                            "Painel de análise enviado com sucesso neste canal!"));
                }, error -> {
                    ctx.replyEphemeral(EmbedFactory.error("Erro",
                            "Não foi possível enviar o painel: " + error.getMessage()));
                });
    }

    // ==================== Status ====================

    @SubCommand(name = "status", description = "Ver estatísticas do sistema de whitelist")
    public void status(CommandContext ctx) {
        long pending = repository.countByStatus(WhitelistApplication.Status.PENDING);
        long approved = repository.countByStatus(WhitelistApplication.Status.APPROVED);
        long rejected = repository.countByStatus(WhitelistApplication.Status.REJECTED);
        long inProgress = repository.countByStatus(WhitelistApplication.Status.IN_PROGRESS);
        long total = repository.count();

        ctx.replyEphemeral(EmbedFactory.create("📊 Whitelist - Estatísticas")
                .addField("📋 Total", String.valueOf(total), true)
                .addField("⏳ Em Andamento", String.valueOf(inProgress), true)
                .addField("📝 Pendentes", String.valueOf(pending), true)
                .addField("✅ Aprovadas", String.valueOf(approved), true)
                .addField("❌ Rejeitadas", String.valueOf(rejected), true)
                .addField("📊 Partes", String.valueOf(config.getPartCount()), true)
                .build());
    }

    // ==================== Check ====================

    @SubCommand(name = "check", description = "Verificar inscrição de um usuário")
    @CommandOption(name = "usuario", description = "O usuário para verificar", type = OptionType.USER, required = true)
    public void check(CommandContext ctx) {
        User target = ctx.getUser("usuario");
        if (target == null) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "Usuário não encontrado."));
            return;
        }

        Optional<WhitelistApplication> appOpt = repository.findById(target.getId());
        if (appOpt.isEmpty()) {
            ctx.replyEphemeral(EmbedFactory.warning("Sem Inscrição",
                    target.getAsMention() + " não possui nenhuma inscrição de whitelist."));
            return;
        }

        WhitelistApplication app = appOpt.get();

        if (app.getStatus() == WhitelistApplication.Status.IN_PROGRESS) {
            ctx.replyEphemeral(EmbedFactory.warning("Inscrição Incompleta",
                    target.getAsMention() + " ainda não enviou a whitelist. A inscrição está em andamento."));
            return;
        }
        var builder = EmbedFactory.userEmbed(target)
                .setTitle("📋 Inscrição de " + target.getName());

        builder.addField("📊 Status", formatStatus(app.getStatus()) +
                (app.isForced() ? " ⚡" : ""), true);
        builder.addField("📝 Parte", app.getCurrentPart() + "/" + config.getPartCount(), true);
        builder.addField("📅 Criada em", "<t:" + app.getCreatedAt().getEpochSecond() + ":R>", true);

        // Show answers
        List<List<WhitelistConfig.QuestionData>> allQuestions = config.getAllQuestions();
        for (int i = 0; i < allQuestions.size(); i++) {
            StringBuilder sb = new StringBuilder();
            for (WhitelistConfig.QuestionData q : allQuestions.get(i)) {
                String answer = app.getAnswer(q.getId());
                sb.append("**").append(q.getLabel()).append(":** ")
                        .append(answer != null ? answer : "*sem resposta*").append("\n");
            }
            String fieldValue = sb.toString();
            if (fieldValue.length() > 1024) {
                fieldValue = fieldValue.substring(0, 1021) + "...";
            }
            builder.addField("📝 " + config.getPartTitle(i), fieldValue, false);
        }

        if (app.getReviewedBy() != null) {
            builder.addField("👤 Revisado por", "<@" + app.getReviewedBy() + ">", true);
        }
        if (app.getReviewNote() != null) {
            builder.addField("📝 Nota", app.getReviewNote(), true);
        }
        if (app.isForced()) {
            builder.addField("⚡ Forçada", "Whitelist forçada por um administrador", false);
        }

        ctx.replyEphemeral(builder.build());
    }

    // ==================== Reset ====================

    @SubCommand(name = "reset", description = "Resetar inscrição de um usuário")
    @CommandOption(name = "usuario", description = "O usuário para resetar", type = OptionType.USER, required = true)
    public void reset(CommandContext ctx) {
        User target = ctx.getUser("usuario");
        if (target == null) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "Usuário não encontrado."));
            return;
        }

        // Get app before deleting (to extract nick for Redis cleanup)
        Optional<WhitelistApplication> appOpt = repository.findById(target.getId());

        int deleted = repository.deleteById(target.getId());
        if (deleted > 0) {
            // Remove from Redis cache
            if (redisSync != null) {
                String oldNick = appOpt.map(a -> a.getAnswer("nick")).orElse(null);
                if (oldNick != null) {
                    redisSync.removeApplication(oldNick);
                }
            }

            ctx.replyEphemeral(EmbedFactory.success("Inscrição Resetada",
                    "A inscrição de " + target.getAsMention() + " foi resetada.\nEle poderá se inscrever novamente."));
        } else {
            ctx.replyEphemeral(EmbedFactory.warning("Sem Inscrição",
                    target.getAsMention() + " não possui inscrição para resetar."));
        }
    }

    // ==================== Reload ====================

    @SubCommand(name = "reload", description = "Recarregar configurações da whitelist")
    public void reload(CommandContext ctx) {
        config.reload();
        ctx.replyEphemeral(EmbedFactory.success("Configuração Recarregada",
                "As configurações da whitelist foram recarregadas com sucesso."));
    }

    // ==================== Nick ====================

    @SubCommand(name = "nick", description = "Trocar o nick Minecraft de um usuário")
    @CommandOption(name = "usuario", description = "O usuário para trocar o nick", type = OptionType.USER, required = true)
    @CommandOption(name = "novo_nick", description = "O novo nick do Minecraft", type = OptionType.STRING, required = true)
    public void nick(CommandContext ctx) {
        User target = ctx.getUser("usuario");
        if (target == null) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "Usuário não encontrado."));
            return;
        }

        String novoNick = ctx.getString("novo_nick");
        if (novoNick == null || novoNick.isBlank()) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "O novo nick não pode estar vazio."));
            return;
        }

        // Validate Minecraft nick: 3-16 chars, alphanumeric + underscore
        if (!novoNick.matches("^[a-zA-Z0-9_]{3,16}$")) {
            ctx.replyEphemeral(EmbedFactory.error("Nick Inválido",
                    "O nick deve ter entre 3 e 16 caracteres e conter apenas letras, números e underscore."));
            return;
        }

        Optional<WhitelistApplication> appOpt = repository.findById(target.getId());
        if (appOpt.isEmpty()) {
            ctx.replyEphemeral(EmbedFactory.warning("Sem Inscrição",
                    target.getAsMention() + " não possui nenhuma inscrição de whitelist."));
            return;
        }

        WhitelistApplication app = appOpt.get();
        String oldNick = app.getAnswer("nick");

        app.setAnswer("nick", novoNick);
        repository.save(app);

        // Sync Redis cache
        if (redisSync != null) {
            if (oldNick != null && !oldNick.isBlank()) {
                redisSync.removeApplication(oldNick);
            }
            if (app.getStatus() != WhitelistApplication.Status.IN_PROGRESS) {
                redisSync.syncApplication(app);
            }
        }

        ctx.replyEphemeral(EmbedFactory.success("Nick Atualizado",
                "O nick de " + target.getAsMention() + " foi atualizado com sucesso!\n\n" +
                "📛 **Nick anterior:** " + (oldNick != null ? oldNick : "*nenhum*") + "\n" +
                "✅ **Nick atual:** " + novoNick));
    }

    // ==================== Force ====================

    @SubCommand(name = "force", description = "Forçar whitelist de um usuário")
    @CommandOption(name = "usuario", description = "O usuário para forçar a whitelist", type = OptionType.USER, required = true)
    @CommandOption(name = "nick_minecraft", description = "O nick do Minecraft do usuário", type = OptionType.STRING, required = true)
    public void force(CommandContext ctx) {
        User target = ctx.getUser("usuario");
        if (target == null) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "Usuário não encontrado."));
            return;
        }

        String nickMc = ctx.getString("nick_minecraft");
        if (nickMc == null || nickMc.isBlank()) {
            ctx.replyEphemeral(EmbedFactory.error("Erro", "O nick do Minecraft não pode estar vazio."));
            return;
        }

        if (!nickMc.matches("^[a-zA-Z0-9_]{3,16}$")) {
            ctx.replyEphemeral(EmbedFactory.error("Nick Inválido",
                    "O nick deve ter entre 3 e 16 caracteres e conter apenas letras, números e underscore."));
            return;
        }

        Optional<WhitelistApplication> existing = repository.findById(target.getId());
        if (existing.isPresent() && existing.get().getStatus() == WhitelistApplication.Status.APPROVED) {
            ctx.replyEphemeral(EmbedFactory.warning("Já Aprovado",
                    target.getAsMention() + " já possui uma whitelist aprovada."));
            return;
        }

        // Delete any existing application first
        if (existing.isPresent()) {
            repository.deleteById(target.getId());
        }

        // Create forced application
        WhitelistApplication app = new WhitelistApplication(target.getId());
        app.setUsername(target.getName());
        app.setAnswer("nick", nickMc);
        app.setCurrentPart(config.getPartCount());
        app.setForced(true);
        app.approve(ctx.getUser().getId(), "Whitelist forçada por administrador");
        repository.save(app);

        // Sync to Redis cache
        if (redisSync != null) {
            redisSync.syncApplication(app);
        }

        // Add approved role, remove pending role
        if (ctx.getGuild() != null) {
            String approvedRoleId = config.getApprovedRoleId();
            String pendingRoleId = config.getPendingRoleId();
            net.dv8tion.jda.api.entities.Guild guild = ctx.getGuild();

            if (approvedRoleId != null && !approvedRoleId.isEmpty()) {
                net.dv8tion.jda.api.entities.Role role = guild.getRoleById(approvedRoleId);
                if (role != null) {
                    guild.addRoleToMember(target, role).queue();
                }
            }
            if (pendingRoleId != null && !pendingRoleId.isEmpty()) {
                net.dv8tion.jda.api.entities.Role role = guild.getRoleById(pendingRoleId);
                if (role != null) {
                    guild.removeRoleFromMember(target, role).queue();
                }
            }
        }

        ctx.replyEphemeral(EmbedFactory.success("⚡ Whitelist Forçada",
                "A whitelist de " + target.getAsMention() + " foi forçada com sucesso!\n\n" +
                "🎮 **Nick:** " + nickMc + "\n" +
                "👤 **Por:** " + ctx.getUser().getAsMention() + "\n" +
                "✅ **Status:** Aprovada (Forçada)"));
    }

    // ==================== Helpers ====================

    private String formatStatus(WhitelistApplication.Status status) {
        switch (status) {
            case IN_PROGRESS: return "⏳ Em Andamento";
            case PENDING: return "📝 Pendente";
            case APPROVED: return "✅ Aprovada";
            case REJECTED: return "❌ Rejeitada";
            default: return status.name();
        }
    }
}
