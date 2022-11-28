package org.codedefenders.servlets.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.rmi.ServerException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.core.env.MissingRequiredPropertiesException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class APIUtils {
    public static void respondJsonError(HttpServletResponse response, String error, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        HashMap<String, String> map = new HashMap<>();
        map.put("error", error);
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(map));
        out.flush();
    }

    public static void respondJsonError(HttpServletResponse response, String error) throws IOException {
        respondJsonError(response, error, HttpStatus.SC_BAD_REQUEST);
    }

    public static Map<String, Object> getParametersOrRespondJsonError(HttpServletRequest request,
                                                                      HttpServletResponse response,
                                                                      Map<String, Class<?>> parameterTypes)
            throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : parameterTypes.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue();
            Optional<?> param;
            if (clazz.equals(String.class)) {
                param = ServletUtils.getStringParameter(request, name);
            } else if (clazz.equals(Integer.class)) {
                param = ServletUtils.getIntParameter(request, name);
            } else if (clazz.equals(Float.class)) {
                param = ServletUtils.getFloatParameter(request, name);
            } else {
                throw new ServerException("Cannot parse argument " + name + " of class " + clazz.getName());
            }
            if (!param.isPresent()) {
                respondJsonError(response, "Missing required parameter \"" + name + "\"");
                throw new MissingRequiredPropertiesException();
            }
            parameters.put(name, param.get());
        }
        return parameters;
    }

    public static Object parsePostOrRespondJsonError(HttpServletRequest request, HttpServletResponse response,
                                                     Class<?> clazz) throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new AnnotatedTypeAdapterFactory())
                .registerTypeAdapterFactory(EnumTypeAdapter.ENUM_FACTORY).create();
        try {
            return gson.fromJson(request.getReader(), clazz);
        } catch (JsonParseException e) {
            respondJsonError(response, e.getMessage());
            throw e;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface JsonOptional {
    }

    /**
     * Forces deserialization of all parameters, except those annotated with {@link JsonOptional}
     *
     * @see <a href="https://stackoverflow.com/a/62013873">StackOverflow</a>
     */
    private static class AnnotatedTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<? super T> rawType = typeToken.getRawType();
            Set<Field> requiredFields =
                    Stream.of(rawType.getDeclaredFields()).filter(f -> f.getAnnotation(JsonOptional.class) == null)
                            .collect(Collectors.toSet());
            if (requiredFields.isEmpty()) {
                return null;
            }
            TypeAdapter<T> baseAdapter = gson.getDelegateAdapter(AnnotatedTypeAdapterFactory.this, typeToken);
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter jsonWriter, T o) throws IOException {
                    baseAdapter.write(jsonWriter, o);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonElement jsonElement = Streams.parse(in);

                    if (jsonElement.isJsonObject()) {
                        ArrayList<String> missingFields = new ArrayList<>();
                        for (Field field : requiredFields) {
                            if (!jsonElement.getAsJsonObject().has(field.getName())) {
                                missingFields.add(field.getName());
                            }
                        }
                        if (!missingFields.isEmpty()) {
                            throw new JsonParseException(
                                    String.format("Missing required fields %s for %s", missingFields,
                                            rawType.getName()));
                        }
                    }
                    return baseAdapter.fromJsonTree(jsonElement);
                }
            };
        }
    }

    /**
     * Converts enum strings to uppercase and fails in case the enum value does not exist
     **/
    static final class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
        public static final TypeAdapterFactory ENUM_FACTORY = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                Class<? super T> rawType = typeToken.getRawType();
                if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
                    return null;
                }
                if (!rawType.isEnum()) {
                    rawType = rawType.getSuperclass(); // handle anonymous subclasses
                }
                @SuppressWarnings({"rawtypes", "unchecked"}) TypeAdapter<T> adapter =
                        (TypeAdapter<T>) new EnumTypeAdapter(rawType);
                return adapter;
            }
        };
        private final Map<String, T> nameToConstant = new HashMap<>();
        private final Map<String, T> stringToConstant = new HashMap<>();
        private final Map<T, String> constantToName = new HashMap<>();

        public EnumTypeAdapter(final Class<T> classOfT) {
            try {
                // Uses reflection to find enum constants to work around name mismatches for obfuscated classes
                // Reflection access might throw SecurityException, therefore run this in privileged context;
                // should be acceptable because this only retrieves enum constants, but does not expose anything else
                Field[] constantFields = AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
                    @Override
                    public Field[] run() {
                        Field[] fields = classOfT.getDeclaredFields();
                        ArrayList<Field> constantFieldsList = new ArrayList<>(fields.length);
                        for (Field f : fields) {
                            if (f.isEnumConstant()) {
                                constantFieldsList.add(f);
                            }
                        }

                        Field[] constantFields = constantFieldsList.toArray(new Field[0]);
                        AccessibleObject.setAccessible(constantFields, true);
                        return constantFields;
                    }
                });
                for (Field constantField : constantFields) {
                    @SuppressWarnings("unchecked") T constant = (T) (constantField.get(null));
                    String name = constant.name();
                    String toStringVal = constant.toString();

                    SerializedName annotation = constantField.getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        name = annotation.value();
                        for (String alternate : annotation.alternate()) {
                            nameToConstant.put(alternate, constant);
                        }
                    }
                    nameToConstant.put(name, constant);
                    stringToConstant.put(toStringVal, constant);
                    constantToName.put(constant, name);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public T read(JsonReader in) throws IOException, JsonParseException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String key = in.nextString().toUpperCase();
            T constant = nameToConstant.get(key);
            if (constant != null) {
                return constant;
            }
            constant = stringToConstant.get(key);
            if (constant != null) {
                return constant;
            }
            throw new JsonParseException(key + " is not a valid enum value");
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value == null ? null : constantToName.get(value));
        }
    }
}
