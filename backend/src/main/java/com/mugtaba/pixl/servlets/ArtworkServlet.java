package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.services.ArtworkService;
import com.mugtaba.pixl.util.JsonUtil;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

/**
 * Servlet handling CRUD operations for artworks.
 * Provide endpoints for creating, retrieving, updating, and deleting artwork.
 * Requires user authentication for all operations.
 *
 * <p>Endpoint structure:
 *  <ul>
 *      <li>GET /api/artworks - Get all artworks for the authenticated user</li>
 *      <li>GET /api/artworks/{id} - Get a specific artwork by ID</li>
 *      <li>POST /api/artworks - Create a new artwork</li>
 *      <li>PUT /api/artworks/{id} - Update an existing artwork</li>
 *      <li>DELETE /api/artworks/{id} - Delete an artwork</li>
 *  </ul>
 * </p>
 */
@WebServlet("/api/artworks/*")
public class ArtworkServlet extends HttpServlet {
    private final ArtworkService artworkService = new ArtworkService();

    /**
     * Handles HTTP GET requests for retrieving artworks.
     * Supports retrieving all artworks for the authenticated user or a specific artwork by ID.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            // Check authentication
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Not authenticated"));
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String pathInfo = request.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                // get all artworks for the authenticated user
                List<Artwork> artworks = artworkService.getArtworksByUserId(userId);
                response.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "Artworks retrieved successfully", artworks));
            } else {
                // get specific artwork
                try {
                    int artworkId = Integer.parseInt(pathInfo.substring(1));
                    Artwork artwork = artworkService.getArtworkById(artworkId);

                    if (artwork != null) {
                        // Check if the authenticated user owns this artwork
                        if (artwork.getUserId() != userId) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Access denied"));
                            return;
                        }

                        response.setStatus(HttpServletResponse.SC_OK);
                        JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "Artwork retrieved successfully", artwork));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Artwork not found"));
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Invalid Artwork ID"));
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Internal server error: " + e.getMessage()));
        }

    }

    /**
     * Handles HTTP POST requests for creating new artworks.
     * Creates a new artwork for the authenticated user.
     *
     * @param request the HttpServletRequest object containing artwork data
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Not authenticated"));
                return;
            }

            int userId = (Integer) session.getAttribute("userId");

            // Parse JSON request body to Artwork object
            Artwork artwork = JsonUtil.parseJsonRequest(request, Artwork.class);
            if (artwork == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Invalid artwork data"));
                return;
            }

            // Set the user ID from session
            artwork.setUserId(userId);

            // Validate required fields
            if (artwork.getTitle() == null || artwork.getTitle().trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Title is required"));
                return;
            }

            if (artwork.getPixels() == null || artwork.getPixels().trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Pixel data is required"));
                return;
            }

            if (artwork.getWidth() <= 0 || artwork.getHeight() <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Width and height must be positive integers"));
                return;
            }

            // Create the artwork
            boolean created = artworkService.createArtwork(artwork);

            if (created) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "Artwork created successfully", artwork));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Failed to create artwork"));
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Handles HTTP PUT requests for updating existing artworks.
     * Updates the artwork if the authenticated user owns it
     *
     * @param request  the HttpServletRequest object containing updated artwork data
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Not authenticated"));
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String pathInfo = request.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Artwork ID is required"));
                return;
            }

            try {
                int artworkId = Integer.parseInt(pathInfo.substring(1));

                // Check if the artwork exist and belongs to the user
                Artwork existingArtwork = artworkService.getArtworkById(artworkId);
                if (existingArtwork == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Artwork not found"));
                    return;
                }

                if (existingArtwork.getUserId() != userId) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Access denied"));
                    return;
                }

                // Parse JSON request body to Artwork object
                Artwork updatedArtwork = JsonUtil.parseJsonRequest(request, Artwork.class);
                if (updatedArtwork == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Invalid artwork data"));
                    return;
                }

                // Preserve the ID and user ID
                updatedArtwork.setId(artworkId);
                updatedArtwork.setUserId(userId);

                // Validate required fields
                if (updatedArtwork.getTitle() == null || updatedArtwork.getTitle().trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Title is required"));
                    return;
                }

                if (updatedArtwork.getPixels() == null || updatedArtwork.getPixels().trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Pixel data is required"));
                    return;
                }

                if (updatedArtwork.getWidth() <= 0 || updatedArtwork.getHeight() <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Width and height must be positive integers"));
                    return;
                }

                // Update the artwork
                boolean updated = artworkService.updateArtwork(updatedArtwork);

                if (updated) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "Artwork updated successfully", updatedArtwork));
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Failed to update artwork"));
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Invalid Artwork ID format"));
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Internal server error: " + e.getMessage()));
        }
    }
}
