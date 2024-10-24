package resproc;

import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Processes the result set for a count query
 * See ConditionalQuery for more info on count queries
 */
public class CountResultProc extends BaseResultProc {
    private final JsonObject result;

    public CountResultProc(JsonObject result) {
        this.result = result;
    }

    public boolean processResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            String count = rs.getString("count");
            result.addProperty("count", count);
            return true;
        }
        return false;
    }
}
