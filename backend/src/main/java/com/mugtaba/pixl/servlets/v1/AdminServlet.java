package com.mugtaba.pixl.servlets.v1;

import com.mugtaba.pixl.exceptions.*;
import com.mugtaba.pixl.models.AdminStats;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.models.UserActivity;
import com.mugtaba.pixl.services.AdminService;
import com.mugtaba.pixl.servlets.BaseServlet;
import com.mugtaba.pixl.util.CacheUtil;
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

/**
 * Admin servlet for managing users, artworks, and monitoring system statistics.
 * protected by AdminAuthFilter - requires admin privileges to access.
 * 
 * Endpoints:
 * - GET    /api/v1/admin/stats - Get platform statistics
 * - GET    /api/v1/admin/users - Get all users (with pagination)
 * - GET    /api/v1/admin/activity - Get user activity logs (with pagination)
 * - DELETE /api/v1/admin/users/{id} - Delete user
 * - PUT    /api/v1/admin/users/{id}/status - Update user status
 * - GET    /api/v1/admin/artworks - Get all artworks (with pagination)
 * - DELETE /api/v1/admin/artworks/{id} - Delete artwork
 * - GET    /api/v1/admin/cache/stats - Get cache statistics
 * - DELETE /api/v1/admin/cache - Clear all cache
 */
@WebServlet("/api/v1/admin/*")
public class AdminServlet extends BaseServlet {

    public static final String COMPONENT_NAME = "AdminServlet[v1]";
    private AdminService adminService;

