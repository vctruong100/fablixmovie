import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            if (System.getenv("FAB-DEV") == null) {
                System.out.println("DashboardServlet: using moviedb-write in prod mode");
                dataSource = (DataSource) new InitialContext()
                        .lookup("java:comp/env/jdbc/moviedb-write");
            } else {
                System.out.println("DashboardServlet: using moviedb-write in dev mode");
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String action = request.getParameter("action");

        switch (action) {
            case "addMovie":
                handleAddMovie(request, response);
                break;
            case "addStar":
                handleAddStar(request, response);
                break;
            case "addGenre":
                handleAddGenre(request, response);
                break;
            default:
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("status", "error");
                responseJson.addProperty("message", "Invalid action.");
                response.getWriter().write(responseJson.toString());
                break;
        }
    }

    // Calls the add_movie stored procedure to add a movie
    private void handleAddMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String movieTitle = request.getParameter("title");
        int movieYear = Integer.parseInt(request.getParameter("year"));
        String movieDirector = request.getParameter("director");
        String starName = request.getParameter("star");
        String genreName = request.getParameter("genre");

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?, ?)}");
            cs.setString(1, movieTitle);
            cs.setInt(2, movieYear);
            cs.setString(3, movieDirector);
            cs.setString(4, starName);
            cs.setString(5, genreName);
            cs.registerOutParameter(6, Types.VARCHAR);

            cs.execute();
            String statusMessage = cs.getString(6);

            JsonObject responseJson = new JsonObject();
            if ("Movie already exists.".equals(statusMessage)) {
                responseJson.addProperty("status", "error");
                responseJson.addProperty("message", statusMessage);
            } else {
                // If no exception and movie added successfully, include all details
                String movieId = fetchLatestMovieId(conn);
                String starId = fetchExistingStarId(conn, starName);
                int genreId = fetchExistingGenreId(conn, genreName);

                responseJson.addProperty("status", "success");
                responseJson.addProperty("message", String.format(
                        "%s\nMovie: %s (ID: %s)\nStar: %s (ID: %s)\nGenre: %s (ID: %d)",
                        statusMessage, movieTitle, movieId, starName, starId, genreName, genreId
                ));
            }

            response.getWriter().write(responseJson.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Error adding movie: " + e.getMessage());
            response.getWriter().write(responseJson.toString());
        }
    }

    // Adds a new star to the database with generated ID
    private void handleAddStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String starName = request.getParameter("starName");
        String birthYearStr = request.getParameter("birthYear");
        Integer birthYear = (birthYearStr == null || birthYearStr.isEmpty()) ? null : Integer.parseInt(birthYearStr);

        try (Connection conn = dataSource.getConnection()) {
            // Generate a new star ID with prefix "nm"
            String starId = "nm0000001";
            String selectMaxStarIdQuery = "SELECT MAX(id) FROM stars WHERE id LIKE 'nm%'";
            Statement selectMaxStarIdStatement = conn.createStatement();
            ResultSet rs = selectMaxStarIdStatement.executeQuery(selectMaxStarIdQuery);
            if (rs.next() && rs.getString(1) != null) {
                starId = "nm" + String.format("%07d", Integer.parseInt(rs.getString(1).substring(2)) + 1);
            }
            // Insert star with optional birthYear
            String insertStarQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(insertStarQuery);
            statement.setString(1, starId);
            statement.setString(2, starName);
            if (birthYear != null) {
                statement.setInt(3, birthYear);
            } else {
                statement.setNull(3, Types.INTEGER);
            }

            int rowsAffected = statement.executeUpdate();
            JsonObject responseJson = new JsonObject();
            if (rowsAffected > 0) {
                responseJson.addProperty("status", "success");
                responseJson.addProperty("message", String.format(
                        "Star added successfully.\nStar: %s (ID: %s)", starName, starId));
            } else {
                responseJson.addProperty("status", "fail");
                responseJson.addProperty("message", "Failed to add star");
            }
            response.getWriter().write(responseJson.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    private void handleAddGenre(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String genreName = request.getParameter("genreName");

        try (Connection conn = dataSource.getConnection()) {
            // Check if genre already exists
            String selectGenreQuery = "SELECT id FROM genres WHERE name = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectGenreQuery)) {
                selectStmt.setString(1, genreName);
                ResultSet rs = selectStmt.executeQuery();

                JsonObject responseJson = new JsonObject();

                if (rs.next()) {
                    int genreId = rs.getInt("id");
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", String.format(
                            "Genre already exists.\nGenre: %s (ID: %d)", genreName, genreId));
                    response.getWriter().write(responseJson.toString());
                } else {
                    // If genre does not exist, insert it
                    String insertGenreQuery = "INSERT INTO genres (name) VALUES (?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertGenreQuery)) {
                        insertStmt.setString(1, genreName);
                        int rowsAffected = insertStmt.executeUpdate();

                        if (rowsAffected > 0) {
                            try (PreparedStatement getIdStmt = conn.prepareStatement(selectGenreQuery)) {
                                getIdStmt.setString(1, genreName);
                                ResultSet idRs = getIdStmt.executeQuery();
                                if (idRs.next()) {
                                    int genreId = idRs.getInt("id");
                                    responseJson.addProperty("status", "success");
                                    responseJson.addProperty("message", String.format(
                                            "Genre added successfully.\nGenre: %s (ID: %d)", genreName, genreId));
                                }
                            }
                        } else {
                            responseJson.addProperty("status", "fail");
                            responseJson.addProperty("message", "Failed to add genre.");
                        }
                        response.getWriter().write(responseJson.toString());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Error adding genre: " + e.getMessage());
            response.getWriter().write(responseJson.toString());
        }
    }

    // Fetch the latest Movie ID
    private String fetchLatestMovieId(Connection conn) throws SQLException {
        String query = "SELECT MAX(id) FROM movies WHERE id LIKE 'tt%'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getString(1) : "tt0000001";
        }
    }

    // Fetch the existing Star ID
    private String fetchExistingStarId(Connection conn, String starName) throws SQLException {
        String query = "SELECT id FROM stars WHERE name = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, starName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("id") : "nm0000001";
            }
        }
    }

    // Fetch the existing Genre ID
    private int fetchExistingGenreId(Connection conn, String genreName) throws SQLException {
        String query = "SELECT id FROM genres WHERE name = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, genreName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String action = request.getParameter("action");

        if ("metadata".equals(action)) {
            handleMetadataRequest(response);
        }
    }

    // Retrieves metadata for all tables in the database
    private void handleMetadataRequest(HttpServletResponse response) throws IOException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            JsonArray tablesJson = new JsonArray();

            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                JsonObject tableJson = new JsonObject();
                tableJson.addProperty("table_name", tableName);
                JsonArray columnsJson = new JsonArray();

                ResultSet columns = metaData.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    JsonObject columnJson = new JsonObject();
                    columnJson.addProperty("column_name", columns.getString("COLUMN_NAME"));
                    columnJson.addProperty("type", columns.getString("TYPE_NAME"));
                    columnsJson.add(columnJson);
                }
                tableJson.add("columns", columnsJson);
                tablesJson.add(tableJson);
            }
            response.getWriter().write(tablesJson.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

}
