package org.codedefenders.notification.web;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import org.apache.commons.beanutils.PropertyUtils;
import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.client.ClientEvent;
import org.springframework.util.ReflectionUtils;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public class EventDecoder implements Decoder.Text<ClientEvent> {
    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public ClientEvent decode(String s) throws DecodeException {
        JsonObject obj;
        String type;
        JsonElement data;
        Class<ClientEvent> eventClass;

        try {
            obj = new JsonParser().parse(s).getAsJsonObject();
            type = obj.getAsJsonPrimitive("type").getAsString();
            data = obj.get("data");
        } catch (ClassCastException | IllegalStateException e) {
            throw new DecodeException(s, "Could not decode client event message type or data.", e);
        }

        try {
            eventClass = EventNames.toClientEvent(type);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new DecodeException(s, "Invalid type for client message: " + type + ".", e);
        }

        Gson gson = new GsonBuilder()
                /* Not sure if the type adapter is much slower than using Gson normally.
                   I'll include it for now to prevent some errors, but we might want to disable it later. */
                .registerTypeAdapter(eventClass, new ClientEventDeserializer(eventClass))
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.fromJson(data, eventClass);
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    /**
     * Deserializer that throws a {@link JsonParseException} on missing JSON attributes
     * instead of using {@code null} or {@code 0} as default values.
     * Uses the the {@link Expose} annotation.
     */
    private static class ClientEventDeserializer implements JsonDeserializer {
        Class clazz;

        ClientEventDeserializer(Class clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object deserialize(JsonElement jsonElement,
                                  Type jsonType,
                                  JsonDeserializationContext context) throws JsonParseException {
            Object obj;
            try {
                obj = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new JsonParseException("Error while creating event object of type: " + clazz.getName() + ". "
                        + "JSON was: " + jsonElement, e);
            }

            JsonObject data = jsonElement.getAsJsonObject();
            PropertyDescriptor[] attributes = PropertyUtils.getPropertyDescriptors(clazz);

            for (PropertyDescriptor attribute : attributes) {
                String name = attribute.getName();

                /* Skip "class" property from PropertyUtils. */
                if (name.equals("class")) {
                    continue;
                }

                /* Skip fields not marked as exposed for deserialization. */
                Field field = ReflectionUtils.findField(clazz, name);
                Expose expose = field.getAnnotation(Expose.class);
                if (expose == null || !expose.deserialize()) {
                    continue;
                }

                Class type = attribute.getPropertyType();
                Object value;

                /* Check if attribute is present in JSON. */
                if (!data.has(name)) {
                    throw new JsonParseException("JSON is missing attribute: " + name + ". "
                            + "JSON was: " + jsonElement);
                }

                /* Deserialize JSON attribute. */
                try {
                    value = context.deserialize(data.get(name), type);
                } catch (Exception e) {
                    throw new JsonParseException("Error while reading attribute from JSON: " + name + ". "
                            + "JSON was: " + jsonElement, e);
                }

                /* Set DTO attribute. */
                try {
                    attribute.getWriteMethod().invoke(obj, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new JsonParseException("Error while writing attribute to DTO: " + name + ". "
                            + "JSON was: " + jsonElement, e);
                }
            }

            return obj;
        }
    }
}
