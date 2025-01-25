package org.pipeman.pancake.backups;

import org.jdbi.v3.core.statement.PreparedBatch;
import org.json.JSONObject;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;
import org.pipeman.pancake.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BackupManager {
    public static void createBackup(long serverId, Path directory, List<Path> ignoredDirectories) throws IOException {
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());
        JSONObject storageConfig = new JSONObject()
                .put("storage_type", "filesystem")
                .put("path", "backups");

        CountingFileVisitor counter = new CountingFileVisitor(ignoredDirectories);
        Files.walkFileTree(directory, counter);

        Set<String> existingHashes = FileManager.getExistingHashes();

        IncrementalBackupVisitor visitor2 = new IncrementalBackupVisitor(ignoredDirectories, existingHashes, storageConfig);
        Files.walkFileTree(directory, visitor2);
        List<IncrementalBackupVisitor.BackupFile> files = visitor2.files();

        long backupId = Main.generateNewId();
        Database.getJdbi().useHandle(h -> {
            PreparedBatch batch = h.prepareBatch("""
                    INSERT INTO backup_files (backup_id, hash, path)
                    VALUES (:backup_id, :hash, :path)
                    """);

            for (IncrementalBackupVisitor.BackupFile file : files) {
                batch.bind("backup_id", backupId)
                        .bind("passwordHash", file.hash)
                        .bind("path", file.path.toString())
                        .add();
            }

            batch.execute();
        });

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO backups (id, server_id, created_at, directory)
                        VALUES (:id, :server_id, :created_at, :directory)
                        """)
                .bind("id", backupId)
                .bind("server_id", serverId)
                .bind("created_at", createdAt)
                .bind("directory", directory.toString())
                .execute());
    }

    private static class IncrementalBackupVisitor extends BackupFileVisitor {
        private final List<BackupFile> files = new ArrayList<>();
        private final Set<String> existingHashes;
        private final JSONObject storageConfig;

        public IncrementalBackupVisitor(List<Path> ignoredDirectories, Set<String> existingHashes, JSONObject storageConfig) {
            super(ignoredDirectories);
            this.existingHashes = existingHashes;
            this.storageConfig = storageConfig;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            byte[] sha256 = Utils.getSha256(Files.newInputStream(file));
            String hash = Utils.bytesToHex(sha256);
            if (!existingHashes.contains(hash)) {
                FileManager.copyFile(file, storageConfig);
            }

            files.add(new BackupFile(hash, file));
            return super.visitFile(file, attrs);
        }

        public List<BackupFile> files() {
            return files;
        }

        private record BackupFile(String hash, Path path) {
        }
    }

    private static class BackupFileVisitor extends SimpleFileVisitor<Path> {
        private static final Logger LOGGER = LoggerFactory.getLogger(BackupFileVisitor.class);
        private final List<Path> ignoredDirectories;

        public BackupFileVisitor(List<Path> ignoredDirectories) {
            this.ignoredDirectories = ignoredDirectories;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            for (Path ignoredDirectory : ignoredDirectories) {
                if (dir.endsWith(ignoredDirectory)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            LOGGER.error("Error visiting file", exc);
            return FileVisitResult.CONTINUE;
        }
    }

    private static class CountingFileVisitor extends BackupFileVisitor {
        private int totalCount = 0;
        private long totalSize = 0;

        public CountingFileVisitor(List<Path> ignoredDirectories) {
            super(ignoredDirectories);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            totalSize += attrs.size();
            totalCount++;
            return super.visitFile(file, attrs);
        }

        public long totalSize() {
            return totalSize;
        }

        public int totalCount() {
            return totalCount;
        }
    }
}
