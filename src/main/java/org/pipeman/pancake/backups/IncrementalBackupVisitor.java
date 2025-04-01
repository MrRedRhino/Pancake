package org.pipeman.pancake.backups;

import org.json.JSONObject;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

class IncrementalBackupVisitor extends BackupFileVisitor {
    private final List<BackupFile> files = new ArrayList<>();
    private final JSONObject storageConfig;

    public IncrementalBackupVisitor(List<Path> ignoredDirectories, JSONObject storageConfig) {
        super(ignoredDirectories);
        this.storageConfig = storageConfig;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        byte[] hash = Utils.getHash(Files.newInputStream(file), "SHA-256");

        // TODO maybe only read the first 8k bytes and calculate the hash from that for speedup.
        //  If the hash does not exist, the file can not already exist. If the hash exists exists we have to calculate the exact hash
        boolean exists = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT exists(SELECT 1 FROM backup_file_meta WHERE hash = :hash)
                        """)
                .bind("hash", hash)
                .mapTo(Boolean.class)
                .first());

        if (!exists) {
            FileManager.copyFile(file, hash, storageConfig);
        }

        files.add(new BackupFile(hash, file, exists));
        return super.visitFile(file, attrs);
    }

    public List<BackupFile> files() {
        return files;
    }

    public record BackupFile(byte[] hash, Path path, boolean existed) {
    }
}
