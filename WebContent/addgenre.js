$(document).ready(function () {
    $("#add-genre-form").submit(function (event) {
        event.preventDefault();

        $.ajax({
            url: "/fab-project/_dashboard",
            method: "POST",
            data: $(this).serialize() + "&action=addGenre",
            dataType: "json",
            success: function (response) {
                $("#response-message").text(response.message);
            },
            error: function (xhr, status, error) {
                console.error("Error adding genre:", error);
                $("#response-message").text("Error adding genre. Please try again.");
            }
        });
    });
});
