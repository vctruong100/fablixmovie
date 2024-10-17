import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        /* This example only allows username/password to be test/test
        /  in the real project, you should talk to the database to verify username/password
        */
        JsonObject responseJsonObject = new JsonObject();
        try (Connection conn = dataSource.getConnection()) {
            String loginQuery = "SELECT c.password from customers c where email = ?";

            PreparedStatement loginStatement = conn.prepareStatement(loginQuery);
            loginStatement.setString(1, username);

            ResultSet loginRs = loginStatement.executeQuery();
            if (loginRs.next()) {
                // Check password
                if (loginRs.getString("password").equals(password)) {
                    // Login success:
                    // Set this user into the session
                    request.getSession().setAttribute("user", new User(username));
                    responseJsonObject.addProperty("status", "success");
                } else {
                    // Login fail: incorrect password
                    responseJsonObject.addProperty("status", "fail");
                    // Log to localhost log
                    request.getServletContext().log("Login failed (incorrect username)");

                    // give user an error message
                    responseJsonObject.addProperty("message", "Incorrect password");
                }
            } else {
                // Login fail: incorrect username
                responseJsonObject.addProperty("status", "fail");
                // Log to localhost log
                request.getServletContext().log("Login failed (incorrect username)");

                // give user an error message
                responseJsonObject.addProperty("message", "Incorrect username");
            }
        } catch(Exception e) {
            // server error
            // Log error to localhost log
            request.getServletContext().log("Error:", e);

            // let the user know that a server error has occurred and to try again later
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error; please try again later");
        }
        response.getWriter().write(responseJsonObject.toString());
    }
}
