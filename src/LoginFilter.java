import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();
    //private final ArrayList<String> customerURIs = new ArrayList<>();
    //private final ArrayList<String> employeeURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String rootContext = request.getServletContext().getContextPath();

        System.out.print("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(rootContext, httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            System.out.println(" (allowed)");
            chain.doFilter(request, response);
            return;
        }

        // Redirect all unauthenticated users to login page
        if (httpRequest.getSession().getAttribute("username") == null
                && httpRequest.getSession().getAttribute("employeeEmail") == null) {
            // If entry point is "_dashboard",
            // redirect to dashboard login instead
            if (httpRequest.getRequestURI().startsWith(
                    rootContext + "/_dashboard")) {
                System.out.println(" (blocked)");
                httpResponse.sendRedirect(rootContext + "/_dashboard/login.html");
            } else {
                System.out.println(" (blocked)");
                httpResponse.sendRedirect(rootContext + "/login.html");
            }
            return;
        }

        // Only allow employees access to _dashboard pages
        if (httpRequest.getRequestURI().startsWith(
                rootContext + "/_dashboard")) {
            if (httpRequest.getSession().getAttribute(
                    "employeeEmail") == null) {
                System.out.println(" (blocked) (customer)");
                httpResponse.sendRedirect(rootContext + "/_dashboard/login.html");
            } else {
                System.out.println(" (allowed) (employee)");
                chain.doFilter(request, response);
            }
            return;
        }

        // Allow customers and employees access to any pages
        // not disallowed here for simplicity
        System.out.println(" (allowed) (any)");
        chain.doFilter(request, response);
    }

    private boolean isUrlAllowedWithoutLogin(
            String rootContext, String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..
         */
        return allowedURIs.stream().map(uri -> rootContext + "/" + uri)
                .anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("_dashboard/login.css");
        allowedURIs.add("_dashboard/login.html");
        allowedURIs.add("_dashboard/login.js");
        allowedURIs.add("login.css");
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");
        allowedURIs.add("shared.css");
    }

    public void destroy() {
        // ignored.
    }

}
