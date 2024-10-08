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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
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

        // Response mime type
        response.setContentType("application/json");

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Create a JSON object for each star
            // Contains 4 fields: id, name, birthYear, and movies (an array of movies)
            JsonObject starJson = new JsonObject();
            JsonArray starMoviesJson = new JsonArray();

            // Construct queries parameterized with ?

            // Query to grab movies associated with a particular star
            String moviesQuery = "SELECT * from stars_in_movies as sim, movies as m " +
                    "where sim.starId = ? and sim.movieId = m.id";

            // Query to grab star info
            String singleStarQuery = "SELECT * from stars as s " +
                    "where s.id = ?";

            // Declare statements
            PreparedStatement moviesStatement = conn.prepareStatement(moviesQuery);
            PreparedStatement singleStarStatement = conn.prepareStatement(singleStarQuery);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            moviesStatement.setString(1, id);
            singleStarStatement.setString(1, id);

            // Perform the single star query
            ResultSet singleStarRs = singleStarStatement.executeQuery();
            if (singleStarRs.first()) {
                String starId = singleStarRs.getString("id");
                String starName = singleStarRs.getString("name");
                String starBirthYear = singleStarRs.getString("birthYear");

                starJson.addProperty("id", starId);
                starJson.addProperty("name", starName);
                starJson.addProperty("birthYear", starBirthYear);
            }
            singleStarRs.close();
            singleStarStatement.close();

            // Perform the movies query
            ResultSet moviesRs = moviesStatement.executeQuery();

            // Iterate through each row of moviesRs
            while (moviesRs.next()) {
                String movieId = moviesRs.getString("movieId");
                String movieTitle = moviesRs.getString("title");
                String movieYear = moviesRs.getString("year");
                String movieDirector = moviesRs.getString("director");

                // Create a JsonObject based on the data we retrieve from moviesRs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", movieId);
                jsonObject.addProperty("title", movieTitle);
                jsonObject.addProperty("year", movieYear);
                jsonObject.addProperty("director", movieDirector);

                // Add to movies array
                starMoviesJson.add(jsonObject);
            }
            starJson.add("movies", starMoviesJson);
            moviesRs.close();
            moviesStatement.close();

            // Write JSON string to output
            out.write(starJson.toString());
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
