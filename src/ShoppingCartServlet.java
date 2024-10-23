import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import query.MovieQuery;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShoppingCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
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

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<Integer, SessionUser.CartItem> cart = user.getShoppingCart();

        double totalPrice = 0.0;
        JsonArray itemsArray = new JsonArray();

        if (!cart.isEmpty()) {
            try (Connection conn = dataSource.getConnection()) {
                for (Map.Entry<Integer, SessionUser.CartItem> entry : cart.entrySet()) {
                    int movieId = entry.getKey();
                    SessionUser.CartItem cartItem = entry.getValue();

                    MovieQuery movieQuery = new MovieQuery(String.valueOf(movieId));
                    try (PreparedStatement statement = movieQuery.prepareStatement(conn);
                         ResultSet rs = statement.executeQuery()) {

                        if (rs.next()) {
                            String movieTitle = rs.getString("m.title");

                            double pricePerItem = cartItem.price;
                            double itemTotalPrice = pricePerItem * cartItem.quantity;
                            totalPrice += itemTotalPrice;

                            JsonObject itemObject = new JsonObject();
                            itemObject.addProperty("title", movieTitle);
                            itemObject.addProperty("quantity", cartItem.quantity);
                            itemObject.addProperty("price", pricePerItem);
                            itemObject.addProperty("total", itemTotalPrice);

                            itemsArray.add(itemObject);

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
        }
        totalPrice = user.calculateTotalPrice();

        JsonObject responseObject = new JsonObject();
        responseObject.add("items", itemsArray);
        responseObject.addProperty("totalPrice", totalPrice);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

        Map<Integer, SessionUser.CartItem> cart = user.getShoppingCart();

        String action = request.getParameter("action");
        int movieId = Integer.parseInt(request.getParameter("movieId"));

        if (movieId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid movie ID.");
            return;
        }

        if ("add".equals(action)) {
            double price = generateRandomPrice();
            user.addToCart(movieId, price);
        } else if ("remove".equals(action)) {
            user.removeFromCart(movieId);
        } else if ("decrease".equals(action)) {
            user.decreaseCartItem(movieId);
        }

        session.setAttribute("user", user);

        response.sendRedirect("shoppingcart");
    }

    private double generateRandomPrice() {
        Random random = new Random();
        return 5.0 + (15.0 * random.nextDouble()); // Generates price between $5 and $20
    }
}
