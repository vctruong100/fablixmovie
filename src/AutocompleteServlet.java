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
import java.util.List;
import java.util.ArrayList;

import query.SearchSubQuery;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();
        JsonArray suggestions = new JsonArray();

        List<String> subQueries = new ArrayList<>();
        List<String> subParams = new ArrayList<>();

        String query = request.getParameter("query");
        if (query != null) {
            query = query.trim().replaceAll("\\s+", " ");
        }

        // Only proceed if the query length is 3 or more characters
        if (query == null || query.length() < 3) {
            response.getWriter().write("[]");
            return;
        }

        // Prepare the full-text search query
        SearchSubQuery.extendFulltext("movies_t", "movies", "title",
                query, subQueries, subParams);

        // Execute the full-text search query
        try (Connection conn = dataSource.getConnection()) {
            String sql = "WITH " + subQueries.get(0)
                    + " SELECT id, title FROM movies_t LIMIT 10";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                for (int i = 0; i < subParams.size(); i++) {
                    statement.setString(i + 1, subParams.get(i));
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        JsonObject suggestion = new JsonObject();
                        suggestion.addProperty("title", rs.getString("title"));
                        suggestion.addProperty("movie_id", rs.getString("id"));
                        suggestions.add(suggestion);
                    }
                }
            }
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            response.setStatus(500);
            out.write(error.toString());
            return;
        }

        out.write(suggestions.toString());
        out.close();
    }
}
