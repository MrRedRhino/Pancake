package org.pipeman.pancake.backups;

import org.json.JSONObject;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;
import org.pipeman.pancake.Utils.HashingOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class FileManager {
    private static final Map<String, Function<JSONObject, BackupFileStorage>> storages = Map.of(
            "filesystem", FilesystemBackupStorage::new
    );

    public static void copyFile(Path path, byte[] hash, JSONObject storageConfig) throws IOException {
        String type = storageConfig.getString("storage_type");
        BackupFileStorage storage = storages.get(type).apply(storageConfig);

        FileMeta file = storage.createFile(Files.newInputStream(path));
        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO backup_file_meta (hash, storage_configuration, url)
                        VALUES (:hash, :storage_type, :url)
                        """)
                .bind("hash", hash)
                .bind("storage_type", file.storageType)
                .bind("url", file.url)
                .execute());
    }

    public abstract static class BackupFileStorage {
        protected final JSONObject config;

        public BackupFileStorage(JSONObject config) {
            this.config = config;
        }

        public abstract FileMeta createFile(InputStream data) throws IOException;

        public abstract void deleteFile(String url) throws IOException;
    }

    public static class FilesystemBackupStorage extends BackupFileStorage {
        public FilesystemBackupStorage(JSONObject config) {
            super(config);
            String path = config.getString("path");
            Path.of(path).toFile().mkdirs();
        }

        @Override
        public FileMeta createFile(InputStream data) throws IOException {
            long id = Main.generateNewId();
            Path path = Path.of(config.getString("path"), String.valueOf(id));

            String hash;
            try (HashingOutputStream os = new HashingOutputStream(Files.newOutputStream(path), "SHA-256")) {
                data.transferTo(os);
                hash = os.digestHex();
            }

            return new FileMeta(hash, "filesystem", path.toAbsolutePath().toString());
        }

        @Override
        public void deleteFile(String url) throws IOException {
            if (!new File(url).delete()) {
                throw new IOException("Failed to delete file " + url);
            }
        }
    }

    public record FileMeta(String hash, String storageType, String url) {
    }
}
