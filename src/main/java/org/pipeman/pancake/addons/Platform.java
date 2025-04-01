package org.pipeman.pancake.addons;

import org.pipeman.pancake.loaders.Loader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Platform {
    HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    List<SearchResult> search(String query, Platform.ContentType contentType, Loader loader, Map<FilterKey, String> filters);

    DownloadInfo getDownloadInfo(VersionInfo versionInfo);

    List<String> getSecondaryFiles(VersionInfo versionInfo);

    Set<ContentType> supportedContentTypes();

    String id();

    String getFingerprint(File file) throws IOException;

    Map<String, AddonData> fetchAddonData(List<String> fingerprints, Loader loader, String gameVersion, ContentType contentType);

    static <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return HTTP_CLIENT.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    enum FilterKey {
        GAME_VERSION("gameVersion", "versions:");
//        LOADER("modLoaderType", "categories:");

        private final String curseforgeName;
        private final String modrinthName;

        FilterKey(String curseforgeName, String modrinthName) {
            this.curseforgeName = curseforgeName;
            this.modrinthName = modrinthName;
        }

        public String curseforgeName() {
            return curseforgeName;
        }

        public String modrinthName() {
            return modrinthName;
        }
    }

    enum ContentType {
        MOD("mods"),
        MODPACK(null),
        PLUGIN("plugins"),
        DATAPACK("datapacks");

        private final String folderName;

        ContentType(String folderName) {
            this.folderName = folderName;
        }

        public String folderName() {
            return folderName;
        }

        public static ContentType fromString(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    record SearchResult(String id, String name, String description, String author, String url, String iconUrl,
                        VersionInfo versionUri) {
    }

    record DownloadInfo(HttpRequest request, String filename) {
        public DownloadInfo(String url, String filename) {
            this(HttpRequest.newBuilder(URI.create(url)).build(), filename);
        }
    }

    record AddonData(String id, String iconUrl, String name, String author, String version, String pageUrl,
                     String versionUri, String updateUri) {
    }
}
