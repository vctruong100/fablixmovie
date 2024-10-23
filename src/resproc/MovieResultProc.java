package resproc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import query.MovieGenresQuery;
import query.MovieStarsQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a single movie query
 * and populates the fields of a defined JSON object
 */
public class MovieResultProc extends BaseResultProc {
    private final JsonObject result;
    private final JsonArray genres;
    private final JsonArray stars;

    private int genreLimit;
    private int starLimit;

    public MovieResultProc(JsonObject result) {
        this.result = result;
        genres = new JsonArray();
        stars = new JsonArray();
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            MovieGenresResultProc mgrp = new MovieGenresResultProc(genres);
            MovieStarsResultProc msrp = new MovieStarsResultProc(stars);

            String movieId = rs.getString("m.id");
            String movieTitle = rs.getString("m.title");
            String movieYear = rs.getString("m.year");
            String movieDirector = rs.getString("m.director");
            String movieRating = rs.getString("r.rating");
            String movieNumVotes = rs.getString("r.numVotes");
            String moviePrice = rs.getString("p.price");

            /* process genres */
            MovieGenresQuery mgQuery = new MovieGenresQuery(
                    rs.getStatement().getConnection(), movieId);
            if (genreLimit > 0) {
                mgQuery.setLimit(genreLimit);
            }

            PreparedStatement mgStatement = mgQuery.prepareStatement();
            mgrp.processResultSet(mgStatement.executeQuery());
            mgStatement.close();

            /* process stars */
            MovieStarsQuery msQuery = new MovieStarsQuery(
                    rs.getStatement().getConnection(), movieId);
            if (starLimit > 0) {
                msQuery.setLimit(starLimit);
            }

            PreparedStatement msStatement = msQuery.prepareStatement();
            msrp.processResultSet(msStatement.executeQuery());
            msStatement.close();

            /* process movie */
            result.addProperty("movie_id", movieId);
            result.addProperty("movie_title", movieTitle);
            result.addProperty("movie_year", movieYear);
            result.addProperty("movie_director", movieDirector);
            result.addProperty("movie_rating", movieRating);
            result.addProperty("movie_num_votes", movieNumVotes);
            result.addProperty("movie_price", moviePrice);
            result.add("movie_genres", genres);
            result.add("movie_stars", stars);

            return true;
        }
        return false;
    }

    public void setGenreLimit(int genreLimit) {
        this.genreLimit = genreLimit;
    }

    public void setStarLimit(int starLimit) {
        this.starLimit = starLimit;
    }
}
