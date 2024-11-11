// Function to handle adding a genre
function handleAddGenre(event) {
    event.preventDefault();

    jQuery.ajax({
        url: "../api/dashboard",
        method: "POST",
        data: jQuery("#add-genre-form").serialize() + "&action=addGenre",
        dataType: "json",
        success: function (response) {
            jQuery("#response-message").text(response.message);
        },
        error: function (xhr, status, error) {
            console.error("Error adding genre:", error);
            jQuery("#response-message").text("Error adding genre. Please try again.");
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
    jQuery("#add-genre-form").submit(handleAddGenre);
    jQuery("#logout_button").on("click", handleLogout);
});
