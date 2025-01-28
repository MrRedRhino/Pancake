package org.pipeman.pancake.rest;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import org.pipeman.pancake.ServerManager;
import org.pipeman.pancake.addons.AddonManager;
import org.pipeman.pancake.addons.AddonManager.AddonMeta;
import org.pipeman.pancake.addons.AddonManager.AddonMetaWithFile;
import org.pipeman.pancake.addons.Platform;
import org.pipeman.pancake.addons.Platforms;
import org.pipeman.pancake.loaders.Loader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilesApi {
    public static void listServerDirectories(Context ctx) {
        String path = ctx.queryParam("path");
        File[] files = new File(path == null ? "" : path).getAbsoluteFile().listFiles(f -> f.isDirectory() && !f.isHidden());

        List<ListServerDirectoriesEntry> result = new ArrayList<>();
        for (File file : files == null ? new File[0] : files) {
            boolean isServer = new File(file.getPath(), "server.properties").exists();
            result.add(new ListServerDirectoriesEntry(file.getAbsolutePath(), isServer));
        }
        ctx.json(result);
    }

    public static void searchMods(Context ctx) {
        String query = ctx.queryParamAsClass("query", String.class).get();
        Platforms platform = ctx.queryParamAsClass("platform", Platforms.class).get();
        Loader loader = ctx.queryParamAsClass("loader", Loader.class).get();
        String gameVersion = ctx.queryParamAsClass("gameVersion", String.class).get();

        ctx.json(platform.search(query, Platform.ContentType.MOD, loader, Map.of(Platform.FilterKey.GAME_VERSION, gameVersion)));
    }

    public static void listAddons(Context ctx) throws IOException {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        Platform.ContentType type = getContentType(ctx);

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        File[] files = new File(serverData.path(), type.folderName()).listFiles(path -> !path.isDirectory() && path.length() > 0);
        if (files == null || files.length == 0) {
            ctx.json(List.of());
            return;
        }

        List<AddonMetaWithFile> meta = AddonManager.listAddons(List.of(files), serverData.loader(), serverData.gameVersion(), type);
        ctx.json(aggregateAddonData(meta, serverData.modPlatformPriorities()));
    }

    public static void setEnabled(Context ctx, boolean enable) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        String fileName = ctx.pathParamAsClass("file", String.class).get();
        Platform.ContentType type = getContentType(ctx);

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        Path containingPath = Path.of(serverData.path(), type.folderName());

        File fileWithDisabled = containingPath.resolve(fileName + ".disabled").toFile();
        Path filePath = containingPath.resolve(fileName);
        File file = filePath.toFile();
        if (!filePath.startsWith(containingPath) || !(file.exists() || fileWithDisabled.exists()))
            throw new NotFoundResponse();

        boolean disabled = fileWithDisabled.exists();
        if (enable && disabled) {
            fileWithDisabled.renameTo(file);
        } else if (!enable && !disabled) {
            file.renameTo(fileWithDisabled);
        }
    }

    public static void upload(Context ctx) throws IOException {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        String path = ctx.queryParamAsClass("path", String.class).get();

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        Path containingPath = Path.of(serverData.path());
        Path filePath = containingPath.resolve(path);
        if (!filePath.startsWith(containingPath)) throw new ForbiddenResponse();

        File file = filePath.toFile();
        if (file.exists()) throw new BadRequestResponse("File already exists");
        file.getParentFile().mkdirs();

        try (OutputStream stream = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
            ctx.bodyInputStream().transferTo(stream);
        }
    }

    public static void uploadAddon(Context ctx) throws IOException {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        String name = ctx.queryParamAsClass("name", String.class).get();
        Platform.ContentType type = getContentType(ctx);

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        Path containingPath = Path.of(serverData.path()).resolve(type.folderName());
        Path filePath = containingPath.resolve(name);
        if (!filePath.getParent().equals(containingPath)) throw new BadRequestResponse("Filename contains directory");

        File file = filePath.toFile();
        if (file.exists()) throw new BadRequestResponse("File already exists");
        file.getParentFile().mkdirs();


        try (OutputStream stream = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
            ctx.bodyInputStream().transferTo(stream);
        }
        List<AddonMetaWithFile> meta = AddonManager.listAddons(List.of(filePath.toFile()), serverData.loader(), serverData.gameVersion(), type);
        ctx.json(aggregateAddonData(meta, serverData.modPlatformPriorities()).getFirst());
    }

    public static void deleteFile(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        String path = ctx.queryParamAsClass("path", String.class).get();

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        Path containingPath = Path.of(serverData.path());
        Path filePath = containingPath.resolve(path);
        if (!filePath.startsWith(containingPath)) throw new ForbiddenResponse();

        filePath.toFile().delete();
    }

    private static Platform.ContentType getContentType(Context ctx) {
        return switch (ctx.pathParamAsClass("type", String.class).getOrDefault("")) {
            case "mods" -> Platform.ContentType.MOD;
            case "plugins" -> Platform.ContentType.PLUGIN;
            case "datapacks" -> Platform.ContentType.DATAPACK;
            default -> throw new NotFoundResponse();
        };
    }

    private static String removeDisabled(String filename) {
        return filename.endsWith(".disabled") ? filename.substring(0, filename.length() - 9) : filename;
    }

    public static void installAddon(Context ctx) throws IOException {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        Platform.ContentType type = getContentType(ctx);
        String versionUri = ctx.queryParamAsClass("versionUri", String.class).get();

        ServerManager.ServerData serverData = ServerManager.getServerData(serverId).orElseThrow(() -> new BadRequestResponse("Server not found"));
        try {
            Path installPath = AddonManager.install(versionUri, Path.of(serverData.path()).resolve(type.folderName()));
            List<AddonMetaWithFile> meta = AddonManager.listAddons(List.of(installPath.toFile()), serverData.loader(), serverData.gameVersion(), type);
            ctx.json(aggregateAddonData(meta, serverData.modPlatformPriorities()).getFirst());
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
    }

    private static List<AggregatedAddonData> aggregateAddonData(List<AddonMetaWithFile> meta, List<Platform> modPlatformPriorities) {
        Map<String, List<AddonMetaWithFile>> aggregatedMeta = new HashMap<>();
        for (AddonMetaWithFile addonMeta : meta) {
            aggregatedMeta.computeIfAbsent(addonMeta.meta().hash(), k -> new ArrayList<>()).add(addonMeta);
        }

        List<AggregatedAddonData> result = new ArrayList<>();
        aggregatedMeta.forEach((hash, addonMetas) -> {
            List<AddonMetaWithFile> sortedMetas = new ArrayList<>();
            AddonMetaWithFile firstFound = null;
            for (Platform platform : modPlatformPriorities) {
                for (AddonMetaWithFile m : addonMetas) {
                    if (Platforms.fromString(m.meta().platform()) == platform) sortedMetas.add(m);
                    if (firstFound == null && m.meta().found()) firstFound = m;
                }
            }

            if (firstFound != null) {
                AddonMeta firstFoundMeta = firstFound.meta();
                List<String> pageUrls = sortedMetas.stream()
                        .filter(m -> m.meta().found())
                        .map(m -> m.meta().platform() + ":" + m.meta().pageUrl())
                        .toList();

                result.add(new AggregatedAddonData(
                        removeDisabled(firstFound.file().getName()),
                        firstFoundMeta.name(),
                        firstFoundMeta.author(),
                        pageUrls,
                        firstFoundMeta.iconUrl(),
                        firstFoundMeta.versionString(),
                        !firstFound.file().getName().endsWith(".disabled"),
                        sortedMetas.stream().anyMatch(m -> m.meta().updateUri() != null)
                ));
            } else {
                String filename = addonMetas.getFirst().file().getName();
                result.add(new AggregatedAddonData(filename, filename, null, null, null, null, !filename.endsWith(".disabled"), false));
            }
        });
        return result;
    }

    private record ListServerDirectoriesEntry(String path, boolean isServer) {
    }

    private record AggregatedAddonData(String filename, String name, String author, List<String> pageUrls,
                                       String iconUrl, String version, boolean enabled, boolean updateAvailable) {
    }
}
