package com.midgardbot.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Utilitário para pingar servidores de Minecraft.
 * Implementa o protocolo Server List Ping (SLP) para obter status (MOTD, jogadores) sem entrar no servidor.
 */
public class MinecraftPing {

    public static class ServerInfo {
        public String motd;
        public int onlinePlayers;
        public int maxPlayers;
        public boolean isOnline;

        public ServerInfo(String motd, int onlinePlayers, int maxPlayers, boolean isOnline) {
            this.motd = motd;
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
            this.isOnline = isOnline;
        }
    }

    public static ServerInfo getPing(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(2000);
            socket.connect(new InetSocketAddress(ip, port), 2000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Handshake
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            handshake.writeByte(0x00); // Packet ID
            writeVarInt(handshake, 47); // Protocol Version (1.8+)
            writeString(handshake, ip);
            handshake.writeShort(port);
            writeVarInt(handshake, 1); // Next State (1 = Status)

            writeVarInt(out, handshakeBytes.size());
            out.write(handshakeBytes.toByteArray());

            // Request
            out.writeByte(0x01); // Size
            out.writeByte(0x00); // Packet ID

            // Response
            readVarInt(in); // Packet Size
            int packetId = readVarInt(in);

            if (packetId != 0x00) {
                throw new IOException("Invalid packet ID");
            }

            int jsonLength = readVarInt(in);
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            String motd = "";
            int online = 0;
            int max = 0;

            if (jsonObject.has("description")) {
                if (jsonObject.get("description").isJsonObject()) {
                    JsonObject desc = jsonObject.get("description").getAsJsonObject();
                    if (desc.has("text")) {
                        motd = desc.get("text").getAsString();
                    }
                } else if (jsonObject.get("description").isJsonPrimitive()) {
                    motd = jsonObject.get("description").getAsString();
                }
            }

            if (jsonObject.has("players")) {
                JsonObject players = jsonObject.get("players").getAsJsonObject();
                if (players.has("online")) online = players.get("online").getAsInt();
                if (players.has("max")) max = players.get("max").getAsInt();
            }

            return new ServerInfo(motd, online, max, true);

        } catch (Exception e) {
            return new ServerInfo(null, 0, 0, false);
        }
    }

    public static String getMotd(String ip, int port) {
        ServerInfo info = getPing(ip, port);
        return info.isOnline ? info.motd : null;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }
}
