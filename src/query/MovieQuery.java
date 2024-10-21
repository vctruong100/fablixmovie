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

    public MovieQuery(Connection conn, String movieId) {
        super(conn);
        this.movieId = movieId;
    }

    public PreparedStatement prepareStatement() throws SQLException {
        String queryString = "SELECT * FROM movies m, ratings r " +
                "WHERE m.id = ? AND r.movieId = m.id";
        PreparedStatement statement = conn.prepareStatement(queryString);
        statement.setString(1, movieId);
        return statement;
    }
}
