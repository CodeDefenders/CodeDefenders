package org.codedefenders.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class JSONUtils {
    /**
     * Serializes a {@link Map} as a list of lists, so that it can be used to construct an ES6 Map. <br>
     * <pre>{@code
     * var json = JSON.parse(jsonString); // e.g. [[1, "one"], [2, "two"], [3, "three"]]
     * var map = new Map(jsonString);
     * }</pre>
     */
    public static class MapSerializer implements JsonSerializer<Map> {
        @Override
        public JsonElement serialize(Map map, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonArray outerArray = new JsonArray();
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                JsonArray innerArray = new JsonArray();
                innerArray.add(jsonSerializationContext.serialize(entry.getKey()));
                innerArray.add(jsonSerializationContext.serialize(entry.getValue()));
                outerArray.add(innerArray);
            }
            return outerArray;
        }
    }
}
