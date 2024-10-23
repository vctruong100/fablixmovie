import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;

public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Unable to lookup DataSource", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        SessionUser user = (SessionUser) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login.html");
            return;
        }

        double totalPrice = user.calculateTotalPrice();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("totalPrice", totalPrice);

        PrintWriter out = response.getWriter();
        out.write(responseObject.toString());
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        SessionUser user = (SessionUser) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login.html");
            return;
        }

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditCardNumber = request.getParameter("creditCardNumber");
        String expirationDate = request.getParameter("expirationDate");

        boolean isPaymentValid = validatePayment(firstName, lastName, creditCardNumber, expirationDate);

        JsonObject responseObject = new JsonObject();

        System.out.println("Payment Details:");
        System.out.println("First Name: " + firstName);
        System.out.println("Last Name: " + lastName);
        System.out.println("Credit Card Number: " + creditCardNumber);
        System.out.println("Expiration Date: " + expirationDate);

        if (isPaymentValid) {
            responseObject.addProperty("status", "success");
            responseObject.addProperty("message", "Payment processed successfully");
            // Redirect to PlaceOrderServlet could be handled on the front-end after receiving the response.
        } else {
            responseObject.addProperty("status", "error");
            responseObject.addProperty("message", "Invalid payment details");
        }


        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(responseObject.toString());
        out.close();
    }

    private boolean validatePayment(String firstName, String lastName, String creditCardNumber, String expirationDate) {
        boolean valid = false;

        // Connect to the database to validate the payment information
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT * FROM creditcards WHERE firstName = ? AND lastName = ? AND id = ? AND expiration = ?";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, creditCardNumber);
                statement.setString(4, expirationDate);

                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    valid = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return valid;
    }

}