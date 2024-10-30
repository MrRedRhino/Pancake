package org.pipeman.pancake.ping;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerPing {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPing.class);

    public static PingResponse pingServer(String address, int port) {
        try (Socket socket = new Socket(address, port)) {
            OutputStream socketOS = socket.getOutputStream();

            // handshake packet
            PacketUtils.writePacket(0x00, socketOS, os -> {
                ByteUtils.writeVarInt(-1, os);
                ByteUtils.writeString(address, os);
                ByteUtils.writeShort(port, os);
                ByteUtils.writeVarInt(1, os);
            });

            // ping request
            PacketUtils.writePacket(0x00, socketOS, new byte[0]);

            // read response
            DataInputStream in = new DataInputStream(socket.getInputStream());
            ByteUtils.readVarInt(in);
            in.readByte();
            JSONObject data = new JSONObject(new String(in.readNBytes(ByteUtils.readVarInt(in))));

            return PingResponse.of(data);
        } catch (Exception e) {
             LOGGER.error("Failed to ping server", e);
        }
        return null;
    }

    public record PingResponse(int maxPlayers, int playerCount, List<String> playerNames) {
        private static PingResponse of(JSONObject data) {
            JSONObject players = data.getJSONObject("players");
            int maxCount = players.getInt("max");
            int online = players.getInt("online");

            List<String> names = new ArrayList<>();
            for (Object sample : players.optJSONArray("sample", new JSONArray())) {
                names.add(((JSONObject) sample).getString("name"));
            }
            return new PingResponse(maxCount, online, names);
        }
    }
}
