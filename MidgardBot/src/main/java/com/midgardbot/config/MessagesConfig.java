package com.midgardbot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesConfig.class);
    private static final File FILE = new File("data/messages.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private static ConfigData data;

    public static void load() {
        if (!FILE.exists()) {
            createDefault();
            return;
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, ConfigData.class);
            LOGGER.info("Mensagens carregadas de data/messages.json");
            save(); // Salva para garantir que novos campos sejam adicionados ao arquivo
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar data/messages.json", e);
            createDefault(); // Fallback to default in memory
        }
    }

    private static void createDefault() {
        data = new ConfigData();
        
        // --- Whitelist Approved ---
        data.whitelist.approved.title = "Whitelist Aprovada! Bem-vindo(a) a Midgard!";
        data.whitelist.approved.description = "**Parabéns, {user}!**\n\nSua jornada em Midgard está prestes a começar! Estamos felizes em recebê-lo(a) em nosso mundo. Prepare-se para criar histórias e aventuras inesquecíveis.";
        data.whitelist.approved.image = "attachment://aprovado.png";
        data.whitelist.approved.thumbnail = "{user_avatar}";
        data.whitelist.approved.color = "#2ECC71"; // Green
        
        data.whitelist.approved.fields.add(new EmbedField("🎮 Como Entrar no Servidor", "**🖥️ Java Edition**\n> `jogar.midgard.com`", false));
        data.whitelist.approved.fields.add(new EmbedField("📚 Primeiros Passos em Midgard", "1️⃣ **Conecte-se** ao servidor usando o IP acima.\n2️⃣ **Explore o spawn** para conhecer as regras e o ambiente.\n3️⃣ **Interaja** com outros jogadores e comece a criar sua história.\n4️⃣ **Divirta-se** e construa sua lenda!", false));
        data.whitelist.approved.fields.add(new EmbedField("🔗 Links Úteis", "> 📦 [Loja Oficial](https://loja.midgard.com) — Itens e vantagens exclusivas.", false));
        
        // --- Whitelist Rejected (Public) ---
        data.whitelist.rejected.title = "Análise de Whitelist Concluída";
        data.whitelist.rejected.description = "Sua whitelist foi **REPROVADA**.\n\nInfelizmente, sua história não atendeu aos requisitos desta vez. Mas não desanime! Você pode tentar novamente em breve.";
        data.whitelist.rejected.image = "attachment://reprovado.png";
        data.whitelist.rejected.thumbnail = "{user_avatar}";
        data.whitelist.rejected.color = "#E74C3C"; // Red
        data.whitelist.rejected.fields.add(new EmbedField("📝 Motivo da Reprovação", "> {reason}", false));
        
        // --- Whitelist Rejected (DM) ---
        data.whitelist.dm_rejected.title = "❌ Atualização sobre sua Whitelist";
        data.whitelist.dm_rejected.description = "Olá, analisamos sua aplicação para o **Midgard RPG** e, infelizmente, ela não foi aprovada desta vez.";
        data.whitelist.dm_rejected.image = "attachment://reprovado.png";
        data.whitelist.dm_rejected.color = "#E74C3C"; // Red
        data.whitelist.dm_rejected.fields.add(new EmbedField("📝 Motivo da Reprovação", "> {reason}", false));
        data.whitelist.dm_rejected.fields.add(new EmbedField("💡 Dica", "Respostas mais detalhadas e criativas têm muito mais chance de aprovação. Não desista!", false));

        // --- Staff Approved ---
        data.whitelist.staff_approved.title = "Whitelist Aprovada";
        data.whitelist.staff_approved.description = "O candidato {user} foi aprovado com sucesso!\nO resultado foi enviado no canal público.";
        data.whitelist.staff_approved.color = "#2ECC71";

        // --- Staff Rejected ---
        data.whitelist.staff_rejected.title = "Whitelist Reprovada";
        data.whitelist.staff_rejected.description = "O candidato {user} foi reprovado.\nO resultado foi enviado no canal público.";
        data.whitelist.staff_rejected.color = "#E74C3C";

        // --- Welcome ---
        data.welcome.join.title = "👋 Bem-vindo(a) a Midgard!";
        data.welcome.join.description = "Olá {user}, seja muito bem-vindo(a) ao nosso servidor!\n\n📜 **Leia as regras** para evitar punições.\n✅ **Faça sua Whitelist** para jogar no servidor.\n💬 **Interaja** com a comunidade nos canais de bate-papo.";
        data.welcome.join.color = "#00FFFF"; // Cyan

        // --- Ticket Setup ---
        data.ticket.setup.title = "📬 Central de Atendimento";
        data.ticket.setup.description = "Bem-vindo ao canal oficial de suporte. Selecione o departamento correspondente à sua necessidade no menu abaixo.\n\nℹ️ **Diretrizes de Atendimento:**\n• Verifique se sua dúvida já consta na **Wiki** ou **FAQ** antes de abrir um chamado.\n• O uso indevido deste sistema está sujeito a sanções administrativas.";
        data.ticket.setup.fields.add(new EmbedField("⏰ Horário de Operação", "**Segunda a Sexta:** 09:00 - 19:00\n**Finais de Semana:** 10:00 - 18:00", false));
        
        data.ticket.created.title = "{emoji} Ticket de {category}";
        data.ticket.created.description = "Olá {user} | {nickname}!\n\n⚠️ **Atenção:** Por favor, **não marque** membros da equipe. Aguarde, você será atendido em breve.\n\nDescreva seu problema detalhadamente abaixo.";
        data.ticket.created.color = "#3498DB"; // Blue

        data.ticket.closed_dm.title = "📬 Atendimento Finalizado";
        data.ticket.closed_dm.description = "Olá, {user}! 👋\n\nSeu ticket foi encerrado com sucesso. Esperamos ter resolvido sua solicitação da melhor maneira possível.\n\n━━━━━━━━━━━━━━━━━━━━━━\n\n**👥 Equipe de Atendimento:**\n{staff_list}\n\n**⭐ Avalie nosso Atendimento**\nSua opinião é essencial para melhorarmos nossos serviços. Por favor, dedique um momento para avaliar sua experiência utilizando os botões abaixo.\n\n*Agradecemos por fazer parte da comunidade Midgard RPG!*";
        data.ticket.closed_dm.color = "#5865F2"; // Blurple

        // --- Moderation ---
        data.moderation.ban.title = "🔨 Usuário Banido";
        data.moderation.ban.description = "**Usuário:** {user_mention}\n**ID:** {user_id}\n\n**Motivo:**\n> {reason}\n\n**Aplicado por:** {staff_mention}";
        data.moderation.ban.thumbnail = "{guild_icon}";
        data.moderation.ban.color = "#E74C3C"; // Red

        data.moderation.warn.title = "⚠️ Usuário Advertido";
        data.moderation.warn.description = "**Usuário:** {user_mention}\n**ID:** {user_id}\n\n**Motivo:**\n> {reason}\n\n**ID da Advertência:** `{warn_id}`\n**Total de Advertências:** `{warn_count}`\n\n**Aplicado por:** {staff_mention}";
        data.moderation.warn.thumbnail = "{guild_icon}";
        data.moderation.warn.color = "#F1C40F"; // Yellow

        data.moderation.dm_warn.title = "⚠️ Aviso de Moderação";
        data.moderation.dm_warn.description = "Você recebeu uma advertência no servidor **{guild_name}**.";
        data.moderation.dm_warn.fields.add(new EmbedField("Motivo", "> {reason}", false));
        data.moderation.dm_warn.fields.add(new EmbedField("ID da Advertência", "`{warn_id}`", true));
        data.moderation.dm_warn.thumbnail = "{guild_icon}";
        data.moderation.dm_warn.color = "#F1C40F"; // Yellow

        data.moderation.unwarn.title = "✅ Advertência Removida";
        data.moderation.unwarn.description = "**Usuário:** {user_mention}\n**ID da Advertência:** `{warn_id}`\n\n**Removido por:** {staff_mention}";
        data.moderation.unwarn.thumbnail = "{guild_icon}";
        data.moderation.unwarn.color = "#2ECC71"; // Green

        // --- General ---
        data.general.access_denied.title = "Acesso Negado";
        data.general.access_denied.description = "Você não tem permissão para isso.";
        data.general.access_denied.color = "#E74C3C";

        save();
    }

    public static void save() {
        try {
            if (FILE.getParentFile() != null) FILE.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            LOGGER.info("Arquivo data/messages.json salvo com sucesso.");
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar data/messages.json", e);
        }
    }

    public static ConfigData get() {
        if (data == null) load();
        return data;
    }

    public static EmbedBuilder buildEmbed(EmbedConfig config, Map<String, String> placeholders) {
        EmbedBuilder eb = new EmbedBuilder();
        
        if (config.title != null) eb.setTitle(replace(config.title, placeholders));
        if (config.description != null) eb.setDescription(replace(config.description, placeholders));
        if (config.color != null) {
            try {
                eb.setColor(Color.decode(config.color));
            } catch (Exception e) {
                LOGGER.error("Cor inválida na config: " + config.color, e);
            }
        }
        if (config.image != null && !config.image.isEmpty()) eb.setImage(replace(config.image, placeholders));
        if (config.thumbnail != null && !config.thumbnail.isEmpty()) eb.setThumbnail(replace(config.thumbnail, placeholders));
        
        for (EmbedField field : config.fields) {
            eb.addField(replace(field.name, placeholders), replace(field.value, placeholders), field.inline);
        }
        
        return eb;
    }

    private static String replace(String text, Map<String, String> placeholders) {
        if (text == null) return null;
        if (placeholders == null) return text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    public static class ConfigData {
        public WhitelistSection whitelist = new WhitelistSection();
        public WelcomeSection welcome = new WelcomeSection();
        public TicketSection ticket = new TicketSection();
        public ModerationSection moderation = new ModerationSection();
        public GeneralSection general = new GeneralSection();
        public ResourcesSection resources = new ResourcesSection();
    }

    public static class ResourcesSection {
        public String img_welcome = "https://i.imgur.com/aSAj2iC.png";
        public String img_rejected = "https://i.imgur.com/dJt6p5f.png";
        public String img_whitelist_panel = "https://i.imgur.com/rLcL49T.png";
        public String img_submitted = "https://i.imgur.com/y7vRk9m.png";
    }

    public static class WhitelistSection {
        public EmbedConfig approved = new EmbedConfig();
        public EmbedConfig rejected = new EmbedConfig();
        public EmbedConfig dm_rejected = new EmbedConfig();
        public EmbedConfig staff_approved = new EmbedConfig();
        public EmbedConfig staff_rejected = new EmbedConfig();
    }

    public static class WelcomeSection {
        public EmbedConfig join = new EmbedConfig();
    }

    public static class TicketSection {
        public EmbedConfig setup = new EmbedConfig();
        public EmbedConfig created = new EmbedConfig(); // For when a ticket is created (if applicable)
        public EmbedConfig closed_dm = new EmbedConfig();
        public String support_label = "Suporte Técnico & Geral";
        public String support_desc = "Auxílio com jogabilidade ou sistema";
        public String support_mention = "<@&ID_DO_CARGO_SUPORTE>"; // Menção automática para Suporte
        
        public String report_label = "Denúncias & Infrações";
        public String report_desc = "Reportar conduta inadequada";
        public String report_mention = "<@&ID_DO_CARGO_DENUNCIA>"; // Menção automática para Denúncia
        
        public String bug_label = "Relatório de Bugs";
        public String bug_desc = "Reportar falhas técnicas";
        public String bug_mention = "<@&ID_DO_CARGO_DEV>"; // Menção automática para Bugs
        
        public String lore_label = "Lore & Roleplay";
        public String lore_desc = "Aprovação de histórias e dúvidas de RP";
        public String lore_mention = "<@&ID_DO_CARGO_LORE>"; // Menção automática para Lore
        
        public TicketVoiceSection voice = new TicketVoiceSection();
        // public TicketAISection ai = new TicketAISection(); // Removido
        public TicketSnippetsSection snippets = new TicketSnippetsSection();
        public TicketCloseSection close = new TicketCloseSection();
        public TicketScheduleSection schedule = new TicketScheduleSection();
    }

    public static class TicketVoiceSection {
        public String exists = "⚠️ Já existe um canal de voz para este ticket: {channel}";
        public String no_category = "❌ Erro: Este ticket não está em uma categoria.";
        public String created = "✅ Canal de voz criado: {channel}";
        public String channel_msg = "🔊 **Canal de Voz Criado:** {channel}\nUse este canal para conversar com a equipe.";
        public String error_perms = "❌ Erro ao criar canal de voz (Permissões?).";
        public String error_internal = "❌ Erro interno ao criar voz.";
    }

    /*
    public static class TicketAISection {
        public String title = "🧠 Análise Inteligente do Ticket";
        public String footer = "Gerado por Gemini AI • MidgardBot";
        public String error_internal = "❌ Erro interno ao processar a IA.";
        public String error_history = "❌ Erro ao recuperar histórico do chat.";
        public String error_api = "❌ Erro na API de IA ({status})";
        public String error_config = "❌ API do Gemini não configurada.";
        public String error_empty = "❌ Histórico vazio ou ilegível.";
    }
    */

    public static class TicketSnippetsSection {
        public String menu_text = "💬 **Respostas Rápidas**\nSelecione uma mensagem para enviar no chat:";
        public String error_load = "❌ Erro ao carregar respostas rápidas.";
        public String success = "✅ Mensagem enviada.";
        
        // Snippet Contents
        public String wait = "⏳ **Aguarde um momento.**\nNossa equipe já está analisando o seu caso e responderá em breve.";
        public String modem = "📶 **Problemas de Conexão?**\nPor favor, tente reiniciar seu modem e o jogo. Verifique também se o firewall não está bloqueando o Java.";
        public String ip = "🔗 **IP do Servidor:**\n`jogar.midgardrpg.com` (Java)\n`bedrock.midgardrpg.com` (Bedrock - Porta 19132)";
        public String cache = "🧹 **Limpeza de Cache:**\n1. Feche o jogo.\n2. Delete a pasta `.minecraft/cache`.\n3. Tente entrar novamente.";
        public String admin = "👮 **Encaminhando...**\nEste caso requer permissões superiores. Estou encaminhando para um Administrador.";
        public String refund = "💸 **Política de Reembolso:**\nConforme nossos termos, itens consumíveis ou ativados não são passíveis de reembolso. Verifique as regras em nosso site.";
    }

    public static class TicketCloseSection {
        public String archiving = "🔒 **Ticket Fechado.** Arquivando e deletando em 5 segundos...";
        public String no_archive = "⚠️ **Aviso:** Sistema de arquivamento indisponível. O ticket será deletado em 10 segundos.";
        public String error = "❌ Erro ao processar fechamento. Deletando forçadamente em 10s.";
    }

    public static class TicketScheduleSection {
        public String title = "⚠️ Atenção, {user_name} \uD83C\uDFEE!";
        public String description = "Você está abrindo um ticket fora do nosso horário de atendimento.\n\n" +
            "\uD83D\uDD70\uFE0F **Horário de Atendimento:**\n" +
            "> Segunda a Sexta: {weekday_start}h às {weekday_end}h\n" +
            "> Sábado e Domingo: {weekend_start}h às {weekend_end}h\n\n" +
            "Nosso suporte ainda poderá analisar seu ticket, mas a resposta poderá demorar " +
            "um pouco mais do que o habitual. Assim que possível, um membro da equipe " +
            "**Midgard RPG** irá entrar em contato com você.";
        public String footer = "Midgard RPG — Atendimento ao Jogador";
        public String color = "#E74C3C";
    }

    public static class ModerationSection {
        public EmbedConfig ban = new EmbedConfig();
        public EmbedConfig warn = new EmbedConfig();
        public EmbedConfig dm_warn = new EmbedConfig();
        public EmbedConfig unwarn = new EmbedConfig();
    }

    public static class GeneralSection {
        public EmbedConfig access_denied = new EmbedConfig();
        public EmbedConfig success = new EmbedConfig();
        public EmbedConfig error = new EmbedConfig();
    }

    public static class EmbedConfig {
        public String title;
        public String description;
        public String image;
        public String thumbnail;
        public String color;
        public List<EmbedField> fields = new ArrayList<>();
    }

    public static class EmbedField {
        public String name;
        public String value;
        public boolean inline;

        public EmbedField(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}
