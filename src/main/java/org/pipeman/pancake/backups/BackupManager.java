package org.pipeman.pancake.backups;

import org.jdbi.v3.core.statement.PreparedBatch;
import org.json.JSONObject;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;

public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    public static void createBackup(long serverId, Path directory, String name, List<Path> ignoredDirectories) throws IOException {
        long backupId = Main.generateNewId();
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());
        JSONObject storageConfig = new JSONObject()
                .put("storage_type", "filesystem")
                .put("path", "backups");

        LOGGER.info("Creating backup of {}", directory);
        CountingFileVisitor counter = new CountingFileVisitor(ignoredDirectories);
        Files.walkFileTree(directory, counter);
        LOGGER.info("Backing up {} files", counter.totalCount());

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO backups (id, server_id, name, created_at, directory, incremental, completed)
                        VALUES (:id, :server_id, :name, :created_at, :directory, true, false)
                        """)
                .bind("id", backupId)
                .bind("server_id", serverId)
                .bind("name", name)
                .bind("created_at", createdAt)
                .bind("directory", directory.toString())
                .execute());

        IncrementalBackupVisitor visitor2 = new IncrementalBackupVisitor(ignoredDirectories, storageConfig);
        Files.walkFileTree(directory, visitor2);
        List<IncrementalBackupVisitor.BackupFile> files = visitor2.files();

        LOGGER.info("Copied {} files", files.stream().filter(f -> !f.existed()).count());
        LOGGER.info("Associating {} files with backup", files.size());
        Database.getJdbi().useHandle(h -> {
            PreparedBatch batch = h.prepareBatch("""
                    INSERT INTO backup_files (backup_id, hash, path)
                    VALUES (:backup_id, :hash, :path)
                    """);

            for (IncrementalBackupVisitor.BackupFile file : files) {
                batch.bind("backup_id", backupId)
                        .bind("hash", file.hash())
                        .bind("path", file.path().toString())
                        .add();
            }

            batch.execute();
        });

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        UPDATE backups
                        SET completed = true
                        WHERE id = :id
                        """)
                .bind("id", backupId)
                .execute());
    }

    public static void main(String[] args) throws IOException {
        // with computing hash: 00:53:20 - 56:47 -> 3 min for 160 gb
        // with db query: 01:05:50 - 01:09:50 -> 4 min for 160 gb


        createBackup(7282177237674102784L, Path.of("/home/pipeman/Desktop/servers/tournament/tournament-dev/world"), "Unnamed Backup", List.of());
    }
}
