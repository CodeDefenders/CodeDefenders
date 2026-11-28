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
        wishList.addWish("Smartphone");
        wishList.addWish("Smartphone");
        wishList.removeWish("Smartphone");
        wishList.removeWish("Smartphone");
        assertEquals(0, wishList.numberOfWishesFor("Smartphone"));
    }

}
