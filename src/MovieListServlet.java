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

        // The log message can be found in localhost log
        request.getServletContext().log("getting top 20 movies by rating");

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // JSON array for the top 20 movies
            // Each movie contains 8 fields: movie_id, movie_title, movie_year, movie_director, movie_rating,
            // movie_num_votes, movie_genres (first 3 genres), movie_stars (first 3 stars)
            JsonArray topMoviesJson = new JsonArray();

            // Query that grabs the top 20 movies by rating
            String topMoviesQuery = "SELECT * from movies as m, ratings as r " +
                    "where m.id = r.movieId " +
                    "order by r.rating desc limit 20";

            // Declare our statement
            PreparedStatement topMoviesStatement = conn.prepareStatement(topMoviesQuery);

            // Perform the top movies query
            ResultSet topMoviesRs = topMoviesStatement.executeQuery();

            // Iterate through each row of rs
            while (topMoviesRs.next()) {
                // Create a JsonArray for the first 3 genres / stars
                JsonArray movieGenresJson = new JsonArray();
                JsonArray movieStarsJson = new JsonArray();

                // Retrieve movie details
                String movieId = topMoviesRs.getString("movieId");
                String movieTitle = topMoviesRs.getString("title");
                String movieYear = topMoviesRs.getString("year");
                String movieDirector = topMoviesRs.getString("director");
                String movieRating = topMoviesRs.getString("rating");
                String movieNumVotes = topMoviesRs.getString("numVotes");

                // Create queries to get first 3 genres and stars for each movie
                // Prepare statements, then perform the queries
                String genresQuery = "SELECT * from genres_in_movies as gim, genres as g " +
                        "where gim.movieId = ? and gim.genreId = g.id " +
                        "limit 3";

                String starsQuery = "SELECT * from stars_in_movies as sim, stars as s " +
                        "where sim.movieId = ? and sim.starId = s.id " +
                        "limit 3";

                PreparedStatement genresStatement = conn.prepareStatement(genresQuery);
                PreparedStatement starsStatement = conn.prepareStatement(starsQuery);
                genresStatement.setString(1, movieId);
                starsStatement.setString(1, movieId);

                ResultSet genresRs = genresStatement.executeQuery();
                while (genresRs.next()) {
                    // Prepare genre info (genre_id, genre_name)
                    String genreId = genresRs.getString("genreId");
                    String genreName = genresRs.getString("name");

                    JsonObject genreJson = new JsonObject();
                    genreJson.addProperty("genre_id", genreId);
                    genreJson.addProperty("genre_name", genreName);

                    movieGenresJson.add(genreJson);
                }
                genresRs.close();
                genresStatement.close();

                ResultSet starsRs = starsStatement.executeQuery();
                while (starsRs.next()) {
                    // Prepare star info (star_id, star_name, star_birth_year)
                    String starId = starsRs.getString("starId");
                    String starName = starsRs.getString("name");
                    String starBirthYear = starsRs.getString("birthYear");

                    JsonObject starJson = new JsonObject();
                    starJson.addProperty("star_id", starId);
                    starJson.addProperty("star_name", starName);
                    starJson.addProperty("star_birth_year", starBirthYear);

                    movieStarsJson.add(starJson);
                }
                starsRs.close();
                starsStatement.close();

                // Create a JsonObject for the movie
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("movie_id", movieId);
                movieJson.addProperty("movie_title", movieTitle);
                movieJson.addProperty("movie_year", movieYear);
                movieJson.addProperty("movie_director", movieDirector);
                movieJson.addProperty("movie_rating", movieRating);
                movieJson.addProperty("movie_num_votes", movieNumVotes);
                movieJson.add("movie_genres", movieGenresJson);
                movieJson.add("movie_stars", movieStarsJson);

                topMoviesJson.add(movieJson);
            }
            topMoviesRs.close();
            topMoviesStatement.close();

            // Write JSON string to output
            out.write(topMoviesJson.toString());
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
