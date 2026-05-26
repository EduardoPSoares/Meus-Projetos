package me.ray.midgard.loader;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.ConsoleUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceInstaller {

    private final JavaPlugin plugin;
    // Pastas que queremos garantir que sejam extraídas do JAR
    private final String[] targetFolders = {"modules", "messages", "examples"};

    public ResourceInstaller(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void install() {
        try {
            installFromJar();
        } catch (Exception e) {
            MidgardLogger.error("Falha ao instalar recursos padrão (arquivos não copiados)", e);
        }
    }

    private void installFromJar() throws Exception {
        URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        JarFile jarFile = null;
        
        try {
            // Converte URL para File para lidar corretamente com espaços e caminhos
            File jarFileOnDisk = new File(url.toURI());
            jarFile = new JarFile(jarFileOnDisk);
            
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Enumeration<JarEntry> entries = jarFile.entries();
            int count = 0;
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // Verifica se o arquivo está em uma das pastas alvo
                boolean shouldExtract = false;
                for (String target : targetFolders) {
                    if (name.startsWith(target + "/")) {
                        shouldExtract = true;
                        break;
                    }
                }
                
                if (shouldExtract) {
                    File outFile = new File(plugin.getDataFolder(), name);
                    
                    if (entry.isDirectory()) {
                        if (!outFile.exists()) {
                            outFile.mkdirs();
                        }
                    } else {
                        // Lógica de atualização inteligente:
                        // 1. Se não existe, cria.
                        // 2. Se existe e tem tamanho 0, sobrescreve.
                        // 3. Se é um arquivo essencial de mensagens e queremos garantir integridade, podemos verificar (mas cuidado com configs de user)
                        
                        boolean shouldWrite = !outFile.exists() || outFile.length() == 0;
                        
                        if (shouldWrite) {
                            if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                                outFile.getParentFile().mkdirs();
                            }
                            
                            try (InputStream in = jarFile.getInputStream(entry);
                                 OutputStream out = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = in.read(buffer)) > 0) {
                                    out.write(buffer, 0, len);
                                }
                            }
                            count++;
                        }
                    }
                }
            }
            
            if (count > 0) {
                ConsoleUtils.success("MidgardLoader: Instalados/Restaurados " + count + " recursos.");
            } else {
                ConsoleUtils.info("MidgardLoader: Recursos verificados, nada a instalar.");
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }
}
