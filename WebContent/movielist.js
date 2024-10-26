/// movielist.js

/*
 * Query parameters to load
 * We use sessionStorage to store/get params unless
 * the user has already specified some parameters in the URL
 */
let queryParams = {
    limit: null,
    page: null,
    sortBy: null,
    title: null,
    year: null,
    director: null,
    star: null,
    alpha: null,
    genre: null,
};
let totalPages = null;

// based on queryParams and appended at the end of a URL
// defer its construction until queryParams is initialized
let queryString = null;


// Elements retrieved from jQuery
// Store here to avoid computing additional unnecessary queries
// Note: $ is short for jQuery
let limitPerPageElement = $("#limit-per-page");
let pageNumberElement = $("#page-number");
let sortByElement = $("#sort-by");
let prevPageElement = $("#prev-page");
let nextPageElement = $("#next-page");
let resultsBtnElement = $("#results-btn");


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
    // Currently, back form parameters are not used, because
    // parameters are locally stored in sessionStorage
    //
    // However, it's a good way to check if the parameters are
    // synced to the client
    let backFormParams = resultData.params;
    console.log("back form params");
    console.log(backFormParams);

    // Total number of movies found for this search result only
    // Does not count ALL movies
    let count = parseInt(resultData.count);
    totalPages = Math.ceil(count / parseInt(queryParams.limit)) || 1;

    // Update nextPage button after total pages has been computed
    nextPageElement.prop("disabled", parseInt(queryParams.page) >= totalPages);

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

            rowHTML += `<td><button class="add-to-cart" data-id="${movie.movie_id}">Add to Cart</button></td>`;

            rowHTML += "</tr>";
            movieListElement.append(rowHTML);
        });
    } else {
        console.error("Expected movies array, but received:", movies);
    }
}


// Update movie list according to the current state of queryParams
const updateMovieList = (queryParams) => {
    // Determine which API to call based on queryParams
    if (queryParams.title || queryParams.year ||
        queryParams.director || queryParams.star) {
        // Case for search
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/search?${queryString}`,
            success: (response) => handleMovieResult(response),
        });
    } else if (queryParams.alpha || queryParams.genre) {
        // Case for browse
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/browse?${queryString}`,
            success: (response) => handleMovieResult(response),
        })
    } else {
        // Default case for general movie listing (no search or filters applied)
        // By default, this uses the previous query session as the base query
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/movie-list`,
            success: (response) => handleMovieResult(response),
        });
    }
}

// Load any query parameters into the local queryParams state
// You should only call this on a new page / URL load
function loadQueryParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const savedState = JSON.parse(sessionStorage.getItem("movieListState")) || {};

    // URL parameters take precedent
    // in case the user initiates a new search/browse query
    if (urlParams.size > 0) {
        // We should look at limit, sortBy variable states to preserve
        // the user's limit and sorting preferences if limit/sortBy are undefined
        for (const [key, value] of urlParams.entries()) {
            queryParams[key] = value;
        }
        if (queryParams.page === null) {
            queryParams.page = 1; // always start at first page
        }
        if (queryParams.limit === null) {
            queryParams.limit = parseInt(savedState.limit) || 10;
        }
        if (queryParams.sortBy === null) {
            queryParams.sortBy = savedState.sortBy || "title-asc-rating-asc";
        }
    } else {
        // No URL parameters, rely on sessionStorage instead
        for (const [key, value] of Object.entries(savedState)) {
            queryParams[key] = value;
        }
    }
}

// Propagate the current query parameters onto any visible elements
function propQueryParams() {
    // Store the current query params into the sessionStorage
    sessionStorage.setItem("movieListState", JSON.stringify(queryParams));

    // Construct a new queryString via queryParams
    queryString = "";
    for (const [key, value] of Object.entries(queryParams)) {
        if (value) {
            queryString += `&${key}=${value}`;
        }
    }
    queryString = queryString.slice(1);

    // Update URL
    const newUrl = `${window.location.pathname}${queryString.length > 0 ? "?" : ""}${queryString}`;
    window.history.pushState({ path: newUrl }, '', newUrl);

    // Update movie list
    updateMovieList(queryParams);

    // Update elements except the next page button
    // We exclude next page because their behavior is reliant on the
    // completion of an async call (ajax)
    // The next page element will update once totalPages has been computed
    pageNumberElement.val(queryParams.page);
    limitPerPageElement.val(queryParams.limit);
    sortByElement.val(queryParams.sortBy);
    prevPageElement.prop("disabled", parseInt(queryParams.page) === 1);
    // nextPageElement.prop("disabled", parseInt(queryParams.page) >= totalPages);
}


/*
 * Begin reactive callbacks
 */


// Hyperlinks
jQuery(document).on("click", "a", function() {
    loadQueryParams();
    propQueryParams();
});

// Handle sorting selection change
sortByElement.on("change", function() {
    queryParams.sortBy = sortByElement.val();
    propQueryParams();
});

// Handle page change
prevPageElement.on("click", function() {
    queryParams.page = parseInt(queryParams.page);
    if (queryParams.page > 1) {
        queryParams.page--;
        propQueryParams();
    }
});

nextPageElement.on("click", function() {
    queryParams.page = parseInt(queryParams.page);
    if (queryParams.page < totalPages) {
        queryParams.page++;
        propQueryParams();
    }
});

// Handle limit (movies per page) change
limitPerPageElement.on("change", function() {
    queryParams.limit = parseInt(limitPerPageElement.val(), 10);  // Update the limit
    propQueryParams();
});

// Handle page jump via input field
pageNumberElement.on("change", function() {
    let requestedPage = parseInt(pageNumberElement.val(), 10);
    if (requestedPage < 1) {
        requestedPage = 1;
    } else if (requestedPage > totalPages) {
        requestedPage = totalPages;
    }
    queryParams.page = requestedPage;
    propQueryParams();
});

// Results button
resultsBtnElement.on("click", function(event) {
    event.preventDefault();
    const movieListState = sessionStorage.getItem("movieListState");
    let queryString = "";
    if (movieListState) {
        const state = JSON.parse(movieListState);
        queryString = `&limit=${state.limit}&page=${state.page}&sortBy=${state.sortBy}`;
        if (state.title) {
            queryString += `&title=${state.title}`;
        }
        if (state.year) {
            queryString += `&year=${state.year}`;
        }
        if (state.director) {
            queryString += `&director=${state.director}`;
        }
        if (state.star) {
            queryString += `&star=${state.star}`;
        }
        if (state.alpha) {
            queryString += `&alpha=${state.alpha}`;
        }
        if (state.genre) {
            queryString += `&genre=${state.genre}`;
        }
    }
    window.location.href = `movielist.html?${queryString}`;
});

// Add to cart buttons
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


/* End reactive callbacks */


/**
 * Once this .js is loaded, the following scripts will be executed by the browser.
 */
jQuery(document).ready(() => {
    loadQueryParams();
    propQueryParams();
    console.log("Initial queryParams: ", queryParams);
    console.log("Initial queryString: ", queryString);
});