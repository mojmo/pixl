package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.services.ArtworkService;
import com.mugtaba.pixl.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet implementation class ArtworkServlet
 * Handles CRUD operations for artworks.
 */
@WebServlet("/api/artworks/*")
public class ArtworkServlet extends HttpServlet {

    private ArtworkService artworkService;

    @Override
    public void init() throws ServletException {
        super.init();
        artworkService = new ArtworkService();
    }

    /**
     * Handles HTTP GET requests for retrieving artworks.
     * Supports fetching all artworks, a specific artwork by ID or shareable link, or artworks by user.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetArtworks(request, response);
            } else if (pathInfo.startsWith("/share/")) {
                handleGetByShareableLink(response, pathInfo.substring(7));
            } else if (pathInfo.startsWith("/user/")) {
                handleGetUserArtworks(request, response, pathInfo.substring(6));
            } else if (pathInfo.matches("/\\d+")) {
                handleGetArtworkById(response, Long.parseLong(pathInfo.substring(1)));
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid artwork ID");
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving artwork: " + e.getMessage());
        }
    }

    /**
     * Handles HTTP POST requests for creating new artworks.
     * Requires user authentication.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        try {
            Artwork artwork = JsonUtil.fromJson(request.getReader(), Artwork.class);
            artwork.setUserId(userId);
            String username = request.getSession(false).getAttribute("username").toString();
            if (username != null && !username.isEmpty()) {
                artwork.setUsername(username);
            }

            Artwork createdArtwork = artworkService.createArtwork(artwork);
            sendSuccessResponse(response, "Artwork created successfully", createdArtwork);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in creating artwork: " + e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating artwork: " + e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating artwork");
        }
    }

    /**
     * Handles HTTP PUT requests for updating existing artworks.
     * Updates the artwork if the authenticated user owns it.
     *
     * @param request the HttpServletRequest object containing updated artwork data
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        String pathInfo = request.getPathInfo();
        // Check if the Artwork ID is a valid integer
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Artwork ID format");
            return;
        }

        try {
            Long artworkId = Long.parseLong(pathInfo.substring(1));

            // Check ownership
            if (!artworkService.isArtworkOwner(artworkId, userId)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to update this artwork");
                return;
            }

            Artwork artwork = JsonUtil.fromJson(request.getReader(), Artwork.class);
            artwork.setId(artworkId);
            artwork.setUserId(userId);

            if (!artwork.validateData()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid artwork data");
                return;
            }

            boolean updated = artworkService.updateArtwork(artwork);
            if (updated) {
                sendSuccessResponse(response, "Artwork updated successfully", artwork);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Artwork not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Artwork ID");
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating artwork: " +e.getMessage());
        }
    }

    /**
     * Handles HTTP DELETE requests for deleting artworks.
     * Deletes the artwork if the authenticated user owns it.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        String pathInfo = request.getPathInfo();
        // Check if the Artwork ID is a valid integer
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Artwork ID format");
            return;
        }

        try {
            Long artworkId = Long.parseLong(pathInfo.substring(1));

            boolean deleted = artworkService.deleteArtwork(artworkId, userId);
            if (deleted) {
                sendSuccessResponse(response, "Artwork deleted successfully", null);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Artwork not found or access denied");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Artwork ID");
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error deleting artwork: " + e.getMessage());
        }
    }

    /**
     * Handles retrieving public artworks with pagination.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     * @throws SQLException if a database access error occurs
     */
    private void handleGetArtworks(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        String type = request.getParameter("type");
        int page = getIntParameter(request, "page", 1);
        int limit = getIntParameter(request, "limit", 20);
        int offset = (page - 1) * limit;

        if ("public".equals(type)) {
            List<Artwork> artworks = artworkService.getPublicArtwork(limit, offset);
            int totalCount = artworkService.getPublicArtworkCount();

            Map<String, Object> result = new HashMap<>();
            result.put("artworks", artworks);
            result.put("pagination", createPaginationInfo(page, limit, totalCount));

            sendSuccessResponse(response, "Public artworks retrieved successfully", result);
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid type parameter");
        }
    }

