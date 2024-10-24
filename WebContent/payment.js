$(document).ready(function() {
    function loadTotalPrice() {
        $.ajax({
            url: "api/payment",
            method: "GET",
            dataType: "json",
            success: function(data) {
                $('#total-price').text(`$${data.totalPrice.toFixed(2)}`);
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
            data: paymentData,
            dataType: "json",
            success: function(response) {
                if (response.status === "success") {
                    window.location.href = "order-confirmation.html";
                } else {
                    $('#error-message').text(response.message);
                }
            }
        });
    });

    loadTotalPrice();
});

jQuery("#results-btn").on("click", function(event) {
    event.preventDefault();

    const movieListState = sessionStorage.getItem("movieListState");
    let queryParams = "";
    if (movieListState) {
        const state = JSON.parse(movieListState);
        queryParams = `&limit=${state.currentLimit}&page=${state.currentPage}&sortBy=${state.currentSortBy}`;

        if (state.currentAlpha) {
            queryParams += `&alpha=${state.currentAlpha}`;
        }
        if (state.currentGenre) {
            queryParams += `&genre=${state.currentGenre}`;
        }
    }

    window.location.href = `movielist.html?${queryParams}`;
});
