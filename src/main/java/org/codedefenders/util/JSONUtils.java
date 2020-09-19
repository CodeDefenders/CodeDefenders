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

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
