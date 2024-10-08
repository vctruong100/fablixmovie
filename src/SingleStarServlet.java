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
            // Contains 4 fields: star_id, star_name, star_birth_year, and star_movies (an array of movies)
            JsonObject starJson = new JsonObject();
            JsonArray starMoviesJson = new JsonArray();

            // Construct queries parameterized with ?

            // Query to grab star info
            String singleStarQuery = "SELECT * from stars as s " +
                    "where s.id = ?";

            // Query to grab movies associated with a particular star
            String starMoviesQuery = "SELECT * from stars_in_movies as sim, movies as m " +
                    "where sim.starId = ? and sim.movieId = m.id";

            // Declare statements
            PreparedStatement singleStarStatement = conn.prepareStatement(singleStarQuery);
            PreparedStatement starMoviesStatement = conn.prepareStatement(starMoviesQuery);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            singleStarStatement.setString(1, id);
            starMoviesStatement.setString(1, id);

            // Perform the single star query
            ResultSet singleStarRs = singleStarStatement.executeQuery();
            if (singleStarRs.first()) {
                String starId = singleStarRs.getString("id");
                String starName = singleStarRs.getString("name");
                String starBirthYear = singleStarRs.getString("birthYear");

                starJson.addProperty("star_id", starId);
                starJson.addProperty("star_name", starName);
                starJson.addProperty("star_birth_year", starBirthYear != null ? starBirthYear : "N/A");
            }
            singleStarRs.close();
            singleStarStatement.close();

            // Perform the movies query
            ResultSet starMoviesRs = starMoviesStatement.executeQuery();

            // Iterate through each row of starMoviesRs
            while (starMoviesRs.next()) {
                String movieId = starMoviesRs.getString("movieId");
                String movieTitle = starMoviesRs.getString("title");
                String movieYear = starMoviesRs.getString("year");
                String movieDirector = starMoviesRs.getString("director");

                // Create a JsonObject based on the data we retrieve from moviesRs
                // Contains 4 fields: movie_id, movie_title, movie_year, movie_director
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                jsonObject.addProperty("movie_url", "/api/single-movie?id=" + movieId);

                // Add to movies array
                starMoviesJson.add(jsonObject);
            }
            starJson.add("star_movies", starMoviesJson);
            starMoviesRs.close();
            starMoviesStatement.close();

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
