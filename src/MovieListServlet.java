import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mysql.cj.Session;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import query.MovieListQuery;
import resproc.MovieListResultProc;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "MovieListServlet", urlPatterns = "/api/movie-list")
public class MovieListServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

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
        response.setContentType("application/json"); // Response mime type

        // Log all query parameters
        System.out.println("Query Parameters:");
        request.getParameterMap().forEach((key, value) -> {
            System.out.println(key + ": " + String.join(", ", value));
        });


        String sortBy = request.getParameter("sortBy");
        System.out.println("Received sortBy parameter: " + sortBy);
        /*
         * This api service shall only be used to retrieve
         * search results using parameters from the HttpSession
         *
         * For parameterized queries, use the search/browse APIs.
         */
        SessionUser sessionUser = (SessionUser)request.getSession().getAttribute("user");

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource
            JsonArray resultArray = new JsonArray();
            MovieListQuery mlQuery = new MovieListQuery(conn);
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);

            String limitParam = request.getParameter("limit");
            String pageParam = request.getParameter("page");

            int limit = limitParam != null ? Integer.parseInt(limitParam) : 10;
            int page = pageParam != null ? Integer.parseInt(pageParam) : 1;

            int offset = limit * (page - 1);

            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

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

            switch(sessionUser.getQueryMode()) {
                case SEARCH:
                    String[] searchParameters = sessionUser.getSearchParameters();
                    String title = searchParameters[0];
                    String director = searchParameters[1];
                    String year = searchParameters[2];
                    String star = searchParameters[3];
                    mlQuery.setTitle(title);
                    mlQuery.setDirector(director);
                    mlQuery.setYear(year);
                    mlQuery.setStar(star);
                    break;
                case BROWSE:
                    String[] browseParameters = sessionUser.getBrowseParameters();
                    String alpha = browseParameters[0];
                    String genreId = browseParameters[1];
                    mlQuery.setAlpha(alpha);
                    mlQuery.setGenreId(genreId);
                    break;
                default:
                    break;
            }

            // Total count query
            String countQuery = "SELECT COUNT(*) AS total FROM movies m LEFT JOIN ratings r ON m.id = r.movieId";
            PreparedStatement countStatement = conn.prepareStatement(countQuery);
            ResultSet countResultSet = countStatement.executeQuery();
            int totalRecords = 0;
            if (countResultSet.next()) {
                totalRecords = countResultSet.getInt("total");
            }
            countStatement.close();
            // Calculate total pages based on the limit
            int totalPages = (int) Math.ceil((double) totalRecords / limit);

            // Build the JSON response
            JsonObject responseObject = new JsonObject();
            responseObject.add("movies", resultArray);  // List of movies
            responseObject.addProperty("totalRecords", totalRecords);  // Total number of matching records
            responseObject.addProperty("totalPages", totalPages);  // Total pages

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();
            // Write JSON string to output
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
