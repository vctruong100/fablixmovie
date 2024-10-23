package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a movie list query,
 * appending constructed JSON objects into the defined JSON array
 */
public class MovieListResultProc extends BaseResultProc {
    private final JsonArray result;

    public MovieListResultProc(JsonArray result) {
        this.result = result;
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        while (true) {
            JsonObject movie = new JsonObject();
            MovieResultProc mrp = new MovieResultProc(movie);
            mrp.setGenreLimit(3);
            mrp.setStarLimit(3);
            if (mrp.processResultSet(rs)) {
                result.add(movie);
            } else {
                break; // no more movies to process
            }
        }
        return !result.isEmpty();
    }
}
