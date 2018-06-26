import java.util.function.BiFunction;

public class SyntaxErrors {
    private Adder adder;

    public SyntaxErrors () {
        adder = new Adder();
    }

    /**
     * A JavaDoc comment
     * @param a The first number to add.
     * @param a The second number to add.
     * @return The sum of the two given numbers.
     */
    public int add (int a, int b) {
        return adder.add(a, b); // An inline comment
    }

    public int sub (int a, int b) {
        /* A normal comment */
        BiFunction<Integer, Integer> subfun = (x,y) -> x-y;
        return subfun.apply(a, b);
    }

    public int getAddCount() {
        return adder.addCount;
    }

    private static class Adder {
        int addCount = 0;

        int add (int a, int b) {
            addCount ++
            return a + b;
        }
    }
}
