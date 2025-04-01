package org.pipeman.pancake.backups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

class BackupFileVisitor extends SimpleFileVisitor<Path> {
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
