package org.pipeman.pancake.ping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PacketUtils {
    public static void writePacket(int packetID, OutputStream os, byte[] data) throws IOException {
        ByteUtils.writeVarInt(data.length + 1, os);
        os.write(packetID);
        os.write(data);
    }

    public static void writePacket(int packetID, OutputStream os, PacketDataConsumer dataProvider) throws IOException {
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        dataProvider.accept(dataStream);
        writePacket(packetID, os, dataStream.toByteArray());
    }

    @FunctionalInterface
    public interface PacketDataConsumer {
        void accept(OutputStream os) throws IOException;
    }
}
