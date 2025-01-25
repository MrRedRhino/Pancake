package org.pipeman.pancake.addons;

import org.json.JSONObject;
import org.pipeman.pancake.Utils;
import org.pipeman.pancake.loaders.Loader;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Modrinth implements Platform {
    // TODO add user agent to every request
    private static final String baseUrl = "https://api.modrinth.com/v2";

    @Override
    public List<ModVersionInfo> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        List<String> gameVersionFacet = filters.containsKey(FilterKey.GAME_VERSION) ?
                List.of("versions:" + filters.get(FilterKey.GAME_VERSION)) :
                List.of();

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", query);
        queryMap.put("limit", 20);
        queryMap.put("facets", List.of(
                gameVersionFacet,
                List.of("project_type:" + mapContentType(contentType)),
                List.of("categories:" + mapLoader(loader))
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/search" + Utils.buildQuery(queryMap))).build();
        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());

        List<ModVersionInfo> results = new ArrayList<>();
        for (Object hit : body.getJSONArray("hits")) {
            JSONObject result = (JSONObject) hit;
            results.add(new ModVersionInfo(
                    result.getString("project_id"),
                    result.getString("title"),
                    result.getString("description"),
                    result.getString("author"),
                    "https://modrinth.com/project/" + result.getString("slug"),
                    result.getString("icon_url"),
                    "modrinth://" + result.getString("project_id") + "/" + result.getString("latest_version")
            ));
        }
        return results;
    }

    @Override
    public DownloadInfo getDownloadUrl(String versionInfo) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/version/" + versionInfo.split("/")[1])).build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());
        JSONObject file = body.getJSONArray("files").getJSONObject(0);
        return new DownloadInfo(file.getString("url"), file.getString("filename"));
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return Set.of(ContentType.MOD, ContentType.PLUGIN, ContentType.MODPACK, ContentType.DATAPACK);
    }

    @Override
    public String id() {
        return "modrinth";
    }

    private static String mapContentType(ContentType contentType) {
        return switch (contentType) {
            case MOD -> "mod";
            case MODPACK -> "modpack";
            case PLUGIN -> "plugin";
            case DATAPACK -> "datapack";
        };
    }

    private static String mapLoader(Loader loader) {
        return switch (loader) {
            case FABRIC -> "fabric";
            case FORGE -> "forge";
            case PAPER -> "paper";
            case PURPUR -> "purpur";
            case VELOCITY -> "velocity";
            case VANILLA -> throw new IllegalArgumentException();
        };
    }
}
