package resproc;

import java.sql.SQLException;
import java.sql.ResultSet;

abstract class BaseResultProc {
    /*
     * BaseResultProc::processResultSet shall return false
     * if and only if the result set was empty at the time
     * the subroutine was called (i.e. no rows processed)
     */
    abstract public boolean processResultSet(ResultSet rs) throws SQLException;
}
