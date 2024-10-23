package query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class BaseQuery {
    abstract PreparedStatement prepareStatement(Connection conn)
            throws SQLException;
}
