package org.pipeman.pancake.addons;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pipeman.pancake.Utils;
import org.pipeman.pancake.loaders.Loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

public class Modrinth implements Platform {
    // TODO add user agent to every request
    private static final String baseUrl = "https://api.modrinth.com/v2";

    @Override
    public List<SearchResult> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        List<List<String>> facets = new ArrayList<>();
        facets.add(List.of("project_type:" + mapContentType(contentType)));
        if (loader != null) facets.add(List.of("categories:" + mapLoader(loader)));
        filters.forEach((k, v) -> facets.add(List.of(k.modrinthName() + v)));

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", query);
        queryMap.put("limit", 20);
        queryMap.put("index", "relevance");
        queryMap.put("facets", facets);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/search" + Utils.buildQuery(queryMap))).build();
        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());

        List<SearchResult> results = new ArrayList<>();
        for (Object hit : body.getJSONArray("hits")) {
            JSONObject result = (JSONObject) hit;
            results.add(new SearchResult(
                    result.getString("project_id"),
                    result.getString("title"),
                    result.getString("description"),
                    result.getString("author"),
                    "https://modrinth.com/project/" + result.getString("slug"),
                    result.getString("icon_url"),
                    new VersionInfo(Platforms.MODRINTH, result.getString("project_id"), result.getString("latest_version"), null)
            ));
        }
        return results;
    }

    @Override
    public DownloadInfo getDownloadInfo(VersionInfo versionInfo) {
        // TODO the versionInfo may not contain a versionId -> fetch newest
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/version/" + versionInfo.versionId())).build();

        HttpResponse<String> response = Platform.sendRequest(request, HttpResponse.BodyHandlers.ofString());
        JSONObject body = new JSONObject(response.body());
        JSONObject file = body.getJSONArray("files").getJSONObject(0);
        return new DownloadInfo(file.getString("url"), file.getString("filename"));
    }

    @Override
    public List<String> getSecondaryFiles(VersionInfo versionInfo) {

        return List.of();
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return Set.of(ContentType.MOD, ContentType.PLUGIN, ContentType.MODPACK, ContentType.DATAPACK);
    }

    @Override
    public String id() {
        return "modrinth";
    }

    @Override
    public String getFingerprint(File file) throws IOException {
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            return Utils.bytesToHex(Utils.getHash(stream, "SHA-512")).toLowerCase();
        }
    }

    @Override
    public Map<String, AddonData> fetchAddonData(List<String> fingerprints, Loader loader, String gameVersion, ContentType contentType) {
        JSONObject versionsBody = new JSONObject()
                .put("algorithm", "sha512")
                .put("hashes", fingerprints);
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/version_files"))
                .POST(HttpRequest.BodyPublishers.ofString(versionsBody.toString()))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> versionsResponse = Platform.sendRequest(versionsRequest, HttpResponse.BodyHandlers.ofString());
        JSONObject versions = new JSONObject(versionsResponse.body());
        List<String> projectIds = new ArrayList<>();
        for (String key : versions.keySet()) {
            projectIds.add(versions.getJSONObject(key).getString("project_id"));
        }

        JSONObject updatesBody = new JSONObject()
                .put("algorithm", "sha512")
                .put("loaders", new JSONArray().put(mapLoader(loader)))
                .put("game_versions", new JSONArray().put(gameVersion))
                .put("hashes", fingerprints);
        HttpRequest updatesRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/version_files/update"))
                .POST(HttpRequest.BodyPublishers.ofString(updatesBody.toString()))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = Platform.sendRequest(updatesRequest, HttpResponse.BodyHandlers.ofString());
        JSONObject updates = new JSONObject(response.body());

        HttpRequest projectsRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/projects" + Utils.buildQuery(Map.of("ids", projectIds)))).build();
        HttpResponse<String> projectsResponse = Platform.sendRequest(projectsRequest, HttpResponse.BodyHandlers.ofString());
        JSONArray projects = new JSONArray(projectsResponse.body());
        Map<String, JSONObject> projectsById = new HashMap<>();
        projects.forEach(object -> {
            JSONObject project = (JSONObject) object;
            projectsById.put(project.getString("id"), project);
        });

        List<String> teamIds = new ArrayList<>();
        for (int i = 0; i < projects.length(); i++) {
            teamIds.add(projects.getJSONObject(i).getString("team"));
        }
        HttpRequest teamRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/teams" + Utils.buildQuery(Map.of("ids", teamIds)))).build();
        HttpResponse<String> teamResponse = Platform.sendRequest(teamRequest, HttpResponse.BodyHandlers.ofString());
        JSONArray teams = new JSONArray(teamResponse.body());
        Map<String, JSONArray> teamsMap = new HashMap<>();
        for (int i = 0; i < teams.length(); i++) {
            JSONArray team = teams.getJSONArray(i);
            teamsMap.put(team.getJSONObject(0).getString("team_id"), team);
        }

        Map<String, AddonData> result = new HashMap<>();
        for (String hash : versions.keySet()) {
            JSONObject version = versions.getJSONObject(hash);
            JSONObject update = updates.optJSONObject(hash, null);
            String projectId = version.getString("project_id");
            JSONObject project = projectsById.get(projectId);
            JSONArray team = teamsMap.get(project.getString("team"));
            String author = team.getJSONObject(0).getJSONObject("user").getString("username");
            for (int i = 0; i < team.length(); i++) {
                JSONObject member = team.getJSONObject(i);
                if (member.getString("role").equals("Owner")) {
                    author = member.getJSONObject("user").getString("username");
                    break;
                }
            }

            boolean hasUpdate = update != null && !version.getString("id").equals(update.getString("id"));
            result.put(hash, new AddonData(
                    projectId,
                    project.getString("icon_url"),
                    project.getString("title"),
                    author,
                    version.getString("version_number"),
                    "https://modrinth.com/project/" + project.getString("slug"),
                    "modrinth://" + projectId + "/" + version.getString("id"),
                    hasUpdate ? "modrinth://" + projectId + "/" + update.getString("id") : null
            ));
        }

        return result;
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

    public static void main(String[] args) throws IOException {
        String fingerprint = Platforms.MODRINTH.getFingerprint(new File("e4mc_minecraft-fabric-5.2.1.jar"));

        System.out.println(Platforms.MODRINTH.fetchAddonData(List.of(fingerprint), Loader.FABRIC, "1.20.1", ContentType.MOD));
    }
}
