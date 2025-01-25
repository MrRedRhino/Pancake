package org.pipeman.pancake.addons;

import org.pipeman.pancake.loaders.Loader;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Platform {
    HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    List<ModVersionInfo> search(String query, Platform.ContentType contentType, Loader loader, Map<FilterKey, String> filters);

    DownloadInfo getDownloadUrl(String versionInfo);

    Set<ContentType> supportedContentTypes();

    String id();

    static <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return HTTP_CLIENT.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    enum FilterKey {
        GAME_VERSION("gameVersion"),
        LOADER("modLoaderType");

        private final String curseforgeName;

        FilterKey(String curseforgeName) {
            this.curseforgeName = curseforgeName;
        }

        public String curseforgeName() {
            return curseforgeName;
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
    }

    record ModVersionInfo(String id, String title, String description, String author, String url, String iconUrl,
                          String versionInfo) {
    }

    record DownloadInfo(String url, String filename) {
    }
}
