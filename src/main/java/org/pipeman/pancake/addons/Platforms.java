package org.pipeman.pancake.addons;

import org.pipeman.pancake.loaders.Loader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum Platforms implements Platform {
    MODRINTH(new Modrinth()),
    CURSEFORGE(new Curseforge());
//    HANGAR(null);
//    SPIGOT

    private final Platform implementation;

    Platforms(Platform implementation) {
        this.implementation = implementation;
    }

    @Override
    public List<SearchResult> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        return implementation.search(query, contentType, loader, filters);
    }

    @Override
    public DownloadInfo getDownloadInfo(String versionInfo) {
        return implementation.getDownloadInfo(versionInfo);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return implementation.supportedContentTypes();
    }

    @Override
    public String id() {
        return implementation.id();
    }

    @Override
    public String getFingerprint(File file) throws IOException {
        return implementation.getFingerprint(file);
    }

    @Override
    public Map<String, AddonData> fetchAddonData(List<String> fingerprints, Loader loader, String gameVersion, ContentType contentType) {
        return implementation.fetchAddonData(fingerprints, loader, gameVersion, contentType);
    }

    public static Platform fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
