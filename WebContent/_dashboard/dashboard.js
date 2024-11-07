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
        let metadataHtml = "";

        metadata.forEach(table => {
            metadataHtml += `
            <div class="metadata-section">
                <h3>${table.table_name}</h3>
                <table class="dashboard-table">
                    <thead>
                        <tr>
                            <th>Column Name</th>
                            <th>Type</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

            table.columns.forEach(column => {
                metadataHtml += `
                <tr>
                    <td>${column.column_name}</td>
                    <td>${column.type}</td>
                </tr>
            `;
            });

            metadataHtml += `
                    </tbody>
                </table>
            </div>
        `;
        });

        $("#metadata-container").html(metadataHtml);
    }

});
