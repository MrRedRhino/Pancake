package org.pipeman.pancake.utils;

public class CurseforgeFingerprint {
    private static boolean isWhitespaceCharacter(byte b) {
        return b == 9 || b == 10 || b == 13 || b == 32;
    }

    private static int safeMul32(int a, int b) {
        return ((a & 0xffff) * b) + ((((a >>> 16) * b) & 0xffff) << 16);
    }

    public static long computeHash(byte[] data) {
        int len = data.length;
        final int magic1 = 0x5bd1e995;
        int seed = 1;
        int h = seed ^ len;

        int j = 0;
        int k;

        // Process 4 bytes at a time
        while (len >= 4) {
            k = data[j] & 0xFF; // Extract byte and treat as unsigned
            k |= (data[j + 1] & 0xFF) << 8;
            k |= (data[j + 2] & 0xFF) << 16;
            k |= (data[j + 3] & 0xFF) << 24;

            k = safeMul32(k, magic1);
            k ^= k >>> 24;
            k = safeMul32(k, magic1);

            h = safeMul32(h, magic1) ^ k;

            j += 4;
            len -= 4;
        }

        // Process the remaining bytes
        switch (len) {
            case 3:
                h ^= (data[j + 2] & 0xFF) << 16;
            case 2:
                h ^= (data[j + 1] & 0xFF) << 8;
            case 1:
                h ^= (data[j] & 0xFF);
                h = safeMul32(h, magic1);
        }

        h ^= h >>> 13;
        h = safeMul32(h, magic1);
        h ^= h >>> 15;

        // Return as an unsigned 32-bit integer
        return h & 0xFFFFFFFFL;
    }

    public static byte[] filterWhitespace(byte[] buffer) {
        byte[] newArray = new byte[buffer.length];
        int newLength = 0;

        for (byte b : buffer) {
            if (!isWhitespaceCharacter(b)) {
                newArray[newLength++] = b;
            }
        }
        byte[] result = new byte[newLength];
        System.arraycopy(newArray, 0, result, 0, newLength);
        return result;
    }
}
