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

        var exception = assertThrows(
                IllegalStateException.class,
                () -> { wishList.removeWish("Toys"); }
        );
        assertEquals(
                "Wish does not exist and cannot be removed.",
                exception.getMessage()
        );
    }

}
