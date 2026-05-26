package de.maxhenkel.voicechat.config;

import de.maxhenkel.voicechat.FallbackTranslations;
import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Messages {

    private static final int CONFIG_VERSION = 2;
    private static final Map<String, String> TRANSLATION_DEFAULTS = createTranslationDefaults();

    private final File file;
    private YamlConfiguration config;

    // === Prefixo ===
    public String prefix;

    // === Comandos Gerais ===
    public String somente_jogadores;
    public String sem_permissao;
    public String jogador_nao_encontrado;

    // === VoiceChat Comandos ===
    public String voicechat_necessario;
    public String jogador_sem_voicechat;
    public String cliente_nao_conectado;
    public String enviando_ping;
    public String ping_enviado_aguardando;
    public String ping_recebido;
    public String ping_recebido_tentativas;
    public String ping_sem_resposta;
    public String ping_tempo_esgotado;
    public String falha_enviar_ping;
    public String nao_esta_no_grupo;
    public String convite_enviado;
    public String grupo_nao_existe;
    public String grupo_nome_ambiguo;
    public String grupos_desativados;
    public String sem_permissao_grupo;
    public String grupo_entrou;
    public String grupo_saiu;

    // === Zonas Restritas - Comandos ===
    public String zona_titulo;
    public String zona_menu;
    public String zona_menu_desc;
    public String zona_pos1;
    public String zona_pos1_desc;
    public String zona_pos2;
    public String zona_pos2_desc;
    public String zona_create;
    public String zona_create_desc;
    public String zona_reload;
    public String zona_reload_desc;
    public String zona_bypass_permissao;
    public String zona_pos1_definida;
    public String zona_pos2_definida;
    public String zona_uso_create;
    public String zona_nome_invalido;
    public String zona_definir_posicoes;
    public String zona_mesmo_mundo;
    public String zona_criada;
    public String zona_sem_permissao_bypass;
    public String zona_ja_existe;
    public String zona_recarregada;
    public String zona_sem_permissao_gerenciar;

    // === Zonas Restritas - Menus GUI ===
    public String gui_zonas_titulo;
    public String gui_zona_mundo;
    public String gui_zona_de;
    public String gui_zona_ate;
    public String gui_zona_voz_ativada;
    public String gui_zona_voz_desativada;
    public String gui_zona_permitidos;
    public String gui_zona_mutados;
    public String gui_zona_clique_gerenciar;
    public String gui_zona_recarregar;
    public String gui_zona_recarregar_desc;
    public String gui_zona_recarregadas;
    public String gui_zona_nao_encontrada;
    public String gui_zona_nao_existe_mais;
    public String gui_zonas_info_titulo;
    public String gui_zonas_info_total;
    public String gui_zonas_info_desc;
    public String gui_zonas_vazio;

    // === Menu Configuracoes da Zona ===
    public String gui_zona_prefixo;
    public String gui_zona_voz_ativada_titulo;
    public String gui_zona_voz_ativada_desc;
    public String gui_zona_voz_ativada_clique;
    public String gui_zona_voz_desativada_titulo;
    public String gui_zona_voz_desativada_desc;
    public String gui_zona_voz_desativada_clique;
    public String gui_zona_jogadores_permitidos;
    public String gui_zona_permitidos_desc1;
    public String gui_zona_permitidos_desc2;
    public String gui_zona_atualmente;
    public String gui_zona_jogadores_mutados;
    public String gui_zona_mutados_desc1;
    public String gui_zona_mutados_desc2;
    public String gui_zona_deletar;
    public String gui_zona_deletar_aviso;
    public String gui_zona_deletar_clique;
    public String gui_zona_voltar;
    public String gui_zona_voltar_lista;
    public String gui_zona_voz_ativada_msg;
    public String gui_zona_voz_desativada_msg;
    public String gui_zona_deletada;

    // === Menu Config Zona - Range Customizado ===
    public String gui_zona_range_titulo;
    public String gui_zona_range_atual;
    public String gui_zona_range_padrao_valor;
    public String gui_zona_range_clique;
    public String gui_zona_range_alterado;

    // === Menu Config Zona - Multiplicador ===
    public String gui_zona_multiplicador_titulo;
    public String gui_zona_multiplicador_atual;
    public String gui_zona_multiplicador_clique;
    public String gui_zona_multiplicador_alterado;

    // === Menu Config Zona - Somente Escuta ===
    public String gui_zona_escuta_ativada;
    public String gui_zona_escuta_desc;
    public String gui_zona_escuta_clique_desativar;
    public String gui_zona_escuta_desativada;
    public String gui_zona_escuta_desc_off;
    public String gui_zona_escuta_clique_ativar;
    public String gui_zona_escuta_ativada_msg;
    public String gui_zona_escuta_desativada_msg;

    // === Menu Config Zona - Temporaria ===
    public String gui_zona_tempo_restante;
    public String gui_zona_expirada;

    // === Menu Jogadores Permitidos ===
    public String gui_permitidos_prefixo;
    public String gui_permitidos_clique_remover;
    public String gui_permitidos_adicionar;
    public String gui_permitidos_adicionar_desc1;
    public String gui_permitidos_adicionar_desc2;
    public String gui_permitidos_voltar;
    public String gui_permitidos_removido;
    public String gui_permitidos_adicionado;

    // === Menu Jogadores Mutados ===
    public String gui_mutados_prefixo;
    public String gui_mutados_clique_desmutar;
    public String gui_mutados_mutar;
    public String gui_mutados_mutar_desc1;
    public String gui_mutados_mutar_desc2;
    public String gui_mutados_voltar;
    public String gui_mutados_desmutado;
    public String gui_mutados_mutado;

    // === Menu Adicionar Jogador ===
    public String gui_add_permitido_prefixo;
    public String gui_add_mutado_prefixo;
    public String gui_add_clique_permitir;
    public String gui_add_clique_mutar;
    public String gui_add_voltar;

    // === Range de Voz - Comandos ===
    public String range_titulo;
    public String range_menu;
    public String range_menu_desc;
    public String range_set;
    public String range_set_desc;
    public String range_remove;
    public String range_remove_desc;
    public String range_info;
    public String range_info_desc;
    public String range_list;
    public String range_list_desc;
    public String range_reload;
    public String range_reload_desc;
    public String range_sem_permissao;
    public String range_uso_set;
    public String range_uso_remove;
    public String range_valor_invalido;
    public String range_definido;
    public String range_removido;
    public String range_sem_custom;
    public String range_info_jogador;
    public String range_info_padrao;
    public String range_lista_vazia;
    public String range_lista_titulo;
    public String range_lista_item;
    public String range_recarregado;

    // === Range de Voz - GUI ===
    public String gui_range_titulo;
    public String gui_range_info_titulo;
    public String gui_range_info_total;
    public String gui_range_info_desc;
    public String gui_range_range_padrao;
    public String gui_range_vazio;
    public String gui_range_jogador_range;
    public String gui_range_clique_gerenciar;
    public String gui_range_definir;
    public String gui_range_definir_desc;
    public String gui_range_recarregar;
    public String gui_range_recarregar_desc;
    public String gui_range_recarregadas;
    public String gui_range_removido_gui;
    public String gui_range_selecionar_prefixo;
    public String gui_range_selecionar_titulo;
    public String gui_range_selecionar_desc;
    public String gui_range_sem_custom_gui;
    public String gui_range_clique_selecionar;
    public String gui_range_voltar;
    public String gui_range_voltar_lista;
    public String gui_range_distancia_prefixo;
    public String gui_range_distancia_atual;
    public String gui_range_distancia_blocos;
    public String gui_range_distancia_clique;
    public String gui_range_distancia_selecionado;
    public String gui_range_distancia_definido;
    public String gui_range_remover;
    public String gui_range_remover_desc;

    // === Global Voice - GUI ===
    public String gui_global_titulo;
    public String gui_global_info_titulo;
    public String gui_global_info_total;
    public String gui_global_info_desc;
    public String gui_global_vazio;
    public String gui_global_jogador_desc;
    public String gui_global_clique_remover;
    public String gui_global_adicionar;
    public String gui_global_adicionar_desc;
    public String gui_global_clique_adicionar;
    public String gui_global_voltar_lista;
    public String gui_global_selecionar_titulo;
    public String gui_global_botao;
    public String gui_global_botao_desc;
    public String gui_global_removido;
    public String gui_global_adicionado;

    // === Global Voice - Comandos ===
    public String range_global;
    public String range_global_desc;
    public String range_global_uso_add;
    public String range_global_uso_remove;
    public String range_global_adicionado;
    public String range_global_ja_global;
    public String range_global_removido;
    public String range_global_nao_global;
    public String range_global_lista_vazia;
    public String range_global_lista_titulo;
    public String range_global_lista_item;

    // === Global Voice - Permissao e Limite ===
    public String global_sem_permissao;
    public String global_limite_atingido;

    // === Status ===
    public String status_titulo;
    public String status_range;
    public String status_range_custom;
    public String status_global_sim;
    public String status_global_nao;
    public String status_global_total;
    public String status_ilimitado;
    public String status_zona_nenhuma;
    public String status_zona_em;
    public String status_zona_voz_ativada;
    public String status_zona_voz_desativada;

    // === Notificacao de Zona ===
    public String zona_entrou_notificacao;
    public String zona_saiu_notificacao;
    public String zona_notificacao_voz_ativada;
    public String zona_notificacao_voz_desativada;

    // === Volume por Jogador ===
    public String range_volume;
    public String range_volume_desc;
    public String volume_uso;
    public String volume_valor_invalido;
    public String volume_definido;
    public String volume_removido;
    public String volume_sem_custom;

    // === Prioridade de Audio ===
    public String range_priority;
    public String range_priority_desc;
    public String priority_uso;
    public String priority_valor_invalido;
    public String priority_definido;

    // === Cooldown de Voz ===
    public String range_cooldown;
    public String range_cooldown_desc;
    public String cooldown_uso;
    public String cooldown_uso_set;
    public String cooldown_valor_invalido;
    public String cooldown_definido;
    public String cooldown_desativado;
    public String cooldown_desativado_info;
    public String cooldown_info;
    public String cooldown_ativo;
    public String cooldown_iniciado;

    // === Stage Mode ===
    public String gui_zona_stage_ativado;
    public String gui_zona_stage_desc;
    public String gui_zona_stage_speakers;
    public String gui_zona_stage_clique_desativar;
    public String gui_zona_stage_desativado;
    public String gui_zona_stage_desc_off;
    public String gui_zona_stage_clique_ativar;
    public String gui_zona_stage_ativado_msg;
    public String gui_zona_stage_desativado_msg;

    // === Zone Cooldown ===
    public String gui_zona_cooldown_titulo;
    public String gui_zona_cooldown_desc;
    public String gui_zona_cooldown_fala;
    public String gui_zona_cooldown_espera;
    public String gui_zona_cooldown_clique;
    public String gui_zona_cooldown_desativado;
    public String gui_zona_cooldown_alterado;
    public String zona_cooldown_ativo;
    public String zona_cooldown_iniciado;

    // === Indicador Global ===
    public String global_indicador_falando;

    // === Gravacao de Audio ===
    public String rec_titulo;
    public String rec_start;
    public String rec_start_desc;
    public String rec_stop;
    public String rec_stop_desc;
    public String rec_active;
    public String rec_active_desc;
    public String rec_list;
    public String rec_list_desc;
    public String rec_info;
    public String rec_info_desc;
    public String rec_delete;
    public String rec_delete_desc;
    public String rec_uso_start;
    public String rec_uso_stop;
    public String rec_uso_info;
    public String rec_uso_delete;
    public String rec_ja_gravando;
    public String rec_iniciada;
    public String rec_id;
    public String rec_nao_gravando;
    public String rec_parada;
    public String rec_salva;
    public String rec_nenhuma_ativa;
    public String rec_ativas_titulo;
    public String rec_ativa_item;
    public String rec_nenhuma_salva;
    public String rec_lista_titulo;
    public String rec_lista_item;
    public String rec_info_titulo;
    public String rec_nao_encontrada;
    public String rec_deletada;

    public Messages() {
        this.file = new File(Voicechat.INSTANCE.getDataFolder(), "mensagens.yml");
        load();
    }

    private static Map<String, String> createTranslationDefaults() {
        Map<String, String> defaults = new LinkedHashMap<>(FallbackTranslations.FALLBACK_TRANSLATIONS);
        defaults.put("argument.entity.notfound.player", "Jogador nao encontrado");
        return defaults;
    }

    public void load() {
        if (!file.exists()) {
            createDefaults();
        } else {
            config = YamlConfiguration.loadConfiguration(file);
            if (config.getInt("config_version", 1) < CONFIG_VERSION) {
                file.delete();
                createDefaults();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadValues();
    }

    private void loadValues() {
        prefix = color(config.getString("prefixo", ""));

        // Comandos Gerais
        somente_jogadores = color(config.getString("geral.somente_jogadores", "&cᴀᴘᴇɴᴀs ᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ."));
        sem_permissao = color(config.getString("geral.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ."));
        jogador_nao_encontrado = color(config.getString("geral.jogador_nao_encontrado", "&cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ."));

        // VoiceChat
        voicechat_necessario = color(config.getString("voicechat.necessario", "&cᴠᴏᴄê ᴘʀᴇᴄɪsᴀ ᴛᴇʀ ᴏ %s ɪɴsᴛᴀʟᴀᴅᴏ ᴘᴀʀᴀ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ."));
        jogador_sem_voicechat = color(config.getString("voicechat.jogador_sem_voicechat", "&c%s ɴãᴏ ᴛᴇᴍ ᴏ %s ɪɴsᴛᴀʟᴀᴅᴏ."));
        cliente_nao_conectado = color(config.getString("voicechat.cliente_nao_conectado", "&cᴄʟɪᴇɴᴛᴇ ɴãᴏ ᴄᴏɴᴇᴄᴛᴀᴅᴏ."));
        enviando_ping = color(config.getString("voicechat.enviando_ping", "&eᴇɴᴠɪᴀɴᴅᴏ ᴘɪɴɢ..."));
        ping_enviado_aguardando = color(config.getString("voicechat.ping_enviado_aguardando", "&eᴘɪɴɢ ᴇɴᴠɪᴀᴅᴏ. ᴀɢᴜᴀʀᴅᴀɴᴅᴏ ʀᴇsᴘᴏsᴛᴀ..."));
        ping_recebido = color(config.getString("voicechat.ping_recebido", "&aʀᴇsᴘᴏsᴛᴀ ʀᴇᴄᴇʙɪᴅᴀ ᴇᴍ %s ᴍs"));
        ping_recebido_tentativas = color(config.getString("voicechat.ping_recebido_tentativas", "&aʀᴇsᴘᴏsᴛᴀ ʀᴇᴄᴇʙɪᴅᴀ ᴇᴍ %s ᴍs apos %s tentativas"));
        ping_sem_resposta = color(config.getString("voicechat.ping_sem_resposta", "&esᴇᴍ ʀᴇsᴘᴏsᴛᴀ. ᴛᴇɴᴛᴀɴᴅᴏ ɴᴏᴠᴀᴍᴇɴᴛᴇ..."));
        ping_tempo_esgotado = color(config.getString("voicechat.ping_tempo_esgotado", "&cᴛᴇᴍᴘᴏ ᴇsɢᴏᴛᴀᴅᴏ ᴀᴘós %s ᴛᴇɴᴛᴀᴛɪᴠᴀs"));
        falha_enviar_ping = color(config.getString("voicechat.falha_enviar_ping", "&cꜰᴀʟʜᴀ ᴀᴏ ᴇɴᴠɪᴀʀ ᴘɪɴɢ: %s"));
        nao_esta_no_grupo = color(config.getString("voicechat.nao_esta_no_grupo", "&cᴠᴏᴄê ɴãᴏ ᴇsᴛá ᴇᴍ ᴜᴍ ɢʀᴜᴘᴏ."));
        convite_enviado = color(config.getString("voicechat.convite_enviado", "&a%s ꜰᴏɪ ᴄᴏɴᴠɪᴅᴀᴅᴏ."));
        grupo_nao_existe = color(config.getString("voicechat.grupo_nao_existe", "&cᴇsᴛᴇ ɢʀᴜᴘᴏ ɴãᴏ ᴇxɪsᴛᴇ."));
        grupo_nome_ambiguo = color(config.getString("voicechat.grupo_nome_ambiguo", "&cᴏ ɴᴏᴍᴇ ᴅᴏ ɢʀᴜᴘᴏ é ᴀᴍʙíɢᴜᴏ."));
        grupos_desativados = color(config.getString("voicechat.grupos_desativados", "&cᴏs ɢʀᴜᴘᴏs ᴇsᴛãᴏ ᴅᴇsᴀᴛɪᴠᴀᴅᴏs ɴᴇsᴛᴇ sᴇʀᴠɪᴅᴏʀ."));
        sem_permissao_grupo = color(config.getString("voicechat.sem_permissao_grupo", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ᴇɴᴛʀᴀʀ ᴇᴍ ɢʀᴜᴘᴏs."));
        grupo_entrou = color(config.getString("voicechat.grupo_entrou", "&aᴇɴᴛʀᴏᴜ ɴᴏ ɢʀᴜᴘᴏ %s."));
        grupo_saiu = color(config.getString("voicechat.grupo_saiu", "&asᴀɪᴜ ᴅᴏ ɢʀᴜᴘᴏ."));

        // Zonas Restritas - Comandos
        zona_titulo = color(config.getString("zona.titulo", "&7ᴢᴏɴᴀs ʀᴇsᴛʀɪᴛᴀs:"));
        zona_menu = color(config.getString("zona.menu", "&e/mvoice zone"));
        zona_menu_desc = color(config.getString("zona.menu_desc", "&7— ᴍᴇɴᴜ ᴅᴇ ɢᴇʀᴇɴᴄɪᴀᴍᴇɴᴛᴏ ᴅᴇ ᴢᴏɴᴀs"));
        zona_pos1 = color(config.getString("zona.pos1", "&e/mvoice zone pos1"));
        zona_pos1_desc = color(config.getString("zona.pos1_desc", "&7— ᴅᴇꜰɪɴɪʀ ᴘʀɪᴍᴇɪʀᴀ ᴘᴏsɪçãᴏ"));
        zona_pos2 = color(config.getString("zona.pos2", "&e/mvoice zone pos2"));
        zona_pos2_desc = color(config.getString("zona.pos2_desc", "&7— ᴅᴇꜰɪɴɪʀ sᴇɢᴜɴᴅᴀ ᴘᴏsɪçãᴏ"));
        zona_create = color(config.getString("zona.create", "&e/mvoice zone create <nome>"));
        zona_create_desc = color(config.getString("zona.create_desc", "&7— ᴄʀɪᴀʀ ᴢᴏɴᴀ ᴀ ᴘᴀʀᴛɪʀ ᴅᴀ sᴇʟᴇçãᴏ"));
        zona_reload = color(config.getString("zona.reload", "&e/mvoice zone reload"));
        zona_reload_desc = color(config.getString("zona.reload_desc", "&7— ʀᴇᴄᴀʀʀᴇɢᴀʀ ᴢᴏɴᴀs"));
        zona_bypass_permissao = color(config.getString("zona.bypass_permissao", "&7ᴘᴇʀᴍɪssãᴏ ᴅᴇ ʙʏᴘᴀss: &fvoicechat.zone.bypass"));
        zona_pos1_definida = color(config.getString("zona.pos1_definida", "&aᴘᴏsɪçãᴏ 1 ᴅᴇꜰɪɴɪᴅᴀ ᴇᴍ: %s"));
        zona_pos2_definida = color(config.getString("zona.pos2_definida", "&aᴘᴏsɪçãᴏ 2 ᴅᴇꜰɪɴɪᴅᴀ ᴇᴍ: %s"));
        zona_uso_create = color(config.getString("zona.uso_create", "&cᴜsᴏ: &e/mvoice zone create <nome>"));
        zona_nome_invalido = color(config.getString("zona.nome_invalido", "&cᴏ ɴᴏᴍᴇ ᴅᴀ ᴢᴏɴᴀ só ᴘᴏᴅᴇ ᴄᴏɴᴛᴇʀ ʟᴇᴛʀᴀs, ɴúᴍᴇʀᴏs, ʜíꜰᴇɴs ᴇ sᴜʙʟɪɴʜᴀᴅᴏs."));
        zona_definir_posicoes = color(config.getString("zona.definir_posicoes", "&cᴠᴏᴄê ᴘʀᴇᴄɪsᴀ ᴅᴇꜰɪɴɪʀ ᴀᴍʙᴀs ᴀs ᴘᴏsɪçõᴇs. ᴜsᴇ /mvoice zone pos1 ᴇ /mvoice zone pos2"));
        zona_mesmo_mundo = color(config.getString("zona.mesmo_mundo", "&cᴀᴍʙᴀs ᴀs ᴘᴏsɪçõᴇs ᴅᴇᴠᴇᴍ ᴇsᴛᴀʀ ɴᴏ ᴍᴇsᴍᴏ ᴍᴜɴᴅᴏ."));
        zona_criada = color(config.getString("zona.criada", "&aᴢᴏɴᴀ ᴅᴇ ᴠᴏᴢ ʀᴇsᴛʀɪᴛᴀ '%s' ᴄʀɪᴀᴅᴀ."));
        zona_sem_permissao_bypass = color(config.getString("zona.sem_permissao_bypass", "&7ᴊᴏɢᴀᴅᴏʀᴇs sᴇᴍ ᴘᴇʀᴍɪssãᴏ 'voicechat.zone.bypass' ɴãᴏ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ áʀᴇᴀ."));
        zona_ja_existe = color(config.getString("zona.ja_existe", "&cᴊá ᴇxɪsᴛᴇ ᴜᴍᴀ ᴢᴏɴᴀ ᴄᴏᴍ ᴇssᴇ ɴᴏᴍᴇ."));
        zona_recarregada = color(config.getString("zona.recarregada", "&aᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ ʀᴇsᴛʀɪᴛᴀs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴀs."));
        zona_sem_permissao_gerenciar = color(config.getString("zona.sem_permissao_gerenciar", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ."));

        // GUI Zonas
        gui_zonas_titulo = color(config.getString("gui.zonas.titulo", "&5✦ ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ"));
        gui_zona_mundo = color(config.getString("gui.zonas.mundo", "&7 › &eᴍᴜɴᴅᴏ • &f%s"));
        gui_zona_de = color(config.getString("gui.zonas.de", "&7 › &eᴅᴇ • &f%s"));
        gui_zona_ate = color(config.getString("gui.zonas.ate", "&7 › &eᴀᴛé • &f%s"));
        gui_zona_voz_ativada = color(config.getString("gui.zonas.voz_ativada", "&7 › &eᴠᴏᴢ • &a✔ ᴀᴛɪᴠᴀᴅᴀ"));
        gui_zona_voz_desativada = color(config.getString("gui.zonas.voz_desativada", "&7 › &eᴠᴏᴢ • &c✖ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ"));
        gui_zona_permitidos = color(config.getString("gui.zonas.permitidos", "&7 › &eᴘᴇʀᴍɪᴛɪᴅᴏs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_zona_mutados = color(config.getString("gui.zonas.mutados", "&7 › &eᴍᴜᴛᴀᴅᴏs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_zona_clique_gerenciar = color(config.getString("gui.zonas.clique_gerenciar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ"));
        gui_zona_recarregar = color(config.getString("gui.zonas.recarregar", "&6✦ &aʀᴇᴄᴀʀʀᴇɢᴀʀ ᴢᴏɴᴀs"));
        gui_zona_recarregar_desc = color(config.getString("gui.zonas.recarregar_desc", "&7 › &fʀᴇᴄᴀʀʀᴇɢᴀ ᴛᴏᴅᴀs ᴀs ᴢᴏɴᴀs"));
        gui_zona_recarregadas = color(config.getString("gui.zonas.recarregadas", "&a✔ ᴢᴏɴᴀs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴀs."));
        gui_zona_nao_encontrada = color(config.getString("gui.zonas.nao_encontrada", "&c✖ ᴢᴏɴᴀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴀ."));
        gui_zona_nao_existe_mais = color(config.getString("gui.zonas.nao_existe_mais", "&c✖ ᴀ ᴢᴏɴᴀ ɴãᴏ ᴇxɪsᴛᴇ ᴍᴀɪs."));
        gui_zonas_info_titulo = color(config.getString("gui.zonas.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ᴢᴏɴᴀs"));
        gui_zonas_info_total = color(config.getString("gui.zonas.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ᴢᴏɴᴀs"));
        gui_zonas_info_desc = color(config.getString("gui.zonas.info_desc", "&7 › &fɢᴇʀᴇɴᴄɪᴇ ᴀs ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ"));
        gui_zonas_vazio = color(config.getString("gui.zonas.vazio", "&7✦ ɴᴇɴʜᴜᴍᴀ ᴢᴏɴᴀ ᴄᴀᴅᴀsᴛʀᴀᴅᴀ"));

        // Menu Config Zona
        gui_zona_prefixo = color(config.getString("gui.zona_config.prefixo", "&5✦ ᴢᴏɴᴀ: "));
        gui_zona_voz_ativada_titulo = color(config.getString("gui.zona_config.voz_ativada_titulo", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ"));
        gui_zona_voz_ativada_desc = color(config.getString("gui.zona_config.voz_ativada_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ"));
        gui_zona_voz_ativada_clique = color(config.getString("gui.zona_config.voz_ativada_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ"));
        gui_zona_voz_desativada_titulo = color(config.getString("gui.zona_config.voz_desativada_titulo", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ"));
        gui_zona_voz_desativada_desc = color(config.getString("gui.zona_config.voz_desativada_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ɴãᴏ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ"));
        gui_zona_voz_desativada_clique = color(config.getString("gui.zona_config.voz_desativada_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ"));
        gui_zona_jogadores_permitidos = color(config.getString("gui.zona_config.jogadores_permitidos", "&6✦ &bᴊᴏɢᴀᴅᴏʀᴇs ᴘᴇʀᴍɪᴛɪᴅᴏs"));
        gui_zona_permitidos_desc1 = color(config.getString("gui.zona_config.permitidos_desc1", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ǫᴜᴇ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ"));
        gui_zona_permitidos_desc2 = color(config.getString("gui.zona_config.permitidos_desc2", "&7   &fᴍᴇsᴍᴏ ᴄᴏᴍ ᴀ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ"));
        gui_zona_atualmente = color(config.getString("gui.zona_config.atualmente", "&7 › &eᴀᴛᴜᴀʟᴍᴇɴᴛᴇ • &f%s ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_zona_jogadores_mutados = color(config.getString("gui.zona_config.jogadores_mutados", "&6✦ &cᴊᴏɢᴀᴅᴏʀᴇs ᴍᴜᴛᴀᴅᴏs"));
        gui_zona_mutados_desc1 = color(config.getString("gui.zona_config.mutados_desc1", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ǫᴜᴇ ᴇsᴛãᴏ ᴍᴜᴛᴀᴅᴏs"));
        gui_zona_mutados_desc2 = color(config.getString("gui.zona_config.mutados_desc2", "&7   &fɪɴᴅᴇᴘᴇɴᴅᴇɴᴛᴇ ᴅᴀs ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ"));
        gui_zona_deletar = color(config.getString("gui.zona_config.deletar", "&c✖ &4ᴅᴇʟᴇᴛᴀʀ ᴢᴏɴᴀ"));
        gui_zona_deletar_aviso = color(config.getString("gui.zona_config.deletar_aviso", "&c › &4ᴀᴠɪsᴏ: ᴀçãᴏ ɪʀʀᴇᴠᴇʀsíᴠᴇʟ."));
        gui_zona_deletar_clique = color(config.getString("gui.zona_config.deletar_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇʟᴇᴛᴀʀ"));
        gui_zona_voltar = color(config.getString("gui.zona_config.voltar", "&c« &fᴠᴏʟᴛᴀʀ"));
        gui_zona_voltar_lista = color(config.getString("gui.zona_config.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ᴢᴏɴᴀs"));
        gui_zona_voz_ativada_msg = color(config.getString("gui.zona_config.voz_ativada_msg", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ ɴᴀ ᴢᴏɴᴀ '%s'."));
        gui_zona_voz_desativada_msg = color(config.getString("gui.zona_config.voz_desativada_msg", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ ɴᴀ ᴢᴏɴᴀ '%s'."));
        gui_zona_deletada = color(config.getString("gui.zona_config.deletada", "&a✔ ᴢᴏɴᴀ '%s' ᴅᴇʟᴇᴛᴀᴅᴀ."));

        // Range Customizado da Zona
        gui_zona_range_titulo = color(config.getString("gui.zona_config.range_titulo", "&6✦ &fʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ"));
        gui_zona_range_atual = color(config.getString("gui.zona_config.range_atual", "&7 › &eʀᴀɴɢᴇ • &a%s"));
        gui_zona_range_padrao_valor = color(config.getString("gui.zona_config.range_padrao_valor", "ᴘᴀᴅʀãᴏ ᴅᴏ sᴇʀᴠɪᴅᴏʀ"));
        gui_zona_range_clique = color(config.getString("gui.zona_config.range_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀᴀʀ"));
        gui_zona_range_alterado = color(config.getString("gui.zona_config.range_alterado", "&a✔ ʀᴀɴɢᴇ ᴅᴀ ᴢᴏɴᴀ '%s' ᴀʟᴛᴇʀᴀᴅᴏ ᴘᴀʀᴀ %s."));

        // Multiplicador de Range
        gui_zona_multiplicador_titulo = color(config.getString("gui.zona_config.multiplicador_titulo", "&6✦ &eᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ ᴅᴇ ʀᴀɴɢᴇ"));
        gui_zona_multiplicador_atual = color(config.getString("gui.zona_config.multiplicador_atual", "&7 › &eᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ • &a%s"));
        gui_zona_multiplicador_clique = color(config.getString("gui.zona_config.multiplicador_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀᴀʀ"));
        gui_zona_multiplicador_alterado = color(config.getString("gui.zona_config.multiplicador_alterado", "&a✔ ᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ ᴅᴀ ᴢᴏɴᴀ '%s' ᴀʟᴛᴇʀᴀᴅᴏ ᴘᴀʀᴀ %s."));

        // Somente Escuta
        gui_zona_escuta_ativada = color(config.getString("gui.zona_config.escuta_ativada", "&b✔ sᴏᴍᴇɴᴛᴇ ᴇsᴄᴜᴛᴀ"));
        gui_zona_escuta_desc = color(config.getString("gui.zona_config.escuta_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴏᴜᴠᴇᴍ ᴍᴀs ɴãᴏ ꜰᴀʟᴀᴍ"));
        gui_zona_escuta_clique_desativar = color(config.getString("gui.zona_config.escuta_clique_desativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ"));
        gui_zona_escuta_desativada = color(config.getString("gui.zona_config.escuta_desativada", "&7✖ sᴏᴍᴇɴᴛᴇ ᴇsᴄᴜᴛᴀ"));
        gui_zona_escuta_desc_off = color(config.getString("gui.zona_config.escuta_desc_off", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ᴇ ᴏᴜᴠɪʀ"));
        gui_zona_escuta_clique_ativar = color(config.getString("gui.zona_config.escuta_clique_ativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ"));
        gui_zona_escuta_ativada_msg = color(config.getString("gui.zona_config.escuta_ativada_msg", "&b✔ sᴏᴍᴇɴᴛᴇ-ᴇsᴄᴜᴛᴀ ᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'."));
        gui_zona_escuta_desativada_msg = color(config.getString("gui.zona_config.escuta_desativada_msg", "&e✖ sᴏᴍᴇɴᴛᴇ-ᴇsᴄᴜᴛᴀ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'."));

        // Zona Temporaria
        gui_zona_tempo_restante = color(config.getString("gui.zona_config.tempo_restante", "&7 › &eᴛᴇᴍᴘᴏ ʀᴇsᴛᴀɴᴛᴇ • &f%s"));
        gui_zona_expirada = color(config.getString("gui.zona_config.expirada", "&c✖ ᴢᴏɴᴀ ᴇxᴘɪʀᴀᴅᴀ."));

        // Menu Jogadores Permitidos
        gui_permitidos_prefixo = color(config.getString("gui.permitidos.prefixo", "&5✦ ᴘᴇʀᴍɪᴛɪᴅᴏs: "));
        gui_permitidos_clique_remover = color(config.getString("gui.permitidos.clique_remover", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ"));
        gui_permitidos_adicionar = color(config.getString("gui.permitidos.adicionar", "&6✦ &aᴀᴅɪᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ"));
        gui_permitidos_adicionar_desc1 = color(config.getString("gui.permitidos.adicionar_desc1", "&7 › &fᴀᴅɪᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ"));
        gui_permitidos_adicionar_desc2 = color(config.getString("gui.permitidos.adicionar_desc2", "&7   &fà ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs"));
        gui_permitidos_voltar = color(config.getString("gui.permitidos.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ"));
        gui_permitidos_removido = color(config.getString("gui.permitidos.removido", "&a✔ &e%s &aʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs."));
        gui_permitidos_adicionado = color(config.getString("gui.permitidos.adicionado", "&a✔ &e%s &aᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs."));

        // Menu Jogadores Mutados
        gui_mutados_prefixo = color(config.getString("gui.mutados.prefixo", "&5✦ ᴍᴜᴛᴀᴅᴏs: "));
        gui_mutados_clique_desmutar = color(config.getString("gui.mutados.clique_desmutar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴍᴜᴛᴀʀ"));
        gui_mutados_mutar = color(config.getString("gui.mutados.mutar", "&6✦ &cᴍᴜᴛᴀʀ ᴊᴏɢᴀᴅᴏʀ"));
        gui_mutados_mutar_desc1 = color(config.getString("gui.mutados.mutar_desc1", "&7 › &fᴍᴜᴛᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ"));
        gui_mutados_mutar_desc2 = color(config.getString("gui.mutados.mutar_desc2", "&7   &fɴᴇsᴛᴀ ᴢᴏɴᴀ"));
        gui_mutados_voltar = color(config.getString("gui.mutados.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ"));
        gui_mutados_desmutado = color(config.getString("gui.mutados.desmutado", "&a✔ &e%s &aᴅᴇsᴍᴜᴛᴀᴅᴏ ɴᴇsᴛᴀ ᴢᴏɴᴀ."));
        gui_mutados_mutado = color(config.getString("gui.mutados.mutado", "&c✖ &e%s &cᴍᴜᴛᴀᴅᴏ ɴᴇsᴛᴀ ᴢᴏɴᴀ."));

        // Menu Adicionar Jogador
        gui_add_permitido_prefixo = color(config.getString("gui.adicionar.permitido_prefixo", "&5✦ ᴘᴇʀᴍɪᴛɪʀ: "));
        gui_add_mutado_prefixo = color(config.getString("gui.adicionar.mutado_prefixo", "&5✦ ᴍᴜᴛᴀʀ: "));
        gui_add_clique_permitir = color(config.getString("gui.adicionar.clique_permitir", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴘᴇʀᴍɪᴛɪʀ ꜰᴀʟᴀʀ"));
        gui_add_clique_mutar = color(config.getString("gui.adicionar.clique_mutar", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴍᴜᴛᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ"));
        gui_add_voltar = color(config.getString("gui.adicionar.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ᴊᴏɢᴀᴅᴏʀᴇs"));

        // Range de Voz - Comandos
        range_titulo = color(config.getString("range.titulo", "&7ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ:"));
        range_menu = color(config.getString("range.menu", "&e/mvoice range"));
        range_menu_desc = color(config.getString("range.menu_desc", "&7— ᴍᴇɴᴜ ᴅᴇ ɢᴇʀᴇɴᴄɪᴀᴍᴇɴᴛᴏ ᴅᴇ ʀᴀɴɢᴇs"));
        range_set = color(config.getString("range.set", "&e/mvoice range set <jogador> <distancia>"));
        range_set_desc = color(config.getString("range.set_desc", "&7— ᴅᴇꜰɪɴɪʀ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ"));
        range_remove = color(config.getString("range.remove", "&e/mvoice range remove <jogador>"));
        range_remove_desc = color(config.getString("range.remove_desc", "&7— ʀᴇᴍᴏᴠᴇʀ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ"));
        range_info = color(config.getString("range.info", "&e/mvoice range info [jogador]"));
        range_info_desc = color(config.getString("range.info_desc", "&7— ᴠᴇʀ ʀᴀɴɢᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ"));
        range_list = color(config.getString("range.list", "&e/mvoice range list"));
        range_list_desc = color(config.getString("range.list_desc", "&7— ʟɪsᴛᴀʀ ᴛᴏᴅᴏs ᴏs ʀᴀɴɢᴇs ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏs"));
        range_reload = color(config.getString("range.reload", "&e/mvoice range reload"));
        range_reload_desc = color(config.getString("range.reload_desc", "&7— ʀᴇᴄᴀʀʀᴇɢᴀʀ ʀᴀɴɢᴇs"));
        range_sem_permissao = color(config.getString("range.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ʀᴀɴɢᴇs ᴅᴇ ᴠᴏᴢ."));
        range_uso_set = color(config.getString("range.uso_set", "&cᴜsᴏ: &e/mvoice range set <jogador> <distancia>"));
        range_uso_remove = color(config.getString("range.uso_remove", "&cᴜsᴏ: &e/mvoice range remove <jogador>"));
        range_valor_invalido = color(config.getString("range.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 1 ᴇ 1000."));
        range_definido = color(config.getString("range.definido", "&aʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %s ʙʟᴏᴄᴏs."));
        range_removido = color(config.getString("range.removido", "&aʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ. ᴜsᴀɴᴅᴏ ʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ."));
        range_sem_custom = color(config.getString("range.sem_custom", "&e%s ɴãᴏ ᴘᴏssᴜɪ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ."));
        range_info_jogador = color(config.getString("range.info_jogador", "&e%s ᴘᴏssᴜɪ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ: &a%s ʙʟᴏᴄᴏs"));
        range_info_padrao = color(config.getString("range.info_padrao", "&e%s ᴜsᴀ ᴏ ʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ: &a%s ʙʟᴏᴄᴏs"));
        range_lista_vazia = color(config.getString("range.lista_vazia", "&eɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ."));
        range_lista_titulo = color(config.getString("range.lista_titulo", "&7ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ:"));
        range_lista_item = color(config.getString("range.lista_item", "&e- %s: &a%s ʙʟᴏᴄᴏs"));
        range_recarregado = color(config.getString("range.recarregado", "&aʀᴀɴɢᴇs ᴅᴇ ᴠᴏᴢ ʀᴇᴄᴀʀʀᴇɢᴀᴅᴏs."));

        // Range de Voz - GUI
        gui_range_titulo = color(config.getString("gui.range.titulo", "&5✦ ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ"));
        gui_range_info_titulo = color(config.getString("gui.range.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ʀᴀɴɢᴇs"));
        gui_range_info_total = color(config.getString("gui.range.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ʀᴀɴɢᴇs"));
        gui_range_info_desc = color(config.getString("gui.range.info_desc", "&7 › &fɢᴇʀᴇɴᴄɪᴇ ᴏ ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ ᴅᴏs ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_range_range_padrao = color(config.getString("gui.range.range_padrao", "&7 › &eʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ • &a%s ʙʟᴏᴄᴏs"));
        gui_range_vazio = color(config.getString("gui.range.vazio", "&7✦ ɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ"));
        gui_range_jogador_range = color(config.getString("gui.range.jogador_range", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs"));
        gui_range_clique_gerenciar = color(config.getString("gui.range.clique_gerenciar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ"));
        gui_range_definir = color(config.getString("gui.range.definir", "&6✦ &aᴅᴇꜰɪɴɪʀ ʀᴀɴɢᴇ"));
        gui_range_definir_desc = color(config.getString("gui.range.definir_desc", "&7 › &fᴅᴇꜰɪɴᴀ ᴏ ʀᴀɴɢᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ"));
        gui_range_recarregar = color(config.getString("gui.range.recarregar", "&6✦ &aʀᴇᴄᴀʀʀᴇɢᴀʀ ʀᴀɴɢᴇs"));
        gui_range_recarregar_desc = color(config.getString("gui.range.recarregar_desc", "&7 › &fʀᴇᴄᴀʀʀᴇɢᴀ ᴛᴏᴅᴏs ᴏs ʀᴀɴɢᴇs"));
        gui_range_recarregadas = color(config.getString("gui.range.recarregadas", "&a✔ ʀᴀɴɢᴇs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴏs."));
        gui_range_removido_gui = color(config.getString("gui.range.removido_gui", "&a✔ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ."));
        gui_range_selecionar_prefixo = color(config.getString("gui.range.selecionar_prefixo", "&5✦ sᴇʟᴇᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ"));
        gui_range_selecionar_titulo = color(config.getString("gui.range.selecionar_titulo", "&6✦ &bsᴇʟᴇᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ"));
        gui_range_selecionar_desc = color(config.getString("gui.range.selecionar_desc", "&7 › &fsᴇʟᴇᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴘᴀʀᴀ ᴅᴇꜰɪɴɪʀ ᴏ ʀᴀɴɢᴇ"));
        gui_range_sem_custom_gui = color(config.getString("gui.range.sem_custom_gui", "&7 › &fsᴇᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ"));
        gui_range_clique_selecionar = color(config.getString("gui.range.clique_selecionar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ sᴇʟᴇᴄɪᴏɴᴀʀ"));
        gui_range_voltar = color(config.getString("gui.range.voltar", "&c« &fᴠᴏʟᴛᴀʀ"));
        gui_range_voltar_lista = color(config.getString("gui.range.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ʀᴀɴɢᴇs"));
        gui_range_distancia_prefixo = color(config.getString("gui.range.distancia_prefixo", "&5✦ ʀᴀɴɢᴇ: "));
        gui_range_distancia_atual = color(config.getString("gui.range.distancia_atual", "&7 › &eʀᴀɴɢᴇ ᴀᴛᴜᴀʟ • &a%s ʙʟᴏᴄᴏs"));
        gui_range_distancia_blocos = color(config.getString("gui.range.distancia_blocos", "&a%s ʙʟᴏᴄᴏs"));
        gui_range_distancia_clique = color(config.getString("gui.range.distancia_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇꜰɪɴɪʀ"));
        gui_range_distancia_selecionado = color(config.getString("gui.range.distancia_selecionado", "&a✔ sᴇʟᴇᴄɪᴏɴᴀᴅᴏ."));
        gui_range_distancia_definido = color(config.getString("gui.range.distancia_definido", "&a✔ ʀᴀɴɢᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %s ʙʟᴏᴄᴏs."));
        gui_range_remover = color(config.getString("gui.range.remover", "&c✖ &4ʀᴇᴍᴏᴠᴇʀ ʀᴀɴɢᴇ"));
        gui_range_remover_desc = color(config.getString("gui.range.remover_desc", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ range customizado"));

        // Global Voice - GUI
        gui_global_titulo = color(config.getString("gui.global.titulo", "&5✦ ᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        gui_global_info_titulo = color(config.getString("gui.global.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        gui_global_info_total = color(config.getString("gui.global.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_global_info_desc = color(config.getString("gui.global.info_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴏᴜᴠɪᴅᴏs ᴘᴏʀ ᴛᴏᴅᴏs ɴᴏ ᴍᴜɴᴅᴏ"));
        gui_global_vazio = color(config.getString("gui.global.vazio", "&7✦ ɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        gui_global_jogador_desc = color(config.getString("gui.global.jogador_desc", "&7 › &aᴛᴏᴅᴏs ᴏᴜᴠᴇᴍ ᴇsᴛᴇ ᴊᴏɢᴀᴅᴏʀ"));
        gui_global_clique_remover = color(config.getString("gui.global.clique_remover", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ"));
        gui_global_adicionar = color(config.getString("gui.global.adicionar", "&6✦ &aᴀᴅɪᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ"));
        gui_global_adicionar_desc = color(config.getString("gui.global.adicionar_desc", "&7 › &fᴀᴅɪᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        gui_global_clique_adicionar = color(config.getString("gui.global.clique_adicionar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴛᴏʀɴᴀʀ ɢʟᴏʙᴀʟ"));
        gui_global_voltar_lista = color(config.getString("gui.global.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ɢʟᴏʙᴀɪs"));
        gui_global_selecionar_titulo = color(config.getString("gui.global.selecionar_titulo", "&5✦ ᴀᴅɪᴄɪᴏɴᴀʀ ɢʟᴏʙᴀʟ"));
        gui_global_botao = color(config.getString("gui.global.botao", "&6✦ &dᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        gui_global_botao_desc = color(config.getString("gui.global.botao_desc", "&7 › &eɢʟᴏʙᴀɪs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs"));
        gui_global_removido = color(config.getString("gui.global.removido", "&c✖ &e%s &cʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        gui_global_adicionado = color(config.getString("gui.global.adicionado", "&a✔ &e%s &aᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ᴠᴏᴢ ɢʟᴏʙᴀʟ."));

        // Global Voice - Comandos
        range_global = color(config.getString("range.global", "&e/mvoice global [add|remove|list]"));
        range_global_desc = color(config.getString("range.global_desc", "&7— ɢᴇʀᴇɴᴄɪᴀʀ ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ"));
        range_global_uso_add = color(config.getString("range.global_uso_add", "&cᴜsᴏ: &e/mvoice global add <jogador>"));
        range_global_uso_remove = color(config.getString("range.global_uso_remove", "&cᴜsᴏ: &e/mvoice global remove <jogador>"));
        range_global_adicionado = color(config.getString("range.global_adicionado", "&a%s ᴀɢᴏʀᴀ ᴛᴇᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        range_global_ja_global = color(config.getString("range.global_ja_global", "&e%s ᴊá ᴘᴏssᴜɪ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        range_global_removido = color(config.getString("range.global_removido", "&a%s ɴãᴏ ᴛᴇᴍ ᴍᴀɪs ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        range_global_nao_global = color(config.getString("range.global_nao_global", "&e%s ɴãᴏ ᴘᴏssᴜɪ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        range_global_lista_vazia = color(config.getString("range.global_lista_vazia", "&eɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        range_global_lista_titulo = color(config.getString("range.global_lista_titulo", "&7ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ:"));
        range_global_lista_item = color(config.getString("range.global_lista_item", "&e- %s"));

        // Global Voice - Permissao e Limite
        global_sem_permissao = color(config.getString("global.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ᴠᴏᴢ ɢʟᴏʙᴀʟ."));
        global_limite_atingido = color(config.getString("global.limite_atingido", "&cʟɪᴍɪᴛᴇ ᴅᴇ ᴊᴏɢᴀᴅᴏʀᴇs ɢʟᴏʙᴀɪs ᴀᴛɪɴɢɪᴅᴏ. (ᴍᴀx: %s)"));

        // Status
        status_titulo = color(config.getString("status.titulo", "&7sᴛᴀᴛᴜs ᴅᴇ &e%s&7:"));
        status_range = color(config.getString("status.range", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs &7(padrao)"));
        status_range_custom = color(config.getString("status.range_custom", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs &e(customizado)"));
        status_global_sim = color(config.getString("status.global_sim", "&7 › &eɢʟᴏʙᴀʟ • &a✔ sɪᴍ"));
        status_global_nao = color(config.getString("status.global_nao", "&7 › &eɢʟᴏʙᴀʟ • &c✖ ɴãᴏ"));
        status_global_total = color(config.getString("status.global_total", "&7 › &eɢʟᴏʙᴀɪs • &f%s/%s"));
        status_ilimitado = color(config.getString("status.ɪʟɪᴍɪᴛᴀᴅᴏ", "ɪʟɪᴍɪᴛᴀᴅᴏ"));
        status_zona_nenhuma = color(config.getString("status.zona_nenhuma", "&7 › &eᴢᴏɴᴀ • &fɴᴇɴʜᴜᴍᴀ"));
        status_zona_em = color(config.getString("status.zona_em", "&7 › &eᴢᴏɴᴀ • &f%s &7(%s)"));
        status_zona_voz_ativada = color(config.getString("status.zona_voz_ativada", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ"));
        status_zona_voz_desativada = color(config.getString("status.zona_voz_desativada", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ"));

        // Notificacao de Zona
        zona_entrou_notificacao = color(config.getString("zona.entrou_notificacao", "&5\u2588 &dVoce entrou na zona &f%s &7(%s)"));
        zona_saiu_notificacao = color(config.getString("zona.saiu_notificacao", "&5\u2588 &dVoce saiu da zona &f%s"));
        zona_notificacao_voz_ativada = color(config.getString("zona.notificacao_voz_ativada", "&aᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ"));
        zona_notificacao_voz_desativada = color(config.getString("zona.notificacao_voz_desativada", "&cᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ"));

        // Volume por Jogador
        range_volume = color(config.getString("range.volume", "&e/mvoice volume <jogador> <valor|reset>"));
        range_volume_desc = color(config.getString("range.volume_desc", "&7— ᴀᴊᴜsᴛᴀʀ ᴠᴏʟᴜᴍᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ"));
        volume_uso = color(config.getString("volume.uso", "&cᴜsᴏ: &e/mvoice volume <jogador> <0.1-5.0|reset>"));
        volume_valor_invalido = color(config.getString("volume.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 0.1 ᴇ 5.0"));
        volume_definido = color(config.getString("volume.definido", "&aᴠᴏʟᴜᴍᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %sx"));
        volume_removido = color(config.getString("volume.removido", "&aᴠᴏʟᴜᴍᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ."));
        volume_sem_custom = color(config.getString("volume.sem_custom", "&e%s ɴãᴏ ᴘᴏssᴜɪ ᴠᴏʟᴜᴍᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ."));

        // Prioridade de Audio
        range_priority = color(config.getString("range.priority", "&e/mvoice priority <jogador> <0-100>"));
        range_priority_desc = color(config.getString("range.priority_desc", "&7— ᴅᴇꜰɪɴɪʀ ᴘʀɪᴏʀɪᴅᴀᴅᴇ ᴅᴇ áᴜᴅɪᴏ"));
        priority_uso = color(config.getString("priority.uso", "&cᴜsᴏ: &e/mvoice priority <jogador> <0-100>"));
        priority_valor_invalido = color(config.getString("priority.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 0 ᴇ 100."));
        priority_definido = color(config.getString("priority.definido", "&aᴘʀɪᴏʀɪᴅᴀᴅᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴀ ᴘᴀʀᴀ %s."));

        // Cooldown de Voz
        range_cooldown = color(config.getString("range.cooldown", "&e/mvoice cooldown [set|off|info]"));
        range_cooldown_desc = color(config.getString("range.cooldown_desc", "&7— ɢᴇʀᴇɴᴄɪᴀʀ ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ"));
        cooldown_uso = color(config.getString("cooldown.uso", "&cᴜsᴏ: &e/mvoice cooldown [set|off|info]"));
        cooldown_uso_set = color(config.getString("cooldown.uso_set", "&cᴜsᴏ: &e/mvoice cooldown set <tempo_fala_seg> <cooldown_seg>"));
        cooldown_valor_invalido = color(config.getString("cooldown.valor_invalido", "&cᴠᴀʟᴏʀᴇs ɪɴᴠáʟɪᴅᴏs. ᴜsᴇ ɴúᴍᴇʀᴏs ᴘᴏsɪᴛɪᴠᴏs."));
        cooldown_definido = color(config.getString("cooldown.definido", "&aᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇꜰɪɴɪᴅᴏ: ꜰᴀʟᴀ=%ss, ᴇsᴘᴇʀᴀ=%ss"));
        cooldown_desativado = color(config.getString("cooldown.desativado", "&aᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ."));
        cooldown_desativado_info = color(config.getString("cooldown.desativado_info", "&7ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ ᴇsᴛá ᴅᴇsᴀᴛɪᴠᴀᴅᴏ."));
        cooldown_info = color(config.getString("cooldown.info", "&7ᴄᴏᴏʟᴅᴏᴡɴ: ꜰᴀʟᴀ=%ss, ᴇsᴘᴇʀᴀ=%ss"));
        cooldown_ativo = color(config.getString("cooldown.ativo", "&cᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ: ᴀɢᴜᴀʀᴅᴇ %ss"));
        cooldown_iniciado = color(config.getString("cooldown.iniciado", "&cᴛᴇᴍᴘᴏ ᴅᴇ ꜰᴀʟᴀ ᴇsɢᴏᴛᴀᴅᴏ. ᴀɢᴜᴀʀᴅᴇ ᴏ ᴄᴏᴏʟᴅᴏᴡɴ."));

        // Stage Mode
        gui_zona_stage_ativado = color(config.getString("gui.zona_config.stage_ativado", "&d✔ sᴛᴀɢᴇ ᴍᴏᴅᴇ"));
        gui_zona_stage_desc = color(config.getString("gui.zona_config.stage_desc", "&7 › &fᴀᴘᴇɴᴀs sᴘᴇᴀᴋᴇʀs ᴅᴇsɪɢɴᴀᴅᴏs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ"));
        gui_zona_stage_speakers = color(config.getString("gui.zona_config.stage_speakers", "&7 › &esᴘᴇᴀᴋᴇʀs • &f%s"));
        gui_zona_stage_clique_desativar = color(config.getString("gui.zona_config.stage_clique_desativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ"));
        gui_zona_stage_desativado = color(config.getString("gui.zona_config.stage_desativado", "&7✖ sᴛᴀɢᴇ ᴍᴏᴅᴇ"));
        gui_zona_stage_desc_off = color(config.getString("gui.zona_config.stage_desc_off", "&7 › &fᴛᴏᴅᴏs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴏʀᴍᴀʟᴍᴇɴᴛᴇ"));
        gui_zona_stage_clique_ativar = color(config.getString("gui.zona_config.stage_clique_ativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ"));
        gui_zona_stage_ativado_msg = color(config.getString("gui.zona_config.stage_ativado_msg", "&d✔ sᴛᴀɢᴇ ᴍᴏᴅᴇ ATIVADO na zona '%s'"));
        gui_zona_stage_desativado_msg = color(config.getString("gui.zona_config.stage_desativado_msg", "&e✖ sᴛᴀɢᴇ ᴍᴏᴅᴇ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'."));

        // Zone Cooldown
        gui_zona_cooldown_titulo = color(config.getString("gui.zona_config.cooldown_titulo", "&6✦ ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴀ ᴢᴏɴᴀ"));
        gui_zona_cooldown_desc = color(config.getString("gui.zona_config.cooldown_desc", "&7 › &fʟɪᴍɪᴛᴀ ᴏ ᴛᴇᴍᴘᴏ ᴅᴇ ꜰᴀʟᴀ ɴᴇsᴛᴀ ᴢᴏɴᴀ"));
        gui_zona_cooldown_fala = color(config.getString("gui.zona_config.cooldown_fala", "&7 › &eᴛᴇᴍᴘᴏ ᴅᴇ ꜰᴀʟᴀ • &f%s"));
        gui_zona_cooldown_espera = color(config.getString("gui.zona_config.cooldown_espera", "&7 › &eᴄᴏᴏʟᴅᴏᴡɴ • &f%s"));
        gui_zona_cooldown_clique = color(config.getString("gui.zona_config.cooldown_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀᴀʀ"));
        gui_zona_cooldown_desativado = color(config.getString("gui.zona_config.cooldown_desativado", "&7ᴅᴇsᴀᴛɪᴠᴀᴅᴏ"));
        gui_zona_cooldown_alterado = color(config.getString("gui.zona_config.cooldown_alterado", "&a✔ ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴀ ᴢᴏɴᴀ '%s' ᴀʟᴛᴇʀᴀᴅᴏ: ꜰᴀʟᴀ=%ss, ᴇsᴘᴇʀᴀ=%ss"));
        zona_cooldown_ativo = color(config.getString("zona.cooldown_ativo", "&c[%s] ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ: ᴀɢᴜᴀʀᴅᴇ %ss"));
        zona_cooldown_iniciado = color(config.getString("zona.cooldown_iniciado", "&c[%s] ᴛᴇᴍᴘᴏ ᴅᴇ ꜰᴀʟᴀ ᴇsɢᴏᴛᴀᴅᴏ. ᴀɢᴜᴀʀᴅᴇ ᴏ ᴄᴏᴏʟᴅᴏᴡɴ."));

        // Indicador Global
        global_indicador_falando = color(config.getString("global.indicador_falando", "ᴇsᴛá ꜰᴀʟᴀɴᴅᴏ (ᴠᴏᴢ ɢʟᴏʙᴀʟ)"));

        // Gravacao de Audio
        rec_titulo = color(config.getString("rec.titulo", "&7ɢʀᴀᴠᴀçãᴏ ᴅᴇ áᴜᴅɪᴏ:"));
        rec_start = color(config.getString("rec.start", "&e/mvoice record start <jogador>"));
        rec_start_desc = color(config.getString("rec.start_desc", "&7— ɪɴɪᴄɪᴀʀ ɢʀᴀᴠᴀçãᴏ"));
        rec_stop = color(config.getString("rec.stop", "&e/mvoice record stop <jogador>"));
        rec_stop_desc = color(config.getString("rec.stop_desc", "&7— ᴘᴀʀᴀʀ ɢʀᴀᴠᴀçãᴏ"));
        rec_active = color(config.getString("rec.active", "&e/mvoice record active"));
        rec_active_desc = color(config.getString("rec.active_desc", "&7— ᴠᴇʀ ɢʀᴀᴠᴀçõᴇs ᴀᴛɪᴠᴀs"));
        rec_list = color(config.getString("rec.list", "&e/mvoice record list [pagina]"));
        rec_list_desc = color(config.getString("rec.list_desc", "&7— ʟɪsᴛᴀʀ ɢʀᴀᴠᴀçõᴇs sᴀʟᴠᴀs"));
        rec_info = color(config.getString("rec.info", "&e/mvoice record info <id>"));
        rec_info_desc = color(config.getString("rec.info_desc", "&7— ᴠᴇʀ ᴅᴇᴛᴀʟʜᴇs ᴅᴇ ᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ"));
        rec_delete = color(config.getString("rec.delete", "&e/mvoice record delete <id>"));
        rec_delete_desc = color(config.getString("rec.delete_desc", "&7— ᴅᴇʟᴇᴛᴀʀ ᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ"));
        rec_uso_start = color(config.getString("rec.uso_start", "&cᴜsᴏ: &e/mvoice record start <jogador>"));
        rec_uso_stop = color(config.getString("rec.uso_stop", "&cᴜsᴏ: &e/mvoice record stop <jogador>"));
        rec_uso_info = color(config.getString("rec.uso_info", "&cᴜsᴏ: &e/mvoice record info <id>"));
        rec_uso_delete = color(config.getString("rec.uso_delete", "&cᴜsᴏ: &e/mvoice record delete <id>"));
        rec_ja_gravando = color(config.getString("rec.ja_gravando", "&c%s ᴊá ᴇsᴛá sᴇɴᴅᴏ ɢʀᴀᴠᴀᴅᴏ."));
        rec_iniciada = color(config.getString("rec.iniciada", "&aɢʀᴀᴠᴀçãᴏ ᴅᴇ %s ɪɴɪᴄɪᴀᴅᴀ."));
        rec_id = color(config.getString("rec.id", "&7ɪᴅ: &f%s"));
        rec_nao_gravando = color(config.getString("rec.nao_gravando", "&c%s ɴãᴏ ᴇsᴛá sᴇɴᴅᴏ ɢʀᴀᴠᴀᴅᴏ."));
        rec_parada = color(config.getString("rec.parada", "&aɢʀᴀᴠᴀçãᴏ ᴅᴇ %s ꜰɪɴᴀʟɪᴢᴀᴅᴀ."));
        rec_salva = color(config.getString("rec.salva", "&7sᴀʟᴠᴀ ᴄᴏᴍᴏ: &f%s &7(ᴅᴜʀᴀçãᴏ: %s, ꜰʀᴀᴍᴇs: %s)"));
        rec_nenhuma_ativa = color(config.getString("rec.nenhuma_ativa", "&eɴᴇɴʜᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ ᴀᴛɪᴠᴀ ɴᴏ ᴍᴏᴍᴇɴᴛᴏ."));
        rec_ativas_titulo = color(config.getString("rec.ativas_titulo", "&7ɢʀᴀᴠᴀçõᴇs ᴀᴛɪᴠᴀs:"));
        rec_ativa_item = color(config.getString("rec.ativa_item", "&e- %s &7(ᴅᴜʀᴀçãᴏ: %s, ꜰʀᴀᴍᴇs: %s)"));
        rec_nenhuma_salva = color(config.getString("rec.nenhuma_salva", "&eɴᴇɴʜᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ sᴀʟᴠᴀ."));
        rec_lista_titulo = color(config.getString("rec.lista_titulo", "&7ɢʀᴀᴠᴀçõᴇs sᴀʟᴠᴀs (%s/%s):"));
        rec_lista_item = color(config.getString("rec.lista_item", "&e- %s"));
        rec_info_titulo = color(config.getString("rec.info_titulo", "&7ᴅᴇᴛᴀʟʜᴇs ᴅᴀ ɢʀᴀᴠᴀçãᴏ:"));
        rec_nao_encontrada = color(config.getString("rec.nao_encontrada", "&cɢʀᴀᴠᴀçãᴏ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴀ."));
        rec_deletada = color(config.getString("rec.deletada", "&aɢʀᴀᴠᴀçãᴏ '%s' ᴅᴇʟᴇᴛᴀᴅᴀ."));
    }

    private void createDefaults() {
        config = new YamlConfiguration();

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("config_version", CONFIG_VERSION);
        defaults.put("prefixo", "");

        // Geral
        defaults.put("geral.somente_jogadores", "&cᴀᴘᴇɴᴀs ᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ.");
        defaults.put("geral.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
        defaults.put("geral.jogador_nao_encontrado", "&cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.");

        // VoiceChat
        defaults.put("voicechat.necessario", "&cᴠᴏᴄê ᴘʀᴇᴄɪsᴀ ᴛᴇʀ ᴏ %s ɪɴsᴛᴀʟᴀᴅᴏ ᴘᴀʀᴀ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ.");
        defaults.put("voicechat.jogador_sem_voicechat", "&c%s ɴãᴏ ᴛᴇᴍ ᴏ %s ɪɴsᴛᴀʟᴀᴅᴏ.");
        defaults.put("voicechat.cliente_nao_conectado", "&cᴄʟɪᴇɴᴛᴇ ɴãᴏ ᴄᴏɴᴇᴄᴛᴀᴅᴏ.");
        defaults.put("voicechat.enviando_ping", "&eᴇɴᴠɪᴀɴᴅᴏ ᴘɪɴɢ...");
        defaults.put("voicechat.ping_enviado_aguardando", "&eᴘɪɴɢ ᴇɴᴠɪᴀᴅᴏ. ᴀɢᴜᴀʀᴅᴀɴᴅᴏ ʀᴇsᴘᴏsᴛᴀ...");
        defaults.put("voicechat.ping_recebido", "&aʀᴇsᴘᴏsᴛᴀ ʀᴇᴄᴇʙɪᴅᴀ ᴇᴍ %s ᴍs");
        defaults.put("voicechat.ping_recebido_tentativas", "&aʀᴇsᴘᴏsᴛᴀ ʀᴇᴄᴇʙɪᴅᴀ ᴇᴍ %s ᴍs apos %s tentativas");
        defaults.put("voicechat.ping_sem_resposta", "&esᴇᴍ ʀᴇsᴘᴏsᴛᴀ. ᴛᴇɴᴛᴀɴᴅᴏ ɴᴏᴠᴀᴍᴇɴᴛᴇ...");
        defaults.put("voicechat.ping_tempo_esgotado", "&cᴛᴇᴍᴘᴏ ᴇsɢᴏᴛᴀᴅᴏ ᴀᴘós %s ᴛᴇɴᴛᴀᴛɪᴠᴀs");
        defaults.put("voicechat.falha_enviar_ping", "&cꜰᴀʟʜᴀ ᴀᴏ ᴇɴᴠɪᴀʀ ᴘɪɴɢ: %s");
        defaults.put("voicechat.nao_esta_no_grupo", "&cᴠᴏᴄê ɴãᴏ ᴇsᴛá ᴇᴍ ᴜᴍ ɢʀᴜᴘᴏ.");
        defaults.put("voicechat.convite_enviado", "&a%s ꜰᴏɪ ᴄᴏɴᴠɪᴅᴀᴅᴏ.");
        defaults.put("voicechat.grupo_nao_existe", "&cᴇsᴛᴇ ɢʀᴜᴘᴏ ɴãᴏ ᴇxɪsᴛᴇ.");
        defaults.put("voicechat.grupo_nome_ambiguo", "&cᴏ ɴᴏᴍᴇ ᴅᴏ ɢʀᴜᴘᴏ é ᴀᴍʙíɢᴜᴏ.");
        defaults.put("voicechat.grupos_desativados", "&cᴏs ɢʀᴜᴘᴏs ᴇsᴛãᴏ ᴅᴇsᴀᴛɪᴠᴀᴅᴏs ɴᴇsᴛᴇ sᴇʀᴠɪᴅᴏʀ.");
        defaults.put("voicechat.sem_permissao_grupo", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ᴇɴᴛʀᴀʀ ᴇᴍ ɢʀᴜᴘᴏs.");
        defaults.put("voicechat.grupo_entrou", "&aᴇɴᴛʀᴏᴜ ɴᴏ ɢʀᴜᴘᴏ %s.");
        defaults.put("voicechat.grupo_saiu", "&asᴀɪᴜ ᴅᴏ ɢʀᴜᴘᴏ.");

        // Zonas
        defaults.put("zona.titulo", "&7ᴢᴏɴᴀs ʀᴇsᴛʀɪᴛᴀs:");
        defaults.put("zona.menu", "&e/mvoice zone");
        defaults.put("zona.menu_desc", "&7— ᴍᴇɴᴜ ᴅᴇ ɢᴇʀᴇɴᴄɪᴀᴍᴇɴᴛᴏ ᴅᴇ ᴢᴏɴᴀs");
        defaults.put("zona.pos1", "&e/mvoice zone pos1");
        defaults.put("zona.pos1_desc", "&7— ᴅᴇꜰɪɴɪʀ ᴘʀɪᴍᴇɪʀᴀ ᴘᴏsɪçãᴏ");
        defaults.put("zona.pos2", "&e/mvoice zone pos2");
        defaults.put("zona.pos2_desc", "&7— ᴅᴇꜰɪɴɪʀ sᴇɢᴜɴᴅᴀ ᴘᴏsɪçãᴏ");
        defaults.put("zona.create", "&e/mvoice zone create <nome>");
        defaults.put("zona.create_desc", "&7— ᴄʀɪᴀʀ ᴢᴏɴᴀ ᴀ ᴘᴀʀᴛɪʀ ᴅᴀ sᴇʟᴇçãᴏ");
        defaults.put("zona.reload", "&e/mvoice zone reload");
        defaults.put("zona.reload_desc", "&7— ʀᴇᴄᴀʀʀᴇɢᴀʀ ᴢᴏɴᴀs");
        defaults.put("zona.bypass_permissao", "&7ᴘᴇʀᴍɪssãᴏ ᴅᴇ ʙʏᴘᴀss: &fvoicechat.zone.bypass");
        defaults.put("zona.pos1_definida", "&aᴘᴏsɪçãᴏ 1 ᴅᴇꜰɪɴɪᴅᴀ ᴇᴍ: %s");
        defaults.put("zona.pos2_definida", "&aᴘᴏsɪçãᴏ 2 ᴅᴇꜰɪɴɪᴅᴀ ᴇᴍ: %s");
        defaults.put("zona.uso_create", "&cᴜsᴏ: &e/mvoice zone create <nome>");
        defaults.put("zona.nome_invalido", "&cᴏ ɴᴏᴍᴇ ᴅᴀ ᴢᴏɴᴀ só ᴘᴏᴅᴇ ᴄᴏɴᴛᴇʀ ʟᴇᴛʀᴀs, ɴúᴍᴇʀᴏs, ʜíꜰᴇɴs ᴇ sᴜʙʟɪɴʜᴀᴅᴏs.");
        defaults.put("zona.definir_posicoes", "&cᴠᴏᴄê ᴘʀᴇᴄɪsᴀ ᴅᴇꜰɪɴɪʀ ᴀᴍʙᴀs ᴀs ᴘᴏsɪçõᴇs. ᴜsᴇ /mvoice zone pos1 ᴇ /mvoice zone pos2");
        defaults.put("zona.mesmo_mundo", "&cᴀᴍʙᴀs ᴀs ᴘᴏsɪçõᴇs ᴅᴇᴠᴇᴍ ᴇsᴛᴀʀ ɴᴏ ᴍᴇsᴍᴏ ᴍᴜɴᴅᴏ.");
        defaults.put("zona.criada", "&aᴢᴏɴᴀ ᴅᴇ ᴠᴏᴢ ʀᴇsᴛʀɪᴛᴀ '%s' ᴄʀɪᴀᴅᴀ.");
        defaults.put("zona.sem_permissao_bypass", "&7ᴊᴏɢᴀᴅᴏʀᴇs sᴇᴍ ᴘᴇʀᴍɪssãᴏ 'voicechat.zone.bypass' ɴãᴏ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ áʀᴇᴀ.");
        defaults.put("zona.ja_existe", "&cᴊá ᴇxɪsᴛᴇ ᴜᴍᴀ ᴢᴏɴᴀ ᴄᴏᴍ ᴇssᴇ ɴᴏᴍᴇ.");
        defaults.put("zona.recarregada", "&aᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ ʀᴇsᴛʀɪᴛᴀs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴀs.");
        defaults.put("zona.sem_permissao_gerenciar", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ.");

        // GUI Zonas
        defaults.put("gui.zonas.titulo", "&5✦ ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ");
        defaults.put("gui.zonas.mundo", "&7 › &eᴍᴜɴᴅᴏ • &f%s");
        defaults.put("gui.zonas.de", "&7 › &eᴅᴇ • &f%s");
        defaults.put("gui.zonas.ate", "&7 › &eᴀᴛé • &f%s");
        defaults.put("gui.zonas.voz_ativada", "&7 › &eᴠᴏᴢ • &a✔ ᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("gui.zonas.voz_desativada", "&7 › &eᴠᴏᴢ • &c✖ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("gui.zonas.permitidos", "&7 › &eᴘᴇʀᴍɪᴛɪᴅᴏs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.zonas.mutados", "&7 › &eᴍᴜᴛᴀᴅᴏs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.zonas.clique_gerenciar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ");
        defaults.put("gui.zonas.recarregar", "&6✦ &aʀᴇᴄᴀʀʀᴇɢᴀʀ ᴢᴏɴᴀs");
        defaults.put("gui.zonas.recarregar_desc", "&7 › &fʀᴇᴄᴀʀʀᴇɢᴀ ᴛᴏᴅᴀs ᴀs ᴢᴏɴᴀs");
        defaults.put("gui.zonas.recarregadas", "&a✔ ᴢᴏɴᴀs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴀs.");
        defaults.put("gui.zonas.nao_encontrada", "&c✖ ᴢᴏɴᴀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴀ.");
        defaults.put("gui.zonas.nao_existe_mais", "&c✖ ᴀ ᴢᴏɴᴀ ɴãᴏ ᴇxɪsᴛᴇ ᴍᴀɪs.");
        defaults.put("gui.zonas.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ᴢᴏɴᴀs");
        defaults.put("gui.zonas.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ᴢᴏɴᴀs");
        defaults.put("gui.zonas.info_desc", "&7 › &fɢᴇʀᴇɴᴄɪᴇ ᴀs ᴢᴏɴᴀs ᴅᴇ ᴠᴏᴢ");
        defaults.put("gui.zonas.vazio", "&7✦ ɴᴇɴʜᴜᴍᴀ ᴢᴏɴᴀ ᴄᴀᴅᴀsᴛʀᴀᴅᴀ");

        // Menu Config Zona
        defaults.put("gui.zona_config.prefixo", "&5✦ ᴢᴏɴᴀ: ");
        defaults.put("gui.zona_config.voz_ativada_titulo", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("gui.zona_config.voz_ativada_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ");
        defaults.put("gui.zona_config.voz_ativada_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.voz_desativada_titulo", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("gui.zona_config.voz_desativada_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ɴãᴏ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ");
        defaults.put("gui.zona_config.voz_desativada_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.jogadores_permitidos", "&6✦ &bᴊᴏɢᴀᴅᴏʀᴇs ᴘᴇʀᴍɪᴛɪᴅᴏs");
        defaults.put("gui.zona_config.permitidos_desc1", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ǫᴜᴇ ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ");
        defaults.put("gui.zona_config.permitidos_desc2", "&7   &fᴍᴇsᴍᴏ ᴄᴏᴍ ᴀ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("gui.zona_config.atualmente", "&7 › &eᴀᴛᴜᴀʟᴍᴇɴᴛᴇ • &f%s ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.zona_config.jogadores_mutados", "&6✦ &cᴊᴏɢᴀᴅᴏʀᴇs ᴍᴜᴛᴀᴅᴏs");
        defaults.put("gui.zona_config.mutados_desc1", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ǫᴜᴇ ᴇsᴛãᴏ ᴍᴜᴛᴀᴅᴏs");
        defaults.put("gui.zona_config.mutados_desc2", "&7   &fɪɴᴅᴇᴘᴇɴᴅᴇɴᴛᴇ ᴅᴀs ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ");
        defaults.put("gui.zona_config.deletar", "&c✖ &4ᴅᴇʟᴇᴛᴀʀ ᴢᴏɴᴀ");
        defaults.put("gui.zona_config.deletar_aviso", "&c › &4ᴀᴠɪsᴏ: ᴀçãᴏ ɪʀʀᴇᴠᴇʀsíᴠᴇʟ.");
        defaults.put("gui.zona_config.deletar_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇʟᴇᴛᴀʀ");
        defaults.put("gui.zona_config.voltar", "&c« &fᴠᴏʟᴛᴀʀ");
        defaults.put("gui.zona_config.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ᴢᴏɴᴀs");
        defaults.put("gui.zona_config.voz_ativada_msg", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ ɴᴀ ᴢᴏɴᴀ '%s'.");
        defaults.put("gui.zona_config.voz_desativada_msg", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ ɴᴀ ᴢᴏɴᴀ '%s'.");
        defaults.put("gui.zona_config.deletada", "&a✔ ᴢᴏɴᴀ '%s' ᴅᴇʟᴇᴛᴀᴅᴀ.");
        defaults.put("gui.zona_config.range_titulo", "&6✦ &fʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ");
        defaults.put("gui.zona_config.range_atual", "&7 › &eʀᴀɴɢᴇ • &a%s");
        defaults.put("gui.zona_config.range_padrao_valor", "ᴘᴀᴅʀãᴏ ᴅᴏ sᴇʀᴠɪᴅᴏʀ");
        defaults.put("gui.zona_config.range_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀᴀʀ");
        defaults.put("gui.zona_config.range_alterado", "&a✔ ʀᴀɴɢᴇ ᴅᴀ ᴢᴏɴᴀ '%s' ᴀʟᴛᴇʀᴀᴅᴏ ᴘᴀʀᴀ %s.");
        defaults.put("gui.zona_config.multiplicador_titulo", "&6✦ &eᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ ᴅᴇ ʀᴀɴɢᴇ");
        defaults.put("gui.zona_config.multiplicador_atual", "&7 › &eᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ • &a%s");
        defaults.put("gui.zona_config.multiplicador_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀᴀʀ");
        defaults.put("gui.zona_config.multiplicador_alterado", "&a✔ ᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ ᴅᴀ ᴢᴏɴᴀ '%s' ᴀʟᴛᴇʀᴀᴅᴏ ᴘᴀʀᴀ %s.");
        defaults.put("gui.zona_config.escuta_ativada", "&b✔ sᴏᴍᴇɴᴛᴇ ᴇsᴄᴜᴛᴀ");
        defaults.put("gui.zona_config.escuta_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴏᴜᴠᴇᴍ ᴍᴀs ɴãᴏ ꜰᴀʟᴀᴍ");
        defaults.put("gui.zona_config.escuta_clique_desativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.escuta_desativada", "&7✖ sᴏᴍᴇɴᴛᴇ ᴇsᴄᴜᴛᴀ");
        defaults.put("gui.zona_config.escuta_desc_off", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ᴇ ᴏᴜᴠɪʀ");
        defaults.put("gui.zona_config.escuta_clique_ativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.escuta_ativada_msg", "&b✔ sᴏᴍᴇɴᴛᴇ-ᴇsᴄᴜᴛᴀ ᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'.");
        defaults.put("gui.zona_config.escuta_desativada_msg", "&e✖ sᴏᴍᴇɴᴛᴇ-ᴇsᴄᴜᴛᴀ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'.");
        defaults.put("gui.zona_config.tempo_restante", "&7 › &eᴛᴇᴍᴘᴏ ʀᴇsᴛᴀɴᴛᴇ • &f%s");
        defaults.put("gui.zona_config.expirada", "&c✖ ᴢᴏɴᴀ ᴇxᴘɪʀᴀᴅᴀ.");

        // Menu Jogadores Permitidos
        defaults.put("gui.permitidos.prefixo", "&5✦ ᴘᴇʀᴍɪᴛɪᴅᴏs: ");
        defaults.put("gui.permitidos.clique_remover", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ");
        defaults.put("gui.permitidos.adicionar", "&6✦ &aᴀᴅɪᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.permitidos.adicionar_desc1", "&7 › &fᴀᴅɪᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ");
        defaults.put("gui.permitidos.adicionar_desc2", "&7   &fà ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs");
        defaults.put("gui.permitidos.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ");
        defaults.put("gui.permitidos.removido", "&a✔ &e%s &aʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs.");
        defaults.put("gui.permitidos.adicionado", "&a✔ &e%s &aᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ʟɪsᴛᴀ ᴅᴇ ᴘᴇʀᴍɪᴛɪᴅᴏs.");

        // Menu Jogadores Mutados
        defaults.put("gui.mutados.prefixo", "&5✦ ᴍᴜᴛᴀᴅᴏs: ");
        defaults.put("gui.mutados.clique_desmutar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴍᴜᴛᴀʀ");
        defaults.put("gui.mutados.mutar", "&6✦ &cᴍᴜᴛᴀʀ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.mutados.mutar_desc1", "&7 › &fᴍᴜᴛᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ");
        defaults.put("gui.mutados.mutar_desc2", "&7   &fɴᴇsᴛᴀ ᴢᴏɴᴀ");
        defaults.put("gui.mutados.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ᴄᴏɴꜰɪɢ. ᴅᴀ ᴢᴏɴᴀ");
        defaults.put("gui.mutados.desmutado", "&a✔ &e%s &aᴅᴇsᴍᴜᴛᴀᴅᴏ ɴᴇsᴛᴀ ᴢᴏɴᴀ.");
        defaults.put("gui.mutados.mutado", "&c✖ &e%s &cᴍᴜᴛᴀᴅᴏ ɴᴇsᴛᴀ ᴢᴏɴᴀ.");

        // Menu Adicionar
        defaults.put("gui.adicionar.permitido_prefixo", "&5✦ ᴘᴇʀᴍɪᴛɪʀ: ");
        defaults.put("gui.adicionar.mutado_prefixo", "&5✦ ᴍᴜᴛᴀʀ: ");
        defaults.put("gui.adicionar.clique_permitir", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴘᴇʀᴍɪᴛɪʀ ꜰᴀʟᴀʀ");
        defaults.put("gui.adicionar.clique_mutar", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴍᴜᴛᴀʀ ɴᴇsᴛᴀ ᴢᴏɴᴀ");
        defaults.put("gui.adicionar.voltar", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ᴊᴏɢᴀᴅᴏʀᴇs");

        // Range de Voz - Comandos
        defaults.put("range.titulo", "&7ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ:");
        defaults.put("range.menu", "&e/mvoice range");
        defaults.put("range.menu_desc", "&7— ᴍᴇɴᴜ ᴅᴇ ɢᴇʀᴇɴᴄɪᴀᴍᴇɴᴛᴏ ᴅᴇ ʀᴀɴɢᴇs");
        defaults.put("range.set", "&e/mvoice range set <jogador> <distancia>");
        defaults.put("range.set_desc", "&7— ᴅᴇꜰɪɴɪʀ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ");
        defaults.put("range.remove", "&e/mvoice range remove <jogador>");
        defaults.put("range.remove_desc", "&7— ʀᴇᴍᴏᴠᴇʀ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ");
        defaults.put("range.info", "&e/mvoice range info [jogador]");
        defaults.put("range.info_desc", "&7— ᴠᴇʀ ʀᴀɴɢᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("range.list", "&e/mvoice range list");
        defaults.put("range.list_desc", "&7— ʟɪsᴛᴀʀ ᴛᴏᴅᴏs ᴏs ʀᴀɴɢᴇs ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏs");
        defaults.put("range.reload", "&e/mvoice range reload");
        defaults.put("range.reload_desc", "&7— ʀᴇᴄᴀʀʀᴇɢᴀʀ ʀᴀɴɢᴇs");
        defaults.put("range.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ʀᴀɴɢᴇs ᴅᴇ ᴠᴏᴢ.");
        defaults.put("range.uso_set", "&cᴜsᴏ: &e/mvoice range set <jogador> <distancia>");
        defaults.put("range.uso_remove", "&cᴜsᴏ: &e/mvoice range remove <jogador>");
        defaults.put("range.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 1 ᴇ 1000.");
        defaults.put("range.definido", "&aʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %s ʙʟᴏᴄᴏs.");
        defaults.put("range.removido", "&aʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ. ᴜsᴀɴᴅᴏ ʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ.");
        defaults.put("range.sem_custom", "&e%s ɴãᴏ ᴘᴏssᴜɪ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ.");
        defaults.put("range.info_jogador", "&e%s ᴘᴏssᴜɪ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ: &a%s ʙʟᴏᴄᴏs");
        defaults.put("range.info_padrao", "&e%s ᴜsᴀ ᴏ ʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ: &a%s ʙʟᴏᴄᴏs");
        defaults.put("range.lista_vazia", "&eɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ.");
        defaults.put("range.lista_titulo", "&7ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ:");
        defaults.put("range.lista_item", "&e- %s: &a%s ʙʟᴏᴄᴏs");
        defaults.put("range.recarregado", "&aʀᴀɴɢᴇs ᴅᴇ ᴠᴏᴢ ʀᴇᴄᴀʀʀᴇɢᴀᴅᴏs.");

        // Range de Voz - GUI
        defaults.put("gui.range.titulo", "&5✦ ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ");
        defaults.put("gui.range.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ʀᴀɴɢᴇs");
        defaults.put("gui.range.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ʀᴀɴɢᴇs");
        defaults.put("gui.range.info_desc", "&7 › &fɢᴇʀᴇɴᴄɪᴇ ᴏ ʀᴀɴɢᴇ ᴅᴇ ᴠᴏᴢ ᴅᴏs ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.range.range_padrao", "&7 › &eʀᴀɴɢᴇ ᴘᴀᴅʀãᴏ • &a%s ʙʟᴏᴄᴏs");
        defaults.put("gui.range.vazio", "&7✦ ɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ");
        defaults.put("gui.range.jogador_range", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs");
        defaults.put("gui.range.clique_gerenciar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ");
        defaults.put("gui.range.definir", "&6✦ &aᴅᴇꜰɪɴɪʀ ʀᴀɴɢᴇ");
        defaults.put("gui.range.definir_desc", "&7 › &fᴅᴇꜰɪɴᴀ ᴏ ʀᴀɴɢᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴏɴʟɪɴᴇ");
        defaults.put("gui.range.recarregar", "&6✦ &aʀᴇᴄᴀʀʀᴇɢᴀʀ ʀᴀɴɢᴇs");
        defaults.put("gui.range.recarregar_desc", "&7 › &fʀᴇᴄᴀʀʀᴇɢᴀ ᴛᴏᴅᴏs ᴏs ʀᴀɴɢᴇs");
        defaults.put("gui.range.recarregadas", "&a✔ ʀᴀɴɢᴇs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴏs.");
        defaults.put("gui.range.removido_gui", "&a✔ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ.");
        defaults.put("gui.range.selecionar_prefixo", "&5✦ sᴇʟᴇᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.range.selecionar_titulo", "&6✦ &bsᴇʟᴇᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.range.selecionar_desc", "&7 › &fsᴇʟᴇᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴘᴀʀᴀ ᴅᴇꜰɪɴɪʀ ᴏ ʀᴀɴɢᴇ");
        defaults.put("gui.range.sem_custom_gui", "&7 › &fsᴇᴍ ʀᴀɴɢᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ");
        defaults.put("gui.range.clique_selecionar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ sᴇʟᴇᴄɪᴏɴᴀʀ");
        defaults.put("gui.range.voltar", "&c« &fᴠᴏʟᴛᴀʀ");
        defaults.put("gui.range.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ʀᴀɴɢᴇs");
        defaults.put("gui.range.distancia_prefixo", "&5✦ ʀᴀɴɢᴇ: ");
        defaults.put("gui.range.distancia_atual", "&7 › &eʀᴀɴɢᴇ ᴀᴛᴜᴀʟ • &a%s ʙʟᴏᴄᴏs");
        defaults.put("gui.range.distancia_blocos", "&a%s ʙʟᴏᴄᴏs");
        defaults.put("gui.range.distancia_clique", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇꜰɪɴɪʀ");
        defaults.put("gui.range.distancia_selecionado", "&a✔ sᴇʟᴇᴄɪᴏɴᴀᴅᴏ.");
        defaults.put("gui.range.distancia_definido", "&a✔ ʀᴀɴɢᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %s ʙʟᴏᴄᴏs.");
        defaults.put("gui.range.remover", "&c✖ &4ʀᴇᴍᴏᴠᴇʀ ʀᴀɴɢᴇ");
        defaults.put("gui.range.remover_desc", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ range customizado");

        // Global Voice - GUI
        defaults.put("gui.global.titulo", "&5✦ ᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.info_titulo", "&d&l✦ ᴍɪᴅɢᴀʀᴅᴠᴏɪᴄᴇ &r&5— ᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.info_total", "&7 › &eᴛᴏᴛᴀʟ • &f%s ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.global.info_desc", "&7 › &fᴊᴏɢᴀᴅᴏʀᴇs ᴏᴜᴠɪᴅᴏs ᴘᴏʀ ᴛᴏᴅᴏs ɴᴏ ᴍᴜɴᴅᴏ");
        defaults.put("gui.global.vazio", "&7✦ ɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.jogador_desc", "&7 › &aᴛᴏᴅᴏs ᴏᴜᴠᴇᴍ ᴇsᴛᴇ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.global.clique_remover", "&c › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ");
        defaults.put("gui.global.adicionar", "&6✦ &aᴀᴅɪᴄɪᴏɴᴀʀ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("gui.global.adicionar_desc", "&7 › &fᴀᴅɪᴄɪᴏɴᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.clique_adicionar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴛᴏʀɴᴀʀ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.voltar_lista", "&7 › &fᴠᴏʟᴛᴀʀ ᴘᴀʀᴀ ʟɪsᴛᴀ ᴅᴇ ɢʟᴏʙᴀɪs");
        defaults.put("gui.global.selecionar_titulo", "&5✦ ᴀᴅɪᴄɪᴏɴᴀʀ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.botao", "&6✦ &dᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("gui.global.botao_desc", "&7 › &eɢʟᴏʙᴀɪs • &f%s ᴊᴏɢᴀᴅᴏʀᴇs");
        defaults.put("gui.global.removido", "&c✖ &e%s &cʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("gui.global.adicionado", "&a✔ &e%s &aᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ᴠᴏᴢ ɢʟᴏʙᴀʟ.");

        // Global Voice - Comandos
        defaults.put("range.global", "&e/mvoice global [add|remove|list]");
        defaults.put("range.global_desc", "&7— ɢᴇʀᴇɴᴄɪᴀʀ ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ");
        defaults.put("range.global_uso_add", "&cᴜsᴏ: &e/mvoice global add <jogador>");
        defaults.put("range.global_uso_remove", "&cᴜsᴏ: &e/mvoice global remove <jogador>");
        defaults.put("range.global_adicionado", "&a%s ᴀɢᴏʀᴀ ᴛᴇᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("range.global_ja_global", "&e%s ᴊá ᴘᴏssᴜɪ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("range.global_removido", "&a%s ɴãᴏ ᴛᴇᴍ ᴍᴀɪs ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("range.global_nao_global", "&e%s ɴãᴏ ᴘᴏssᴜɪ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("range.global_lista_vazia", "&eɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("range.global_lista_titulo", "&7ᴊᴏɢᴀᴅᴏʀᴇs ᴄᴏᴍ ᴠᴏᴢ ɢʟᴏʙᴀʟ:");
        defaults.put("range.global_lista_item", "&e- %s");

        // Global Voice - Permissao e Limite
        defaults.put("global.sem_permissao", "&cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɢᴇʀᴇɴᴄɪᴀʀ ᴠᴏᴢ ɢʟᴏʙᴀʟ.");
        defaults.put("global.limite_atingido", "&cʟɪᴍɪᴛᴇ ᴅᴇ ᴊᴏɢᴀᴅᴏʀᴇs ɢʟᴏʙᴀɪs ᴀᴛɪɴɢɪᴅᴏ. (ᴍᴀx: %s)");

        // Status
        defaults.put("status.titulo", "&7sᴛᴀᴛᴜs ᴅᴇ &e%s&7:");
        defaults.put("status.range", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs &7(padrao)");
        defaults.put("status.range_custom", "&7 › &eʀᴀɴɢᴇ • &a%s ʙʟᴏᴄᴏs &e(customizado)");
        defaults.put("status.global_sim", "&7 › &eɢʟᴏʙᴀʟ • &a✔ sɪᴍ");
        defaults.put("status.global_nao", "&7 › &eɢʟᴏʙᴀʟ • &c✖ ɴãᴏ");
        defaults.put("status.global_total", "&7 › &eɢʟᴏʙᴀɪs • &f%s/%s");
        defaults.put("status.ɪʟɪᴍɪᴛᴀᴅᴏ", "ɪʟɪᴍɪᴛᴀᴅᴏ");
        defaults.put("status.zona_nenhuma", "&7 › &eᴢᴏɴᴀ • &fɴᴇɴʜᴜᴍᴀ");
        defaults.put("status.zona_em", "&7 › &eᴢᴏɴᴀ • &f%s &7(%s)");
        defaults.put("status.zona_voz_ativada", "&a✔ ᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("status.zona_voz_desativada", "&c✖ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ");

        // Notificacao de Zona
        defaults.put("zona.entrou_notificacao", "&5\u2588 &dVoce entrou na zona &f%s &7(%s)");
        defaults.put("zona.saiu_notificacao", "&5\u2588 &dVoce saiu da zona &f%s");
        defaults.put("zona.notificacao_voz_ativada", "&aᴠᴏᴢ ᴀᴛɪᴠᴀᴅᴀ");
        defaults.put("zona.notificacao_voz_desativada", "&cᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴀ");

        // Volume
        defaults.put("range.volume", "&e/mvoice volume <jogador> <valor|reset>");
        defaults.put("range.volume_desc", "&7— ᴀᴊᴜsᴛᴀʀ ᴠᴏʟᴜᴍᴇ ᴅᴇ ᴜᴍ ᴊᴏɢᴀᴅᴏʀ");
        defaults.put("volume.uso", "&cᴜsᴏ: &e/mvoice volume <jogador> <0.1-5.0|reset>");
        defaults.put("volume.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 0.1 ᴇ 5.0");
        defaults.put("volume.definido", "&aᴠᴏʟᴜᴍᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴏ ᴘᴀʀᴀ %sx");
        defaults.put("volume.removido", "&aᴠᴏʟᴜᴍᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ ᴅᴇ %s ʀᴇᴍᴏᴠɪᴅᴏ.");
        defaults.put("volume.sem_custom", "&e%s ɴãᴏ ᴘᴏssᴜɪ ᴠᴏʟᴜᴍᴇ ᴄᴜsᴛᴏᴍɪᴢᴀᴅᴏ.");

        // Prioridade
        defaults.put("range.priority", "&e/mvoice priority <jogador> <0-100>");
        defaults.put("range.priority_desc", "&7— ᴅᴇꜰɪɴɪʀ ᴘʀɪᴏʀɪᴅᴀᴅᴇ ᴅᴇ áᴜᴅɪᴏ");
        defaults.put("priority.uso", "&cᴜsᴏ: &e/mvoice priority <jogador> <0-100>");
        defaults.put("priority.valor_invalido", "&cᴠᴀʟᴏʀ ɪɴᴠáʟɪᴅᴏ. ᴜsᴇ ᴜᴍ ɴúᴍᴇʀᴏ ᴇɴᴛʀᴇ 0 ᴇ 100.");
        defaults.put("priority.definido", "&aᴘʀɪᴏʀɪᴅᴀᴅᴇ ᴅᴇ %s ᴅᴇꜰɪɴɪᴅᴀ ᴘᴀʀᴀ %s.");

        // Cooldown
        defaults.put("range.cooldown", "&e/mvoice cooldown [set|off|info]");
        defaults.put("range.cooldown_desc", "&7— ɢᴇʀᴇɴᴄɪᴀʀ ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ");
        defaults.put("cooldown.uso", "&cᴜsᴏ: &e/mvoice cooldown [set|off|info]");
        defaults.put("cooldown.uso_set", "&cᴜsᴏ: &e/mvoice cooldown set <tempo_fala_seg> <cooldown_seg>");
        defaults.put("cooldown.valor_invalido", "&cᴠᴀʟᴏʀᴇs ɪɴᴠáʟɪᴅᴏs. ᴜsᴇ ɴúᴍᴇʀᴏs ᴘᴏsɪᴛɪᴠᴏs.");
        defaults.put("cooldown.definido", "&aᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇꜰɪɴɪᴅᴏ: ꜰᴀʟᴀ=%ss, ᴇsᴘᴇʀᴀ=%ss");
        defaults.put("cooldown.desativado", "&aᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ.");
        defaults.put("cooldown.desativado_info", "&7ᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ ᴇsᴛá ᴅᴇsᴀᴛɪᴠᴀᴅᴏ.");
        defaults.put("cooldown.info", "&7ᴄᴏᴏʟᴅᴏᴡɴ: ꜰᴀʟᴀ=%ss, ᴇsᴘᴇʀᴀ=%ss");
        defaults.put("cooldown.ativo", "&cᴄᴏᴏʟᴅᴏᴡɴ ᴅᴇ ᴠᴏᴢ: ᴀɢᴜᴀʀᴅᴇ %ss");
        defaults.put("cooldown.iniciado", "&cᴛᴇᴍᴘᴏ ᴅᴇ ꜰᴀʟᴀ ᴇsɢᴏᴛᴀᴅᴏ. ᴀɢᴜᴀʀᴅᴇ ᴏ ᴄᴏᴏʟᴅᴏᴡɴ.");

        // Stage Mode
        defaults.put("gui.zona_config.stage_ativado", "&d✔ sᴛᴀɢᴇ ᴍᴏᴅᴇ");
        defaults.put("gui.zona_config.stage_desc", "&7 › &fᴀᴘᴇɴᴀs sᴘᴇᴀᴋᴇʀs ᴅᴇsɪɢɴᴀᴅᴏs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ");
        defaults.put("gui.zona_config.stage_speakers", "&7 › &esᴘᴇᴀᴋᴇʀs • &f%s");
        defaults.put("gui.zona_config.stage_clique_desativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴅᴇsᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.stage_desativado", "&7✖ sᴛᴀɢᴇ ᴍᴏᴅᴇ");
        defaults.put("gui.zona_config.stage_desc_off", "&7 › &fᴛᴏᴅᴏs ᴘᴏᴅᴇᴍ ꜰᴀʟᴀʀ ɴᴏʀᴍᴀʟᴍᴇɴᴛᴇ");
        defaults.put("gui.zona_config.stage_clique_ativar", "&a › &eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴛɪᴠᴀʀ");
        defaults.put("gui.zona_config.stage_ativado_msg", "&d✔ sᴛᴀɢᴇ ᴍᴏᴅᴇ ATIVADO na zona '%s'");
        defaults.put("gui.zona_config.stage_desativado_msg", "&e✖ sᴛᴀɢᴇ ᴍᴏᴅᴇ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ ɴᴀ ᴢᴏɴᴀ '%s'.");

        // Indicador Global
        defaults.put("global.indicador_falando", "ᴇsᴛá ꜰᴀʟᴀɴᴅᴏ (ᴠᴏᴢ ɢʟᴏʙᴀʟ)");

        // Gravacao de Audio
        defaults.put("rec.titulo", "&7ɢʀᴀᴠᴀçãᴏ ᴅᴇ áᴜᴅɪᴏ:");
        defaults.put("rec.start", "&e/mvoice record start <jogador>");
        defaults.put("rec.start_desc", "&7— ɪɴɪᴄɪᴀʀ ɢʀᴀᴠᴀçãᴏ");
        defaults.put("rec.stop", "&e/mvoice record stop <jogador>");
        defaults.put("rec.stop_desc", "&7— ᴘᴀʀᴀʀ ɢʀᴀᴠᴀçãᴏ");
        defaults.put("rec.active", "&e/mvoice record active");
        defaults.put("rec.active_desc", "&7— ᴠᴇʀ ɢʀᴀᴠᴀçõᴇs ᴀᴛɪᴠᴀs");
        defaults.put("rec.list", "&e/mvoice record list [pagina]");
        defaults.put("rec.list_desc", "&7— ʟɪsᴛᴀʀ ɢʀᴀᴠᴀçõᴇs sᴀʟᴠᴀs");
        defaults.put("rec.info", "&e/mvoice record info <id>");
        defaults.put("rec.info_desc", "&7— ᴠᴇʀ ᴅᴇᴛᴀʟʜᴇs ᴅᴇ ᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ");
        defaults.put("rec.delete", "&e/mvoice record delete <id>");
        defaults.put("rec.delete_desc", "&7— ᴅᴇʟᴇᴛᴀʀ ᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ");
        defaults.put("rec.uso_start", "&cᴜsᴏ: &e/mvoice record start <jogador>");
        defaults.put("rec.uso_stop", "&cᴜsᴏ: &e/mvoice record stop <jogador>");
        defaults.put("rec.uso_info", "&cᴜsᴏ: &e/mvoice record info <id>");
        defaults.put("rec.uso_delete", "&cᴜsᴏ: &e/mvoice record delete <id>");
        defaults.put("rec.ja_gravando", "&c%s ᴊá ᴇsᴛá sᴇɴᴅᴏ ɢʀᴀᴠᴀᴅᴏ.");
        defaults.put("rec.iniciada", "&aɢʀᴀᴠᴀçãᴏ ᴅᴇ %s ɪɴɪᴄɪᴀᴅᴀ.");
        defaults.put("rec.id", "&7ɪᴅ: &f%s");
        defaults.put("rec.nao_gravando", "&c%s ɴãᴏ ᴇsᴛá sᴇɴᴅᴏ ɢʀᴀᴠᴀᴅᴏ.");
        defaults.put("rec.parada", "&aɢʀᴀᴠᴀçãᴏ ᴅᴇ %s ꜰɪɴᴀʟɪᴢᴀᴅᴀ.");
        defaults.put("rec.salva", "&7sᴀʟᴠᴀ ᴄᴏᴍᴏ: &f%s &7(ᴅᴜʀᴀçãᴏ: %s, ꜰʀᴀᴍᴇs: %s)");
        defaults.put("rec.nenhuma_ativa", "&eɴᴇɴʜᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ ᴀᴛɪᴠᴀ ɴᴏ ᴍᴏᴍᴇɴᴛᴏ.");
        defaults.put("rec.ativas_titulo", "&7ɢʀᴀᴠᴀçõᴇs ᴀᴛɪᴠᴀs:");
        defaults.put("rec.ativa_item", "&e- %s &7(ᴅᴜʀᴀçãᴏ: %s, ꜰʀᴀᴍᴇs: %s)");
        defaults.put("rec.nenhuma_salva", "&eɴᴇɴʜᴜᴍᴀ ɢʀᴀᴠᴀçãᴏ sᴀʟᴠᴀ.");
        defaults.put("rec.lista_titulo", "&7ɢʀᴀᴠᴀçõᴇs sᴀʟᴠᴀs (%s/%s):");
        defaults.put("rec.lista_item", "&e- %s");
        defaults.put("rec.info_titulo", "&7ᴅᴇᴛᴀʟʜᴇs ᴅᴀ ɢʀᴀᴠᴀçãᴏ:");
        defaults.put("rec.nao_encontrada", "&cɢʀᴀᴠᴀçãᴏ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴀ.");
        defaults.put("rec.deletada", "&aɢʀᴀᴠᴀçãᴏ '%s' ᴅᴇʟᴇᴛᴀᴅᴀ.");

        defaults.put("formats.blocks", "%s blocos");
        defaults.put("formats.seconds", "%ss");
        defaults.put("formats.cooldown_pair", "%s fala / %s espera");
        defaults.put("formats.cooldown_compact", "%s/%s");
        defaults.put("formats.duration_days_hours", "%sd %sh");
        defaults.put("formats.duration_hours_minutes", "%sh %sm");
        defaults.put("formats.duration_minutes_seconds", "%sm %ss");
        defaults.put("formats.duration_seconds", "%ss");
        defaults.put("voice.area_blocked", "&cVoce nao pode usar o chat de voz nesta area.");
        defaults.put("voice.auth_still_connecting_kick", "&cA autenticacao do voice chat foi tentada cedo demais durante a conexao.");

        for (Map.Entry<String, String> entry : TRANSLATION_DEFAULTS.entrySet()) {
            defaults.put("translations." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        saveConfig();
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String text(String path, String defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            saveConfig();
        }
        return color(config.getString(path, defaultValue));
    }

    public String format(String path, String defaultValue, Object... args) {
        return String.format(text(path, defaultValue), args);
    }

    public String translation(String key, Object... args) {
        String defaultValue = TRANSLATION_DEFAULTS.getOrDefault(key, key);
        return String.format(text("translations." + key, defaultValue), args);
    }

    public List<String> textList(String path, String... defaultValues) {
        if (!config.contains(path)) {
            config.set(path, Arrays.asList(defaultValues));
            saveConfig();
            return colorList(Arrays.asList(defaultValues));
        }

        List<String> values = config.getStringList(path);
        if (values.isEmpty() && defaultValues.length > 0) {
            config.set(path, Arrays.asList(defaultValues));
            saveConfig();
            return colorList(Arrays.asList(defaultValues));
        }

        return colorList(values);
    }

    private List<String> colorList(List<String> values) {
        List<String> colored = new ArrayList<>(values.size());
        for (String value : values) {
            colored.add(color(value));
        }
        return colored;
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            Voicechat.LOGGER.error("Falha ao salvar arquivo de mensagens", e);
        }
    }
}
