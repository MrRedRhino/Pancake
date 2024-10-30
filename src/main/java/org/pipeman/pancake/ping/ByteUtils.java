package org.pipeman.pancake.ping;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteUtils {
    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public static void writeVarInt(int value, OutputStream os) throws IOException {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                os.write(value);
                return;
            }

            os.write((value & SEGMENT_BITS) | CONTINUE_BIT);
            value >>>= 7;
        }
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }

        return i;
    }

    public static void writeString(String s, OutputStream os) throws IOException {
        writeVarInt(s.length(), os);
        os.write(s.getBytes());
    }

    public static void writeShort(int i, OutputStream os) throws IOException {
        os.write(i >> 8);
        os.write(i);
    }
}
