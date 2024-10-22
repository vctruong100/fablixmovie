import java.util.Map;

public class SessionUser {
    public enum QueryMode {
        NONE, SEARCH, BROWSE
    }
    private static class UserQuery {
        public QueryMode mode = QueryMode.NONE;
        public int limit = 10;
        public int page = 1;

        /* search */
        public String title;
        public String director;
        public String year;
        public String star;

        /* browse */
        public String alpha;
        public String genreId;
    }

    private final String username;
    private final UserQuery userQuery;

    public SessionUser(String username) {
        this.username = username;
        this.userQuery = new UserQuery();
    }

    public String getUsername() {
        return username;
    }

    public QueryMode getQueryMode() {
        return userQuery.mode;
    }

    public String[] getBrowseParameters() {
        return new String[]{
                userQuery.alpha,
                userQuery.genreId,
        };
    }

    public String[] getSearchParameters() {
        return new String[]{
                userQuery.title,
                userQuery.director,
                userQuery.year,
                userQuery.star,
        };
    }

    public void setLimitParameter(int limit) {
        userQuery.limit = limit;
    }

    public void setPageParameter(int page) {
        userQuery.page = page;
    }

    public void setBrowseParameters(String alpha, String genreId) {
        userQuery.mode = QueryMode.BROWSE;
        userQuery.alpha = alpha;
        userQuery.genreId = genreId;
    }

    public void setSearchParameters(
            String title, String year, String director, String star) {
        userQuery.mode = QueryMode.SEARCH;
        userQuery.title = title;
        userQuery.year = year;
        userQuery.director = director;
        userQuery.star = star;
    }

    public int parseAndSetLimit(String limitString) throws IllegalArgumentException {
        if (limitString == null || limitString.isEmpty()) {
            return userQuery.limit;
        } else {
            int limit;
            try {
                limit = Integer.parseInt(limitString);
            } catch (Exception e) {
                throw new IllegalArgumentException("limit must be an integer");
            }
            if (limit < 1 || limit > 100) {
                throw new IllegalArgumentException("limit must be between 1 and 100");
            }
            userQuery.limit = limit;
            return limit;
        }
    }

    public int parseAndSetPage(String pageString) throws IllegalArgumentException {
        if (pageString == null || pageString.isEmpty()) {
            return userQuery.page;
        } else {
            int page;
            try {
                page = Integer.parseInt(pageString);
            } catch (Exception e) {
                throw new IllegalArgumentException("page must be an integer");
            }
            if (page < 1) {
                throw new IllegalArgumentException("page must be greater than 0");
            }
            userQuery.page = page;
            return page;
        }
    }
}
