// Function to handle adding a star
function handleAddStar(event) {
    event.preventDefault();

    jQuery.ajax({
        url: "../api/dashboard",
        method: "POST",
        data: jQuery("#add-star-form").serialize() + "&action=addStar",
        dataType: "json",
        success: function (response) {
            jQuery("#response-message").text(response.message);
        },
        error: function (xhr, status, error) {
            console.error("Error adding star:", error);
            jQuery("#response-message").text("Error adding star. Please try again.");
        }
    });
}

// Function to handle logout
function handleLogout() {
    jQuery.ajax({
        url: "../api/logout",
        method: "POST",
        success: function() {
            window.location.replace("_dashboard/login.html");
        },
        error: function() {
            alert("Logout failed. Please try again.");
        }
    });
}

jQuery(document).ready(function () {
    jQuery("#add-star-form").submit(handleAddStar);
    jQuery("#logout_button").on("click", handleLogout);
});
