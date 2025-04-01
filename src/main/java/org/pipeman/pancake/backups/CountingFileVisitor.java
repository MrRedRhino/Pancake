package org.pipeman.pancake.backups;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

class CountingFileVisitor extends BackupFileVisitor {
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
