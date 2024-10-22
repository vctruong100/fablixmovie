let resultsUrl = "movielist.html?limit=10&page=1";

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
    const alphabet = [...'ABCDEFGHIJKLMNOPQRSTUVWXYZ*'];

    alphabet.forEach(letter => {
        let letterHTML = `<li><a href="${resultsUrl}&alpha=${letter}">${letter}</a></li>`;
        alphabetListElement.append(letterHTML);
    });
}

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

    // Handle search form submission
    jQuery("#search_form").submit((event) => {
        event.preventDefault();
        const title = jQuery('input[name="title"]').val();
        const year = jQuery('input[name="year"]').val();
        const director = jQuery('input[name="director"]').val();
        const star = jQuery('input[name="star"]').val();

        let searchQuery = `${resultsUrl}&title=${title}&year=${year}&director=${director}&star=${star}`;
        window.location.href = searchQuery;  // Redirect to Movie List page with search parameters
    });
});