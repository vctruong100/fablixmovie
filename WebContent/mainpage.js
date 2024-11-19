let resultsUrl = "movielist.html?page=1";
let cache = {}; // Cache for storing previous queries and their results
let typingTimer;
const typingDelay = 300; // Delay time in milliseconds
const titleSearchBox = $("#title_search_box");
const dropdown = $("#autocomplete_dropdown");

// Fetch genres from the server and populate the genre list
function handleGenreResult(resultData) {
    let genreListElement = jQuery("#genre_list");
    resultData.forEach(genre => {
        let genreHTML = `<li><a href="${resultsUrl}&genre=${genre.genre_id}">${genre.genre_name}</a></li>`;
        genreListElement.append(genreHTML);
    });
}

// Populate the alphabet list for title browsing
function handleAlphabetList() {
    let alphabetListElement = jQuery("#alphabet_list");
    const alphabet = [...'0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ*'];

    alphabet.forEach(letter => {
        let letterHTML = `<li><a href="${resultsUrl}&alpha=${letter}">${letter}</a></li>`;
        alphabetListElement.append(letterHTML);
    });
}

jQuery("#results-btn").on("click", function(event) {
    event.preventDefault();
    window.location.href = `movielist.html`;
});

jQuery(document).ready(() => {
    // Load genres from MainPageServlet
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/main-page",  // Calls MainPageServlet to get genres
        success: handleGenreResult
    });

    // Populate alphabet list
    handleAlphabetList();

});

// Handle logout button click
jQuery("#logout_button").on("click", function() {
    jQuery.ajax({
        url: "api/logout",
        method: "POST",
        success: function() {
            // Redirect to the login page after logging out
            window.location.replace("login.html");
        },
        error: function() {
            alert("Logout failed. Please try again.");
        }
    });
});

// Handle search form submission
jQuery("#search_form").submit((event) => {
    event.preventDefault();
    const title = jQuery('input[name="title"]').val();
    const year = jQuery('input[name="year"]').val();
    const director = jQuery('input[name="director"]').val();
    const star = jQuery('input[name="star"]').val();

    let searchQuery = `${resultsUrl}&title=${title}&year=${year}&director=${director}&star=${star}`;
    window.location.href = searchQuery;
});

// Function to perform the autocomplete search
function performSearch(query) {
    if (cache[query]) {
        console.log("Using cached results for:", query);
        displaySuggestions(cache[query]);
    } else {
        // Otherwise, make an AJAX request to the backend
        console.log("Fetching results from server for:", query);
        $.ajax({
            url: "api/autocomplete",
            method: "GET",
            data: { query: query },
            success: function (data) {
                cache[query] = data;
                displaySuggestions(data);
            },
            error: function () {
                console.error("Failed to fetch autocomplete suggestions.");
            }
        });
    }
}

// Function to display the autocomplete suggestions
function displaySuggestions(suggestions) {
    dropdown.empty().hide();
    if (suggestions.length > 0) {
        suggestions.forEach(suggestion => {
            const suggestionDiv = $("<div>").text(suggestion.title);
            suggestionDiv.on("click", function () {
                titleSearchBox.val(suggestion.title);
                dropdown.hide();
            });
            dropdown.append(suggestionDiv);
        });
        dropdown.show();
    }
}

// Event listener for the title search box
titleSearchBox.on("input", function () {
    clearTimeout(typingTimer);
    const query = $(this).val().trim();

    if (query.length >= 3) {
        typingTimer = setTimeout(() => performSearch(query), typingDelay);
    } else {
        dropdown.hide();
    }
});