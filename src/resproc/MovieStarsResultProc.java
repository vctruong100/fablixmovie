package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a movie genre query,
 * appending constructed JSON objects into a defined JSON array
 */
public class MovieStarsResultProc extends BaseResultProc {
    private final JsonArray result;

    public MovieStarsResultProc(JsonArray result) {
        this.result = result;
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        while (rs.next()) {
            JsonObject star = new JsonObject();

            String starId = rs.getString("s.id");
            String starName = rs.getString("s.name");
            String starBirthYear = rs.getString("s.birthYear");
            String starMovieCount = rs.getString("movieCount");

            star.addProperty("star_id", starId);
            star.addProperty("star_name", starName);
            star.addProperty("star_birth_year", starBirthYear);
            star.addProperty("star_movie_count", starMovieCount);

            result.add(star);
        }
        return true;
    }
}
