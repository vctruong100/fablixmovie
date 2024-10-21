package query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class BaseQuery {
    protected final Connection conn;
    public BaseQuery(Connection conn) {
        this.conn = conn;
    }
    abstract PreparedStatement prepareStatement() throws SQLException;
}
