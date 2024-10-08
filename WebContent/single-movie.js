/**
 * Handles the data returned by the SingleMovieServlet and populates the HTML elements
 * @param resultData JSON object containing movie details
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: populating movie details from resultData");

    // Populate movie details
    jQuery("#movie-title").text(resultData["movie_title"]);
    jQuery("#movie-year").text(resultData["movie_year"]);
    jQuery("#movie-director").text(resultData["movie_director"]);
    jQuery("#movie-rating").text(resultData["movie_rating"]);
    jQuery("#movie-votes").text(resultData["movie_num_votes"]);

    // Populate genres
    let movieGenresElement = jQuery("#movie-genres");
    let genres = resultData["movie_genres"];
    for (let i = 0; i < genres.length; i++) {
        let genreItem = "<li>" + genres[i]["genre_name"] + "</li>";
        movieGenresElement.append(genreItem);
    }

    // Populate stars
    let movieStarsElement = jQuery("#movie-stars");
    let stars = resultData["movie_stars"];
    for (let i = 0; i < stars.length; i++) {
        let starItem = "<li><a href='single-star.html?id=" + stars[i]["star_id"] + "'>" +
            stars[i]["star_name"] + " (Born: " + (stars[i]["star_birth_year"] ? stars[i]["star_birth_year"] : "N/A") + ")" +
            "</a></li>";
        movieStarsElement.append(starItem);
    }
}

/**
 * Once the JS is loaded, the following code will be executed.
 */
jQuery.ajax({
    dataType: "json",  // Return data type
    method: "GET",     // Request method
    url: "api/single-movie?id=" + new URLSearchParams(window.location.search).get('id'),  // Request URL with id parameter
    success: (resultData) => handleMovieResult(resultData)  // Success callback
});
