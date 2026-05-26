package com.midgardbot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente RCON (Source RCON Protocol) simples para enviar comandos ao servidor Minecraft.
 */
public class RconClient implements AutoCloseable {

    public static final int SERVERDATA_EXECCOMMAND = 2;
    public static final int SERVERDATA_AUTH = 3;

    private final String host;
    private final int port;
    private final String password;
    private Socket socket;
    private final AtomicInteger requestId = new AtomicInteger(1);

    public RconClient(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void connect() throws IOException {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(5000);
            
            // Autenticar
            if (!authenticate()) {
                throw new IOException("Falha na autenticação RCON (Senha incorreta?)");
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private boolean authenticate() throws IOException {
        int id = requestId.getAndIncrement();
        send(id, SERVERDATA_AUTH, password);
        // Ler resposta
        // Protocolo diz que se falhar, retorna ID sentinela -1
        // Pode vir múltiplos pacotes, mas normalmente auth é 1-1
        RconPacket response = receive();
        return response.getId() == id;
    }

    public String sendCommand(String command) throws IOException {
        int id = requestId.getAndIncrement();
        send(id, SERVERDATA_EXECCOMMAND, command);
        
        RconPacket response = receive();
        if (response.getId() != id) {
            // Pode acontecer desincronia se ler pacote errado, mas para uso simples ok
            // Em implementações robustas, leríamos até achar o ID.
        }
        return response.getBody();
    }

    private void send(int id, int type, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + bodyBytes.length + 2; // ID(4) + Type(4) + Body + 2 Nulls

        ByteBuffer buffer = ByteBuffer.allocate(length + 4); // + Length(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(bodyBytes);
        buffer.put((byte) 0); // Null term body
        buffer.put((byte) 0); // Null term packet

        OutputStream out = socket.getOutputStream();
        out.write(buffer.array());
        out.flush();
    }

    private RconPacket receive() throws IOException {
        InputStream in = socket.getInputStream();
        byte[] lengthBytes = new byte[4];
        if (readFully(in, lengthBytes) < 4) {
            throw new IOException("Fim do stream ao ler tamanho do pacote RCON");
        }

        int length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length < 10 || length > 4110) { // Min: 10, Max: 4096 + overhead
             throw new IOException("Pacote RCON inválido (tamanho: " + length + ")");
        }

        byte[] payload = new byte[length];
        readFully(in, payload);

        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        int id = buffer.getInt();
        int type = buffer.getInt();
        
        // O corpo vai até length - 8 (ID+Type) - 2 (Nulls)
        int bodyLength = length - 8 - 2;
        byte[] bodyBytes = new byte[bodyLength];
        buffer.get(bodyBytes);
        
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        return new RconPacket(id, type, body);
    }

    private int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read == -1) break;
            total += read;
        }
        return total;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static class RconPacket {
        private final int id;
        private final int type;
        private final String body;

        public RconPacket(int id, int type, String body) {
            this.id = id;
            this.type = type;
            this.body = body;
        }

        public int getId() { return id; }
        public int getType() { return type; }
        public String getBody() { return body; }
    }
}
