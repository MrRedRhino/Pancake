package org.pipeman.pancake;

import io.javalin.Javalin;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Main {
    public static void main(String[] args) throws IOException {
        Jdbi jdbi = Jdbi.create("jdbc:h2:./pancake");
        try (InputStream stream = Main.class.getResourceAsStream("/setup.sql")) {
            String setupScript = new String(Objects.requireNonNull(stream).readAllBytes());
            jdbi.useHandle(h -> h.createScript(setupScript).execute());
        }

        MinecraftServer server = new MinecraftServer(Config.mcServerDirectory(), Config.mcServerStartCommand());
        server.start();

        Javalin.create((c -> {
            c.showJavalinBanner = false;

            c.router.apiBuilder(() -> {
                path("api", () -> {
                    get("command", ctx -> {
                        server.outputWriter().write("stop\n");
                        server.outputWriter().write(9);
                        server.outputWriter().flush();
                    });
                });
            });
        })).start(Config.serverHost(), Config.serverPort());
    }
}