package session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class ShoppingCartSession {
    private final Map<String, CartItem> shoppingCart = new HashMap<>();

    public static class CartItem {
        public int quantity;
        public BigDecimal price;

        public CartItem(int quantity, BigDecimal price) {
            this.quantity = quantity;
            this.price = price;
        }
    }

    public Map<String, CartItem> getShoppingCart() {
        return shoppingCart;
    }

    public void clearCart() { shoppingCart.clear(); }

    public void addToCart(String movieId, BigDecimal price) {
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

    public BigDecimal calculateTotalPrice() {
        BigDecimal totalPrice = new BigDecimal("0.00")
                .setScale(2, RoundingMode.HALF_UP);
        for (CartItem cartItem : shoppingCart.values()) {
            totalPrice = totalPrice.add(cartItem.price.multiply(new BigDecimal(cartItem.quantity)));
        }
        return totalPrice;
    }

}
