package com.mugtaba.pixl.util;

import com.mugtaba.pixl.exceptions.ValidationException;

/**
 * Utility class for validating various input parameters in the Pixl application.
*/
public class ValidationUtil {

    /**
     * Validates pagination parameters.
     * @param page the page number (1-based)
     * @param limit the number of items per page
     * @throws ValidationException if validation fails
     */
    public static void validatePagination(int page, int limit) throws ValidationException {
        if (page < 1) {
            throw new ValidationException("Page number must be 1 or greater");
        }

        if (limit < 1) {
            throw new ValidationException("Limit must be 1 or greater");
        }

        if (limit > 100) {
            throw new ValidationException("Limit cannot exceed 100 items per page");
        }
    }

    /**
     * Validates that an ID is a positive number.
     * @param id the ID to validate
     * @param resourceName the name of the resource for error messaging
     * @throws ValidationException if validation fails
     */
    public static void validateId(Long id, String resourceName) throws ValidationException {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid " + resourceName + " ID");
        }
    }

    /**
     * Validates that a string is neither null nor empty.
     * @param value the string to validate
     * @param fieldName the name of the field for error messaging
     * @throws ValidationException if validation fails
     */
    public static void validateStringNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required and cannot be empty");
        }
    }

    /**
     * Validates that a string's length is within specified bounds.
     * @param value the string to validate
     * @param fieldName the name of the field for error messaging
     * @param minLength the minimum length of the string
     * @param maxLength the maximum length of the string
     * @throws ValidationException if validation fails
     */
    public static void validateStringLength(String value, String fieldName, int minLength, int maxLength) throws ValidationException {
        if (value == null) {
            throw new ValidationException(fieldName + " is required");
        }

        if (value.length() < minLength) {
            throw new ValidationException(fieldName + " must be at least " + minLength + " characters long");
        }

        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }

    /**
     * Validates that an integer is within a specified range.
     * @param value the integer to validate
     * @param fieldName the name of the field for error messaging
     * @param min the minimum acceptable value
     * @param max the maximum acceptable value
     * @throws ValidationException if validation fails
     */
    public static void validateRange(int value, String fieldName, int min, int max) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(fieldName + " must be between " + min + "and " + max);
        }
    }

    /**
     * Parses and validates the "page" query parameter.
     * @param pageParam the page parameter as a string
     * @return the validated page number
     * @throws ValidationException if validation fails
     */
    public static int parsePageParameter(String pageParam) throws ValidationException {
        if (pageParam == null || pageParam.isEmpty()) {
            return 1; // Default page
        }

        try {
            int page = Integer.parseInt(pageParam);
            if (page < 1) {
                throw new ValidationException("Page number must be 1 or greater");
            }
            return page;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid page number format");
        }
    }

    /**
     * Parses and validates the "limit" query parameter.
     * @param limitParam the limit parameter as a string
     * @return the validated limit
     * @throws ValidationException if validation fails
     */
    public static int parseLimitParameter(String limitParam) throws ValidationException {
        if (limitParam == null || limitParam.isEmpty()) {
            return 20; // Default limit
        }

        try {
            int limit = Integer.parseInt(limitParam);
            if (limit < 1) {
                throw new ValidationException("Limit must be 1 or greater");
            }

            if (limit > 100) {
                throw new ValidationException("Limit cannot exceed 100 items per page");
            }

            return limit;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid limit format");
        }
    }

    /**
     * Parses and validates an ID parameter from a string.
     * @param idParam the ID parameter as a string
     * @param resourceName the name of the resource for error messaging
     * @return the validated ID as a long
     * @throws ValidationException if validation fails
     */
    public static Long parseIdParameter(String idParam, String resourceName) throws ValidationException {
        if (idParam == null || idParam.trim().isEmpty()) {
            throw new ValidationException("Invalid " + resourceName + " ID");
        }
        
        // Trim and check for invalid characters
        String cleanId = idParam.trim();
        
        // Check if contains only digits
        if (!cleanId.matches("^\\d+$")) {
            throw new ValidationException("Invalid " + resourceName + " ID format. Only numbers are allowed.");
        }
        
        try {
            long id = Long.parseLong(cleanId);
            if (id <= 0) {
                throw new ValidationException("Invalid " + resourceName + " ID. Must be greater than 0.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid " + resourceName + " ID format. Only numbers are allowed.");
        }
    }

    /**
     * Validates email format using regex pattern.
     * @param email the email to validate
     * @return true if email format is valid
    */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

        return email.trim().toLowerCase().matches(emailRegex);
    }
}
