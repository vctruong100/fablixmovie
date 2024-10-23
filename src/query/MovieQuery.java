package query;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/*
 * Collects movie fields from a given movie id excluding any
 * grouped fields (e.g. genres and stars)
 */
public class MovieQuery extends BaseQuery {
    private final String movieId;

    public MovieQuery(String movieId) {
        this.movieId = movieId;
    }

    public PreparedStatement prepareStatement(Connection conn)
            throws SQLException {
        String queryString =
                "SELECT m.*, IFNULL(r.rating, 0) 'r.rating', "
                + "IFNULL(r.numVotes, 0) 'r.numVotes', p.price "
                + "FROM movies m LEFT JOIN ratings r ON m.id = r.movieId "
                + "LEFT JOIN prices p ON m.id = r.movieId WHERE m.id = ?";
        PreparedStatement statement = conn.prepareStatement(queryString);
        statement.setString(1, movieId);
        return statement;
    }
}
