package com.midgardbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada da aplicação.
 * Configura o console e delega a inicialização para {@link BotInitializer}.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Configura o console para UTF-8 para suportar emojis e acentos
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verifica se foi passado um perfil de configuração (ex: --profile test)
        String profile = null;
        for (int i = 0; i < args.length; i++) {
            if ("--profile".equals(args[i]) && i + 1 < args.length) {
                profile = args[i + 1];
                break;
            }
        }

        BotInitializer.initialize(profile);
    }
}