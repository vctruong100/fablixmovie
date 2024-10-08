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

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

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

        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting movie id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Create a JSON object for each movie
            // Contains 8 fields: movie_id, movie_title, movie_year, movie_director, movie_rating,
            // movie_num_votes, movie_genres (array of genres), movie_stars (array of stars)
            JsonObject movieJson = new JsonObject();
            JsonArray movieGenresJson = new JsonArray();
            JsonArray movieStarsJson = new JsonArray();

            // Query that grabs info about the movie including its rating
            String singleMovieQuery = "SELECT * from movies as m, ratings as r" +
                    "where m.id = ? and r.movieId = m.id";

            // Query that grabs genres associated with the movie
            String movieGenresQuery = "SELECT * from genres_in_movies as gim, genres as g " +
                    "where gim.movieId = ? and gim.genreId = g.id";

            // Query that grabs stars associated with the movie
            String movieStarsQuery = "SELECT * from stars_in_movies as sim, stars as s " +
                    "where sim.movieId = ? and sim.starId = s.id";

            // Declare statements
            PreparedStatement singleMovieStatement = conn.prepareStatement(singleMovieQuery);
            PreparedStatement movieGenresStatement = conn.prepareStatement(movieGenresQuery);
            PreparedStatement movieStarsStatement = conn.prepareStatement(movieStarsQuery);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            singleMovieStatement.setString(1, id);
            movieGenresStatement.setString(1, id);
            movieStarsStatement.setString(1, id);

            // Perform the single movie query
            ResultSet singleMovieRs = singleMovieStatement.executeQuery();
            if (singleMovieRs.first()) {
                String movieId = singleMovieRs.getString("movieId");
                String movieTitle = singleMovieRs.getString("title");
                String movieYear = singleMovieRs.getString("year");
                String movieDirector = singleMovieRs.getString("director");
                String movieRating = singleMovieRs.getString("rating");
                String movieNumVotes = singleMovieRs.getString("numVotes");

                movieJson.addProperty("movie_id", movieId);
                movieJson.addProperty("movie_title", movieTitle);
                movieJson.addProperty("movie_year", movieYear);
                movieJson.addProperty("movie_director", movieDirector);
                movieJson.addProperty("movie_rating", movieRating);
                movieJson.addProperty("movie_num_votes", movieNumVotes);
            }
            singleMovieRs.close();
            singleMovieStatement.close();

            // Perform the movie genres query
            ResultSet movieGenresRs = movieGenresStatement.executeQuery();

            // Iterate through each row of movieGenresRs
            while (movieGenresRs.next()) {
                String genreId = movieGenresRs.getString("genreId");
                String genreName = movieGenresRs.getString("name");

                // Create a JsonObject based on the data we retrieve from movieGenresRs
                // Contains 2 fields: genre_id, genre_name
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("genre_id", genreId);
                jsonObject.addProperty("genre_name", genreName);

                // Add to genres array
                movieGenresJson.add(jsonObject);
            }
            movieJson.add("movie_genres", movieGenresJson);
            movieGenresRs.close();
            movieGenresStatement.close();

            // Perform the movie stars query
            ResultSet movieStarsRs = movieStarsStatement.executeQuery();

            // Iterate through each row of movieStarsRs
            while (movieStarsRs.next()) {
                String starId = movieStarsRs.getString("starId");
                String starName = movieStarsRs.getString("name");
                String starBirthYear = movieStarsRs.getString("birthYear");

                // JsonObject for each star
                // Contains 3 fields: star_id, star_name, star_birth_year
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("star_id", starId);
                jsonObject.addProperty("star_name", starName);
                jsonObject.addProperty("star_birth_year", starBirthYear);
                jsonObject.addProperty("star_url", "/api/single-star?id=" + starId);

                // Add to stars array
                movieStarsJson.add(jsonObject);
            }
            movieJson.add("movie_stars", movieStarsJson);
            movieStarsRs.close();
            movieStarsStatement.close();

            // Write JSON string to output
            out.write(movieJson.toString());
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
