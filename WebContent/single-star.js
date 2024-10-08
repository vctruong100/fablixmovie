/**
 * Handles the data returned by the SingleStarServlet and populates the HTML elements
 * @param resultData JSON object containing star details
 */
function handleStarResult(resultData) {
    console.log("handleStarResult: populating star details from resultData");

    jQuery("#star-name").text(resultData["star_name"]);

    const birthYear = resultData["star_birth_year"] ? resultData["star_birth_year"] : "N/A";
    jQuery("#star-birth-year").text(birthYear);

    // Populate the list of movies
    let starMoviesElement = jQuery("#star-movies");
    let movies = resultData["star_movies"];

    if (movies.length === 0) {
        starMoviesElement.append("<li>No movies available</li>");
    } else {
        for (let i = 0; i < movies.length; i++) {
            let movieItem = "<li><a href='single-movie.html?id=" + movies[i]["movie_id"] + "'>" +
                movies[i]["movie_title"] + " (" + movies[i]["movie_year"] + "), directed by " + movies[i]["movie_director"] +
                "</a></li>";
            starMoviesElement.append(movieItem);
        }
    }
}

jQuery.ajax({
    dataType: "json",  // Return data type
    method: "GET",     // Request method
    url: "api/single-star?id=" + new URLSearchParams(window.location.search).get('id'),  // Request URL with id parameter
    success: (resultData) => handleStarResult(resultData)  // Success callback
});
