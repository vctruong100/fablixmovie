$(document).ready(function() {
    function loadCart() {
        $.ajax({
            url: "api/shoppingcart",
            method: "GET",
            dataType: "json",
            success: function(data) {
                let cartItems = data.items;
                let totalPrice = data.totalPrice;
                let cartTableBody = $('#cart-items-body');
                cartTableBody.empty();

                if (cartItems.length > 0) {
                    cartItems.forEach(item => {
                        let rowHTML = `
                            <tr>
                                <td>${item.title}</td>
                                <td><button class="decrease-quantity" data-id="${item.movieId}">-</button>
                                    ${item.quantity}
                                    <button class="increase-quantity" data-id="${item.movieId}">+</button></td>
                                <td>$${item.price.toFixed(2)}</td>
                                <td>$${item.total.toFixed(2)}</td>
                                <td><button class="remove-item" data-id="${item.movieId}">Remove</button></td>
                            </tr>`;
                        cartTableBody.append(rowHTML);
                    });
                }
                $('#total-price').text(`$${totalPrice.toFixed(2)}`);
            }
        });
    }

    function updateCart(movieId, action) {
        $.ajax({
            url: "api/shoppingcart",
            method: "POST",
            data: { movieId: movieId, action: action },
            success: loadCart
        });
    }

    $(document).on('click', '.increase-quantity', function() {
        updateCart($(this).data('id'), 'add');
    });

    $(document).on('click', '.decrease-quantity', function() {
        updateCart($(this).data('id'), 'decrease');
    });

    $(document).on('click', '.remove-item', function() {
        updateCart($(this).data('id'), 'remove');
    });

    $('#proceed-to-payment').click(function() {
        window.location.href = 'payment.html';
    });

    loadCart();
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
