package org.pipeman.pancake.addons;

import org.json.JSONObject;
import org.pipeman.pancake.Config;
import org.pipeman.pancake.Utils;
import org.pipeman.pancake.loaders.Loader;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Curseforge implements Platform {
    private static final String baseUrl = "https://api.curseforge.com/v1";

    @Override
    public List<ModVersionInfo> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        Map<String, Object> queryMap = new HashMap<>();
        filters.forEach((filterKey, s) -> queryMap.put(filterKey.curseforgeName(), s));
        queryMap.put("searchFilter", query);
        queryMap.put("gameId", 432);
        queryMap.put("limit", 10);
        queryMap.put("sortField", 2);
        queryMap.put("sortOrder", "desc");
        addLoaderFilters(queryMap, loader, contentType);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/mods/search" + Utils.buildQuery(queryMap)))
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());

        List<ModVersionInfo> results = new ArrayList<>();
        for (Object entry : body.getJSONArray("data")) {
            JSONObject result = (JSONObject) entry;
            if (result.getJSONArray("latestFiles").getJSONObject(0).isNull("downloadUrl")) continue;
            results.add(new ModVersionInfo(
                    String.valueOf(result.getInt("id")),
                    result.getString("name"),
                    result.getString("summary"),
                    result.getJSONArray("authors").getJSONObject(0).getString("name"),
                    "https://www.curseforge.com/minecraft/mc-mods/" + result.getString("slug"),
                    result.getJSONObject("logo").getString("thumbnailUrl"),
                    "curseforge://" + result.getInt("id") + "/" + result.getJSONArray("latestFiles").getJSONObject(0).getInt("id")
            ));
        }
        return results;
    }

    @Override
    public DownloadInfo getDownloadUrl(String versionInfo) {
        String[] split = versionInfo.split("/", 2);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/mods/" + split[0] + "/files/" + split[1]))
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body()).getJSONObject("data");

        return new DownloadInfo(body.getString("downloadUrl"), body.getString("fileName"));
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return Set.of(ContentType.MOD, ContentType.MODPACK, ContentType.PLUGIN, ContentType.DATAPACK);
    }

    @Override
    public String id() {
        return "curseforge";
    }

    // https://api.curseforge.com/v1/categories?gameId=432&classesOnly=true
    private static void addLoaderFilters(Map<String, Object> queryMap, Loader loader, ContentType contentType) {
        if (contentType == ContentType.PLUGIN || Set.of(Loader.PAPER, Loader.PURPUR).contains(loader)) {
            queryMap.put("classId", "5");
        } else {
            String loaderType = switch (contentType) {
                case MOD -> "6";
                case MODPACK -> "4471";
                case DATAPACK -> "6945";
                default -> throw new IllegalArgumentException();
            };
            queryMap.put("modLoaderType", loaderType);
        }
    }
}
