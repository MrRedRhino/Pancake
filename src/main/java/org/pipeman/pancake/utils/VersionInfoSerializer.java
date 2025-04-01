package org.pipeman.pancake.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.pipeman.pancake.addons.VersionInfo;

import java.io.IOException;

public class VersionInfoSerializer extends JsonSerializer<VersionInfo> {
    @Override
    public void serialize(VersionInfo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    }


}
