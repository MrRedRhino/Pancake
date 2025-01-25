package org.pipeman.pancake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.beans.ConstructorProperties;
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
                .map(data -> new MinecraftServer(data.id(), data.path(), data.startCommand(), executorService))
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
                        INSERT INTO servers (id, name, path, start_command)
                        VALUES (:id, :name, :path, :start_command)
                        """)
                .bind("id", serverData.id())
                .bind("name", serverData.name())
                .bind("path", serverData.path())
                .bind("start_command", serverData.startCommand())
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

    public record ServerData(long id, String name, String path, String startCommand, List<String> modPlatformPriorities,
                             boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder) {
        @ConstructorProperties({"id", "name", "path", "start_command", "mod_platform_priorities", "show_plugin_folder", "show_mods_folder", "show_datapacks_folder"})
        public ServerData(long id, String name, String path, String startCommand, String modPlatformPriorities, boolean showPluginFolder, boolean showModsFolder, boolean showDatapacksFolder) {
            this(
                    id,
                    name,
                    path,
                    startCommand,
                    Main.deserialize(modPlatformPriorities, new TypeReference<List<String>>() {
                    }),
                    showPluginFolder,
                    showModsFolder,
                    showDatapacksFolder
            );
        }
    }
}
