package query;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/*
 * Collects movie fields from a given movie id excluding any
 * grouped fields (e.g. genres and stars)
 */
public class StarQuery extends BaseQuery {
    private final String starId;

    public StarQuery(Connection conn, String starId) {
        super(conn);
        this.starId = starId;
    }

    public PreparedStatement prepareStatement() throws SQLException {
        String queryString = "SELECT s.id, s.name, s.birthYear FROM stars s " +
                "WHERE s.id = ?";
        PreparedStatement statement = conn.prepareStatement(queryString);
        statement.setString(1, starId);
        return statement;
    }
}
