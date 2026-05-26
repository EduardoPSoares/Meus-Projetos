package me.ray.midgard.modules.item.gui;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Sessão de edição de item.
 * <p>
 * Mantém uma referência única ao {@link ItemEditionGui} principal para que todos os
 * sub-menus e editores possam navegar de volta ao menu correto sem criar novas
 * instâncias. Garante que ao retornar de um sub-menu, o menu pai é sempre
 * atualizado automaticamente com os dados mais recentes.
 * <p>
 * Ciclo de vida:
 * <ol>
 *   <li>{@code ItemEditionGui} chama {@link #registerEditor} no construtor.</li>
 *   <li>Sub-menus usam {@link #get(Player)} para obter a sessão corrente.</li>
 *   <li>{@link #end(Player)} — limpa a sessão ao fechar o editor.</li>
 * </ol>
 */
public class EditorSession {

    private static final Map<UUID, EditorSession> SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();

    private final Player player;
    private final ItemModule module;
    private final MidgardItem item;
    private final ItemEditionGui mainEditor;

    private EditorSession(Player player, ItemModule module, MidgardItem item, ItemEditionGui editor) {
        this.player = player;
        this.module = module;
        this.item = item;
        this.mainEditor = editor;
    }

    // ─── Lifecycle ──────────────────────────────────────────

    /**
     * Registra o editor principal na sessão. Chamado pelo construtor de {@link ItemEditionGui}.
     */
    public static void registerEditor(Player player, ItemModule module, MidgardItem item, ItemEditionGui editor) {
        SESSIONS.put(player.getUniqueId(), new EditorSession(player, module, item, editor));
    }

    /**
     * Obtém a sessão ativa para o jogador, ou null se não houver.
     */
    public static EditorSession get(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    /**
     * Encerra a sessão do jogador.
     */
    public static void end(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    // ─── Acessores ──────────────────────────────────────────

    public Player getPlayer() { return player; }
    public ItemModule getModule() { return module; }
    public MidgardItem getItem() { return item; }

    /**
     * Retorna a instância única do editor principal (não cria nova).
     */
    public ItemEditionGui getMainEditor() { return mainEditor; }

    // ─── Navegação ──────────────────────────────────────────

    /**
     * Volta ao editor principal, atualizando-o automaticamente.
     * Equivalente a {@code mainEditor.open()} (que chama {@code initializeItems()}).
     */
    public void returnToMain() {
        mainEditor.open();
    }
}
