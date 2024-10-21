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
import java.util.MissingFormatArgumentException;

import query.MovieListQuery;
import query.MovieStarsQuery;
import query.MovieGenresQuery;
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

        String alpha = request.getParameter("alpha");
        String genreId = request.getParameter("genre");

        request.getServletContext().log("browse " + "(alpha=" + alpha +
                ", genre=" + genreId + ")");

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            JsonArray resultArray = new JsonArray();
            MovieListQuery mlQuery = new MovieListQuery(conn);
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);

            /* get movies based on the defined parameter */
            if (alpha != null) {
                if (alpha.length() != 1 ||
                    !Character.isLetterOrDigit(alpha.charAt(0)) ||
                    alpha.charAt(0) != '*') {
                    throw new IllegalArgumentException("alpha must be a single alphanumeric char or *");
                }
                mlQuery.setAlpha(alpha);
            } else if (genreId != null) {
                mlQuery.setGenreId(genreId);
            } else {
                /* no args defined */
                throw new MissingFormatArgumentException("must define either alpha or genre");
            }

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();

            // Write JSON string to output
            out.write(resultArray.toString());
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
