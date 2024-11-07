let login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleLoginResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If login succeeds, it will redirect the user to index.html
    if (resultDataJson["status"] === "success") {
        if (resultDataJson["role"] === "employee") {
            window.location.replace("_dashboard/dashboard.html");
        } else {
            window.location.replace("mainpage.html");
        }
    } else {
        // If login fails, the web page will display
        // error messages on <div> with id "login_error_message"
        console.log("show error message");
        console.log(resultDataJson["message"]);
        $("#login_error_message").text(resultDataJson["message"]);

        // grecaptcha.reset();

    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitLoginForm(formSubmitEvent) {
    console.log("submit login form");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();

    // let recaptchaResponse = grecaptcha.getResponse();

    // let recaptchaResponse = "dummy-response"; // Temporary response for testing
    // console.log("reCAPTCHA response bypassed for testing");
    //
    // if (!recaptchaResponse) {
    //     $("#login_error_message").text("Please complete the reCAPTCHA");
    //     return;
    // }

    $.ajax(
        "api/login", {
            method: "POST",
            // Serialize the login form to the data sent by POST request
            data: login_form.serialize(),
            success: handleLoginResult
        }
    );
}
// data: login_form.serialize() + "&g-recaptcha-response=" + recaptchaResponse,

// Bind the submit action of the form to a handler function
login_form.submit(submitLoginForm);
