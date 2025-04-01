package org.pipeman.pancake.addons;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record VersionInfo(Platform platform, String projectId, @Nullable String versionId, @Nullable String file) {
    public static VersionInfo fromString(String s) throws IllegalArgumentException {
        StringBuilder currentValue = new StringBuilder();
        List<String> values = new ArrayList<>(4);

        char[] charArray = s.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == ':') {
                if (charArray.length > i + 1 && charArray[i + 1] == ':') {
                    currentValue.append(c);
                    i++;
                } else {
                    values.add(currentValue.toString());
                    currentValue = new StringBuilder();
                    if (values.size() == 4) break;
                }
            } else {
                currentValue.append(c);
            }
        }
        if (!currentValue.isEmpty()) values.add(currentValue.toString());

        return new VersionInfo(
                Platforms.fromString(values.getFirst()),
                values.get(1),
                values.size() == 2 ? null : values.get(2),
                values.size() <= 3 ? null : values.get(3)
        );
    }


    @Override
    @JsonValue
    public @NotNull String toString() {
        String s = platform.id() + ":" + projectId;
        if (versionId != null) s += ":" + versionId;
        if (file != null) s += ":" + file;
        return s;
    }
}
