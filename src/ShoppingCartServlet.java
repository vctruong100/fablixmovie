import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import query.MovieQuery;
import resproc.MovieResultProc;
import session.ShoppingCartSession;
import session.ShoppingCartSession.CartItem;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shoppingcart")
public class ShoppingCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ShoppingCartSession scSession = (ShoppingCartSession)
                request.getSession().getAttribute("shoppingCart");

        if (scSession == null) {
            scSession = new ShoppingCartSession();
            request.getSession().setAttribute("shoppingCart", scSession);
        }

        Map<String, CartItem> cart = scSession.getShoppingCart();

        BigDecimal totalPrice;
        JsonArray itemsArray = new JsonArray();

        if (!cart.isEmpty()) {
            try (Connection conn = dataSource.getConnection()) {
                for (var entry : cart.entrySet()) {
                    String movieId = entry.getKey();
                    CartItem cartItem = entry.getValue();

                    // Fetch movie title and other details for display purposes
                    MovieQuery movieQuery = new MovieQuery(movieId);

                    try (PreparedStatement statement = movieQuery.prepareStatement(conn);
                         ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            // Construct JSON object for each cart item
                            JsonObject movieObject = new JsonObject();
                            movieObject.addProperty("movieId", movieId);
                            movieObject.addProperty("title", rs.getString("title"));
                            movieObject.addProperty("price", cartItem.price.toPlainString());
                            movieObject.addProperty("quantity", cartItem.quantity);
                            movieObject.addProperty("total", cartItem.price.multiply(BigDecimal.valueOf(cartItem.quantity)).toPlainString());

                            itemsArray.add(movieObject);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
        }
        totalPrice = scSession.calculateTotalPrice();

        JsonObject responseObject = new JsonObject();
        responseObject.add("items", itemsArray);
        responseObject.addProperty("totalPrice", totalPrice.toPlainString());

        PrintWriter out = response.getWriter();
        out.write(responseObject.toString());
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ShoppingCartSession scSession = (ShoppingCartSession)
                request.getSession().getAttribute("shoppingCart");

        String action = request.getParameter("action");
        String movieId = request.getParameter("movieId");

        if (movieId == null || movieId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid movie ID.");
            return;
        }

        switch(action) {
            case "add":
                BigDecimal price = fetchPriceFromDatabase(movieId);
                scSession.addToCart(movieId, price);
                break;
            case "remove":
                scSession.removeFromCart(movieId);
            case "decrease":
                scSession.decreaseCartItem(movieId);
        }

        response.sendRedirect("shoppingcart");
    }

    private BigDecimal fetchPriceFromDatabase(String movieId) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT price FROM prices WHERE movieId = ?";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, movieId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBigDecimal("price");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return new BigDecimal("0.00");
    }
}
