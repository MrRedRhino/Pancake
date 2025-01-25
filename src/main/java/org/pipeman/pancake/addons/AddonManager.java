package org.pipeman.pancake.addons;

import com.fasterxml.jackson.core.type.TypeReference;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class AddonManager {
    public static List<CachedAddonData> getCachedAddonData(List<String> paths) {
        return Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT file_path, icon_url, name, author, version_string, last_update_check, version_data
                        FROM addons_version_cache
                        WHERE file_path IN (<paths>)
                        """)
                .bindList("paths", paths)
                .mapTo(CachedAddonData.class)
                .list());
    }

    public static void install(String versionUri, Path directory) {
        URI uri = URI.create(versionUri);
        Platform platform = Platforms.fromString(uri.getScheme().toUpperCase());
        if (platform == null) throw new IllegalArgumentException("Invalid version URI: " + versionUri);

        Platform.DownloadInfo downloadUrl = platform.getDownloadUrl(uri.getSchemeSpecificPart().substring(2));
        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl.url())).build();
        Path path = directory.resolve(downloadUrl.filename());
        if (path.toFile().exists()) {
            throw new IllegalArgumentException("File already exists: " + path);
        }
        Platform.sendRequest(request, HttpResponse.BodyHandlers.ofFile(path));
    }

    public record CachedAddonData(String path, String iconUrl, String name, String author, String version,
                                  Timestamp lastUpdateCheck, Map<String, VersionData> versionData) {
        @ConstructorProperties({"file_path", "icon_url", "name", "author", "version_string", "last_update_check", "version_data"})
        public CachedAddonData(String path, String iconUrl, String name, String author, String version, Timestamp lastUpdateCheck, String versionData) {
            this(
                    path,
                    iconUrl,
                    name,
                    author,
                    version,
                    lastUpdateCheck,
                    versionData == null ? Map.of() : Main.deserialize(versionData, new TypeReference<>() {
                    })
            );
        }
    }

    public record VersionData(String pageUrl, String versionUri, String updateUri, String modId) {
    }
}
