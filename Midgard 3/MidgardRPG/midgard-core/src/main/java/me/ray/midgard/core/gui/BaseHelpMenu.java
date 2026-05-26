package me.ray.midgard.core.gui;

import me.ray.midgard.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Classe base para menus de ajuda interativos.
 * Fornece estrutura padronizada para menus de ajuda com múltiplas páginas e tópicos.
 */
public abstract class BaseHelpMenu extends BaseGui {
    
    protected final String[] topics;
    protected final int currentPage;
    protected final int topicsPerPage;
    protected final BaseGui parentMenu;
    
    // Slots padrão de navegação
    protected static final int PREVIOUS_PAGE_SLOT = 45;
    protected static final int BACK_SLOT = 49;
    protected static final int NEXT_PAGE_SLOT = 53;
    
    // Slots para tópicos (área central do menu 6x9)
    protected static final int[] TOPIC_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    
    /**
     * Construtor principal do menu de ajuda.
     * 
     * @param player Jogador visualizando o menu
     * @param title Título do menu de ajuda
     * @param topics Array de IDs dos tópicos de ajuda
     * @param currentPage Página atual (0-indexed)
     * @param topicsPerPage Número de tópicos por página
     * @param parentMenu Menu pai para voltar (pode ser null)
     */
    public BaseHelpMenu(Player player, String title, String[] topics, int currentPage, 
                       int topicsPerPage, BaseGui parentMenu) {
        super(player, 6, title);
        this.topics = topics;
        this.currentPage = currentPage;
        this.topicsPerPage = Math.min(topicsPerPage, TOPIC_SLOTS.length);
        this.parentMenu = parentMenu;
    }
    
    /**
     * Construtor simplificado (primeira página, sem menu pai).
     */
    public BaseHelpMenu(Player player, String title, String[] topics) {
        this(player, title, topics, 0, 28, null);
    }
    
    /**
     * Obtém o material para representar um tópico específico.
     * Deve ser implementado pela classe filha.
     * 
     * @param topicId ID do tópico
     * @return Material a ser usado no ícone
     */
    protected abstract Material getTopicMaterial(String topicId);
    
    /**
     * Obtém o nome do tópico.
     * Deve ser implementado pela classe filha.
     * 
     * @param topicId ID do tópico
     * @return Nome formatado do tópico
     */
    protected abstract String getTopicName(String topicId);
    
    /**
     * Obtém a lore (descrição) do tópico.
     * Deve ser implementado pela classe filha.
     * 
     * @param topicId ID do tópico
     * @return Lista de linhas da lore
     */
    protected abstract List<String> getTopicLore(String topicId);
    
    /**
     * Cria um novo menu de ajuda na página especificada.
     * Usado para navegação entre páginas.
     * 
     * @param page Nova página
     * @return Nova instância do menu de ajuda
     */
    protected abstract BaseHelpMenu createNewPage(int page);
    
    @Override
    public void initializeItems() {
        // Limpar inventário
        inventory.clear();
        
        // Adicionar filler (vidro cinza)
        ItemStack filler = GuiUtils.createFiller();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        
        // Renderizar tópicos da página atual
        renderTopicsPage();
        
        // Adicionar botões de navegação
        addNavigationButtons();
    }
    
    /**
     * Renderiza os tópicos da página atual.
     */
    protected void renderTopicsPage() {
        int startIndex = currentPage * topicsPerPage;
        int endIndex = Math.min(startIndex + topicsPerPage, topics.length);
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= TOPIC_SLOTS.length) { break; }
            
            String topicId = topics[i];
            ItemStack topicItem = createTopicItem(topicId);
            inventory.setItem(TOPIC_SLOTS[slotIndex], topicItem);
            slotIndex++;
        }
    }
    
    /**
     * Cria o ItemStack para um tópico específico.
     */
    protected ItemStack createTopicItem(String topicId) {
        Material material = getTopicMaterial(topicId);
        String name = getTopicName(topicId);
        List<String> lore = getTopicLore(topicId);
        
        ItemBuilder builder = new ItemBuilder(material)
                .setName(name);
        
        for (String line : lore) {
            builder.addLore(line);
        }
        
        return builder.build();
    }
    
    /**
     * Adiciona botões de navegação padrão.
     */
    protected void addNavigationButtons() {
        int totalPages = (int) Math.ceil((double) topics.length / topicsPerPage);
        
        // Botão "Anterior" (se não estiver na primeira página)
        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, GuiUtils.createPreviousButton());
        }
        
        // Botão "Voltar" (se houver menu pai)
        if (parentMenu != null) {
            inventory.setItem(BACK_SLOT, GuiUtils.createBackButton());
        } else {
            // Item de informação da página
            ItemStack pageInfo = new ItemBuilder(Material.PAPER)
                    .setName("§e§lPágina " + (currentPage + 1) + "/" + totalPages)
                    .addLore("")
                    .addLore("§7Navegue usando as setas")
                    .build();
            inventory.setItem(BACK_SLOT, pageInfo);
        }
        
        // Botão "Próximo" (se não estiver na última página)
        if (currentPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, GuiUtils.createNextButton());
        }
    }
    
    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        // Navegação: Página Anterior
        if (slot == PREVIOUS_PAGE_SLOT && currentPage > 0) {
            createNewPage(currentPage - 1).open();
            return;
        }
        
        // Navegação: Próxima Página
        int totalPages = (int) Math.ceil((double) topics.length / topicsPerPage);
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages - 1) {
            createNewPage(currentPage + 1).open();
            return;
        }
        
        // Navegação: Voltar ao menu pai
        if (slot == BACK_SLOT && parentMenu != null) {
            parentMenu.open();
            return;
        }
        
        // Verificar se clicou em um tópico
        for (int i = 0; i < TOPIC_SLOTS.length; i++) {
            if (slot == TOPIC_SLOTS[i]) {
                int topicIndex = (currentPage * topicsPerPage) + i;
                if (topicIndex < topics.length) {
                    onTopicClick(topics[topicIndex], event);
                }
                return;
            }
        }
    }
    
    /**
     * Chamado quando um tópico é clicado.
     * Pode ser sobrescrito para adicionar comportamento customizado.
     * 
     * @param topicId ID do tópico clicado
     * @param event Evento do clique
     */
    protected void onTopicClick(String topicId, InventoryClickEvent event) {
        // Por padrão, não faz nada (os tópicos são apenas informativos)
        // Classes filhas podem sobrescrever para adicionar interações
    }
}
