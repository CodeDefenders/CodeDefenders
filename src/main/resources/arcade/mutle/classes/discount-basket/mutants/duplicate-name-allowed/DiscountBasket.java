import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A basket holds up to eight uniquely named items priced in whole cents and applies one percentage discount.
 * Invalid names, prices, or discounts are rejected; totals round down after the discount and an empty basket totals zero.
 */
public class DiscountBasket {
    private final Map<String, Integer> prices = new LinkedHashMap<>();
    private int discountPercent;

    public boolean addItem(String name, int priceCents) {
        requireName(name);
        if (priceCents < 0 || priceCents > 100000) {
            throw new IllegalArgumentException("price must be between 0 and 100000");
        }
        if (prices.size() == 8) {
            return false;
        }
        prices.put(name, priceCents);
        return true;
    }

    public boolean removeItem(String name) {
        requireName(name);
        return prices.remove(name) != null;
    }

    public boolean setDiscountPercent(int newDiscountPercent) {
        if (newDiscountPercent < 0 || newDiscountPercent > 50) {
            throw new IllegalArgumentException("discount must be between 0 and 50");
        }
        if (discountPercent == newDiscountPercent) {
            return false;
        }
        discountPercent = newDiscountPercent;
        return true;
    }

    public int totalCents() {
        int subtotal = 0;
        for (int price : prices.values()) {
            subtotal += price;
        }
        return subtotal * (100 - discountPercent) / 100;
    }

    public int itemCount() {
        return prices.size();
    }

    private static void requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
