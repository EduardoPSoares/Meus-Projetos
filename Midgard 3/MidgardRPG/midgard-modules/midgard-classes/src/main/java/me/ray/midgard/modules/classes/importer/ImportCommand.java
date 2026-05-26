package me.ray.midgard.modules.classes.importer;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.classes.ClassesModule;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comando para importar pacotes externos (MMOCore, etc).
 * 
 * Uso: /midgard import <tipo> <pasta>
 * Exemplo: /midgard import mmocore archer_pack
 */
public class ImportCommand extends MidgardCommand {

    private final ClassesModule module;

    public ImportCommand(ClassesModule module) {
        super("import", "midgard.admin", false);
        this.module = module;
    }

    private String msg(String key) {
        return module.getMessage("import." + key);
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.import_description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.import_usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, msg("usage"));
            MessageUtils.send(sender, msg("available_types"));
            MessageUtils.send(sender, msg("folder_hint"));
            return;
        }

        String type = args[0].toLowerCase();
        String folderName = args[1];

        File importsFolder = new File(module.getPlugin().getDataFolder(), "imports");
        File targetFolder = new File(importsFolder, folderName);

        // Validate path is within imports folder to prevent directory traversal
        try {
            if (!targetFolder.getCanonicalPath().startsWith(importsFolder.getCanonicalPath())) {
                MessageUtils.send(sender, msg("invalid_path"));
                return;
            }
        } catch (java.io.IOException e) {
            MessageUtils.send(sender, msg("path_error"));
            return;
        }

        if (!targetFolder.exists()) {
            MessageUtils.send(sender, msg("folder_not_found").replace("%path%", targetFolder.getPath()));
            MessageUtils.send(sender, msg("folder_hint_specific").replace("%folder%", folderName));
            return;
        }

        switch (type) {
            case "mmocore":
                importMMOCore(sender, targetFolder);
                break;
            default:
                MessageUtils.send(sender, msg("unknown_type").replace("%type%", type));
                MessageUtils.send(sender, msg("available_types"));
                break;
        }
    }

    private void importMMOCore(CommandSender sender, File packageFolder) {
        MessageUtils.send(sender, msg("starting"));
        
        MMOCoreImporter importer = new MMOCoreImporter(module.getPlugin());
        MMOCoreImporter.ImportResult result = importer.importPackage(packageFolder);

        if (result.success) {
            MessageUtils.send(sender, msg("success").replace("%message%", result.message));
            
            if (!result.warnings.isEmpty()) {
                MessageUtils.send(sender, msg("warnings_title"));
                int maxWarnings = Math.min(result.warnings.size(), 10);
                for (int i = 0; i < maxWarnings; i++) {
                    MessageUtils.send(sender, msg("warning_item").replace("%warning%", result.warnings.get(i)));
                }
                if (result.warnings.size() > 10) {
                    MessageUtils.send(sender, msg("warnings_more").replace("%count%", String.valueOf(result.warnings.size() - 10)));
                }
            }
            
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, msg("files_title"));
            MessageUtils.send(sender, msg("files_classes"));
            MessageUtils.send(sender, msg("files_spells"));
            MessageUtils.send(sender, msg("files_items"));
            
            if (result.mythicSkillsCopied > 0) {
                MessageUtils.send(sender, msg("files_mythic"));
            }
            
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, msg("reload_hint"));
            MessageUtils.send(sender, msg("reload_mythic_hint"));
        } else {
            MessageUtils.send(sender, msg("failed").replace("%message%", result.message));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("mmocore"), args[0]);
        }
        
        if (args.length == 2) {
            // Listar pastas em imports/
            File importsFolder = new File(module.getPlugin().getDataFolder(), "imports");
            if (importsFolder.exists()) {
                File[] folders = importsFolder.listFiles(File::isDirectory);
                if (folders != null) {
                    List<String> names = new ArrayList<>();
                    for (File folder : folders) {
                        names.add(folder.getName());
                    }
                    return filterStartsWith(names, args[1]);
                }
            }
        }
        
        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
