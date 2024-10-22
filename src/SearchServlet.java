import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.StringWriter;

import com.mysql.cj.exceptions.NumberOutOfRange;
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

import query.MovieListQuery;
import resproc.MovieListResultProc;

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

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String limitString = request.getParameter("limit");
        String pageString = request.getParameter("page");

        request.getServletContext().log("search " + "(title=" + title +
                ", year=" + year + ", director=" + director + ", star=" + star +
                ", limit=" + limitString + ", page=" + pageString + ")");

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            JsonArray resultArray = new JsonArray();

            int limit = Integer.parseInt(limitString);
            if (limit < 1 || limit > 100) {
                throw new NumberOutOfRange("limit must be between 1 and 100");
            }
            int page = Integer.parseInt(pageString);
            if (page < 1) {
                throw new NumberOutOfRange("page must be greater than 0");
            }
            int offset = limit * (page - 1);

            MovieListQuery mlQuery = new MovieListQuery(conn);
            MovieListResultProc mlrp = new MovieListResultProc(resultArray);

            mlQuery.setTitle(title);
            mlQuery.setYear(year);
            mlQuery.setDirector(director);
            mlQuery.setStar(star);
            mlQuery.setLimit(limit);
            mlQuery.setOffset(offset);

            PreparedStatement mlStatement = mlQuery.prepareStatement();
            mlrp.processResultSet(mlStatement.executeQuery());
            mlStatement.close();

            // Write JSON string to output
            out.write(resultArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
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

    } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}
