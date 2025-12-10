package dev.fusionize.common.parser;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class JsonParser<T> {
    public final Gson gson;
    public static final JsonParser<Map> MAP = new JsonParser<>();

    public JsonParser() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
                .create();
    }

    public static class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss");

        @Override
        public JsonElement serialize(LocalDateTime localDateTime, Type srcType,
                                     JsonSerializationContext context) {

            return new JsonPrimitive(formatter.format(localDateTime));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {

            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    public class ZonedDateTimeTypeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss z");

        @Override
        public JsonElement serialize(ZonedDateTime zonedDateTime, Type srcType,
                                     JsonSerializationContext context) {

            return new JsonPrimitive(formatter.format(zonedDateTime));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {

            return ZonedDateTime.parse(json.getAsString(), formatter);
        }
    }

    public T fromJson(String json, Class<T> typeOfT){
        return gson.fromJson(json, typeOfT);
    }

    public String toJson(T t, Class<T> typeOfT){
        return gson.toJson(t, typeOfT);
    }
}
