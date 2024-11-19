let resultsUrl = "movielist.html?page=1";

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
