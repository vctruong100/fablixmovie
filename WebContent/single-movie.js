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
    genres.forEach((genre) => {
        let genreItem = `<li><a href="movielist.html?genre=${genre.genre_id}">${genre.genre_name}</a></li>`;
        movieGenresElement.append(genreItem);
    });

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


jQuery("#add-to-cart").on("click", function() {
    let movieId = $(this).data('id');
    $.ajax({
        url: "api/shoppingcart",
        method: "POST",
        data: { movieId: movieId, action: "add" },
        success: function() {
            alert("Added to cart successfully!");
        },
        error: function() {
            alert("Failed to add to cart.");
        }
    });
});

jQuery("#back-to-list").on("click", function() {
    const movieListState = sessionStorage.getItem("movieListState");
    let queryParams = "";
    if (movieListState) {
        const state = JSON.parse(movieListState);
        queryParams = `&limit=${state.currentLimit}&page=${state.currentPage}&sortBy=${state.currentSortBy}`;

        if (state.currentAlpha) {
            queryParams += `&alpha=${state.currentAlpha}`;
        }
        if (state.currentGenre) {
            queryParams += `&genre=${state.currentGenre}`;
        }
    }

    window.location.href = `movielist.html?${queryParams}`;
});


/**
 * Once the JS is loaded, the following code will be executed.
 */
jQuery.ajax({
    dataType: "json",  // Return data type
    method: "GET",     // Request method
    url: "api/single-movie?id=" + new URLSearchParams(window.location.search).get('id'),  // Request URL with id parameter
    success: (resultData) => handleMovieResult(resultData)  // Success callback
});
