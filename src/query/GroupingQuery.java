package query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * Collects a grouping of rows based on a single kind of id
 */
abstract class GroupingQuery extends BaseQuery {
    protected final StringBuilder builder;
    private final String kindId;
    private int limit;

    public GroupingQuery(Connection conn, String kindId) {
        super(conn);
        this.kindId = kindId;
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
        statement = conn.prepareStatement(queryString);
        statement.setString(1, kindId);
        return statement;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
