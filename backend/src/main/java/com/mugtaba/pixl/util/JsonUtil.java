package com.mugtaba.pixl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Utility class for JSON serialization and response handling.
 * Provides methods to convert Java objects to JSON format and write JSON responses
 * to HTTP servlet responses. Uses Jackson ObjectMapper for JSON processing.
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates and configures an ObjectMapper with Java 8 date/time support.
     *
     * @return configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());

        // configure data formatting
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /**
     * Writes a Java object as JSON to the HTTP servlet response.
     * Sets the appropriate content type and character encoding for JSON responses.
     *
     * @param response the HttpServletResponse to write the JSON to
     * @param data the Java object to be serialized to JSON
     * @throws IOException if an I/O error occurs during writing to the response
     * @throws IllegalArgumentException if the response or data parameters are null
     */
    public static void writeJsonResponse(HttpServletResponse response, Object data) throws IOException {

        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data object cannot be null");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Serialize the Java object to JSON and write to response output stream
        objectMapper.writeValue(response.getWriter(), data);
    }

    /**
     * Parses a JSON request body into a Java object
     *
     * @param request the HttpServletRequest containing JSON data
     * @param tClass the class of the object to parse
     * @return the parsed object, or null if parsing fails
     * @param <T> the type of the object to parse
     */
    public static <T> T parseJsonRequest(HttpServletRequest request, Class<T> tClass) {
        try {
            return objectMapper.readValue(request.getReader(), tClass);
        } catch (IOException e) {
            System.err.println("Error parsing JSON request: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a Java object to its JSON string representation.
     *
     * @param data the Java object to be serialized to JSON string
     * @return JSON string representation of the object
     * @throws IOException if an error occurs during JSON serialization
     * @throws IllegalArgumentException if the data parameter is null
     */
    public static String toJsonString(Object data) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data object cannot be null");
        }

        return objectMapper.writeValueAsString(data);
    }
}
