package org.pipeman.pancake.addons;

import org.pipeman.pancake.loaders.Loader;

import java.util.List;
import java.util.Map;
import java.util.Set;

public enum Platforms implements Platform {
    MODRINTH(new Modrinth()),
    CURSEFORGE(new Curseforge()),
    HANGAR(null);
//    SPIGOT

    private final Platform implementation;

    Platforms(Platform implementation) {
        this.implementation = implementation;
    }

    @Override
    public List<ModVersionInfo> search(String query, ContentType contentType, Loader loader, Map<FilterKey, String> filters) {
        return implementation.search(query, contentType, loader, filters);
    }

    @Override
    public DownloadInfo getDownloadUrl(String versionInfo) {
        return implementation.getDownloadUrl(versionInfo);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return implementation.supportedContentTypes();
    }

    @Override
    public String id() {
        return implementation.id();
    }

    public static Platform fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
