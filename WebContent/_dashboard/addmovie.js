// Function to handle adding a movie
function handleAddMovie(event) {
    event.preventDefault();

    jQuery.ajax({
        url: "../api/dashboard",
        method: "POST",
        data: jQuery("#add-movie-form").serialize() + "&action=addMovie",
        dataType: "json",
        success: function (response) {
            jQuery("#response-message").text(response.message);
        },
        error: function (xhr, status, error) {
            console.error("Error adding movie:", error);
            jQuery("#response-message").text("Error adding movie. Please try again.");
        }
    });
}

// Function to handle logout
function handleLogout() {
    jQuery.ajax({
        url: "../api/logout",
        method: "POST",
        success: function() {
            grecaptcha.reset();

            window.location.replace("_dashboard/login.html");
        },
        error: function() {
            alert("Logout failed. Please try again.");
        }
    });
}

// Bind event handlers
jQuery(document).ready(function () {
    jQuery("#add-movie-form").submit(handleAddMovie);
    jQuery("#logout_button").on("click", handleLogout);
});
