package com.services.sf.domain.basesql;

import io.vertx.core.json.JsonObject;

import javax.persistence.AttributeConverter;
import java.util.Map;
import java.util.Objects;

public class MapJsonConverter implements AttributeConverter<Map<String, Object>, JsonObject> {

    @Override
    public JsonObject convertToDatabaseColumn(Map<String, Object> map) {
        if (Objects.isNull(map)) {
            return null;
        }
        return new JsonObject(map);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(JsonObject jsonObject) {
        if (Objects.isNull(jsonObject)) {
            return null;
        }
        return jsonObject.getMap();
    }
}
