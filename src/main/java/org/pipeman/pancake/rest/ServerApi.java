package org.pipeman.pancake.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.http.Context;
import org.pipeman.pancake.Main;
import org.pipeman.pancake.MinecraftServer;
import org.pipeman.pancake.ServerManager;
import org.pipeman.pancake.addons.Platform;
import org.pipeman.pancake.loaders.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServerApi {
    public static void getServers(Context ctx) {
        List<Server> servers = new ArrayList<>();
        for (ServerManager.ServerData server : ServerManager.getServers()) {
            servers.add(new Server(server.id(), server.name(), server.modPlatformPriorities(), server.showPluginFolder(), server.showModsFolder(), server.showDatapacksFolder(), server.loader(), server.gameVersion()));
        }
        ctx.json(servers);
    }

    public static void start(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).start();
    }

    public static void stop(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).stop();
    }

    public static void terminate(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).terminate();
    }

    public static void sendCommand(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        SendCommandPayload payload = ctx.bodyValidator(SendCommandPayload.class)
                .check(p -> !p.command().isBlank(), "Command empty")
                .get();

        ServerManager.getServerById(serverId).sendCommand(payload.command());
    }

    private record SendCommandPayload(@JsonProperty(required = true) String command) {
    }

    public static void importServer(Context ctx) {
        long id = Main.generateNewId();
        ImportServerBody body = ctx.bodyAsClass(ImportServerBody.class);
        ServerManager.addServer(new ServerManager.ServerData(id, body.name(), body.path(), body.startCommand(), List.of(), true, true, true, Loader.FABRIC, "1.20.1", "1.16.9")); // TODO adapt server software
        ctx.json(Map.of("id", id));
    }

    private record Server(long id, String name, MinecraftServer.State state, long startedAt,
                          List<Platform> modPlatformPriorities, boolean showPluginFolder, boolean showModsFolder,
                          boolean showDatapacksFolder, Loader loader, String gameVersion) {
        public Server(long id, String name, List<Platform> modPlatformPriorities, boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder, Loader loader, String gameVersion) {
            this(id, name, ServerManager.optServerById(id), modPlatformPriorities, showPluginFolder, showModsFolder, showDatapacksFolder, loader, gameVersion);
        }

        public Server(long id, String name, Optional<MinecraftServer> server, List<Platform> modPlatformPriorities, boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder, Loader loader, String gameVersion) {
            this(id,
                    name,
                    server.map(MinecraftServer::state).orElse(MinecraftServer.State.STOPPED),
                    server.map(MinecraftServer::startedAt).orElse(0L),
                    modPlatformPriorities,
                    showPluginFolder,
                    showModsFolder,
                    showDatapacksFolder,
                    loader,
                    gameVersion
            );
        }
    }

    private record ImportServerBody(String name, String path, String startCommand) {
    }
}
