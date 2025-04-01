package org.pipeman.pancake.addons;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pipeman.pancake.Config;
import org.pipeman.pancake.Utils;
import org.pipeman.pancake.loaders.Loader;
import org.pipeman.pancake.utils.CurseforgeFingerprint;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

public class Curseforge implements Platform {
    private static final int gameId = 432;
    private static final String baseUrl = "https://api.curseforge.com/v1";

    @Override
    public List<SearchResult> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        Map<String, Object> queryMap = new HashMap<>();
        filters.forEach((filterKey, s) -> queryMap.put(filterKey.curseforgeName(), s));
        queryMap.put("searchFilter", query);
        queryMap.put("gameId", gameId);
        queryMap.put("limit", 10);
        queryMap.put("sortField", 1);
        queryMap.put("sortOrder", "desc");

        addLoaderFilters(queryMap, loader, contentType);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/mods/search" + Utils.buildQuery(queryMap)))
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());

        List<SearchResult> results = new ArrayList<>();
        for (Object entry : body.getJSONArray("data")) {
            JSONObject result = (JSONObject) entry;
            if (result.getJSONArray("latestFiles").getJSONObject(0).isNull("downloadUrl")) continue;
            results.add(new SearchResult(
                    String.valueOf(result.getInt("id")),
                    result.getString("name"),
                    result.getString("summary"),
                    result.getJSONArray("authors").getJSONObject(0).getString("name"),
                    "https://www.curseforge.com/minecraft/mc-mods/" + result.getString("slug"),
                    result.optJSONObject("logo", new JSONObject()).optString("thumbnailUrl", ""),
                    new VersionInfo(Platforms.CURSEFORGE,
                            String.valueOf(result.getInt("id")),
                            String.valueOf(result.getJSONArray("latestFiles").getJSONObject(0).getInt("id")),
                            null)
            ));
        }
        return results;
    }

    @Override
    public DownloadInfo getDownloadInfo(VersionInfo versionInfo) {
        // TODO versionInfo may not contain a version -> we have to fetch newest version
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/mods/" + versionInfo.projectId() + "/files/" + versionInfo.versionId()))
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body()).getJSONObject("data");

        HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(body.getString("downloadUrl")))
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();
        return new DownloadInfo(downloadRequest, body.getString("fileName"));
    }

    @Override
    public List<String> getSecondaryFiles(VersionInfo versionInfo) {
        return List.of();
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return Set.of(ContentType.MOD, ContentType.MODPACK, ContentType.PLUGIN, ContentType.DATAPACK);
    }

    @Override
    public String id() {
        return "curseforge";
    }

    @Override
    public String getFingerprint(File file) throws IOException {
        byte[] filteredBytes = CurseforgeFingerprint.filterWhitespace(Files.readAllBytes(file.toPath()));
        return String.valueOf(CurseforgeFingerprint.computeHash(filteredBytes));
    }

    @Override
    public Map<String, AddonData> fetchAddonData(List<String> fingerprints, Loader loader, String gameVersion, ContentType contentType) {
        JSONObject body = new JSONObject();
        body.put("fingerprints", fingerprints.stream().map(Long::parseLong).toList());

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/fingerprints/" + gameId))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();
        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONArray matches = new JSONObject(response.body()).getJSONObject("data").getJSONArray("exactMatches");
        if (matches.isEmpty()) return Map.of();

        JSONArray modIds = new JSONArray();
        for (int i = 0; i < matches.length(); i++) {
            modIds.put(matches.getJSONObject(i).getInt("id"));
        }

        JSONObject projectRequestBody = new JSONObject().put("modIds", modIds);
        HttpRequest projectRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/mods"))
                .POST(HttpRequest.BodyPublishers.ofString(projectRequestBody.toString()))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", Config.curseforgeApiKey())
                .build();
        HttpResponse<String> projectsResponse = Platform.sendRequest(projectRequest, HttpResponse.BodyHandlers.ofString());
        JSONArray projects = new JSONObject(projectsResponse.body()).getJSONArray("data");
        Map<Long, JSONObject> projectMap = new HashMap<>();
        for (int i = 0; i < projects.length(); i++) {
            JSONObject project = projects.getJSONObject(i);
            projectMap.put(project.getLong("id"), project);
        }

        Map<String, AddonData> data = new HashMap<>();
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            JSONObject project = projectMap.get(match.getLong("id"));
            JSONObject file = match.getJSONObject("file");

            Map<String, Object> query = new HashMap<>();
            query.put("gameVersion", gameVersion);
            query.put("limit", 1);
            if (contentType == ContentType.MOD) {
                query.put("modLoaderType", switch (loader) {
                    case FABRIC -> "4";
                    case FORGE -> "1";
                    default -> throw new IllegalArgumentException("Loader does not support mods");
                });
            }
            HttpRequest modFilesRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/mods/" + match.getLong("id") + "/files" + Utils.buildQuery(query)))
                    .header("X-Api-Key", Config.curseforgeApiKey())
                    .build();
            HttpResponse<String> modFilesResponse = Platform.sendRequest(modFilesRequest, HttpResponse.BodyHandlers.ofString());
            JSONArray newestFiles = new JSONObject(modFilesResponse.body()).getJSONArray("data");
            JSONObject newestFile = newestFiles.optJSONObject(0);
            String updateUri = !newestFiles.isEmpty() && newestFile.getLong("id") != file.getLong("id") ? "curseforge://" + project.getLong("id") + "/" + newestFile.getLong("id") : null;

            data.put(file.getLong("fileFingerprint") + "", new AddonData(
                    match.getLong("id") + "",
                    project.getJSONObject("logo").getString("thumbnailUrl"),
                    project.getString("name"),
                    project.getJSONArray("authors").getJSONObject(0).getString("name"),
                    file.getString("displayName"),
                    "https://www.curseforge.com/minecraft/mc-mods/" + project.getString("slug"),
                    "curseforge://" + project.getLong("id") + "/" + file.getLong("id"),
                    updateUri
            ));
        }
        return data;
    }

    // https://api.curseforge.com/v1/categories?gameId=432&classesOnly=true
    private static void addLoaderFilters(Map<String, Object> queryMap, Loader loader, ContentType contentType) {
        if (loader != null && loader.supportsMods()) {
            queryMap.put("modLoaderType", switch (loader) {
                case FORGE -> "1";
                case FABRIC -> "4";
                default -> throw new IllegalArgumentException("Loader does not support mods");
            });
        } else if (loader != null && loader.supportsPlugins()) {
            queryMap.put("classId", "5");
        } else {
            queryMap.put("classId", switch (contentType) {
                case MOD -> "6";
                case MODPACK -> "4471";
                case DATAPACK -> "6945";
                default -> throw new IllegalArgumentException();
            });
        }


//        if (contentType == ContentType.PLUGIN || (loader != null && Set.of(Loader.PAPER, Loader.PURPUR).contains(loader))) {
//            queryMap.put("classId", "5");
//        } else {
//            String loaderType = switch (contentType) {
//                case MOD -> "6"; // TODO make this the actual loader not class for some reason
//                case MODPACK -> "4471";
//                case DATAPACK -> "6945";
//                default -> throw new IllegalArgumentException();
//            };
//            queryMap.put("classId", loaderType);
//        }
    }

    public static void main(String[] args) throws IOException {
        String fingerprint = Platforms.CURSEFORGE.getFingerprint(new File("create-mc1.18.2_v0.5.0.jar"));

        System.out.println(Platforms.CURSEFORGE.fetchAddonData(List.of(fingerprint), Loader.FORGE, "1.18.2", ContentType.MOD));
    }
}
