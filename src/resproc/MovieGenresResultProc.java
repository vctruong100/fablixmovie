package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a movie genres query,
 * appending constructed JSON objects into the defined JSON array
 */
public class MovieGenresResultProc extends BaseResultProc {
    private final JsonArray result;

    public MovieGenresResultProc(JsonArray result) {
        this.result = result;
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        while (rs.next()) {
            JsonObject genre = new JsonObject();

            String genreId = rs.getString("genreId");
            String genreName = rs.getString("name");

            genre.addProperty("genre_id", genreId);
            genre.addProperty("genre_name", genreName);

            result.add(genre);
        }
        return true;
    }
}
