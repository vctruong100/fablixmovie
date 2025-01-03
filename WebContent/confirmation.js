$(document).ready(function() {
    function loadOrderDetails() {
        let saleId = sessionStorage.getItem('saleId');
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
        $("#sale-id").text(`#${saleId}`)
        $("#total-price").text(`$${totalPrice}`);
    }

    loadOrderDetails();
});

jQuery("#results-btn").on("click", function(event) {
    event.preventDefault();
    window.location.href = `movielist.html`;
});
