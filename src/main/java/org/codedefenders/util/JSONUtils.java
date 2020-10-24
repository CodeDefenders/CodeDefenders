/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class JSONUtils {
    /**
     * Serializes a {@link Map} as a list of lists, so that it can be used to construct an ES6 Map. <br>
     * <pre>{@code
     * var json = JSON.parse(jsonString); // e.g. [[1, "one"], [2, "two"], [3, "three"]]
     * var map = new Map(jsonString);
     * }</pre>
     * @deprecated This doesn't work on some subtypes of Map, e.g. {@code Collections$UnmodifiableMap},
     *             use {@link MapTypeAdapterFactory} instead.
     */
    @Deprecated
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

    /**
     * Serializes a {@link Map} as a list of lists, so that it can be used to construct an ES6 Map. <br>
     * <pre>{@code
     * var json = JSON.parse(jsonString); // e.g. [[1, "one"], [2, "two"], [3, "three"]]
     * var map = new Map(jsonString);
     * }</pre>
     */
    public static class MapTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (Map.class.isAssignableFrom(type.getRawType())) {
                return (TypeAdapter<T>) new MapTypeAdapter<>(gson);
            }
            return null;
        }

        public static class MapTypeAdapter<K, V> extends TypeAdapter<Map<K, V>> {
            private final Gson gson;

            public MapTypeAdapter(Gson gson) {
                this.gson = gson;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void write(JsonWriter jsonWriter, Map<K, V> map) throws IOException {
                jsonWriter.beginArray();
                if (!map.isEmpty()) {
                    Map.Entry<K, V> firstEntry = map.entrySet().iterator().next();
                    TypeAdapter<K> keyAdapter = (TypeAdapter<K>) gson.getAdapter(firstEntry.getKey().getClass());
                    TypeAdapter<V> valueAdapter = (TypeAdapter<V>) gson.getAdapter(firstEntry.getValue().getClass());
                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        jsonWriter.beginArray();
                        keyAdapter.write(jsonWriter, entry.getKey());
                        valueAdapter.write(jsonWriter, entry.getValue());
                        jsonWriter.endArray();
                    }
                }
                jsonWriter.endArray();
            }

            @Override
            public Map<K, V> read(JsonReader jsonReader) {
                throw new NotImplementedException();
            }
        }
    }
}
