import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import session.ShoppingCartSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
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
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ShoppingCartSession scSession = (ShoppingCartSession)
                request.getSession().getAttribute("shoppingCart");

        BigDecimal totalPrice = scSession.calculateTotalPrice();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("totalPrice", totalPrice.toPlainString());

        PrintWriter out = response.getWriter();
        out.write(responseObject.toString());
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ShoppingCartSession scSession = (ShoppingCartSession)
                request.getSession().getAttribute("shoppingCart");

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

        System.out.println("Payment processing...");
        if (isPaymentValid) {
            String saleInsertQuery = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, NOW())";
            System.out.println("Payment is valid");
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                PreparedStatement saleStmt = conn.prepareStatement(saleInsertQuery, Statement.RETURN_GENERATED_KEYS);

                int customerId = getCustomerId(request.getSession().getAttribute("username"));

                System.out.println("Customer ID: " + customerId);
                for (Map.Entry<String, ShoppingCartSession.CartItem> entry : scSession.getShoppingCart().entrySet()) {
                    String movieId = entry.getKey();
                    int quantity = entry.getValue().quantity;
                    BigDecimal price = entry.getValue().price;

                    saleStmt.setInt(1, customerId);
                    saleStmt.setString(2, movieId);
                    saleStmt.executeUpdate();

                    ResultSet rs = saleStmt.getGeneratedKeys();
                    if (rs.next()) {
                        int saleId = rs.getInt(1);
                        System.out.println("Sale ID: " + saleId);
                        String saleRecordInsertQuery = "INSERT INTO sales_records (saleId, movieId, salePrice, quantity) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement saleRecordStmt = conn.prepareStatement(saleRecordInsertQuery)) {
                            saleRecordStmt.setInt(1, saleId);
                            saleRecordStmt.setString(2, movieId);
                            saleRecordStmt.setBigDecimal(3, price);
                            saleRecordStmt.setInt(4, quantity);
                            saleRecordStmt.executeUpdate();
                        }
                    }
                }
                conn.commit();
                responseObject.addProperty("status", "success");
                responseObject.addProperty("message", "Payment processed successfully.");
                scSession.clearCart();
            } catch (SQLException e) {
                e.printStackTrace();
                responseObject.addProperty("status", "error");
                responseObject.addProperty("message", "An error occurred during the transaction. Please try again.");
            }
        } else {
            responseObject.addProperty("status", "error");
            responseObject.addProperty("message", "Invalid payment details");
        }

        System.out.println("Payment processed");
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
    private int getCustomerId(Object username) throws SQLException {
        int customerId = -1;
        String customerQuery = "SELECT id FROM customers WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(customerQuery)) {
            statement.setString(1, (String) username);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                customerId = rs.getInt("id");
            }
        }
        return customerId;
    }

}