    /**
     * Handles retrieving artworks for a specific user.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @param userIdStr the user ID as a string ("me" for current user or numeric ID)
     * @throws IOException if an input or output error occurs
     * @throws SQLException if a database access error occurs
     */
    private void handleGetUserArtworks(HttpServletRequest request, HttpServletResponse response,
                                    String userIdStr) throws IOException, SQLException {
        
        Long currentUserId = getUserIdFromSession(request);
        if (currentUserId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        try {
            Long userId = "me".equals(userIdStr) ? currentUserId : Long.parseLong(userIdStr);
            List<Artwork> artworks = artworkService.getArtworksByUser(userId);
            sendSuccessResponse(response, "User artworks retrieved successfully", artworks);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID");
        }
    }

    /**
     * Handles retrieving an artwork by its ID.
     *
     * @param response the HttpServletResponse object
     * @param artworkId the ID of the artwork to retrieve
     * @throws SQLException if a database access error occurs
     * @throws IOException if an input or output error occurs
     */
    private void handleGetArtworkById(HttpServletResponse response,
                                      Long artworkId) throws SQLException, IOException {

        Optional<Artwork> artwork = artworkService.getArtworkById(artworkId);
        if (artwork.isPresent()) {
            sendSuccessResponse(response, "Artwork retrieved successfully", artwork.get());
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Artwork not found");
        }
    }

    /**
     * Handles retrieving an artwork by its shareable link.
     *
     * @param response the HttpServletResponse object
     * @param shareableLink the shareable link of the artwork
     * @throws IOException if an input or output error occurs
     * @throws SQLException if a database access error occurs
     */
    private void handleGetByShareableLink(HttpServletResponse response,
                                          String shareableLink) throws IOException, SQLException {

        Optional<Artwork> artwork = artworkService.getArtworkByLink(shareableLink);

        if (artwork.isPresent()) {
            sendSuccessResponse(response, "Artwork retrieved successfully", artwork.get());
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Artwork not found");
        }
    }

    /**
     * Retrieves the user ID from the session.
     *
     * @param request the HttpServletRequest object
     * @return the user ID if present, otherwise null
     */
    private Long getUserIdFromSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object userId = session.getAttribute("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            } 
        }
        return null;
    }

    /**
     * Retrieves an integer parameter from the request, returning a default value if not present or invalid.
     *
     * @param request the HttpServletRequest object
     * @param paramName the name of the parameter to retrieve
     * @param defaultValue the default value to return if the parameter is not present or invalid
     * @return the integer value of the parameter or the default value
     */
    private int getIntParameter(HttpServletRequest request, String paramName, int defaultValue) {
        String param = request.getParameter(paramName);
        if (param != null) {
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Creates pagination information for the response.
     *
     * @param currentPage the current page number
     * @param limit the number of items per page
     * @param totalCount the total number of items
     * @return a map containing pagination details
     */
    private Map<String, Object> createPaginationInfo(int currentPage, int limit, int totalCount) {

        Map<String, Object> pagination = new HashMap<>();

        pagination.put("currentPage", currentPage);
        pagination.put("limit", limit);
        pagination.put("totalCount", totalCount);
        pagination.put("totalPages", (int) Math.ceil((double) totalCount / limit));
        pagination.put("hasNext", currentPage * limit < totalCount);
        pagination.put("hasPrevious", currentPage > 1);

        return pagination;
    }

    /**
     * Sends a success response with the specified message and data.
     * 
     * @param response the HttpServletResponse object
     * @param message the success message
     * @param data the response data
     * @throws IOException if an input or output error occurs
     */
    private void sendSuccessResponse(HttpServletResponse response, String message, Object data) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = new ApiResponse<>(true, message, data);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends an error response with the specified status code and message.
     * 
     * @param response the HttpServletResponse object
     * @param statusCode the HTTP status code
     * @param message the error message
     * @throws IOException if an input or output error occurs
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = new ApiResponse<>(false, message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }
}
