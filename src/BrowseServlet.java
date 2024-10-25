import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import session.QuerySession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@WebServlet(name = "BrowseServlet", urlPatterns = "/api/browse")
public class BrowseServlet extends HttpServlet {
    private static final long serialVersionUID = 201L;

    // Create a dataSource which is registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        /*
         * Initiate a new browse query using the defined user parameters
         * Note: This will replace the current stored session user parameters if they exist
         */

        QuerySession querySession = (QuerySession)request.getSession()
                .getAttribute("query");

        String alpha = request.getParameter("alpha");
        String genreId = request.getParameter("genre");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");
        String sortCatString = request.getParameter("sortBy");

        querySession.setBrowseParameters(alpha, genreId);
        querySession.getSetLimit(limitString);
        querySession.getSetPage(pageString);
        querySession.getSetSortCategory(sortCatString);

        request.getServletContext().log("browse " + "(alpha=" + alpha +
                ", genre=" + genreId + ", limit=" + limitString +
                ", page=" + pageString + ")");

        /*
         * Forward the request and response to MovieListServlet
         * which is able to handle searching with the saved session parameters
         */
        try {
            request.getRequestDispatcher("/api/movie-list")
                    .forward(request, response);
        } catch(Exception e) {
            PrintWriter out = response.getWriter();

            // Write user error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Capture and log the stack trace to help identify where the error occurred
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Log error to localhost log with full details
            request.getServletContext().log("Error: " + e.getMessage());
            request.getServletContext().log(stackTrace);  // Log stack trace in server logs
            System.out.println(stackTrace);

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
            out.close();
        }
    }
}