    @Override
    public void init() throws ServletException {
        super.init();
        adminService = new AdminService();
        LogUtil.logInfo(COMPONENT_NAME, "init", "AdminServlet v1 initialized successfully");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo();

        try {
            switch (pathInfo) {
                case "/stats" -> handleGetStats(response);
                case "/users" -> handleGetUsers(request, response);
                case "/users/activity" -> handleGetUserActivity(request, response);
                case "/artworks" -> handleGetArtworks(request, response);
                case "/cache/stats" -> handleGetCacheStats(response);
                default -> {
                    if (pathInfo.startsWith("/users/")) {
                        throw new ResourceNotFoundException("User endpoint");
                    } else if (pathInfo.startsWith("/artworks/")) {
                        throw new ResourceNotFoundException("Artwork endpoint");
                    } else {
                        throw new ResourceNotFoundException("Admin endpoint");
                    }
                }
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", e.getLogMessage(), e);    
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", "Unexpected error in admin GET request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to process admin request. Please try again later."
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo.startsWith("/users/") && pathInfo.endsWith("/admin")) {
                handleToggleUserAdmin(request, response, pathInfo);
            } else {
                throw new ResourceNotFoundException("Admin endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", "Unexpected error in admin POST request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to process admin request. Please try again later."
            );
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo.startsWith("/users/")) {
                handleDeleteUser(response, pathInfo);
            } else if (pathInfo.startsWith("/artworks/")) {
                handleDeleteArtwork(response, pathInfo);
            } else if (pathInfo.equals("/cache")) {
                handleClearCache(response);
            } else {
                throw new ResourceNotFoundException("Admin endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doDelete", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doDelete", "Unexpected error in admin DELETE request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to process admin request. Please try again later."
            );
        }
    }

    /**
     * Handle getting system statistics.
     * @param response HttpServletResponse object
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleGetStats(HttpServletResponse response) throws SQLException, IOException {
        AdminStats stats = adminService.getSystemStats();

        LogUtil.logInfo(COMPONENT_NAME, "handleGetStats", "System statistics retrieved");
        sendSuccessResponse(response, "System statistics retrieved successfully", stats);
    }

    /**
     * Handle getting all users with pagination.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleGetUsers(HttpServletRequest request, HttpServletResponse response) throws PixlException, SQLException, IOException {

        Map<String, Integer> pagination = validateAndGetPagination(request);
        int page = pagination.get("page");
        int limit = pagination.get("limit");
        int offset = pagination.get("offset");

        List<User> users = adminService.getAllUsers(limit, offset);
        long totalCount = adminService.getUserCount();

        // Convert to public users (hide sensitive info)
        List<User> publicUsers = users.stream()
            .map(User::toPublicUser)
            .toList();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("users", publicUsers);
        responseData.put("pagination", createPaginationInfo(page, limit, totalCount));

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetUsers",
            String.format("Retrieved %d users (page; %d, limit: %d)", users.size(), page, limit)
        );
        sendSuccessResponse(response, "Users retrieved successfully", responseData);
    }

    /**
     * Handle getting user activity stats with pagination.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleGetUserActivity (HttpServletRequest request, HttpServletResponse response) throws PixlException, SQLException, IOException {

        Map<String, Integer> pagination = validateAndGetPagination(request);
        int page = pagination.get("page");
        int limit = pagination.get("limit");
        int offset = pagination.get("offset");

        List<UserActivity> activities = adminService.getUserActivity(limit, offset);
        long totalCount = adminService.getUserCount();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("activities", activities);
        responseData.put("pagination", createPaginationInfo(page, limit, totalCount));

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetUserActivity",
            String.format("Retrieved %d user activities (page: %d, limit: %d)", activities.size(), page, limit)
        );
        sendSuccessResponse(response, "User activities retrieved successfully", responseData);
    }

    /**
     * Handle getting all artworks with pagination.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleGetArtworks(HttpServletRequest request, HttpServletResponse response) throws PixlException, SQLException, IOException {

        Map<String, Integer> pagination = validateAndGetPagination(request);
        int page = pagination.get("page");
        int limit = pagination.get("limit");
        int offset = pagination.get("offset");

        List<Artwork> artworks = adminService.getAllArtworks(limit, offset);
        long totalCount = adminService.getArtworkCount();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("artworks", artworks);
        responseData.put("pagination", createPaginationInfo(page, limit, totalCount));

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetArtworks",
            String.format("Retrieved %d artworks (page: %d, limit: %d)", artworks.size(), page, limit)
        );
        sendSuccessResponse(response, "Artworks retrieved successfully", responseData);
    }

    /**
     * Handle getting cache statistics
     * @param response HttpServletResponse object
     * @throws IOException if there is a problem writing the response
     */
    private void handleGetCacheStats(HttpServletResponse response) throws IOException {
        Map<String, Object> cacheStats = CacheUtil.getStats();

        LogUtil.logInfo(COMPONENT_NAME, "handleGetCacheStats", "Cache statistics retrieved");
        sendSuccessResponse(response, "Cache statistics retrieved successfully", cacheStats);
    }

    /**
     * Handle toggling user admin status.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @param pathInfo the path info from the request to extract user ID
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleToggleUserAdmin(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws PixlException, SQLException, IOException {

        // Extract user ID from path
        String[] parts = pathInfo.split("/");
        if (parts.length < 3) {
            throw new ValidationException("Invalid user ID format");
        }

        Long userId = ValidationUtil.parseIdParameter(parts[2], "user");

        Map<String, String> requestData = extractRequestData(request);
        String isAdminStr = requestData.get("isAdmin");

        if (isAdminStr == null) {
            throw new ValidationException("isAdmin field is required");
        }

        boolean isAdmin = Boolean.parseBoolean(isAdminStr);

        boolean updated = adminService.updateUserAdminStatus(userId, isAdmin);

        if (updated) {
            LogUtil.logInfo(
                COMPONENT_NAME, "handleToggleUserAdmin",
                String.format("Updated user %d admin status to: %s", userId, isAdmin)
            );
            sendSuccessResponse(response, "User admin status updated successfully");
        } else {
            throw new ResourceNotFoundException("User");
        }
    }

    /**
     * Handle deleting a user

     * @param response HttpServletResponse object
     * @param pathInfo the path info from the request to extract user ID
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleDeleteUser(HttpServletResponse response, String pathInfo) throws PixlException, SQLException, IOException {

        // extract user ID from path
        String idStr = pathInfo.substring(7).trim(); // remove "/users/"

        if (!idStr.matches("^\\d+$")) {
            throw new ValidationException("Invalid user ID format. Only numbers are allowed.");
        }

        Long userId = ValidationUtil.parseIdParameter(idStr, "user");

        boolean deleted = adminService.deleteUser(userId);

        if (deleted) {
            LogUtil.logInfo(
                COMPONENT_NAME, "handleDeleteUser",
                String.format("Admin deleted user %d", userId)
            );
            sendSuccessResponse(response, "User deleted successfully");
        } else {
            throw new ResourceNotFoundException("User");
        }

    }

    /**
     * Handle deleting an artwork
     * @param response HttpServletResponse object
     * @param pathInfo the path info from the request to extract artwork ID
     * @throws PixlException if there is a problem with the request
     * @throws SQLException if there is a problem with the database
     * @throws IOException if there is a problem writing the response
     */
    private void handleDeleteArtwork(HttpServletResponse response, String pathInfo) throws PixlException, SQLException, IOException {

        // extract artwork ID from path
        String idStr = pathInfo.substring(10).trim(); // Remove "/artworks/"
        
        if (!idStr.matches("^\\d+$")) {
            throw new ValidationException("Invalid artwork ID format. Only numbers are allowed.");
        }
        
        Long artworkId = ValidationUtil.parseIdParameter(idStr, "artwork");
        
        boolean deleted = adminService.deleteArtwork(artworkId);
        
        if (deleted) {
            LogUtil.logInfo(
                COMPONENT_NAME, "handleDeleteArtwork",
                String.format("Admin deleted artwork %d", artworkId)
            );
            
            sendSuccessResponse(response, "Artwork deleted successfully");
        } else {
            throw new ResourceNotFoundException("Artwork");
        }
    }

    /**
     * Handle clearing the application cache
     * @param response HttpServletResponse object
     * @throws IOException if there is a problem writing the response
     */
    private void handleClearCache(HttpServletResponse response) throws IOException {
        CacheUtil.clear();

        LogUtil.logInfo(COMPONENT_NAME, "handleClearCache", "Admin cleared application cache");
        sendSuccessResponse(response, "Application cache cleared successfully");
    }

}
