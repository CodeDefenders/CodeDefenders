import java.util.HashMap;
import java.util.Map;

public class WishList {

    private final Map<String, Integer> wishes = new HashMap<>();

    public void addWish(String wish) {
        if (wishes.containsKey(wish)) {
            int n = wishes.get(wish) + 1;
            wishes.put(wish, n);
        } else {
            wishes.put(wish, 1);
        }
    }

    public void removeWish(String wish) {
        if (wishes.containsKey(wish)) {
            int n = wishes.get(wish) - 1;
            if (n < 1) {
                wishes.remove(wish);
            } else {
                wishes.put(wish, n);
            }
        } else {
            throw new IllegalStateException("Wish does not exist and cannot be removed.");
        }
    }

    public int numberOfWishesFor(String wish) {
        return wishes.getOrDefault(wish, 0);
    }

    public int numberOfWishes() {
        int sum = 0;
        for (int wishes : wishes.values()) {
            sum += wishes;
        }
        return sum;
    }

}
