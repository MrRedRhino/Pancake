package org.pipeman.pancake;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ServerManager {
    private static final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(4);
    private static final MinecraftServer SERVER = new MinecraftServer(
            1,
            Config.mcServerDirectory(),
            Config.mcServerStartCommand(),
            executorService
    );

    public static List<MinecraftServer> getServers() {
        return List.of(SERVER);
    }

    public static MinecraftServer getServerById(long serverId) {
        return SERVER;
    }
}
