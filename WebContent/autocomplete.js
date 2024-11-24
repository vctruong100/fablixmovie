let cache = JSON.parse(sessionStorage.getItem("autocompleteCache")) || {};
let typingTimer;
const typingDelay = 300; // Delay time in milliseconds
const titleSearchBox = $("#title_search_box");
const dropdown = $("#autocomplete_dropdown");
let currentIndex = -1;

function saveCache() {
    sessionStorage.setItem("autocompleteCache", JSON.stringify(cache));
}

// Function to perform the autocomplete search
function performSearch(query) {
    console.log("Autocomplete search initiated for:", query);
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
                console.log("Received suggestions from server:", data);
                cache[query] = data;
                saveCache();
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
    console.log("Displaying suggestions:", suggestions);

    dropdown.empty().hide();
    if (suggestions.length > 0) {
        currentIndex = -1;
        suggestions.slice(0, 10).forEach((suggestion, index) => {
            console.log(suggestion);
            const suggestionDiv = $("<div>")
                .text(suggestion.title)
                .attr("data-index", index)
                .attr("data-movie_id", suggestion.movie_id)
                .addClass("suggestion-item")
                .on("click", function () {
                    window.location.href = `single-movie.html?id=${suggestion.movie_id}`;
                })
                .on("mouseenter", function () {
                    $(".highlight").removeClass("highlight");
                    $(this).addClass("highlight");
                    currentIndex = index;
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

titleSearchBox.on("keydown", function (event) {
    const suggestions = dropdown.children(".suggestion-item");
    if (event.key === "ArrowDown") {
        event.preventDefault();
        if (currentIndex < suggestions.length - 1) {
            currentIndex++;
            $(".highlight").removeClass("highlight");
            $(suggestions[currentIndex]).addClass("highlight");

            scrollToSuggestion(suggestions[currentIndex]);
        }
    } else if (event.key === "ArrowUp") {
        event.preventDefault();
        if (currentIndex > 0) {
            currentIndex--;
            $(".highlight").removeClass("highlight");
            $(suggestions[currentIndex]).addClass("highlight");

            scrollToSuggestion(suggestions[currentIndex]);
        } else {
            currentIndex = -1;
            $(".highlight").removeClass("highlight");
        }
    } else if (event.key === "Enter") {
        event.preventDefault();
        if (currentIndex >= 0 && currentIndex < suggestions.length) {
            // If a suggestion is selected, redirect to the single movie page
            const selectedSuggestion = suggestions[currentIndex];
            const movieId = $(selectedSuggestion).data("movie_id");
            window.location.href = `single-movie.html?id=${movieId}`;

        } else {
            $("#search_form").submit();
        }
    }
});

titleSearchBox.on("blur", function () {
    setTimeout(() => dropdown.hide(), 200);
});

function scrollToSuggestion(element) {
    const dropdownTop = dropdown.scrollTop();
    const dropdownHeight = dropdown.height();
    const elementTop = $(element).position().top + dropdownTop;
    const elementHeight = $(element).outerHeight();

    // Scroll down if the element is below the visible area
    if (elementTop + elementHeight > dropdownTop + dropdownHeight) {
        dropdown.scrollTop(elementTop + elementHeight - dropdownHeight);
    }
    // Scroll up if the element is above the visible area
    else if (elementTop < dropdownTop) {
        dropdown.scrollTop(elementTop);
    }
}


jQuery("#search_form").submit((event) => {
    event.preventDefault();
    const title = jQuery('input[name="title"]').val();
    const year = jQuery('input[name="year"]').val();
    const director = jQuery('input[name="director"]').val();
    const star = jQuery('input[name="star"]').val();

    let searchQuery = `movielist.html?page=1&title=${title}&year=${year}&director=${director}&star=${star}`;
    window.location.href = searchQuery;
});
