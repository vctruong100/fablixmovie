import com.google.gson.JsonObject;
import org.jasypt.util.password.StrongPasswordEncryptor;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import session.QuerySession;
import session.ShoppingCartSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 10L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        try {
            // Verify reCAPTCHA
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "reCAPTCHA verification failed");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String userType = request.getParameter("userType");

        JsonObject responseJsonObject = new JsonObject();
        try (Connection conn = dataSource.getConnection()) {
            boolean loginSuccess = false;

            // Check if the user is a customer or an employee
            if ("customer".equals(userType)) {
                loginSuccess = attemptCustomerLogin(conn, username, password, request, responseJsonObject);
                responseJsonObject.addProperty("redirect", "mainpage.html");
                responseJsonObject.addProperty("role", "customer");
            } else if ("employee".equals(userType)) {
                loginSuccess = attemptEmployeeLogin(conn, username, password, request, responseJsonObject);
                responseJsonObject.addProperty("redirect", "dashboard.html");
                responseJsonObject.addProperty("role", "employee");
            }

            if (!loginSuccess) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Incorrect username or password");
            } else {
                responseJsonObject.addProperty("status", "success");
            }
            response.getWriter().write(responseJsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error; please try again later");
            response.getWriter().write(responseJsonObject.toString());
        }
    }

    private boolean attemptCustomerLogin(Connection conn, String username, String password, HttpServletRequest request, JsonObject responseJsonObject) throws Exception {
        String customerQuery = "SELECT password FROM customers WHERE email = ?";
        try (PreparedStatement statement = conn.prepareStatement(customerQuery)) {
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                String encryptedPassword = rs.getString("password");

                System.out.println("Input Password: " + password);
                System.out.println("Encrypted Password from DB: " + encryptedPassword);
                System.out.println("Password Match: " + passwordEncryptor.checkPassword(password, encryptedPassword));

                if (passwordEncryptor.checkPassword(password, encryptedPassword)) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("username", username);
                    session.setAttribute("role", "customer");

                    session.setAttribute("query", new QuerySession());
                    session.setAttribute("shoppingCart", new ShoppingCartSession());

                    responseJsonObject.addProperty("status", "success");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attemptEmployeeLogin(Connection conn, String username, String password, HttpServletRequest request, JsonObject responseJsonObject) throws Exception {
        String employeeQuery = "SELECT password, fullname FROM employees WHERE email = ?";
        try (PreparedStatement statement = conn.prepareStatement(employeeQuery)) {
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                String encryptedPassword = rs.getString("password");

                System.out.println("Input Password: " + password);
                System.out.println("Encrypted Password from DB: " + encryptedPassword);
                System.out.println("Password Match: " + passwordEncryptor.checkPassword(password, encryptedPassword));

                if (passwordEncryptor.checkPassword(password, encryptedPassword)) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("employeeEmail", username);
                    session.setAttribute("employeeName", rs.getString("fullname"));
                    session.setAttribute("role", "employee");

                    responseJsonObject.addProperty("status", "success");
                    return true;
                }
            }
        }
        return false;
    }
}
