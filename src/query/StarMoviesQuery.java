package query;

import java.sql.Connection;

/*
 * Collects movies associated with a star id and orders the results
 * by latest movie year and title
 */
public class StarMoviesQuery extends GroupingQuery {
    public StarMoviesQuery(String starId) {
        super(starId);
        builder.append(
            "SELECT * FROM stars_in_movies sim, movies m "
            + "WHERE sim.movieId = m.id AND sim.starId = ? "
            + "ORDER BY m.year DESC, m.title ASC");
    }
}
