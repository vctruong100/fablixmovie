$(document).ready(function() {
    function loadCart() {
        $.ajax({
            url: "api/shoppingcart",
            method: "GET",
            dataType: "json",
            success: function(data) {
                console.log("Cart data:", data);
                let cartItems = data.items;
                let totalPrice = data.totalPrice;
                let cartTableBody = $('#cart-items-body');
                cartTableBody.empty();

                if (cartItems.length > 0) {
                    cartItems.forEach(item => {
                        let rowHTML = `
                            <tr>
                                <td>${item.title || "No Title Available"}</td>
                                <td>
                                    <button class="decrease-quantity" data-id="${item.movieId}">-</button>
                                    ${item.quantity}
                                    <button class="increase-quantity" data-id="${item.movieId}">+</button>
                                </td>
                                <td>$${item.price}</td>
                                <td>$${item.total}</td>
                                <td><button class="remove-item" data-id="${item.movieId}">Remove</button></td>
                            </tr>`;
                        cartTableBody.append(rowHTML);
                    });
                } else {
                    cartTableBody.append("<tr><td colspan='5'>Your cart is empty.</td></tr>");
                }
                $('#total-price').text(`$${totalPrice}`);
            }
        });
    }

    function updateCart(movieId, action) {
        $.ajax({
            url: "api/shoppingcart",
            method: "POST",
            data: { movieId: movieId, action: action },
            success: loadCart,
            error: function() {
                alert("Failed to update cart. Please try again.");
            }
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
    window.location.href = `movielist.html`;
});
