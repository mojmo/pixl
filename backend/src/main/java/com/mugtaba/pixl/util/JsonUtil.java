package com.mugtaba.pixl.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Utility class for JSON serialization and response handling.
 * Provides methods to convert Java objects to JSON format and write JSON responses
 * to HTTP servlet responses. Uses Jackson ObjectMapper for JSON processing.
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();

        // Configure for java 8 time support
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure deserialization
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // Configure serialization
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configOverride(Map.class).setInclude(
                Value.construct(
                        JsonInclude.Include.NON_NULL,
                        JsonInclude.Include.NON_NULL
                )
        );
    }

    /**
     * Converts object to JSON string
     *
     * @param object the object to convert
     * @return JSON string representation of the object
     * @throws JsonProcessingException if conversion fails
     */
    public static String toJson(Object object) throws JsonProcessingException {
        if (object == null) {
            return null;
        }

        return objectMapper.writeValueAsString(object);
    }

    /**
     * Converts JSON string to object
     *
     * @param <T>   the type of the object
     * @param json  the JSON string
     * @param clazz the class of the object
     * @return the converted object
     * @throws JsonProcessingException if conversion fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        return objectMapper.readValue(json, clazz);
    }


    /**
     * Reads JSON from BufferReader and converts to object
     *
     * @param <T>    the type of the object
     * @param reader the BufferedReader to read JSON from
     * @param clazz  the class of the object
     * @return the converted object
     * @throws IOException if reading or conversion fails
     */
    public static <T> T fromJson(BufferedReader reader, Class<T> clazz) throws IOException {
        if (reader == null) {
            return null;
        }

        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }

        String json = jsonBuilder.toString().trim();
        if (json.isEmpty()) {
            return null;
        }

        return fromJson(json, clazz);
    }

    /**
     * Reads JSON from HttpServletRequest and converts to object
     *
     * @param <T>     the type of the object
     * @param request the HttpServletRequest to read JSON from
     * @param clazz   the class of the object
     * @return the converted object
     * @throws IOException if read operation failed
     */
    public static <T> T fromRequest(HttpServletRequest request, Class<T> clazz) throws IOException {
        return fromJson(request.getReader(), clazz);
    }


    /**
     * Writes object as JSON to Writer
     *
     * @param writer the Writer to write JSON to
     * @param object the object to convert to JSON
     * @throws IOException if writing fails
     */
    public static void writeJson(Writer writer, Object object) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("Writer cannot be null");
        }

        if (object == null) {
            writer.write("null");
            return;
        }

        String json = toJson(object);
        writer.write(json);
        writer.flush();
    }
}