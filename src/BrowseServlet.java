import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mysql.cj.exceptions.NumberOutOfRange;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import query.MovieListQuery;
import resproc.MovieListResultProc;

@WebServlet(name = "BrowseServlet", urlPatterns = "/api/browse")
public class BrowseServlet extends HttpServlet {
    private static final long serialVersionUID = 201L;

    // Create a dataSource which is registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        SessionUser sessionUser = (SessionUser)request.getSession().getAttribute("user");

        String sortBy = request.getParameter("sortBy");
        System.out.println("Received sortBy parameter: " + sortBy);

        String alpha = request.getParameter("alpha");
        String genreId = request.getParameter("genre");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");

        sessionUser.setBrowseParameters(alpha, genreId);

        request.getServletContext().log("browse " + "(alpha=" + alpha +
                ", genre=" + genreId + ", limit=" + limitString +
                ", page=" + pageString + ")");

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            JsonArray resultArray = new JsonArray();
            MovieListQuery mlQuery = new MovieListQuery(conn);
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);

            int limit = sessionUser.parseAndSetLimit(limitString);
            int page = sessionUser.parseAndSetPage(pageString);
            int offset = limit * (page - 1);

            if (sortBy != null) {
                switch (sortBy) {
                    case "title-asc-rating-asc":
                        mlQuery.orderByTitleRating(MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.ASC);
                        break;
                    case "title-asc-rating-desc":
                        mlQuery.orderByTitleRating(MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.DESC);
                        break;
                    case "title-desc-rating-asc":
                        mlQuery.orderByTitleRating(MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.ASC);
                        break;
                    case "title-desc-rating-desc":
                        mlQuery.orderByTitleRating(MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.DESC);
                        break;
                    case "rating-asc-title-asc":
                        mlQuery.orderByRatingTitle(MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.ASC);
                        break;
                    case "rating-asc-title-desc":
                        mlQuery.orderByRatingTitle(MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.DESC);
                        break;
                    case "rating-desc-title-asc":
                        mlQuery.orderByRatingTitle(MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.ASC);
                        break;
                    case "rating-desc-title-desc":
                        mlQuery.orderByRatingTitle(MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.DESC);
                        break;
                    default:
                        mlQuery.orderByRatingTitle(MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.ASC);
                        break;
                }
            }

            /* get movies based on the defined parameter */
            if (alpha != null) {
                if (alpha.length() != 1 ||
                    !Character.isLetterOrDigit(alpha.charAt(0)) &&
                    alpha.charAt(0) != '*') {
                    throw new IllegalArgumentException("alpha must be a single alphanumeric char or *");
                }
                mlQuery.setAlpha(alpha);
            } else if (genreId != null) {
                mlQuery.setGenreId(genreId);
            } else {
                /* no args defined */
                throw new IllegalArgumentException("either alpha or genre must be defined");
            }
            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();

            String countQuery = "SELECT COUNT(*) AS total FROM movies m LEFT JOIN ratings r ON m.id = r.movieId WHERE 1=1";

            if (alpha != null && !alpha.isEmpty()) {
                countQuery += " AND m.title LIKE ?";
            } else if (genreId != null && !genreId.isEmpty()) {
                countQuery += " AND EXISTS (SELECT 1 FROM genres_in_movies gim WHERE gim.genreId = ? AND gim.movieId = m.id)";
            }

            PreparedStatement countStatement = conn.prepareStatement(countQuery);

            int paramIndex = 1;
            if (alpha != null && !alpha.isEmpty() && !alpha.equals("*")) {
                countStatement.setString(paramIndex++, alpha + "%");
            } else if (genreId != null && !genreId.isEmpty()) {
                countStatement.setString(paramIndex++, genreId);
            }

            ResultSet countResultSet = countStatement.executeQuery();
            int totalRecords = 0;
            if (countResultSet.next()) {
                totalRecords = countResultSet.getInt("total");
            }

            countStatement.close();
            int totalPages = (int) Math.ceil((double) totalRecords / limit);

            // Construct the JSON response
            JsonObject responseObject = new JsonObject();
            responseObject.add("movies", resultArray);
            responseObject.addProperty("totalRecords", totalRecords);
            responseObject.addProperty("totalPages", totalPages);

            out.write(responseObject.toString());

            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}
