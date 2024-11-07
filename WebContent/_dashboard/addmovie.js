$(document).ready(function () {
    $("#add-movie-form").submit(function (event) {
        event.preventDefault();

        $.ajax({
            url: "../api/dashboard",
            method: "POST",
            data: $(this).serialize() + "&action=addMovie",
            dataType: "json",
            success: function (response) {
                $("#response-message").text(response.message);
            },
            error: function (xhr, status, error) {
                console.error("Error adding movie:", error);
                $("#response-message").text("Error adding movie. Please try again.");
            }
        });
    });
});
