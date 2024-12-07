package query;

import java.util.List;

/*
 * Extends conditional queries with a subquery
 * that supports LIKE / fulltext and fuzzy search
 */
public class SearchSubQuery {
    // disable if edth is not installed
    // see: edth.sh
    public static final boolean FUZZY_SEARCH = false;

    public static void extendFulltext(
            String subQueryName,
            String tableName,
            String field,
            String query,
            List<String> subQueries,
            List<String> subParams) {
        StringBuilder sb = new StringBuilder();
        StringBuilder msb = new StringBuilder();
        beginSubQuery(sb, subQueryName, tableName);
        sb.append("MATCH(");
        sb.append(field);
        sb.append(") AGAINST (? IN BOOLEAN MODE)");
        for (String word : query.split("\\s+")) {
            msb.append("+");
            msb.append(word.toLowerCase());
            msb.append("*");
        }
        subParams.add(msb.toString());
        if (FUZZY_SEARCH) {
            addFuzzySearch(sb, field, query, subParams);
        }
        endSubQuery(sb, subQueries);
    }

    public static void extendLike(
            String subQueryName,
            String tableName,
            String field,
            String query,
            List<String> subQueries,
            List<String> subParams) {
        StringBuilder sb = new StringBuilder();
        beginSubQuery(sb, subQueryName, tableName);
        sb.append("LOWER(");
        sb.append(field);
        sb.append(") LIKE LOWER(?)");
        subParams.add("%" + query + "%");
        if (FUZZY_SEARCH) {
            addFuzzySearch(sb, field, query, subParams);
        }
        endSubQuery(sb, subQueries);
    }

    private static void beginSubQuery(
            StringBuilder sb,
            String subQueryName,
            String tableName) {
        sb.append(subQueryName);
        sb.append(" AS (SELECT * FROM ");
        sb.append(tableName);
        sb.append(" WHERE ");
    }

    private static void endSubQuery(
            StringBuilder sb,
            List<String> subQueries) {
        sb.append(")");
        subQueries.add(sb.toString());
    }

    private static void addFuzzySearch(
            StringBuilder sb,
            String field,
            String query,
            List<String> subParams) {
        int editDistanceThreshold = getEditDistanceThreshold(query);
        sb.append(" OR edth(");
        sb.append(field);
        sb.append(", ?, ");
        sb.append(editDistanceThreshold);
        sb.append(")");
        subParams.add(query);
    }

    private static int getEditDistanceThreshold(String query) {
        int len = query.length();
        return (int)Math.floor(len / 2.5);
    }
}
