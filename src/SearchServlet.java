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
import query.MovieStarsQuery;
import query.MovieGenresQuery;

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

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");

        request.getServletContext().log("search " + "(title=" + title +
                ", year=" + year + ", director=" + director + ", star=" + star +
                ", limit=" + limitString + ", page=" + pageString + ")");

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            JsonArray resultArray = new JsonArray();

            int limit = Integer.parseInt(limitString);
            if (limit < 1 || limit > 100) {
                throw new NumberOutOfRange("limit must be between 1 and 100");
            }
            int offset = limit * Integer.parseInt(pageString);

            MovieListQuery mlQuery = new MovieListQuery(conn);
            mlQuery.setTitle(title);
            mlQuery.setYear(year);
            mlQuery.setDirector(director);
            mlQuery.setStar(star);
            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            ResultSet mlRs = mlStatement.executeQuery();
            while (mlRs.next()) {
                JsonObject movieObj = new JsonObject();
                JsonArray movieGenres = new JsonArray();
                JsonArray movieStars = new JsonArray();

                String movieId = mlRs.getString("movieId");
                String movieTitle = mlRs.getString("title");
                String movieYear = mlRs.getString("year");
                String movieDirector = mlRs.getString("director");
                String movieRating = mlRs.getString("rating");
                String movieNumVotes = mlRs.getString("numVotes");

                /* get genres associated with movie (limit 3) */
                MovieGenresQuery mgQuery = new MovieGenresQuery(conn, movieId);
                mgQuery.setLimit(3);

                PreparedStatement mgStatement = mgQuery.prepareStatement();
                ResultSet mgRs = mgStatement.executeQuery();
                while (mgRs.next()) {
                    JsonObject genreObj = new JsonObject();
                    String genreId = mgRs.getString("genreId");
                    String genreName = mgRs.getString("name");

                    genreObj.addProperty("genreId", genreId);
                    genreObj.addProperty("genreName", genreName);

                    movieGenres.add(genreObj);
                }
                mgRs.close();
                mgStatement.close();

                /* get stars associated with movies */
                MovieStarsQuery msQuery = new MovieStarsQuery(conn, movieId);
                msQuery.setLimit(3);

                PreparedStatement msStatement = msQuery.prepareStatement();
                ResultSet msRs = msStatement.executeQuery();
                while (msRs.next()) {
                    JsonObject starObj = new JsonObject();
                    String starId = msRs.getString("starId");
                    String starName = msRs.getString("name");
                    String starBirthYear = msRs.getString("birthYear");
                    String starMovieCount = msRs.getString("movieCount");

                    starObj.addProperty("star_id", starId);
                    starObj.addProperty("star_name", starName);
                    starObj.addProperty("star_birth_year", starBirthYear);
                    starObj.addProperty("star_movie_count", starMovieCount);

                    movieStars.add(starObj);
                }
                msRs.close();
                msStatement.close();

                /* build movie and add to result array */
                movieObj.addProperty("movie_id", movieId);
                movieObj.addProperty("movie_title", movieTitle);
                movieObj.addProperty("movie_year", movieYear);
                movieObj.addProperty("movie_director", movieDirector);
                movieObj.addProperty("movie_rating", movieRating);
                movieObj.addProperty("movie_num_votes", movieNumVotes);
                movieObj.add("movie_genres", movieGenres);
                movieObj.add("movie_stars", movieStars);

                resultArray.add(movieObj);
            }
            mlRs.close();
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
