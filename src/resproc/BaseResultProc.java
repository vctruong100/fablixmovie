package resproc;

import java.sql.SQLException;
import java.sql.ResultSet;

abstract class BaseResultProc {
    abstract public boolean processResultSet(ResultSet rs) throws SQLException;
}
