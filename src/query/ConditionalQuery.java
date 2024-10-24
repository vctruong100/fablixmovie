package query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/*
 * Collects rows based on
 * several conditions with support for counting,
 * ordering, and pagination
 *
 * You should derive this class if you require dynamic queries
 * with an arbitrary number of conditions
 */
abstract class ConditionalQuery extends BaseQuery {
    public enum OrderMode {
        ASC, DESC,
    }

    protected final StringBuilder builder;
    protected final List<String> selectClauses;
    protected final List<String> joinClauses;
    protected final List<String> whereClauses;

    protected String[] order;
    protected String[] params;
    protected int paramCount;
    protected boolean pleaseUpdate;

    private int limit;
    private int offset;

    public ConditionalQuery() {
        builder = new StringBuilder();
        selectClauses = new ArrayList<>();
        joinClauses = new ArrayList<>();
        whereClauses = new ArrayList<>();
        limit = 10;
        offset = 0;
        order = null;
        params = null;
    }

    /*
     * Shall be called before any statement is prepared
     * if and only if a setter has marked an unobserved change
     * via the 'pleaseUpdate' flag
     *
     * Derived classes should process any derived fields
     * and add them to their respective clauses array
     * using this function
     */
    abstract void update();

    /*
     * Prepares a parameterized countable statement built using the
     * select count(), join, where clauses
     *
     * See prepareStatement for a field-selective version of this query
     */
    public final PreparedStatement prepareCountStatement(Connection conn)
            throws SQLException {
        if (pleaseUpdate) {
            pleaseUpdate = false;
            update();
        }

        String queryString;
        PreparedStatement statement;

        builder.setLength(0);

        /* base statement */
        buildSelectCount();
        buildJoinClauses();
        buildWhereClauses();

        queryString = builder.toString();
        statement = conn.prepareStatement(queryString);
        for (int i = 0; i < paramCount; i++) {
            statement.setString(i + 1, params[i]);
        }
        System.out.println("ConditionalQuery::countStatement --> " + statement);
        return statement;
    }
    /*
     * Prepares a parameterized statement built using the
     * select, join, where, orderBy (if defined), and
     * limit/offset (if defined) clauses
     *
     * See prepareCountStatement for a countable version of this query
     */
    public final PreparedStatement prepareStatement(Connection conn)
            throws SQLException {
        if (pleaseUpdate) {
            pleaseUpdate = false;
            update();
        }

        String queryString;
        PreparedStatement statement;

        builder.setLength(0);

        /* base statement */
        buildSelectClauses();
        buildJoinClauses();
        buildWhereClauses();

        /* optional clauses */
        buildOrderByClauses();
        buildPageClauses();

        queryString = builder.toString();
        statement = conn.prepareStatement(queryString);
        for (int i = 0; i < paramCount; i++) {
            statement.setString(i + 1, params[i]);
        }
        System.out.println("ConditionalQuery::statement --> " + statement);
        return statement;
    }

    public final void setLimit(int limit) {
        this.limit = limit;
        pleaseUpdate = true;
    }

    public final void setOffset(int offset) {
        this.offset = offset;
        pleaseUpdate = true;
    }

    /*
     * This shall be called first when preparing
     * a count statement (excludes select, limit, offset)
     */
    private void buildSelectCount() {
        builder.append(" SELECT COUNT(*) count ");
    }

    /*
     * This shall be called first when preparing
     * a base statement (no limit / offset)
     *
     * At least 1 select clause is required to be defined
     */
    private void buildSelectClauses() {
        builder.append(" SELECT ");
        for (String clause : selectClauses.subList(
                0, selectClauses.size() - 1)) {
            builder.append(clause);
            builder.append(", ");
        }
        builder.append(selectClauses.get(
                selectClauses.size() - 1));
    }

    /*
     * This shall be called immediately
     * after select clauses or count are appended
     *
     * At least 1 join clause is required to be defined
     */
    private void buildJoinClauses() {
        builder.append(" FROM ");
        for (String clause : joinClauses) {
            builder.append(" ");
            builder.append(clause);
        }
    }

    /*
     * This shall be called immediately
     * after join clauses are appended
     *
     * Where clauses are optional but a lack of
     * could lead to suboptimal queries
     */
    private void buildWhereClauses() {
        if (!whereClauses.isEmpty()) {
            builder.append(" WHERE ");
            for (String clause : whereClauses.subList(
                    0, whereClauses.size() - 1)) {
                builder.append("(");
                builder.append(clause);
                builder.append(") AND ");
            }
            builder.append("(");
            builder.append(whereClauses.get(
                    whereClauses.size() - 1));
            builder.append(")");
        }
    }

    /*
     * This shall be called immediately
     * after where clauses are appended
     *
     * If no orderBy clauses have been defined,
     * then the query skips ordering
     */
    private void buildOrderByClauses() {
        if (order != null) {
            builder.append(" ORDER BY ");
            for (int i = 0; i < order.length - 1; i++) {
                builder.append(order[i]);
                builder.append(", ");
            }
            builder.append(order[order.length - 1]);
        }
    }

    /*
     * This shall be called after all other
     * clauses have been appended
     *
     * If no limit/offset have been defined,
     * then the query will forego the limit/offset clauses
     */
    private void buildPageClauses() {
        if (limit > 0) {
            builder.append(" LIMIT ");
            builder.append(limit);
        }
        if (offset > 0) {
            builder.append(" OFFSET ");
            builder.append(offset);
        }
    }
}
