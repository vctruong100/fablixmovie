$(document).ready(function() {
    function loadOrderDetails() {
        let salesData = JSON.parse(sessionStorage.getItem('saleDetails'));
        let totalPrice = sessionStorage.getItem('totalPrice');
        let tableBody = $("#cart-items-body");

        tableBody.empty();

        salesData.forEach(function(sale) {
            let rowHTML = `<tr>
                <td>${sale.movieTitle}</td>
                <td>${sale.quantity}</td>
                <td>$${sale.salePrice}</td>
                <td>$${sale.lineTotal}</td>
            </tr>`;
            tableBody.append(rowHTML);
        });

        $("#total-price").text(`$${totalPrice}`);
    }

    loadOrderDetails();
});

jQuery("#results-btn").on("click", function(event) {
    event.preventDefault();
    window.location.href = `movielist.html`;
});

jQuery("#search_form").submit((event) => {
    event.preventDefault();
    const title = jQuery('input[name="title"]').val();
    const year = jQuery('input[name="year"]').val();
    const director = jQuery('input[name="director"]').val();
    const star = jQuery('input[name="star"]').val();

    let searchQuery = `movielist.html?page=1&title=${title}&year=${year}&director=${director}&star=${star}`;
    window.location.href = searchQuery;
});

