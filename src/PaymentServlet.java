import com.google.gson.JsonArray;
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

    private static class CustomerInfo {
        int customerId;
        String firstName;
        String lastName;
        String ccId;

        public CustomerInfo(int customerId, String firstName, String lastName, String ccId) {
            this.customerId = customerId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.ccId = ccId;
        }
    }

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            if (System.getenv("FAB-DEV") == null) {
                System.out.println("PaymentServlet: using moviedb-write in prod mode");
                dataSource = (DataSource) new InitialContext()
                        .lookup("java:comp/env/jdbc/moviedb-write");
            } else {
                System.out.println("PaymentServlet: using moviedb-write in dev mode");
            }
        } catch (NamingException e) {
            e.printStackTrace();
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

        CustomerInfo cInfo = getCustomerInfo(
                request.getSession().getAttribute("username"));
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditCardNumber = request.getParameter("creditCardNumber");
        String expirationDate = request.getParameter("expirationDate");

        boolean isPaymentValid = validatePayment(
                cInfo, firstName, lastName, creditCardNumber, expirationDate);

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

                Map<String, ShoppingCartSession.CartItem> shoppingCart =
                        scSession.getShoppingCart();

                // Get details to add to the initial sale record
                // for the sales table
                int customerId = cInfo.customerId;
                String firstMovieId = shoppingCart.entrySet().iterator().next().getKey();

                // Insert a single sale record into the sales table
                // at checkout
                PreparedStatement saleStmt = conn.prepareStatement(
                        saleInsertQuery, Statement.RETURN_GENERATED_KEYS);

                saleStmt.setInt(1, customerId);
                saleStmt.setString(2, firstMovieId);
                saleStmt.executeUpdate();

                // Get generated sale ID
                int saleId;
                ResultSet rs = saleStmt.getGeneratedKeys();
                if (rs.next()) {
                    saleId = rs.getInt(1);
                } else {
                    throw new RuntimeException("no sale ID generated");
                }
                System.out.println("Sale ID: " + saleId);
                System.out.println("Customer ID: " + customerId);

                for (var entry : shoppingCart.entrySet()) {
                    String movieId = entry.getKey();
                    int quantity = entry.getValue().quantity;
                    BigDecimal price = entry.getValue().price;
                    String saleRecordInsertQuery = "INSERT INTO sales_records (saleId, movieId, salePrice, quantity) "
                            + "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement saleRecordStmt = conn.prepareStatement(saleRecordInsertQuery)) {
                        saleRecordStmt.setInt(1, saleId);
                        saleRecordStmt.setString(2, movieId);
                        saleRecordStmt.setBigDecimal(3, price);
                        saleRecordStmt.setInt(4, quantity);
                        saleRecordStmt.executeUpdate();
                    } catch(Exception e) {
                        throw new RuntimeException("bad sales record insertion");
                    }
                }
                conn.commit();

                // Retrieve sale details for confirmation page
                String saleDetailsQuery = "SELECT m.title AS movieTitle, sr.quantity, sr.salePrice " +
                        "FROM sales_records sr " +
                        "JOIN movies m ON sr.movieId = m.id " +
                        "WHERE sr.saleId = ?";
                PreparedStatement saleDetailsStmt = conn.prepareStatement(saleDetailsQuery);
                saleDetailsStmt.setInt(1, saleId);
                ResultSet detailsRs = saleDetailsStmt.executeQuery();

                JsonArray salesArray = new JsonArray();
                BigDecimal totalPrice = BigDecimal.ZERO;

                while (detailsRs.next()) {
                    JsonObject saleItem = new JsonObject();
                    saleItem.addProperty("movieTitle", detailsRs.getString("movieTitle"));
                    saleItem.addProperty("quantity", detailsRs.getInt("quantity"));
                    BigDecimal salePrice = detailsRs.getBigDecimal("salePrice");
                    saleItem.addProperty("salePrice", salePrice.toPlainString());

                    BigDecimal lineTotal = salePrice.multiply(BigDecimal.valueOf(detailsRs.getInt("quantity")));
                    saleItem.addProperty("lineTotal", lineTotal.toPlainString());

                    salesArray.add(saleItem);
                    totalPrice = totalPrice.add(lineTotal);
                }

                responseObject.addProperty("status", "success");
                responseObject.addProperty("message", "Payment processed successfully.");
                responseObject.add("sales", salesArray);
                responseObject.addProperty("saleId", saleId);
                responseObject.addProperty("totalPrice", totalPrice.toPlainString());

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

    private boolean validatePayment(
            CustomerInfo cInfo,
            String firstName,
            String lastName,
            String creditCardNumber,
            String expirationDate) {
        boolean valid = false;

        // Does not match first name / last name on customer info
        if (!firstName.equals(cInfo.firstName) ||
            !lastName.equals(cInfo.lastName)) {
            return false;
        }

        // Connect to the database to validate the payment information
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT * FROM creditcards "
                    + "WHERE firstName = ? AND lastName = ? AND id = ? AND expiration = ?";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, creditCardNumber);
                statement.setString(4, expirationDate);

                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    // Only valid if customer info ccId matches
                    // credit card supplied by the user
                    valid = (rs.getString("id").equals(cInfo.ccId));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return valid;
    }

    private CustomerInfo getCustomerInfo(Object username) {
        String customerQuery = "SELECT id, firstName, lastName, ccId FROM customers WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(customerQuery)) {
            statement.setString(1, (String) username);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new CustomerInfo(
                        rs.getInt("id"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("ccId")
                );
            }
        } catch(Exception e) {
            return new CustomerInfo(-1, "", "", "");
        }
        return new CustomerInfo(-1, "", "", "");
    }

}


