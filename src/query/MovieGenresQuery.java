package query;

import java.sql.Connection;

/*
 * Collects genres associated with a movie id and orders the results
 * by the genre name
 */
public class MovieGenresQuery extends GroupingQuery {
    public MovieGenresQuery(String movieId) {
        super(movieId);
        builder.append(
                "SELECT * FROM genres_in_movies gim, genres g "
                + "WHERE gim.genreId = g.id AND gim.movieId = ? "
                + "ORDER BY g.name ASC");
    }
}
