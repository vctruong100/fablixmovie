$(document).ready(function () {
    $("#add-star-form").submit(function (event) {
        event.preventDefault();

        $.ajax({
            url: "/fab-project/_dashboard",
            method: "POST",
            data: $(this).serialize() + "&action=addStar",
            dataType: "json",
            success: function (response) {
                $("#response-message").text(response.message);
            },
            error: function (xhr, status, error) {
                console.error("Error adding star:", error);
                $("#response-message").text("Error adding star. Please try again.");
            }
        });
    });
});
