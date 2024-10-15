
/**
 * Handles the data returned by the API, read the jsonObject and populate data into HTML elements
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: populating movie list from resultData");

    // Find the element to populate the movie list
    let movieListElement = jQuery("#movie_list_body");

    // Iterate through resultData, display up to 20 movies
    for (let i = 0; i < Math.min(20, resultData.length); i++) {
        // Create a new row for each movie
        let rowHTML = "<tr>";

        // Add movie title with a link to the single movie page
        rowHTML +=
            "<td><a href='single-movie.html?id=" + resultData[i]['movie_id'] + "'>" +
            resultData[i]['movie_title'] +
            "</a></td>";

        rowHTML += "<td>" + resultData[i]['movie_year'] + "</td>";
        rowHTML += "<td>" + resultData[i]['movie_director'] + "</td>";
        rowHTML += "<td>" + resultData[i]['movie_rating'] + "</td>";
        rowHTML += "<td>" + resultData[i]['movie_num_votes'] + "</td>";

        // Genres
        rowHTML += "<td>";
        let genres = resultData[i]["movie_genres"];
        for (let j = 0; j < genres.length; j++) {
            rowHTML += genres[j]["genre_name"];
            if (j < genres.length - 1) {
                rowHTML += ", ";
            }
        }
        rowHTML += "</td>";

        // Stars (with hyperlinks)
        rowHTML += "<td>";
        let stars = resultData[i]["movie_stars"];
        for (let j = 0; j < stars.length; j++) {
            rowHTML += "<a href='single-star.html?id=" + stars[j]["star_id"] + "'>" +
                stars[j]["star_name"] + "</a>";
            if (j < stars.length - 1) {
                rowHTML += ", ";
            }
        }
        rowHTML += "</td>";

        rowHTML += "</tr>";

        // Append the row to the table body
        movieListElement.append(rowHTML);
    }
}

/**
 * Once this .js is loaded, the following scripts will be executed by the browser.
 */

// Makes the HTTP GET request and registers on success callback function handleMovieResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",     // Setting request method
    url: "api/movie-list",  // Setting request URL, which is mapped by MovieListServlet
    success: (resultData) => handleMovieResult(resultData)  // Setting callback function to handle the returned data
});
