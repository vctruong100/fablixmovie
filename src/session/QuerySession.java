package session;

import com.google.gson.JsonObject;

public class QuerySession {
    public enum QueryMode {
        NONE, SEARCH, BROWSE
    }

    public enum SortCategory {
        RATING_ASC_TITLE_ASC,
        RATING_ASC_TITLE_DESC,
        RATING_DESC_TITLE_ASC,
        RATING_DESC_TITLE_DESC,
        TITLE_ASC_RATING_ASC,
        TITLE_ASC_RATING_DESC,
        TITLE_DESC_RATING_ASC,
        TITLE_DESC_RATING_DESC,
        USER,
    }

    public static class QueryParameterSet {
        public QueryMode queryMode;
        public SortCategory sortCategory;
        public int limit, page;
        public String[] params;
    }

    /* constants */
    private final static int DEFAULT_LIMIT = 10,
            DEFAULT_PAGE = 1,
            USER_LIMIT = -1,
            USER_PAGE = -1;
    private final static SortCategory DEFAULT_SORT =
            SortCategory.RATING_ASC_TITLE_ASC;

    /* local state */
    public QueryMode queryMode = QueryMode.NONE;
    public int limit = DEFAULT_LIMIT,
            page = DEFAULT_PAGE;
    public SortCategory sortCategory = DEFAULT_SORT;

    /* search */
    public String title,
            year,
            director,
            star;

    /* browse */
    public String alpha,
            genreId;

    public static int parseLimit(String limitString)
            throws IllegalArgumentException {
        if (limitString == null || limitString.isEmpty()) {
            return USER_LIMIT;
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
            return limit;
        }
    }

    public static int parsePage(String pageString)
            throws IllegalArgumentException {
        if (pageString == null || pageString.isEmpty()) {
            return DEFAULT_PAGE;
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
            return page;
        }
    }

    public static SortCategory parseSortCategory(String sortCatString)
            throws IllegalArgumentException {
        if (sortCatString == null || sortCatString.isEmpty()) {
            return SortCategory.USER;
        } else {
            SortCategory sortCat;
            try {
                sortCat = SortCategory.valueOf(
                        sortCatString.replace("-", "_").toUpperCase()
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid sort category");
            }
            return sortCat;
        }
    }

    public static JsonObject backFormParameters(QueryParameterSet qpSet) {
        JsonObject backFormParameters = new JsonObject();
        backFormParameters.addProperty("queryMode",
                qpSet.queryMode.name().toLowerCase());
        backFormParameters.addProperty("limit",
                Integer.toString(qpSet.limit));
        backFormParameters.addProperty("page",
                Integer.toString(qpSet.page));
        backFormParameters.addProperty("sortBy",
                qpSet.sortCategory.name()
                        .toLowerCase()
                        .replace("_", "-"));
        switch(qpSet.queryMode) {
            case SEARCH:
                backFormParameters.addProperty("title", qpSet.params[0]);
                backFormParameters.addProperty("year", qpSet.params[1]);
                backFormParameters.addProperty("director", qpSet.params[2]);
                backFormParameters.addProperty("star", qpSet.params[3]);
                break;
            case BROWSE:
                backFormParameters.addProperty("alpha", qpSet.params[0]);
                backFormParameters.addProperty("genre", qpSet.params[1]);
                break;
            default:
        }
        return backFormParameters;
    }

    public synchronized QueryParameterSet getParameters() {
        QueryParameterSet qpSet = new QueryParameterSet();
        qpSet.queryMode = queryMode;
        qpSet.limit = limit;
        qpSet.page = page;
        qpSet.sortCategory = sortCategory;
        switch (queryMode) {
            case SEARCH:
                qpSet.params = new String[]{title, year, director, star};
                return qpSet;
            case BROWSE:
                qpSet.params = new String[]{alpha, genreId};
                return qpSet;
            default:
                return null;
        }
    }

    public synchronized void setBrowseParameters(
            int limit, int page, SortCategory sortCat,
            String alpha, String genreId) {
        setLimitPageSort(limit, page, sortCat);
        this.queryMode = QueryMode.BROWSE;
        this.alpha = alpha;
        this.genreId = genreId;
    }

    public synchronized void setSearchParameters(
            int limit, int page, SortCategory sortCat,
            String title, String year, String director, String star) {
        setLimitPageSort(limit, page, sortCat);
        this.queryMode = QueryMode.SEARCH;
        this.title = title;
        this.year = year;
        this.director = director;
        this.star = star;
    }

    private void setLimitPageSort(
            int limit, int page, SortCategory sortCat) {
        if (limit != USER_LIMIT) {
            this.limit = limit;
        }
        if (page != USER_PAGE) {
            this.page = page;
        }
        if (sortCat != SortCategory.USER) {
            this.sortCategory = sortCat;
        }
    }
}
