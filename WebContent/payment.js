$(document).ready(function() {
    function loadTotalPrice() {
        $.ajax({
            url: "api/payment",
            method: "GET",
            dataType: "json",
            success: function(data) {
                $('#total-price').text(`$${data.totalPrice}`);
            },
            error: function() {
                $('#total-price').text("Error loading total price.");
            }
        });
    }

    $('#payment-form').submit(function(event) {
        event.preventDefault();

        let paymentData = {
            firstName: $('#first-name').val(),
            lastName: $('#last-name').val(),
            creditCardNumber: $('#credit-card-number').val(),
            expirationDate: $('#expiration-date').val()
        };

        $.ajax({
            url: "api/payment",
            method: "POST",
            data: $.param(paymentData),
            dataType: "json",
            success: function(response) {
                console.log("Success: " + response);
                if (response.status === "success") {
                    window.location.href = "confirmation.html";
                } else {
                    $('#error-message').text(response.message);
                }
            },
            error: function() {
                $('#error-message').text("An unexpected error occurred. Please try again.");
            }
        });
    });

    loadTotalPrice();
});

jQuery("#results-btn").on("click", function(event) {
    event.preventDefault();
    window.location.href = `movielist.html`;
});
