package org.pipeman.pancake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.pipeman.pancake.addons.Platform;
import org.pipeman.pancake.addons.Platforms;
import org.pipeman.pancake.loaders.Loader;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ServerManager {
    private static final LoadingCache<Long, MinecraftServer> loadedServers = Caffeine.newBuilder()
            .build(ServerManager::loadServer);
    private static final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(4);

    private static MinecraftServer loadServer(long serverId) {
        return getServerData(serverId)
                .map(data -> new MinecraftServer(data.id(), executorService))
                .orElse(null);
    }

    public static List<ServerData> getServers() {
        return Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM servers
                        """)
                .mapTo(ServerData.class)
                .list());
    }

    public static Optional<ServerData> getServerData(long serverId) {
        return Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM servers
                        WHERE id = :id
                        """)
                .bind("id", serverId)
                .mapTo(ServerData.class)
                .findFirst());
    }

    public static void addServer(ServerData serverData) {
        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO servers (id, name, path, start_command, mod_platform_priorities, show_plugin_folder, show_mods_folder, show_datapacks_folder, loader, game_version, loader_version)
                        VALUES (:id, :name, :path, :start_command, :mod_platform_priorities, :show_plugin_folder, :show_mods_folder, :show_datapacks_folder, :loader, :game_version, :loader_version)
                        """)
                .bind("id", serverData.id())
                .bind("name", serverData.name())
                .bind("path", serverData.path())
                .bind("start_command", serverData.startCommand())
                .bind("mod_platform_priorities", Main.serialize(serverData.modPlatformPriorities()))
                .bind("show_plugin_folder", serverData.showPluginFolder())
                .bind("show_mods_folder", serverData.showModsFolder())
                .bind("show_datapacks_folder", serverData.showDatapacksFolder())
                .bind("loader", serverData.loader())
                .bind("game_version", serverData.gameVersion())
                .bind("loader_version", serverData.loaderVersion())
                .execute());
    }

    public static MinecraftServer getServerById(long serverId) {
        return optServerById(serverId, true).orElseThrow();
    }

    public static Optional<MinecraftServer> optServerById(long serverId, boolean loadIfNotLoaded) {
        return Optional.ofNullable(loadIfNotLoaded ? loadedServers.get(serverId) : loadedServers.getIfPresent(serverId));
    }

    public static Optional<MinecraftServer> optServerById(long serverId) {
        return optServerById(serverId, false);
    }

    public record ServerData(long id, String name, String path, String startCommand,
                             List<Platform> modPlatformPriorities,
                             boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder,
                             Loader loader, String gameVersion, String loaderVersion, RestartPolicy restartPolicy) {
        @ConstructorProperties({"id", "name", "path", "start_command", "mod_platform_priorities", "show_plugin_folder", "show_mods_folder", "show_datapacks_folder", "loader", "game_version", "loader_version"})
        public ServerData(long id, String name, String path, String startCommand, String modPlatformPriorities, boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder, String loader, String gameVersion, String loaderVersion) {
            this(
                    id,
                    name,
                    path,
                    startCommand,
                    deserializePlatforms(modPlatformPriorities),
                    showPluginFolder,
                    showModsFolder,
                    showDatapacksFolder,
                    Loader.valueOf(loader),
                    gameVersion,
                    loaderVersion,
                    new RestartPolicy(3, 3, false)
            );
        }

        private static List<Platform> deserializePlatforms(String s) {
            Platform[] priorities = Main.deserialize(s, new TypeReference<List<String>>() {
                    })
                    .stream()
                    .map(Platforms::fromString)
                    .toArray(Platform[]::new);
            ArrayList<Platform> list = new ArrayList<>(List.of(priorities));
            for (Platforms platform : Platforms.values()) {
                if (!list.contains(platform)) list.add(platform);
            }
            return list;
        }
    }
}
