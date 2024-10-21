package query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * Collects a grouping of rows based on a single movie id
 */
abstract class MovieGroupingQuery extends BaseQuery {
    protected final StringBuilder builder;
    private final String movieId;
    private int limit;

    public MovieGroupingQuery(Connection conn, String movieId) {
        super(conn);
        this.movieId = movieId;
        builder = new StringBuilder();
    }

    public PreparedStatement prepareStatement() throws SQLException {
        String queryString;
        PreparedStatement statement;

        if (limit > 0) {
            builder.append(" LIMIT ");
            builder.append(limit);
        }

        queryString = builder.toString();
        statement = conn.prepareStatement(builder.toString());
        statement.setString(1, movieId);
        return statement;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
