
/*
 * NOTE: These values do not persist across pages, but
 * the values will always be assigned ONCE using the current
 * session values on page load UNLESS a new search / browse query is initiated
 *
 * This is achieved via back form parameters from the API call in
 * case you don't like to read code
 */
let totalPages = null;
let currentPage = null;
let currentLimit = null;
let currentSortBy = null;

let currentTitle = null;
let currentYear = null;
let currentDirector = null;
let currentStar = null;

let currentAlpha = null;
let currentGenre = null;

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

    // List of parameters used to retrieve the result
    // Parameter fields are dependent on whether search query mode was search or browse by genre
    // Maybe this can be used to restore the page with the query info? (for example HTML field values?)
    let backFormParams = resultData.params;
    console.log("back form params");
    console.log(backFormParams);

    // store session values
    currentPage = parseInt(backFormParams.page);
    currentLimit = parseInt(backFormParams.limit);
    currentSortBy = backFormParams.sortBy;

    if (backFormParams.queryMode === "search") {
        currentTitle = backFormParams.title;
        currentYear = backFormParams.year;
        currentDirector = backFormParams.director;
        currentStar = backFormParams.star;
    } else if (backFormParams.queryMode === "browse") {
        currentAlpha = backFormParams.alpha;
        currentGenre = backFormParams.genre;
    }

    // Total number of movies found for this search result only
    // Does not count ALL movies
    let count = parseInt(resultData.count);
    totalPages = Math.ceil(count / currentLimit) || 1

    let movies = resultData.results;

    if (Array.isArray(movies)) {
        movies.forEach(movie => {
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
    } else {
        console.error("Expected movies array, but received:", movies);
    }
    jQuery("#prev-page").prop("disabled", currentPage === 1);
    jQuery("#next-page").prop("disabled", currentPage === totalPages);
}

jQuery(document).on("click", "a", function() {
    storeCurrentState();
});

function storeCurrentState() {
    const state = {
        currentPage: currentPage,
        currentLimit: currentLimit,
        currentSortBy: currentSortBy,
        currentTitle: currentTitle,
        currentYear: currentYear,
        currentDirector: currentDirector,
        currentStar: currentStar,
        currentAlpha: currentAlpha,
        currentGenre: currentGenre,
    };
    sessionStorage.setItem("movieListState", JSON.stringify(state));
}

function restoreState() {
    const savedState = sessionStorage.getItem("movieListState");
    if (savedState) {
        const state = JSON.parse(savedState);
        currentPage = state.currentPage || 1;
        currentLimit = state.currentLimit || 10;
        currentSortBy = state.currentSortBy || "title-asc-rating-asc";
        currentTitle = state.currentTitle || "";
        currentYear = state.currentYear || "";
        currentDirector = state.currentDirector || "";
        currentStar = state.currentStar || "";
        currentAlpha = state.currentAlpha || "";
        currentGenre = state.currentGenre || "";

        // Update the UI with the restored state
        jQuery("#page-number").val(currentPage);
        jQuery("#limit-per-page").val(currentLimit);
        jQuery("#sort-by").val(currentSortBy);
    }
}
// Extract query parameters from the URL for search or browsing
function getQueryParams() {
    const params = new URLSearchParams(window.location.search);
    let queryString = "";
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
        console.log("Alpha value: " + alphaValue);
        queryString += `&alpha=${params.get("alpha")}`;
    }

    // Only assign limit and page if they are assigned
    // Assignment is deferred until the back form parameters
    // are returned via API
    if (currentLimit != null) {
        queryString += `&limit=${currentLimit}`;
    }
    if (currentPage != null) {
        queryString += `&page=${currentPage}`;
    }

    const sortByValue = jQuery("#sort-by").val();
    if (sortByValue) {
        queryString += `&sortBy=${sortByValue}`;
    }
    console.log("Generated query string: " + queryString);

    return queryString;
}

const updateMovieList = (queryParams) => {
    // Determine which API to call based on queryParams
    if (queryParams.includes("title=") || queryParams.includes("year=") ||
        queryParams.includes("director=") || queryParams.includes("star=")) {
        // Case for search
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/search?${queryParams}`,
            success: (resultData) => handleMovieResult(resultData),
        });
    }
    // Case for browsing by alphabet or genre
    else if (queryParams.includes("alpha=") || queryParams.includes("genre=")) {
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/browse?${queryParams}`,
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
    updateUrl(queryParams);
});

// Handle page change
jQuery("#prev-page").on("click", function() {
    if (currentPage > 1) {
        currentPage--;
        const queryParams = getQueryParams();
        updateMovieList(queryParams);
        updateUrl(queryParams);
        jQuery("#page-number").val(currentPage);
    }
    jQuery(this).prop("disabled", currentPage === 1);
});

jQuery("#next-page").on("click", function() {
    currentPage++;
    const queryParams = getQueryParams();
    updateMovieList(queryParams);
    updateUrl(queryParams);
    jQuery("#page-number").val(currentPage);
    jQuery(this).prop("disabled", currentPage === totalPages);
});

// Handle limit (movies per page) change
jQuery("#limit-per-page").on("change", function() {
    currentLimit = parseInt(jQuery(this).val(), 10); // Update the limit
    currentPage = 1; // Reset to the first page when changing limit
    jQuery("#page-number").val(currentPage); // Update the page input to display 1
    const queryParams = getQueryParams();
    updateMovieList(queryParams);
    updateUrl(queryParams);
    jQuery("#prev-page").prop("disabled", true);
    jQuery("#next-page").prop("disabled", false);
});

// Handle page jump via input field
jQuery("#page-number").on("change", function() {
    let requestedPage = parseInt(jQuery(this).val(), 10);
    if (requestedPage < 1) {
        requestedPage = 1;
    } else if (requestedPage > totalPages) {
    requestedPage = totalPages;
    }

    currentPage = requestedPage;
    jQuery(this).val(currentPage);

    const queryParams = getQueryParams();
    updateMovieList(queryParams);
    updateUrl(queryParams);
    jQuery("#prev-page").prop("disabled", currentPage === 1);
    jQuery("#next-page").prop("disabled", currentPage === totalPages);
});

function updateUrl(queryParams) {
    // Update the URL with the new query params, preserving the current state
    const newUrl = `${window.location.pathname}?${queryParams}`;
    window.history.pushState({ path: newUrl }, '', newUrl);
}

$(document).on('click', '.add-to-cart', function() {
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

/**
 * Once this .js is loaded, the following scripts will be executed by the browser.
 */
jQuery(document).ready(() => {
    restoreState();
    const queryParams = getQueryParams();
    console.log("Initial queryParams: ", queryParams);
    updateMovieList(queryParams);
});