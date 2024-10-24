import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import query.MovieListQuery;
import resproc.CountResultProc;
import resproc.MovieListResultProc;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

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

        /*
         * This api service shall only be used to retrieve
         * search results using parameters from the HttpSession.
         * In other words, this is a PARAMETERLESS API service!
         *
         *
         * For parameterized queries, please use the search/browse APIs
         * in combination with this service
         *
         * @response {
         *   params: {...}      // back formation of query params + query mode
         *   results: {...},    // search results
         *   count: string,     // converted from an integer
         * }
         */
        SessionUser sessionUser = (SessionUser)request.getSession().getAttribute("user");

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            JsonObject responseObject = new JsonObject();
            JsonObject paramsObject = new JsonObject();
            JsonArray resultArray = new JsonArray();

            MovieListQuery mlQuery = new MovieListQuery();
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);
            CountResultProc crp = new CountResultProc(responseObject);

            int limit = sessionUser.getSetLimit(null);
            int page = sessionUser.getSetPage(null);
            int offset = limit * (page - 1);

            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

            switch(sessionUser.getSetSortCategory(null)) {
                case RATING_ASC_TITLE_ASC:
                    mlQuery.orderByRatingTitle(
                            MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.ASC
                    );
                    break;
                case RATING_ASC_TITLE_DESC:
                    mlQuery.orderByRatingTitle(
                            MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.DESC
                    );
                    break;
                case RATING_DESC_TITLE_DESC:
                    mlQuery.orderByRatingTitle(
                            MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.DESC
                    );
                    break;
                case TITLE_ASC_RATING_ASC:
                    mlQuery.orderByTitleRating(
                            MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.ASC
                    );
                    break;
                case TITLE_ASC_RATING_DESC:
                    mlQuery.orderByTitleRating(
                            MovieListQuery.OrderMode.ASC, MovieListQuery.OrderMode.DESC
                    );
                    break;
                case TITLE_DESC_RATING_ASC:
                    mlQuery.orderByTitleRating(
                            MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.ASC
                    );
                    break;
                case TITLE_DESC_RATING_DESC:
                    mlQuery.orderByTitleRating(
                            MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.DESC
                    );
                    break;
                case RATING_DESC_TITLE_ASC:
                default:
                    mlQuery.orderByRatingTitle(
                            MovieListQuery.OrderMode.DESC, MovieListQuery.OrderMode.ASC
                    );
                    break;
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

            // Back form parameters
            sessionUser.backFormParameters(paramsObject);

            // Retrieve search results
            PreparedStatement mlStatement = mlQuery.prepareStatement(conn);
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();

            // Count number of movies found for this search result
            // This should automatically add a count property to the response object
            PreparedStatement mlCountStatement = mlQuery.prepareCountStatement(conn);
            crp.processResultSet(mlCountStatement.executeQuery());
            mlCountStatement.close();

            responseObject.add("params", paramsObject);
            responseObject.add("results", resultArray);

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
