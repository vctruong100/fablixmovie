import com.google.gson.JsonObject;

import java.util.Map;
import java.util.HashMap;

public class SessionUser {
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
    }

    private static class UserQuery {
        public QueryMode mode = QueryMode.NONE;
        public SortCategory sortCategory;
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

    public int getSetLimit(String limitString)
            throws IllegalArgumentException {
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

    public int getSetPage(String pageString)
            throws IllegalArgumentException {
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

    public SortCategory getSetSortCategory(String sortCatString)
            throws IllegalArgumentException {
        if (sortCatString == null || sortCatString.isEmpty()) {
            return userQuery.sortCategory;
        } else {
            SortCategory sortCat;
            try {
                sortCat = SortCategory.valueOf(
                        sortCatString.replace("-", "_").toUpperCase()
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid sort category");
            }
            userQuery.sortCategory = sortCat;
            return sortCat;
        }
    }

    public void backFormParameters(JsonObject whereToStore) {
        String limitString = Integer.toString(userQuery.limit);
        String pageString = Integer.toString(userQuery.page);
        String sortCatString = userQuery.sortCategory.name()
                .toLowerCase().replace("_", "-");

        whereToStore.addProperty("queryMode", userQuery.mode.name().toLowerCase());
        whereToStore.addProperty("limit", limitString);
        whereToStore.addProperty("page", pageString);
        whereToStore.addProperty("sortBy", sortCatString);

        switch(userQuery.mode) {
            case SEARCH:
                whereToStore.addProperty("title", userQuery.title);
                whereToStore.addProperty("year", userQuery.year);
                whereToStore.addProperty("director", userQuery.director);
                whereToStore.addProperty("star", userQuery.star);
                break;
            case BROWSE:
                whereToStore.addProperty("alpha", userQuery.alpha);
                whereToStore.addProperty("genre", userQuery.genreId);
                break;
            default:
                break;
        }
    }

    private Map<Integer, CartItem> shoppingCart = new HashMap<>();

    public static class CartItem {
        public int quantity;
        public double price;

        public CartItem(int quantity, double price) {
            this.quantity = quantity;
            this.price = price;
        }
    }

    public Map<Integer, CartItem> getShoppingCart() {
        return shoppingCart;
    }

    public void addToCart(int movieId, double price) {
        CartItem item = shoppingCart.getOrDefault(movieId, new CartItem(0, price));
        item.quantity++;
        shoppingCart.put(movieId, item);
    }

    public void removeFromCart(int movieId) {
        shoppingCart.remove(movieId);
    }

    public void decreaseCartItem(int movieId) {
        CartItem item = shoppingCart.get(movieId);
        if (item != null) {
            if (item.quantity > 1) {
                item.quantity--;
            } else {
                shoppingCart.remove(movieId);  // Remove the item if quantity reaches 0
            }
        }
    }

    public double calculateTotalPrice() {
        double totalPrice = 0.0;

        for (CartItem cartItem : shoppingCart.values()) {
            totalPrice += cartItem.price * cartItem.quantity;  // Use stored prices
        }

        return totalPrice;
    }

}
