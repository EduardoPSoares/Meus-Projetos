package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DatabaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DatabaseCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public String getDescription() {
        return "Gerencia o banco de dados (Exportar/Importar)";
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
            new SubcommandData("export", "Exporta o banco de dados atual para um arquivo SQL"),
            new SubcommandData("import", "Importa um arquivo SQL para o banco de dados atual")
                .addOption(OptionType.ATTACHMENT, "file", "O arquivo .sql para importar", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_DATABASE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
             event.reply("⛔ Acesso somente para administradores.").setEphemeral(true).queue();
             return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        switch (subcommand) {
            case "export":
                handleExport(event);
                break;
            case "import":
                handleImport(event);
                break;
        }
    }

    private void handleExport(SlashCommandInteractionEvent event) {
        event.reply("⏳ Iniciando exportação do banco de dados...").setEphemeral(true).queue();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) {
                event.getHook().sendMessage("❌ Erro ao conectar ao banco de dados.").queue();
                return;
            }

            StringBuilder sqlDump = new StringBuilder();
            sqlDump.append("-- MidgardBot Database Dump\n");
            sqlDump.append("-- Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");

            // Tables to dump
            String[] tables = {"midgard_whitelist", "midgard_streamers", "midgard_tickets", "midgard_links"};

            for (String table : tables) {
                try {
                    dumpTable(conn, table, sqlDump);
                } catch (Exception e) {
                    sqlDump.append("-- Error dumping table ").append(table).append(": ").append(e.getMessage()).append("\n");
                }
            }

            File file = new File("database_backup_" + System.currentTimeMillis() + ".sql");
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(sqlDump.toString());
            }

            event.getHook().sendFiles(FileUpload.fromData(file)).queue(msg -> {
                file.delete(); // Cleanup
            });

        } catch (Exception e) {
            event.getHook().sendMessage("❌ Erro grave ao exportar: " + e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private void dumpTable(Connection conn, String tableName, StringBuilder sb) throws SQLException {
        sb.append("-- Structure for table ").append(tableName).append("\n");
        // We do strict DELETE FROM to clear old data before import, avoiding duplicates.
        sb.append("DELETE FROM ").append(tableName).append(";\n");

        sb.append("-- Data for table ").append(tableName).append("\n");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            while (rs.next()) {
                sb.append("INSERT INTO ").append(tableName).append(" (");
                for (int i = 1; i <= columnCount; i++) {
                    sb.append(meta.getColumnName(i));
                    if (i < columnCount) sb.append(", ");
                }
                sb.append(") VALUES (");
                
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        sb.append("NULL");
                    } else if (value instanceof Number) {
                        sb.append(value);
                    } else if (value instanceof Boolean) {
                         sb.append(((Boolean) value) ? 1 : 0);
                    } else {
                        // Escape single quotes for SQL
                        String valStr = value.toString().replace("'", "''").replace("\\", "\\\\");
                        sb.append("'").append(valStr).append("'");
                    }
                    if (i < columnCount) sb.append(", ");
                }
                sb.append(");\n");
            }
        }
        sb.append("\n");
    }

    private void handleImport(SlashCommandInteractionEvent event) {
        event.reply("⏳ Baixando e processando arquivo SQL...").setEphemeral(true).queue();
        
        var attachment = event.getOption("file").getAsAttachment();
        String fileExtension = attachment.getFileExtension();
        
        if (fileExtension == null || !fileExtension.equalsIgnoreCase("sql")) {
            event.getHook().sendMessage("❌ Por favor, envie um arquivo .sql válido.").queue();
            return;
        }

        attachment.getProxy().download().thenAccept(inputStream -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 Connection conn = DatabaseManager.getConnection()) {
                
                if (conn == null) {
                    event.getHook().sendMessage("❌ Erro ao conectar ao banco de dados.").queue();
                    return;
                }

                conn.setAutoCommit(false); // Transaction
                
                StringBuilder sqlStatement = new StringBuilder();
                String line;
                int count = 0;
                
                try (Statement stmt = conn.createStatement()) {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("--")) continue;
                        
                        sqlStatement.append(line);
                        // Checks for semicolon at the end of the line
                        if (line.endsWith(";")) {
                            String sql = sqlStatement.toString();
                            // Remove ; if needed (some drivers dont like it in execute(), but most accept it if it is a single statement)
                            // We will remove it to be safe
                            if (sql.endsWith(";")) {
                                sql = sql.substring(0, sql.length() - 1);
                            }
                            
                            try {
                                stmt.execute(sql);
                                count++;
                            } catch (SQLException ex) {
                                // Log but allows continue? No, rollback is safer.
                                throw ex;
                            }
                            
                            sqlStatement.setLength(0);
                        } else {
                            sqlStatement.append(" ");
                        }
                    }
                    conn.commit();
                    event.getHook().sendMessage("✅ Importação concluída com sucesso! " + count + " instruções executadas.").queue();
                } catch (Exception e) {
                    conn.rollback();
                    event.getHook().sendMessage("❌ Erro durante a importação (Rollback realizado). \nErro: " + e.getMessage()).queue();
                    e.printStackTrace();
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (Exception e) {
                event.getHook().sendMessage("❌ Erro ao ler arquivo ou conectar invocado: " + e.getMessage()).queue();
            }
        });
    }
}
