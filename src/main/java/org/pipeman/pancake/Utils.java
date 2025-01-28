package org.pipeman.pancake;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static class HashingOutputStream extends FilterOutputStream {
        private final MessageDigest digest;

        public HashingOutputStream(OutputStream out, String algorithm) {
            super(out);
            digest = Utils.getDigest(algorithm);
        }

        @Override
        public void write(int b) throws IOException {
            digest.update((byte) b);
            out.write(b);
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            digest.update(b);
            out.write(b);
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            digest.update(b, off, len);
            out.write(b, off, len);
        }

        public byte[] digest() {
            return digest.digest();
        }

        public String digestHex() {
            return bytesToHex(digest());
        }
    }

    public static byte[] getHash(InputStream stream, String algorithm) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        MessageDigest digest = Utils.getDigest(algorithm);
        while ((count = stream.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }

        return digest.digest();
    }

    public static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildQuery(Map<String, Object> query) {
        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            queryBuilder.append(queryBuilder.isEmpty() ? "?" : "&");

            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String value;
            if (entry.getValue() instanceof Collection<?> list) {
                try {
                    value = Main.objectMapper().writeValueAsString(list);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                value = String.valueOf(entry.getValue());
            }

            queryBuilder.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return queryBuilder.toString();
    }
}
