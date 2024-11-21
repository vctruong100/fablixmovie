import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "MainPageServlet", urlPatterns = "/api/main-page")
public class MainPageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataSource replicaDataSource;

    public void init(ServletConfig config) {
        try {
            replicaDataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/replica");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        request.getServletContext().log("getting movie genres");

        PrintWriter out = response.getWriter();

        try (Connection conn = replicaDataSource.getConnection()) {
            JsonArray genresJsonArray = new JsonArray();

            String genresQuery = "SELECT * FROM genres ORDER BY name ASC";
            PreparedStatement genresStatement = conn.prepareStatement(genresQuery);

            ResultSet genresRs = genresStatement.executeQuery();

            while (genresRs.next()) {
                JsonObject genreJson = new JsonObject();
                genreJson.addProperty("genre_id", genresRs.getString("id"));
                genreJson.addProperty("genre_name", genresRs.getString("name"));
                genresJsonArray.add(genreJson);
            }
            genresRs.close();
            genresStatement.close();

            out.write(genresJsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }

    }
}
