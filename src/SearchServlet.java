import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.StringWriter;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "SearchServlet", urlPatterns = "/api/search")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 200L;

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

        SessionUser sessionUser = (SessionUser)request.getSession().getAttribute("user");

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");
        String sortCatString = request.getParameter("sortBy");

        sessionUser.setSearchParameters(title, year, director, star);
        sessionUser.getSetLimit(limitString);
        sessionUser.getSetPage(pageString);
        sessionUser.getSetSortCategory(sortCatString);

        request.getServletContext().log("search " + "(title=" + title +
                ", year=" + year + ", director=" + director + ", star=" + star +
                ", limit=" + limitString + ", page=" + pageString + ")");

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
