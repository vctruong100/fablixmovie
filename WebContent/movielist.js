
/**
 * Handles the data returned by the API, read the jsonObject and populate data into HTML elements
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: populating movie list from resultData");
    console.log(resultData);

    // Find the element to populate the movie list
    let movieListElement = jQuery("#movie_list_body");
    movieListElement.empty();

    // Iterate through resultData and populate the movie list
    resultData.forEach(movie => {
        let rowHTML = "<tr>";
        rowHTML += `<td><a href="single-movie.html?id=${movie.movie_id}">${movie.movie_title}</a></td>`;
        rowHTML += `<td>${movie.movie_year}</td>`;
        rowHTML += `<td>${movie.movie_director}</td>`;
        rowHTML += `<td>${movie.movie_rating}</td>`;
        rowHTML += `<td>${movie.movie_num_votes}</td>`;

        // Genres
        rowHTML += "<td>";
        movie.movie_genres.slice(0, 3).forEach((genre, index) => {
            rowHTML += `<a href="movielist.html?genre=${genre.genre_id}">${genre.genre_name}</a>`;
            if (index < movie.movie_genres.length - 1) rowHTML += ", ";
        });
        rowHTML += "</td>";

        // Stars
        rowHTML += "<td>";
        movie.movie_stars.forEach((star, index) => {
            rowHTML += `<a href="single-star.html?id=${star.star_id}">${star.star_name}</a>`;
            if (index < movie.movie_stars.length - 1) rowHTML += ", ";
        });
        rowHTML += "</td>";

        rowHTML += "</tr>";
        movieListElement.append(rowHTML);
    });
}

// Extract query parameters from the URL for search or browsing
function getQueryParams() {
    const params = new URLSearchParams(window.location.search);
    let queryString = "";

    if (params.has("limit")) {
        queryString += `&limit=${params.get("limit")}`;
    }
    if (params.has("page")) {
        queryString += `&page=${params.get("page")}`;
    }
    if (params.has("title")) {
        queryString += `&title=${params.get("title")}`;
    }
    if (params.has("year")) {
        queryString += `&year=${params.get("year")}`;
    }
    if (params.has("director")) {
        queryString += `&director=${params.get("director")}`;
    }
    if (params.has("star")) {
        queryString += `&star=${params.get("star")}`;
    }
    if (params.has("genre")) {
        queryString += `&genre=${params.get("genre")}`;
    }
    if (params.has("start")) {
        queryString += `&start=${params.get("start")}`;
    }
    if (params.has("alpha")) {
        const alphaValue = params.get("alpha");
        console.log("Alpha value: " + alphaValue);  // Log the alpha value for debugging
        queryString += `&alpha=${params.get("alpha")}`;
    }
    const sortByValue = jQuery("#sort-by").val();
    if (sortByValue) {
        queryString += `&sortBy=${sortByValue}`;
    }
    console.log("Generated query string: " + queryString);

    return queryString;
}

/**
 * Once this .js is loaded, the following scripts will be executed by the browser.
 */
jQuery(document).ready(() => {
    const queryParams = getQueryParams();
    // placeholder4

    const updateMovieList = (queryParams) => {
        // Determine which API to call based on queryParams
        if (queryParams.includes("sortBy")) {
            jQuery.ajax({
                dataType: "json",
                method: "GET",
                url: `api/movie-list?${queryParams}`,
                success: (resultData) => handleMovieResult(resultData),
            });
        }
        // Case for browsing by alphabet or genre
        else if (queryParams.includes("alpha") || queryParams.includes("genre")) {
            jQuery.ajax({
                dataType: "json",
                method: "GET",
                url: `api/browse?${queryParams}`,
                success: (resultData) => handleMovieResult(resultData),
            });
        }
        // Case for search
        else if (queryParams.includes("title") || queryParams.includes("year") ||
            queryParams.includes("director") || queryParams.includes("star")) {
            jQuery.ajax({
                dataType: "json",
                method: "GET",
                url: `api/search?${queryParams}`,
                success: (resultData) => handleMovieResult(resultData),
            });
        }
        // Default case for general movie listing (no search or filters applied)
        else {
            jQuery.ajax({
                dataType: "json",
                method: "GET",
                url: `api/movie-list?${queryParams}`,
                success: (resultData) => handleMovieResult(resultData),
            });
        }
    };

    // Handle sorting selection change
    jQuery("#sort-by").on("change", function() {
        const queryParams = getQueryParams();
        updateMovieList(queryParams);
    });

    updateMovieList(queryParams);
});