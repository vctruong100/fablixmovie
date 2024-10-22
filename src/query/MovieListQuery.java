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
    public static class OrderMode {
        public static int ASC = 0;
        public static int DESC = 1;
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
        orderByTitleRating(OrderMode.ASC, OrderMode.DESC);
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
            joinClauses.add("stars_in_movies sim, stars s");
            whereClauses.add("m.id = sim.movieId AND sim.starId = s.id AND LOWER(s.name) = LOWER(?)");
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
            joinClauses.add("genres_in_movies gim, genres g");
            whereClauses.add("m.id = gim.movieId AND g.id = gim.genreId AND g.id = ?");
            params[paramCount++] = genreId;
        }

        /* append builder with option clauses and pagination clauses */
        for (int i = 0; i < joinClauses.size() - 1; i++) {
            builder.append(joinClauses.get(i));
            builder.append(", ");
        }
        builder.append(joinClauses.get(joinClauses.size() - 1));

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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setStar(String star) {
        this.star = star;
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

    public void orderByTitleRating(int titleMode, int ratingMode) {
        this.order[0] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
    }

    public void orderByRatingTitle(int ratingMode, int titleMode) {
        this.order[0] = "rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
    }
}
