package org.pipeman.pancake.addons;

import org.jdbi.v3.core.statement.PreparedBatch;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Utils;
import org.pipeman.pancake.loaders.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;

public class AddonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddonManager.class);

    public static Path install(VersionInfo versionInfo, Path directory) {
        Platform.DownloadInfo downloadInfo = versionInfo.platform().getDownloadInfo(versionInfo);
        Path path = directory.resolve(downloadInfo.filename());
        if (path.toFile().exists()) {
            throw new IllegalArgumentException("File already exists: " + path);
        }
        path.toFile().getParentFile().mkdirs();
        Platform.sendRequest(downloadInfo.request(), HttpResponse.BodyHandlers.ofFile(path));
        return path;
    }

    public static List<AddonMetaWithFile> listAddons(List<File> files, Loader loader, String gameVersion, Platform.ContentType contentType) throws IOException {
        List<FileHash> fileHashes = new ArrayList<>();
        Set<String> hashes = new HashSet<>();
        for (File file : files) {
            byte[] hash = Utils.getHash(Files.newInputStream(file.toPath()), "SHA-256");
            String hexHash = Utils.bytesToHex(hash);
            fileHashes.add(new FileHash(file, hexHash));
            hashes.add(hexHash);
        }

        List<AddonMeta> cachedMetaList = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM addon_meta
                        WHERE hash IN (<hashes>)
                        """)
                .bindList("hashes", hashes)
                .mapTo(AddonMeta.class)
                .list());
        Map<String, List<AddonMeta>> cachedMeta = new HashMap<>();
        for (AddonMeta meta : cachedMetaList) {
            cachedMeta.computeIfAbsent(meta.hash(), k -> new ArrayList<>()).add(meta);
        }

        List<AddonMeta> metaToStore = new ArrayList<>();
        for (Platforms platform : Platforms.values()) {
            ArrayList<FileHash> missingHashes = new ArrayList<>(fileHashes);
            missingHashes.removeIf(hash -> {
                List<AddonMeta> cached = cachedMeta.get(hash.hash());
                return cached != null && cached.stream().anyMatch(m -> m.platform.equals(platform.name()));
            });

            HashMap<String, FileHash> fingerprints = new HashMap<>();
            for (FileHash hash : missingHashes) {
                fingerprints.put(platform.getFingerprint(hash.file()), hash);
            }
            if (fingerprints.isEmpty()) continue;

            Map<String, Platform.AddonData> addonData;
            try {
                addonData = platform.fetchAddonData(List.copyOf(fingerprints.keySet()), loader, gameVersion, contentType);
            } catch (Exception e) {
                LOGGER.error("Failed to retrieve addon data on platform {}", platform, e);
                continue;
            }

            fingerprints.forEach((fingerprint, fileHash) -> {
                Platform.AddonData data = addonData.get(fingerprint);
                FileHash hash = fingerprints.get(fingerprint);
                if (data == null) {
                    metaToStore.add(new AddonMeta(hash.hash(), platform.name(), false, null, null, null, null, null, null, null, null, null));
                } else {
                    metaToStore.add(new AddonMeta(
                            hash.hash(),
                            platform.name(),
                            true,
                            data.id(),
                            data.iconUrl(),
                            data.name(),
                            data.author(),
                            data.version(),
                            data.pageUrl(),
                            data.versionUri(),
                            data.updateUri(),
                            new Timestamp(System.currentTimeMillis())
                    ));
                }
            });
        }
        Database.getJdbi().useHandle(h -> {
            PreparedBatch batch = h.prepareBatch("""
                    INSERT INTO addon_meta (hash, platform, found, id, icon_url, name, author, version_string, page_url, version_uri, update_uri, fetched_at)
                    VALUES (:hash, :platform, :found, :id, :icon_url, :name, :author, :version_string, :page_url, :version_uri, :update_uri, :fetched_at)
                    """);
            metaToStore.forEach(meta -> {
                batch.bind("hash", meta.hash());
                batch.bind("platform", meta.platform());
                batch.bind("found", meta.found());
                batch.bind("id", meta.id());
                batch.bind("icon_url", meta.iconUrl());
                batch.bind("name", meta.name());
                batch.bind("author", meta.author());
                batch.bind("version_string", meta.versionString());
                batch.bind("page_url", meta.pageUrl());
                batch.bind("version_uri", meta.versionUri());
                batch.bind("update_uri", meta.updateUri());
                batch.bind("fetched_at", meta.fetchedAt());
                batch.add();
            });
            batch.execute();
        });

        Map<String, File> hashToFileMap = new HashMap<>();
        fileHashes.forEach(hash -> hashToFileMap.put(hash.hash(), hash.file()));

        List<AddonMetaWithFile> result = new ArrayList<>(cachedMeta.size() + metaToStore.size());
        for (AddonMeta meta : cachedMetaList) result.add(new AddonMetaWithFile(hashToFileMap.get(meta.hash()), meta));
        for (AddonMeta meta : metaToStore) result.add(new AddonMetaWithFile(hashToFileMap.get(meta.hash()), meta));
        return result;
    }

    public record AddonMeta(String hash, String platform, boolean found, String id, String iconUrl, String name,
                            String author, String versionString, String pageUrl, String versionUri, String updateUri,
                            Timestamp fetchedAt) {
    }

    public record AddonMetaWithFile(File file, AddonMeta meta) {
    }

    private record FileHash(File file, String hash) {
    }
}
