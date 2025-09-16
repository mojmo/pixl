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
}
