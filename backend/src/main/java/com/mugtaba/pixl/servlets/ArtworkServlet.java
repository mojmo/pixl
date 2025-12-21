package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.exceptions.*;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.services.ArtworkService;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.ValidationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
public class ArtworkServlet extends BaseServlet {

    private static final String COMPONENT_NAME = "ArtworkServlet";
    private ArtworkService artworkService;

    @Override
    public void init() throws ServletException {
        super.init();
        artworkService = new ArtworkService();
        LogUtil.logInfo(COMPONENT_NAME, "init", "ArtworkServlet initialized successfully");
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
                String shareableLink = pathInfo.substring(7).trim();
                if (shareableLink.isEmpty()) {
                    throw new ValidationException("Shareable link is required");
                }
                handleGetByShareableLink(response, shareableLink);
            } else if (pathInfo.startsWith("/user/")) {
                String userIdStr = pathInfo.substring(6).trim();
                if (userIdStr.isEmpty()) {
                    throw new ValidationException("User ID is required");
                }
                handleGetUserArtworks(request, response, userIdStr);
            } else {
                // Try to parse as artwork ID - this handles URLs like /api/artworks/123
                String idStr = pathInfo.substring(1).trim(); // Remove leading slash
                if (idStr.isEmpty()) {
                    throw new ValidationException("Artwork ID is required");
                }

                if (!idStr.matches("^\\d+$")) {
                    throw new ValidationException("Invalid artwork ID format. Only numbers are allowed.");
                }
                
                Long artworkId = ValidationUtil.parseIdParameter(idStr, "artwork");
                handleGetArtworkById(request, response, artworkId);
            }
            
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", "Unexpected error in GET request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Unable to retrieve artwork. Please try again later."
            );
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

        try {

            Long userId = requireAuthentication(request);

            Artwork artwork = extractRequestObject(request, Artwork.class);

            // sanitize inputs before validation
            artwork.setTitle(sanitize(artwork.getTitle()));
            artwork.setDescription(sanitize(artwork.getDescription()));
            validateSqlSafe(artwork.getTitle(), "title");
            validateSqlSafe(artwork.getTitle(), "description");
            validateArtworkForCreation(artwork);

            artwork.setUserId(userId);

            // Get username from session for display
            Map<String, Object> sessionInfo = getUserSessionInfo(request);
            String username = sessionInfo.get("username").toString();
            if (username != null && !username.isEmpty()) {
                artwork.setUsername(username.trim());
            }

            Artwork createdArtwork = artworkService.createArtwork(artwork);

            LogUtil.logInfo(
                COMPONENT_NAME, "doPost",
                String.format("User %d created artwork %d successfully", userId, createdArtwork.getId())
            );
            sendCreatedResponse(response, "Artwork created successfully", createdArtwork);

        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", e.getMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (SQLException e) {
            LogUtil.logError(
                COMPONENT_NAME, "doPost",
                "Database error during artwork creation", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to create artwork. Please try again later."
            );
        } catch (Exception e) {
            LogUtil.logError(
                COMPONENT_NAME, "doPost",
                "Unexpected error in POST request", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to create artwork. Please try again later."
            );
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

        try {

            Long userId = requireAuthentication(request);

            String pathInfo = request.getPathInfo();
            // Check if the Artwork ID is a valid integer
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new ValidationException("Invalid artwork ID format");
            }

            Long artworkId = ValidationUtil.parseIdParameter(pathInfo.substring(1), "artwork");

            // Check ownership
            if (artworkService.isOwner(artworkId, userId)) {
                LogUtil.logUnauthorizedAccess(
                    COMPONENT_NAME, "doPut",
                    String.format("User %d attempted to update artwork %d", userId, artworkId)
                );
                throw new UnauthorizedException("You can only update your own artworks");
            }

            Artwork artwork = extractRequestObject(request, Artwork.class);

            // sanitize inputs
            if (artwork.getTitle() != null) {
                artwork.setTitle(sanitize(artwork.getTitle()));
            }
            if (artwork.getDescription() != null) {
                artwork.setDescription(sanitize(artwork.getDescription()));
            }

            validateArtworkForUpdate(artwork);

            artwork.setId(artworkId);
            artwork.setUserId(userId);

            boolean updated = artworkService.updateArtwork(artwork);
            if (updated) {
                LogUtil.logInfo(
                    COMPONENT_NAME, "doPut",
                    String.format("User %d updated artwork %d successfully", userId, artworkId)
                );
                sendSuccessResponse(response, "Artwork updated successfully", artwork);
            } else {
                throw new ResourceNotFoundException("Artwork");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPut", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (SQLException e) {
            LogUtil.logError(
                COMPONENT_NAME, "doPut",
                "Database error during artwork update", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to update artwork. Please try again later."
            );
        } catch (Exception e) {
            LogUtil.logError(
                COMPONENT_NAME, "doPut",
                "Unexpected error in PUT request", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to update artwork. Please try again later."
            );
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

        try {

            Long userId = requireAuthentication(request);

            String pathInfo = request.getPathInfo();
            // Check if the Artwork ID is a valid integer
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new ValidationException("Invalid Artwork ID format");
            }

            Long artworkId = ValidationUtil.parseIdParameter(pathInfo.substring(1), "artwork");

            // Check ownership
            Optional<Artwork> artworkOpt = artworkService.getArtworkById(artworkId);
            if (artworkOpt.isEmpty()) {
                throw new ResourceNotFoundException("Artwork not found");
            }

            validateOwnership(request, artworkOpt.get().getUserId());

            boolean deleted = artworkService.deleteArtwork(artworkId, userId);
            if (deleted) {
                LogUtil.logInfo(
                    COMPONENT_NAME, "doDelete",
                    String.format("User %d deleted artwork %d successfully", userId, artworkId)
                );
                sendSuccessResponse(response, "Artwork deleted successfully", null);
            } else {
                LogUtil.logWarning(
                    COMPONENT_NAME, "doDelete",
                    String.format(
                        "User %d attempted to delete non-existent or unauthorized artwork %d",
                        userId, artworkId
                    )
                );
                throw new ResourceNotFoundException("Artwork");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doDelete", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (SQLException e) {
            LogUtil.logError(COMPONENT_NAME, "doDelete", "Database error during artwork deletion", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Unable to delete artwork. Please try again later."
            );
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doDelete", "Unexpected error in DELETE request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to delete artwork. Please try again later"
            );
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
            throws PixlException, IOException, SQLException {

        String type = request.getParameter("type");
        if (!"public".equals(type)) {
            throw new ValidationException("Invalid type parameter. Only 'public' is supported.");
        }

        Map<String, Integer> pagination = validateAndGetPagination(request);

        int page = pagination.get("page");
        int limit = pagination.get("limit");
        int offset = pagination.get("offset");

        List<Artwork> artworks = artworkService.getPublicArtwork(limit, offset);
        int totalCount = artworkService.getPublicArtworkCount();

        Map<String, Object> result = new HashMap<>();
        result.put("artworks", artworks);
        result.put("pagination", createPaginationInfo(page, limit, totalCount));

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetArtworks",
            String.format(
                "Retrieved %d public artworks (page %d, limit %d)",
                artworks.size(), page, limit
            )
        );

        sendSuccessResponse(response, "Public artworks retrieved successfully", result);
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
                                    String userIdStr) throws PixlException, IOException, SQLException {
        
        Long currentUserId = requireAuthentication(request);
        Long userId;

        if ("me".equals(userIdStr)) {
            userId = currentUserId;
        } else {
            userId = ValidationUtil.parseIdParameter(userIdStr, "user");

            // Only allow users to view their own artworks
            if (!userId.equals(currentUserId)) {
                LogUtil.logUnauthorizedAccess(
                    COMPONENT_NAME, "handleGetUserArtworks",
                    String.format(
                        "User %d attempted to access user %d's artwork",
                        currentUserId, userId
                    )
                );
                throw new UnauthorizedException("You can only view your own artworks");
            }
        }

        List<Artwork> artworks = artworkService.getArtworksByUser(userId);

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetUserArtworks",
            String.format("Retrieved %d artworks for user %d", artworks.size(), userId)
        );

        sendSuccessResponse(response, "User artworks retrieved successfully", artworks);
    }

    /**
     * Handles retrieving an artwork by its ID.
     *
     * @param response the HttpServletResponse object
     * @param artworkId the ID of the artwork to retrieve
     * @throws SQLException if a database access error occurs
     * @throws IOException if an input or output error occurs
     */
    private void handleGetArtworkById(HttpServletRequest request, HttpServletResponse response,
                                    Long artworkId) throws PixlException, SQLException, IOException {

        Optional<Artwork> artworkOpt = artworkService.getArtworkById(artworkId);

        if (artworkOpt.isEmpty()) {
            throw new ResourceNotFoundException("Artwork");
        }

        Artwork artwork = artworkOpt.get();

        if (!artwork.isPublic()) {
            try {
                Long userId = requireAuthentication(request);
                Map<String, Object> sessionInfo = getUserSessionInfo(request);
                boolean isAdmin = Boolean.parseBoolean(sessionInfo.get("isAdmin").toString());
                // allow access if owner or admin
                if (!artwork.getUserId().equals(userId) && !isAdmin) {
                    throw new ForbiddenException("You don't have permission to view this artwork");
                }
            } catch (UnauthorizedException e) {
                // not Authenticated - can't view private artwork
                throw new UnauthorizedException("You don't have permission to view this artwork");
            }
        }

        LogUtil.logInfo(COMPONENT_NAME, "handleGetArtworkById", String.format("Retrieved artwork %d successfully", artworkId));
        sendSuccessResponse(response, "Artwork retrieved successfully", artwork);
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
                                        String shareableLink) throws PixlException, IOException, SQLException {
        
        ValidationUtil.validateStringNotEmpty(shareableLink, "Shareable link");

        Optional<Artwork> artwork = artworkService.getArtworkByLink(shareableLink);

        if (artwork.isPresent()) {
            LogUtil.logInfo(
                COMPONENT_NAME, "handleGetByShareableLink",
                String.format("Retrieved artwork via shareable link %s", shareableLink)
            );
            sendSuccessResponse(response, "Shared artwork retrieved successfully", artwork.get());
        } else {
            throw new ResourceNotFoundException("Shared artwork");
        }
    }

    /**
     * Validates the artwork for creation.
     * @param artwork the artwork to validate
     * @throws ValidationException if validation fails
     */
    private void validateArtworkForCreation(Artwork artwork) throws ValidationException {
        ValidationUtil.validateStringNotEmpty(artwork.getTitle(), "Title");
        ValidationUtil.validateStringLength(artwork.getTitle(), "Title", 1, 100);

        if (artwork.getDescription() != null) {
            ValidationUtil.validateStringLength(artwork.getDescription(), "Description", 0, 1000);
        }

        ValidationUtil.validateStringNotEmpty(artwork.getPixelData(), "Pixel data");
        ValidationUtil.validateRange(artwork.getWidth(), "Width", 1, 512);
        ValidationUtil.validateRange(artwork.getHeight(), "Height", 1, 512);

        // Validate total pixels don't exceed reasonable limit
        if (artwork.getWidth() * artwork.getHeight() > 65536) {
            throw new ValidationException("Artwork size is too large. Maximum 65,536 pixels allowed.");
        }
    }

    private void validateArtworkForUpdate(Artwork artwork) throws ValidationException {
        validateArtworkForCreation(artwork); // same validation rules
    }
}
