package org.pipeman.pancake.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.http.Context;
import org.pipeman.pancake.Main;
import org.pipeman.pancake.MinecraftServer;
import org.pipeman.pancake.ServerManager;
import org.pipeman.pancake.addons.Platform;
import org.pipeman.pancake.addons.Platforms;
import org.pipeman.pancake.loaders.Loader;

import java.util.ArrayList;
import java.util.List;
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

        List<ServerManager.ServerData> existingServers = ServerManager.getServers();
        List<Platform> platformPriorities = existingServers.stream()
                .filter(serverData -> serverData.loader() == body.loader())
                .map(ServerManager.ServerData::modPlatformPriorities)
                .findFirst()
                .or(() -> Optional.ofNullable(existingServers.isEmpty() ? null : existingServers.getFirst().modPlatformPriorities()))
                .orElse(List.of(Platforms.MODRINTH, Platforms.CURSEFORGE));

        ServerManager.addServer(new ServerManager.ServerData(id,
                body.name(),
                body.path(),
                "",
                platformPriorities,
                body.loader().supportsPlugins(),
                body.loader().supportsMods(),
                true,
                body.loader(),
                body.gameVersion(),
                null,
                null));


        ctx.json(new Server(id, body.name(), platformPriorities, body.loader().supportsPlugins(), body.loader().supportsMods(), true, body.loader(), body.gameVersion()));
    }

    public static void listSupportedGameVersionsByLoader(Context ctx) {
        Loader loader = ctx.pathParamAsClass("loader", Loader.class).get();
        ctx.json(loader.getGameVersions());
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

    private record ImportServerBody(String name, String path, Loader loader, String gameVersion) {
    }
}
