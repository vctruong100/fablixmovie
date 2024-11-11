// Load metadata on page load
function loadMetadata() {
    jQuery.ajax({
        url: "../api/dashboard",
        method: "GET",
        data: { action: "metadata" },
        dataType: "json",
        success: function (data) {
            displayMetadata(data);
        },
        error: function (xhr, status, error) {
            console.error("Error loading metadata:", error);
            jQuery("#metadata-container").html("<p>Error loading metadata. Please try again later.</p>");
        }
    });
}

// Display the metadata in a structured format
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

    jQuery("#metadata-container").html(metadataHtml);
}

// Handle logout button click
function handleLogout() {
    jQuery.ajax({
        url: "../api/logout",
        method: "POST",
        success: function() {
            grecaptcha.reset();

            // Redirect to the employee login page after logging out
            window.location.replace("_dashboard/login.html");
        },
        error: function() {
            alert("Logout failed. Please try again.");
        }
    });
}

jQuery(document).ready(function () {
    loadMetadata();
    jQuery("#logout_button").on("click", handleLogout);
});
