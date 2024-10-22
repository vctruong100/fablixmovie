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

        // Get search parameters from the request
        String title = request.getParameter("title");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String year = request.getParameter("year");

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // JSON array for movies (whether it's search results or top 20 movies)
            // Each movie contains 8 fields: movie_id, movie_title, movie_year, movie_director, movie_rating,
            // movie_num_votes, movie_genres (first 3 genres), movie_stars (first 3 stars)
            JsonArray topMoviesJson = new JsonArray();
            String movieQuery;
            PreparedStatement movieStatement;

            // If any search parameter is provided, perform search query
            if (title != null || director != null || star != null || year != null) {
                // Construct search query using AND logic
                movieQuery = "SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating, r.numVotes "
                        + "FROM movies m LEFT JOIN ratings r ON m.id = r.movieId "
                        + "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId "
                        + "LEFT JOIN stars s ON sim.starId = s.id "
                        + "WHERE 1=1 "; // 'WHERE 1=1' ensures we can safely append conditions

                if (title != null && !title.isEmpty()) {
                    movieQuery += "AND m.title LIKE ? ";
                }
                if (director != null && !director.isEmpty()) {
                    movieQuery += "AND m.director LIKE ? ";
                }
                if (star != null && !star.isEmpty()) {
                    movieQuery += "AND s.name LIKE ? ";
                }
                if (year != null && !year.isEmpty()) {
                    movieQuery += "AND m.year = ? ";
                }

                movieQuery += "ORDER BY r.rating DESC LIMIT 20";

                movieStatement = conn.prepareStatement(movieQuery);

                // Set parameters dynamically based on search input
                int paramIndex = 1;
                if (title != null && !title.isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + title + "%");
                }
                if (director != null && !director.isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + director + "%");
                }
                if (star != null && !star.isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + star + "%");
                }
                if (year != null && !year.isEmpty()) {
                    movieStatement.setString(paramIndex++, year);
                }

            } else {
                // If no search parameters, show top 20 rated movies
                movieQuery = "SELECT * from movies as m, ratings as r " +
                        "where m.id = r.movieId " +
                        "order by r.rating desc limit 20";
                movieStatement = conn.prepareStatement(movieQuery);
            }

            // Perform the top movies query
            ResultSet topMoviesRs = movieStatement.executeQuery();

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
            movieStatement.close();

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
