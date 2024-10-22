package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import query.MovieGenresQuery;
import query.MovieStarsQuery;
import query.StarMoviesQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a single movie query
 * and populates the fields of a defined JSON object
 */
public class StarResultProc extends BaseResultProc {
    private final JsonObject result;
    private final JsonArray movies;

    private int movieLimit;

    public StarResultProc(JsonObject result) {
        this.result = result;
        movies = new JsonArray();
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            StarMoviesResultProc smrp = new StarMoviesResultProc(movies);

            String starId = rs.getString("starId");
            String starName = rs.getString("name");
            String starBirthYear = rs.getString("birthYear");

            /* process movies */
            StarMoviesQuery smQuery = new StarMoviesQuery(
                    rs.getStatement().getConnection(), starId);
            if (movieLimit > 0) {
                smQuery.setLimit(movieLimit);
            }

            PreparedStatement smStatement = smQuery.prepareStatement();
            smrp.processResultSet(smStatement.executeQuery());
            smStatement.close();

            /* process star */
            result.addProperty("star_id", starId);
            result.addProperty("star_name", starName);
            result.addProperty("star_birth_year", starBirthYear);
            result.add("star_movies", movies);

            return true;
        }
        return false;
    }

    public void setMovieLimit(int movieLimit) {
        this.movieLimit = movieLimit;
    }
}
