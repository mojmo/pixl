package com.mugtaba.pixl.exceptions;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception class for resource not found errors in the Pixl application.
 * This exception represents client-side errors, typically associated with HTTP status code 404 (Not Found).
 * It extends the base PixlException class to provide specific handling for resource not found issues.
 */
public class ResourceNotFoundException extends PixlException {

    /**
     * Constructor for ResourceNotFoundException.
     * This exception is initialized with a user-friendly message and defaults to HTTP status code 404.
     * @param resource the name or identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resource) {
        super(
            HttpServletResponse.SC_NOT_FOUND,
            resource + " not found",
            "Resource not found: " + resource
        );
    }

}
