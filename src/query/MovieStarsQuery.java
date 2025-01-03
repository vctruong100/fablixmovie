package query;

/*
 * Collects stars associated with a movie id and orders the results
 * by how prolific a star is and their names
 */
public class MovieStarsQuery extends GroupingQuery {
    public MovieStarsQuery(String movieId) {
        super(movieId);
        builder.append(
                "SELECT s.id, s.name, s.birthYear, COUNT(*) movieCount FROM stars_in_movies sim, stars s "
                + "WHERE sim.starId = s.id AND s.id IN ("
                + "SELECT sim2.starId FROM stars_in_movies sim2 WHERE sim2.movieId = ?) "
                + "GROUP BY s.id ORDER BY movieCount DESC, s.name ASC");
    }
}
