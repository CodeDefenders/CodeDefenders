import static utils.Utils.consume;
import java.util.function.Supplier;
import java.util.ArrayList;

public class Playground {
    public Playground() {
        new ArrayList<Integer>() {
            @Override
            public int size() {
                return 3;
            }

            @Override
            public boolean isEmpty() {
                return size() == 0;
            }
        };
    }

    private
        Runnable r = () -> {};


    public int consume(Runnable r) {
        return 4;
    }

    public void test() {
        // some very long line for testing the page template -------------------------------------------------------------------------------------------------------------------------------

        if (System.currentTimeMillis()

                > System.currentTimeMillis()
            +
            1
            == true) {
            System.out.println("ok");
        }

        if (
                consume(() -> {})
            ==
                consume(() -> {})
        ) {
            System.out.println("ok");
        }

        System.out.println(
                System.currentTimeMillis() > 0 ? 1 : 0);

        System.out.println(
                System.currentTimeMillis() > 0
                        ? 1
                        : 0);


        if (
                ((Supplier<Integer>) () -> {

                    return 4;
                }).get() == 3) {

        }

        System.out.println("1"); System.out.println("2");
    }
}
