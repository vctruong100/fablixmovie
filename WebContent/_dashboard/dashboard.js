// dashboard.js
$(document).ready(function () {
    // Load metadata on page load
    loadMetadata();

    function loadMetadata() {
        $.ajax({
            url: "../api/dashboard",
            method: "GET",
            data: { action: "metadata" },
            dataType: "json",
            success: function (data) {
                displayMetadata(data);
            },
            error: function (xhr, status, error) {
                console.error("Error loading metadata:", error);
                $("#metadata-container").html("<p>Error loading metadata. Please try again later.</p>");
            }
        });
    }

    function displayMetadata(metadata) {
        let metadataHtml = "<ul>";

        metadata.forEach(table => {
            metadataHtml += `<li><strong>${table.table_name}</strong><ul>`;
            table.columns.forEach(column => {
                metadataHtml += `<li>${column.column_name}: ${column.type}</li>`;
            });
            metadataHtml += "</ul></li>";
        });

        metadataHtml += "</ul>";
        $("#metadata-container").html(metadataHtml);
    }
});
