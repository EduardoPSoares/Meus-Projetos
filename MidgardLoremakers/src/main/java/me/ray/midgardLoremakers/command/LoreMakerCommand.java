package me.ray.midgardLoremakers.command;

import me.ray.midgardLoremakers.MidgardLoremakers;
import me.ray.midgardLoremakers.config.PluginConfiguration;
import me.ray.midgardLoremakers.model.IssuedAccessToken;
import me.ray.midgardLoremakers.model.LoreBook;
import me.ray.midgardLoremakers.service.ValidationException;
import me.ray.midgardLoremakers.util.BookTextFormatter;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LoreMakerCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter TOKEN_EXPIRY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String RELOAD_PERMISSION = "midgardloremakers.reload";

    private final MidgardLoremakers plugin;

    public LoreMakerCommand(MidgardLoremakers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length != 3) {
                player.sendMessage(Component.text("Uso: /loremaker give <jogador> <bookId>", NamedTextColor.RED));
                return true;
            }
            return handleGive(player, args[1], args[2]);
        }

        if (args.length > 1) {
            sender.sendMessage("Uso: /loremaker [token|reload|import|give <jogador> <id>]");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("import")) {
            return handleImport(player);
        }

        if (args.length == 1 && !args[0].equalsIgnoreCase("token")) {
            sender.sendMessage("Uso: /loremaker [token|reload|import|give <jogador> <id>]");
            return true;
        }

        PluginConfiguration pluginConfiguration = plugin.pluginConfiguration();
        if (!pluginConfiguration.hasValidPublicRouting()) {
            player.sendMessage(Component.text("Configuracao publica invalida: " + pluginConfiguration.publicRoutingValidationError(), NamedTextColor.RED));
            return true;
        }

        try {
            String connectionHost = resolveConnectionHost(player);
            if (!pluginConfiguration.hasExplicitPublicUrlOverride()) {
                String hostError = pluginConfiguration.automaticHostValidationError(connectionHost);
                if (hostError != null) {
                    player.sendMessage(Component.text(hostError, NamedTextColor.RED));
                    return true;
                }
            }

            IssuedAccessToken issuedToken = plugin.tokenService().issueToken(player.getUniqueId(), player.getName());
            String panelUrl = pluginConfiguration.buildPanelUrl(issuedToken.rawToken(), connectionHost);
            Book book = buildPortalBook(player.getName(), panelUrl, issuedToken.session().expiresAt());

            player.openBook(book);
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao gerar acesso para " + player.getName() + ": " + exception.getMessage());
            player.sendMessage(Component.text("Nao foi possivel abrir o painel agora. Veja o console do servidor.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            java.util.ArrayList<String> suggestions = new java.util.ArrayList<>();

            if (sender instanceof Player && "token".startsWith(input)) {
                suggestions.add("token");
            }
            if (sender instanceof Player && "import".startsWith(input)) {
                suggestions.add("import");
            }
            if (sender instanceof Player && "give".startsWith(input)) {
                suggestions.add("give");
            }
            if (sender.hasPermission(RELOAD_PERMISSION) && "reload".startsWith(input)) {
                suggestions.add("reload");
            }

            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String input = args[1].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        return List.of();
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage(Component.text("Voce nao tem permissao para recarregar o Midgard LoreMaker.", NamedTextColor.RED));
            return true;
        }

        try {
            PluginConfiguration reloadedConfiguration = plugin.reloadRuntimeConfiguration();
            sender.sendMessage(Component.text("Midgard LoreMaker recarregado.", NamedTextColor.GREEN)
                    .append(Component.text(" Bind: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(reloadedConfiguration.web().bindHost() + ":" + reloadedConfiguration.web().port(), NamedTextColor.AQUA))
                    .append(Component.text(" | Rota publica: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(reloadedConfiguration.publicRoutingDescription(), NamedTextColor.AQUA)));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao recarregar configuracao: " + exception.getMessage());
            sender.sendMessage(Component.text("Falha ao recarregar configuracao: " + exception.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleImport(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.WRITTEN_BOOK) {
            player.sendMessage(Component.text("Segure um livro escrito (Written Book) na mao principal para importar.", NamedTextColor.RED));
            return true;
        }

        BookMeta meta = (BookMeta) heldItem.getItemMeta();
        String title = meta.getTitle();
        if (title == null || title.isBlank()) {
            title = "Livro importado";
        }

        List<Component> bookPages = meta.pages();
        List<String> textPages = new ArrayList<>();
        for (Component page : bookPages) {
            textPages.add(serializeComponent(page));
        }

        if (textPages.isEmpty()) {
            textPages.add("");
        }

        try {
            LoreBook imported = plugin.loreBookService().saveBook(
                    player.getUniqueId(),
                    null,
                    title,
                    "Sem categoria",
                    List.of(),
                    textPages
            );
            player.sendMessage(Component.text("Livro \"" + imported.title() + "\" importado com sucesso! (", NamedTextColor.GREEN)
                    .append(Component.text(imported.pageCount() + " pagina(s)", NamedTextColor.AQUA))
                    .append(Component.text(")", NamedTextColor.GREEN)));
        } catch (ValidationException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao importar livro de " + player.getName() + ": " + exception.getMessage());
            player.sendMessage(Component.text("Falha ao importar o livro. Veja o console do servidor.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleGive(Player sender, String targetName, String rawBookId) {
        long bookId;
        try {
            bookId = Long.parseLong(rawBookId);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("ID de livro invalido: " + rawBookId, NamedTextColor.RED));
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Jogador \"" + targetName + "\" nao esta online.", NamedTextColor.RED));
            return true;
        }

        try {
            LoreBook book = plugin.loreBookService().findBook(sender.getUniqueId(), bookId)
                    .orElse(null);
            if (book == null) {
                sender.sendMessage(Component.text("Livro nao encontrado ou nao pertence a voce.", NamedTextColor.RED));
                return true;
            }

            ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) bookItem.getItemMeta();
            meta.setTitle(book.title());
            meta.setAuthor(sender.getName());

            for (String page : book.pages()) {
                meta.addPages(BookTextFormatter.parseFormattedText(page));
            }

            meta.setEnchantmentGlintOverride(true);
            meta.displayName(Component.text(book.title()).color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Presente de " + sender.getName()).color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));

            bookItem.setItemMeta(meta);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(bookItem);
            if (!overflow.isEmpty()) {
                target.getWorld().dropItemNaturally(target.getLocation(), overflow.values().iterator().next());
                sender.sendMessage(Component.text("Inventario de " + target.getName() + " cheio. O livro foi largado no chao.", NamedTextColor.YELLOW));
            }

            sender.sendMessage(Component.text("Livro \"" + book.title() + "\" enviado para " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Voce recebeu o livro \"" + book.title() + "\" de " + sender.getName() + "!", NamedTextColor.GREEN));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao dar livro para " + targetName + ": " + exception.getMessage());
            sender.sendMessage(Component.text("Falha ao enviar o livro. Veja o console do servidor.", NamedTextColor.RED));
        }

        return true;
    }

    private static String serializeComponent(Component component) {
        StringBuilder result = new StringBuilder();
        serializeComponentRecursive(component, result, null, false, false, false, false);
        return result.toString();
    }

    private static void serializeComponentRecursive(Component component, StringBuilder result,
                                                     TextColor parentColor, boolean parentBold,
                                                     boolean parentItalic, boolean parentUnderlined,
                                                     boolean parentStrikethrough) {
        TextColor color = component.color() != null ? component.color() : parentColor;
        boolean bold = resolveDecoration(component, TextDecoration.BOLD, parentBold);
        boolean italic = resolveDecoration(component, TextDecoration.ITALIC, parentItalic);
        boolean underlined = resolveDecoration(component, TextDecoration.UNDERLINED, parentUnderlined);
        boolean strikethrough = resolveDecoration(component, TextDecoration.STRIKETHROUGH, parentStrikethrough);

        boolean hasFormatting = color != null || bold || italic || underlined || strikethrough;

        if (component instanceof TextComponent textComponent) {
            String content = textComponent.content();
            if (!content.isEmpty()) {
                if (hasFormatting) {
                    String colorCode = mapTextColorToCode(color);
                    if (colorCode != null) {
                        result.append('\u00A7').append(colorCode);
                    }
                    if (bold) result.append("\u00A7l");
                    if (italic) result.append("\u00A7o");
                    if (underlined) result.append("\u00A7n");
                    if (strikethrough) result.append("\u00A7m");
                }
                result.append(content);
            }
        }

        for (Component child : component.children()) {
            serializeComponentRecursive(child, result, color, bold, italic, underlined, strikethrough);
        }
    }

    private static boolean resolveDecoration(Component component, TextDecoration decoration, boolean parentValue) {
        TextDecoration.State state = component.decoration(decoration);
        return switch (state) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> parentValue;
        };
    }

    private static final Map<TextColor, String> COLOR_TO_CODE = Map.ofEntries(
            Map.entry(NamedTextColor.BLACK, "0"),
            Map.entry(NamedTextColor.DARK_BLUE, "1"),
            Map.entry(NamedTextColor.DARK_GREEN, "2"),
            Map.entry(NamedTextColor.DARK_AQUA, "3"),
            Map.entry(NamedTextColor.DARK_RED, "4"),
            Map.entry(NamedTextColor.DARK_PURPLE, "5"),
            Map.entry(NamedTextColor.GOLD, "6"),
            Map.entry(NamedTextColor.GRAY, "7"),
            Map.entry(NamedTextColor.DARK_GRAY, "8"),
            Map.entry(NamedTextColor.BLUE, "9"),
            Map.entry(NamedTextColor.GREEN, "a"),
            Map.entry(NamedTextColor.AQUA, "b"),
            Map.entry(NamedTextColor.RED, "c"),
            Map.entry(NamedTextColor.LIGHT_PURPLE, "d"),
            Map.entry(NamedTextColor.YELLOW, "e"),
            Map.entry(NamedTextColor.WHITE, "f")
    );

    private static String mapTextColorToCode(TextColor color) {
        if (color == null) return null;
        String code = COLOR_TO_CODE.get(color);
        if (code != null) return code;

        int closestDistance = Integer.MAX_VALUE;
        String closestCode = null;
        for (Map.Entry<TextColor, String> entry : COLOR_TO_CODE.entrySet()) {
            int distance = colorDistance(color, entry.getKey());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestCode = entry.getValue();
            }
        }
        return closestCode;
    }

    private static int colorDistance(TextColor a, TextColor b) {
        int dr = a.red() - b.red();
        int dg = a.green() - b.green();
        int db = a.blue() - b.blue();
        return dr * dr + dg * dg + db * db;
    }

    private Book buildPortalBook(String playerName, String panelUrl, long expiresAt) {
        Component title = Component.text("Midgard LoreMaker", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component author = Component.text("Midgard", NamedTextColor.DARK_GRAY);

        Component firstPage = Component.text()
                .append(Component.text("Midgard LoreMaker\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Saudacoes, " + playerName + ".\n\nSeu painel pessoal para criar e organizar livros ja esta pronto.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Abrir painel", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(panelUrl))
                        .hoverEvent(HoverEvent.showText(Component.text("Clique para abrir no navegador"))))
                .append(Component.text("\n\nSe o cliente pedir confirmacao, aceite a abertura do link.", NamedTextColor.DARK_GRAY))
                .build();

        Component secondPage = Component.text()
                .append(Component.text("Acesso seguro\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Este link carrega um token pessoal de entrada unica. Depois do primeiro acesso, o painel segue pela sessao do navegador.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Valido ate: ", NamedTextColor.BLACK))
                .append(Component.text(TOKEN_EXPIRY_FORMAT.format(Instant.ofEpochMilli(expiresAt)), NamedTextColor.DARK_AQUA))
                .append(Component.text("\n\nSe este token expirar, use /loremaker novamente para receber o proximo acesso.", NamedTextColor.BLACK))
                .build();

        return Book.book(title, author, List.of(firstPage, secondPage));
    }

    private String resolveConnectionHost(Player player) {
        InetSocketAddress virtualHost = player.getVirtualHost();
        if (virtualHost != null && virtualHost.getHostString() != null && !virtualHost.getHostString().isBlank()) {
            return virtualHost.getHostString();
        }

        return null;
    }
}
