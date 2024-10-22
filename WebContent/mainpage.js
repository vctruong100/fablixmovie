// Fetch genres from the server and populate the genre list
function loadGenres() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/main-page",
        success: (resultData) => {
            console.log("Genres loaded:", resultData);

            let genreListElement = jQuery("#genre_list");
            resultData.forEach(genre => {
                genreListElement.append(`<li><a href="movielist.html?genre=${genre.genre_name}">${genre.genre_name}</a></li>`);
            });
        },
        error: (error) => {
            console.error("Error loading genres:", error);
        }
    });
}

// Populate the alphabet list for title browsing
function loadAlphabetList() {
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split('');
    let alphabetListElement = jQuery("#alphabet_list");

    alphabet.forEach(letter => {
        alphabetListElement.append(`<li><a href="movielist.html?start=${letter}">${letter}</a></li>`);
    });
    alphabetListElement.append(`<li><a href="movielist.html?start=*">#</a></li>`);
}

// Handle the search form submission
jQuery("#search_form").submit(function (event) {
    event.preventDefault();

    const searchParams = jQuery(this).serialize();
    window.location.href = `movielist.html?${searchParams}`;
});

// Load the genres and alphabet list when the page is ready
jQuery(document).ready(() => {
    loadGenres();
    loadAlphabetList();
});
