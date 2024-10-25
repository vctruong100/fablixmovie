package session;

import java.util.HashMap;
import java.util.Map;

public class ShoppingCartSession {
    private final Map<String, CartItem> shoppingCart = new HashMap<>();

    public static class CartItem {
        public int quantity;
        public double price;

        public CartItem(int quantity, double price) {
            this.quantity = quantity;
            this.price = price;
        }
    }

    public Map<String, CartItem> getShoppingCart() {
        return shoppingCart;
    }

    public void clearCart() { shoppingCart.clear(); }

    public void addToCart(String movieId, double price) {
        CartItem item = shoppingCart.getOrDefault(movieId, new CartItem(0, price));
        item.quantity++;
        shoppingCart.put(movieId, item);
    }

    public void removeFromCart(String movieId) {
        shoppingCart.remove(movieId);
    }

    public void decreaseCartItem(String movieId) {
        CartItem item = shoppingCart.get(movieId);
        if (item != null) {
            if (item.quantity > 1) {
                item.quantity--;
            } else {
                shoppingCart.remove(movieId); // Remove the item if quantity reaches 0
            }
        }
    }

    public double calculateTotalPrice() {
        double totalPrice = 0.0;

        for (CartItem cartItem : shoppingCart.values()) {
            totalPrice += cartItem.price * cartItem.quantity;
        }

        return totalPrice;
    }

}
