package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a movie genre query,
 * appending constructed JSON objects into a defined JSON array
 */
public class StarMoviesResultProc extends BaseResultProc {
    private final JsonArray result;

    public StarMoviesResultProc(JsonArray result) {
        this.result = result;
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        while (rs.next()) {
            JsonObject movie = new JsonObject();

            String movieId = rs.getString("m.id");
            String movieTitle = rs.getString("m.title");
            String movieYear = rs.getString("m.year");
            String movieDirector = rs.getString("m.director");

            movie.addProperty("movie_id", movieId);
            movie.addProperty("movie_title", movieTitle);
            movie.addProperty("movie_year", movieYear);
            movie.addProperty("movie_director", movieDirector);

            result.add(movie);
        }
        return true;
    }
}
