package org.pipeman.pancake.rest;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.NotFoundResponse;
import org.pipeman.pancake.ServerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class LogsApi {
    public static void listLogs(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.ServerData server = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));

        File[] nullableFiles = new File(server.path(), "logs").listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".log.gz"));
        List<File> files = nullableFiles == null ? List.of() : List.of(nullableFiles);

        ctx.json(files.stream()
                .sorted(Comparator.comparing(File::lastModified).reversed()) // TODO sort by filename
                .map(LogsApi::getLogname)
                .toList());
    }

    public static void getLogContent(Context ctx) throws IOException {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        String logName = ctx.pathParamAsClass("log-name", String.class).get();

        ServerManager.ServerData server = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        String[] files = new File(server.path(), "logs").list((dir, name) -> getLogname(new File(dir, name)).equals(logName));
        if (files == null || files.length == 0) throw new NotFoundResponse("Log not found");

        try (InputStream stream = Files.newInputStream(Path.of(server.path(), "logs", files[0]))) {
            if (files[0].endsWith(".gz")) {
                ctx.header(Header.CONTENT_ENCODING, "gzip");
            }
            stream.transferTo(ctx.outputStream());
        }
    }

    private static String getLogname(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        }

        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? fileName : fileName.substring(0, lastDot); // TODO unnecessary cases since files are filtered beforehand
    }
}
