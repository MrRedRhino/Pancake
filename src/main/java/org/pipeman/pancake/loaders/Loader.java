package org.pipeman.pancake.loaders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.OrderedJSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.text.MessageFormat.format;

public enum Loader {
    FABRIC(Loader::getFabricGameVersions, Loader::getFabricLoaderVersions, false, true),
    FORGE(Loader::getForgeGameVersions, Loader::getForgeLoaderVersions, false, true),
    PAPER(List::of, gameVersion -> List.of(), true, false),
    PURPUR(List::of, gameVersion -> List.of(), true, false),
    // NEOFORGE
    // QUILT
    VELOCITY(List::of, gameVersion -> List.of(), true, false),
    VANILLA(Loader::getVanillaGameVersions, Loader::getVanillaLoaderVersions, false, false);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final LoadingCache<Loader, List<GameVersion>> gameVersionCache = Caffeine.newBuilder()
            .build(key -> key.gameVerionsSupplier.get());

    private final Supplier<List<GameVersion>> gameVerionsSupplier;
    private final LoadingCache<String, List<LoaderVersion>> loaderVersionsCache;
    private final boolean supportsPlugins;
    private final boolean supportsMods;

    Loader(Supplier<List<GameVersion>> gameVerionsSupplier, Function<String, List<LoaderVersion>> loaderVersionsSupplier, boolean supportsPlugins, boolean supportsMods) {
        this.gameVerionsSupplier = gameVerionsSupplier;
        this.loaderVersionsCache = Caffeine.newBuilder().build(loaderVersionsSupplier::apply);
        this.supportsPlugins = supportsPlugins;
        this.supportsMods = supportsMods;
    }

    public List<GameVersion> getGameVersions() {
        return gameVersionCache.get(this);
    }

    public Optional<GameVersion> getGameVersion(String gameVersion) {
        for (GameVersion version : getGameVersions()) {
            if (version.gameVersion().equals(gameVersion)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    public List<LoaderVersion> getLoaderVersions(String gameVersion) {
        return loaderVersionsCache.get(gameVersion);
    }

    public void invalidateCache() {
        gameVersionCache.invalidate(this);
        loaderVersionsCache.invalidateAll();
    }

    private static List<GameVersion> getForgeGameVersions() {
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json")).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        return new OrderedJSONObject(response).keySet().stream()
                .map(version -> new GameVersion(version, true))
                .toList()
                .reversed();
    }

    private static List<LoaderVersion> getForgeLoaderVersions(String gameVersion) {
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json")).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        JSONArray versions = new JSONObject(response).getJSONArray(gameVersion);
        if (versions == null) return List.of();

        List<LoaderVersion> loaderVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            String version = versions.getString(i);
            loaderVersions.add(new LoaderVersion(gameVersion, version, format("https://maven.minecraftforge.net/net/minecraftforge/forge/{0}/forge-{0}-installer.jar", version)));
        }
        return loaderVersions.reversed();
    }

    private static List<GameVersion> getFabricGameVersions() {
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://meta.fabricmc.net/v2/versions/game")).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        JSONArray versions = new JSONArray(response);

        List<GameVersion> gameVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            gameVersions.add(new GameVersion(version.getString("version"), version.getBoolean("stable")));
        }
        return gameVersions;
    }

    private static List<LoaderVersion> getFabricLoaderVersions(String gameVersion) {
        HttpRequest installersRequest = HttpRequest.newBuilder(URI.create("https://meta.fabricmc.net/v2/versions/installer")).build();
        String installers = sendRequest(installersRequest, HttpResponse.BodyHandlers.ofString()).body();
        String installerVersion = new JSONArray(installers).getJSONObject(0).getString("version");

        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://meta.fabricmc.net/v2/versions/loader/" + gameVersion)).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        JSONArray versions = new JSONArray(response);

        List<LoaderVersion> loaderVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            String loaderVersion = versions.getJSONObject(i).getJSONObject("loader").getString("version");
            loaderVersions.add(new LoaderVersion(gameVersion, loaderVersion, format("https://meta.fabricmc.net/v2/versions/loader/{0}/{1}/{2}/server/jar", gameVersion, loaderVersion, installerVersion)));
        }
        return loaderVersions;
    }

    private static List<GameVersion> getVanillaGameVersions() {
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        JSONObject obj = new JSONObject(response);
        JSONArray versions = new JSONArray(obj.getJSONArray("versions"));

        List<GameVersion> gameVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            gameVersions.add(new GameVersion(version.getString("id"), version.getString("type").equals("release"), version.getString("url")));
        }
        return gameVersions;
    }

    private static List<LoaderVersion> getVanillaLoaderVersions(String gameVersion) {
        Optional<GameVersion> version = VANILLA.getGameVersion(gameVersion);
        if (version.isEmpty()) return List.of();

        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create(version.get().metaUrl())).build();
        String response = sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString()).body();
        String url = new JSONObject(response)
                .getJSONObject("downloads")
                .getJSONObject("server")
                .getString("url");

        return List.of(new LoaderVersion(gameVersion, "", url));
    }

    private static <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return HTTP_CLIENT.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean supportsPlugins() {
        return supportsPlugins;
    }

    public boolean supportsMods() {
        return supportsMods;
    }

    // NeoForge: https://github.com/modrinth/code/blob/d5f2ada8f7e483db76ec27f900e8f1f8f0fbc7d3/apps/daedalus_client/src/forge.rs#L100

    public record LoaderVersion(String gameVersion, String version, String downloadUrl) {
    }

    public record GameVersion(String gameVersion, boolean stable, @JsonIgnore String metaUrl) {
        public GameVersion(String gameVersion, boolean stable) {
            this(gameVersion, stable, null);
        }
    }

    public static Loader fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
