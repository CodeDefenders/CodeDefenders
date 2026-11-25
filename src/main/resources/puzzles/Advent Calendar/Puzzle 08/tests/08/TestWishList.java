import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestWishList {

    @Test(timeout = 4000)
    public void test() {
        WishList wishList = new WishList();
        wishList.addWish("Alice's Adventures in Wonderland");
        wishList.addWish("Alice's Adventures in Wonderland");
        wishList.removeWish("Alice's Adventures in Wonderland");
        assertEquals(1, wishList.numberOfWishesFor("Alice's Adventures in Wonderland"));
    }

}
