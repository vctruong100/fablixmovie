package query;

/*
 * Collects a paginated list of movies with optional conditions
 * on its fields and orders rows by either its title/rating or its rating/title
 */
public class MovieListQuery extends ConditionalQuery {
    /* search params */
    private String title;
    private String director;
    private String year;
    private String star;

    /* browse params */
    private String alpha;
    private String genreId;

    public MovieListQuery() {
        selectClauses.add(
                "m.*, IFNULL(r.rating, 0) 'r.rating', "
                + "IFNULL(r.numVotes, 0) 'r.numVotes', p.price"
        );
        joinClauses.add(
                "movies m LEFT JOIN ratings r ON m.id = r.movieId "
                + "LEFT JOIN prices p ON m.id = p.movieId"
        );
        order = new String[2];
        orderByRatingTitle(OrderMode.DESC, OrderMode.ASC);
    }

    public void update() {
        /* reset state */
        subQueries.clear();
        selectClauses.subList(1, selectClauses.size()).clear();
        joinClauses.subList(1, joinClauses.size()).clear();
        whereClauses.clear();
        params.clear();
        subParams.clear();

        /* search clauses */
        if (title != null && !title.isEmpty()) {
            SearchSubQuery.extendFulltext("movies_t", "movies", "title",
                    title, subQueries, subParams);
            whereClauses.add("m.id IN (SELECT id FROM movies_t)");
        }
        if (director != null && !director.isEmpty()) {
            SearchSubQuery.extendLike("movies_d", "movies", "director",
                    director, subQueries, subParams);
            whereClauses.add("m.id IN (SELECT id FROM movies_d)");
        }
        if (year != null && !year.isEmpty()) {
            whereClauses.add("m.year = ?");
            params.add(year);
        }
        if (star != null && !star.isEmpty()) {
            SearchSubQuery.extendLike("stars_n", "stars", "name",
                    star, subQueries, subParams);
            joinClauses.add("JOIN stars_in_movies sim ON m.id = sim.movieId");
            joinClauses.add("JOIN stars_n s ON sim.starId = s.id");
        }

        /* browse clauses */
        if (alpha != null && !alpha.isEmpty()) {
            if (alpha.charAt(0) == '*') {
                whereClauses.add("REGEXP_LIKE(m.title, ?)");
                params.add("^[^a-zA-Z0-9].+");
            } else {
                whereClauses.add("LOWER(m.title) LIKE LOWER(?)");
                params.add(alpha + "%");
            }
        }
        if (genreId != null && !genreId.isEmpty()) {
            joinClauses.add("JOIN genres_in_movies gim ON m.id = gim.movieId");
            joinClauses.add("JOIN genres g ON gim.genreId = g.id");
            whereClauses.add("g.id = ?");
            params.add(genreId);
        }
    }

    /*
     * "title", "director", "year", and "star" are user defined queries,
     * so these fields are trimmed in case the user includes
     * leading/trailing whitespace (or just spaces)
     *
     * Note: Setters are required to mark the 'pleaseUpdate' flag
     */

    public final void setTitle(String title) {
        this.title = title != null
                ? title.trim().replaceAll("\\s+", " ")
                : null;
        pleaseUpdate = true;
    }

    public final void setDirector(String director) {
        this.director = director != null
                ? director.trim().replaceAll("\\s+", " ")
                : null;
        pleaseUpdate = true;
    }

    public final void setYear(String year) {
        this.year = year != null ? year.trim() : null;
        pleaseUpdate = true;
    }

    public final void setStar(String star) {
        this.star = star != null
                ? star.trim().replaceAll("\\s+", " ")
                : null;
        pleaseUpdate = true;
    }

    public final void setAlpha(String alpha) {
        this.alpha = alpha;
        pleaseUpdate = true;
    }

    public final void setGenreId(String genreId) {
        this.genreId = genreId;
        pleaseUpdate = true;
    }

    /*
     * Order functions need not mark the 'pleaseUpdate' flag,
     * as the order strings are "re-built" each time a statement is prepared.
     */

    public final void orderByTitleRating(OrderMode titleMode, OrderMode ratingMode) {
        this.order[0] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "r.rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
    }

    public final void orderByRatingTitle(OrderMode ratingMode, OrderMode titleMode) {
        this.order[0] = "r.rating " + (ratingMode == OrderMode.ASC ? "ASC" : "DESC");
        this.order[1] = "m.title " + (titleMode == OrderMode.ASC ? "ASC" : "DESC");
    }
}
