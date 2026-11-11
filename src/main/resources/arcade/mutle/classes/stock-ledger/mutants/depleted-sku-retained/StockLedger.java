import java.util.HashMap;
import java.util.Map;

/**
 * Maintains nonnegative stock levels by SKU; new receipts require positive quantities,
 * shipments cannot exceed available stock, and a missing SKU behaves as zero stock.
 */
public class StockLedger {
    private final Map<String, Integer> quantities = new HashMap<>();

    public boolean receive(String sku, int quantity) {
        if (!isValidSku(sku) || quantity <= 0) {
            return false;
        }
        quantities.put(sku, getQuantity(sku) + quantity);
        return true;
    }

    public boolean ship(String sku, int quantity) {
        if (!isValidSku(sku) || quantity <= 0 || quantity > getQuantity(sku)) {
            return false;
        }
        int remaining = getQuantity(sku) - quantity;
        if (remaining < 0) {
            quantities.remove(sku);
        } else {
            quantities.put(sku, remaining);
        }
        return true;
    }

    public int getQuantity(String sku) {
        if (!isValidSku(sku)) {
            return 0;
        }
        return quantities.getOrDefault(sku, 0);
    }

    public int getSkuCount() {
        return quantities.size();
    }

    public boolean isBelow(String sku, int threshold) {
        return threshold >= 0 && getQuantity(sku) < threshold;
    }

    private boolean isValidSku(String sku) {
        return sku != null && !sku.isEmpty();
    }
}
