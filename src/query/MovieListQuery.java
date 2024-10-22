package query;

import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/*
 * Collects a paginated list of movies with optional conditions
 * on its fields and orders rows by either its title/rating or its rating/title
 */
public class MovieListQuery extends BaseQuery {
    public enum OrderMode {
        ASC, DESC
    }

    private final StringBuilder builder;
    private final String[] params;
    private int paramCount;

    private int limit;
    private int offset;
    private final String[] order;

    /* search params */
    private String title;
    private String director;
    private String year;
    private String star;

    /* browse params */
    private String alpha;
    private String genreId;

    public MovieListQuery(Connection conn) {
        super(conn);
        builder = new StringBuilder(
                "SELECT m.*, IFNULL(r.rating, 0) 'r.rating', IFNULL(r.numVotes, 0) 'r.numVotes' " +
                "FROM movies m LEFT JOIN ratings r ON m.id = r.movieId");
        params = new String[6]; /* upper bound based on the final query string */
        paramCount = 0;

        title = null;
        director = null;
        year = null;
        star = null;

        alpha = null;
        genreId = null;

        /* page defaults */
        limit = 10;
        offset = 0;
        order = new String[2];
        orderByRatingTitle(OrderMode.DESC, OrderMode.ASC);
    }

    public PreparedStatement prepareStatement() throws SQLException {
        String queryString;
        PreparedStatement statement;
        ArrayList<String> joinClauses = new ArrayList<>();
        ArrayList<String> whereClauses = new ArrayList<>();

        /* search clauses */
        if (title != null && !title.isEmpty()) {
            whereClauses.add("LOWER(m.title) LIKE LOWER(?)");
            params[paramCount++] = "%" + title + "%";
        }
        if (director != null && !director.isEmpty()) {
            whereClauses.add("LOWER(m.director) LIKE LOWER(?)");
            params[paramCount++] = "%" + director + "%";
        }
        if (year != null && !year.isEmpty()) {
            whereClauses.add("m.year = ?");
            params[paramCount++] = year;
        }
        if (star != null && !star.isEmpty()) {
            joinClauses.add("JOIN stars_in_movies sim ON m.id = sim.movieId");
            joinClauses.add("JOIN stars s ON sim.starId = s.id");
            whereClauses.add("LOWER(s.name) LIKE LOWER(?)");
            params[paramCount++] = "%" + star + "%";
        }
        /* browse clauses */
        if (alpha != null && !alpha.isEmpty()) {
            if (alpha.charAt(0) == '*') {
                whereClauses.add("REGEXP_LIKE(m.title, ?)");
                params[paramCount++] = "^[^a-zA-Z0-9].+";
            } else {
                whereClauses.add("LOWER(m.title) LIKE LOWER(?)");
                params[paramCount++] = alpha + "%";
            }
        }
        if (genreId != null && !genreId.isEmpty()) {
            joinClauses.add("JOIN genres_in_movies gim ON m.id = gim.movieId");
            joinClauses.add("JOIN genres g ON gim.genreId = g.id");
            whereClauses.add("g.id = ?");
            params[paramCount++] = genreId;
        }

        /* append builder with option clauses and pagination clauses */
        for (String joinClause : joinClauses) {
            builder.append(" ").append(joinClause);
        }

        if (!whereClauses.isEmpty()) {
            builder.append(" WHERE ");
            for (int i = 0; i < whereClauses.size() - 1; i++) {
                builder.append(whereClauses.get(i));
                builder.append(" AND ");
            }
            builder.append(whereClauses.get(whereClauses.size() - 1));
        }

        builder.append(" ORDER BY ");
        builder.append(order[0]);
        builder.append(", ");
        builder.append(order[1]);

        if (limit > 0) {
            builder.append(" LIMIT ");
            builder.append(limit);
        }
        if (offset > 0) {
            builder.append(" OFFSET ");
            builder.append(offset);
        }

        queryString = builder.toString();
        statement = conn.prepareStatement(queryString);
        for (int i = 0; i < paramCount; i++) {
            statement.setString(i + 1, params[i]);
        }
        return statement;
    }

    /*
     * "title", "director", "year", and "star" are user defined queries, so
     * these fields shall be trimmed in case the user includes
     * leading/trailing whitespace (or just spaces)
     */
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public void setDirector(String director) {
        this.director = director != null ? director.trim() : null;
    }

    public void setYear(String year) {
        this.year = year != null ? year.trim() : null;
    }

    public void setStar(String star) {
        this.star = star != null ? star.trim() : null;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setAlpha(String alpha) {
        this.alpha = alpha;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public void orderByTitleRating(OrderMode titleMode, OrderMode ratingMode) {
        this.order[0] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "r.rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
    }

    public void orderByRatingTitle(OrderMode ratingMode, OrderMode titleMode) {
        this.order[0] = "r.rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
    }
}
