import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.StringWriter;

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

@WebServlet(name = "SearchServlet", urlPatterns = "/api/search")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 200L;

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

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");

        sessionUser.setSearchParameters(title, year, director, star);

        request.getServletContext().log("search " + "(title=" + title +
                ", year=" + year + ", director=" + director + ", star=" + star +
                ", limit=" + limitString + ", page=" + pageString + ")");

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            JsonArray resultArray = new JsonArray();

            int limit = sessionUser.parseAndSetLimit(limitString);
            int page = sessionUser.parseAndSetPage(pageString);
            int offset = limit * (page - 1);


            MovieListQuery mlQuery = new MovieListQuery(conn);
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);

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

            mlQuery.setTitle(title);
            mlQuery.setYear(year);
            mlQuery.setDirector(director);
            mlQuery.setStar(star);
            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();

            // Total records query for pagination
            String countQuery = "SELECT COUNT(*) AS total FROM movies m LEFT JOIN ratings r ON m.id = r.movieId WHERE 1=1";

            if (title != null && !title.isEmpty()) {
                countQuery += " AND m.title LIKE ?";
            }
            if (year != null && !year.isEmpty()) {
                countQuery += " AND m.year = ?";
            }
            if (director != null && !director.isEmpty()) {
                countQuery += " AND m.director LIKE ?";
            }
            if (star != null && !star.isEmpty()) {
                countQuery += " AND EXISTS (SELECT 1 FROM stars_in_movies sm JOIN stars s ON sm.starId = s.id WHERE s.name LIKE ? AND sm.movieId = m.id)";
            }

            PreparedStatement countStatement = conn.prepareStatement(countQuery);

            int paramIndex = 1;
            if (title != null && !title.isEmpty()) {
                countStatement.setString(paramIndex++, "%" + title + "%");
            }
            if (year != null && !year.isEmpty()) {
                countStatement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                countStatement.setString(paramIndex++, "%" + director + "%");
            }
            if (star != null && !star.isEmpty()) {
                countStatement.setString(paramIndex++, "%" + star + "%");
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
            // Write user error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Capture and log the stack trace to help identify where the error occurred
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Log error to localhost log with full details
            request.getServletContext().log("Error: " + e.getMessage());
            request.getServletContext().log(stackTrace);  // Log stack trace in server logs
            System.out.println(stackTrace);

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);

        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}
