import java.util.function.Supplier;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;

import utils.Call;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>Lambdas
 * <p>JaCoCo coverage: Lambdas with expressions: Covers the expression.
 *                     Lambdas with blocks: If the block doesn't always jump, covers the closing brace of the block.
 *                     <p>Note: Java 16/17 seems to deduplicate lambdas. Therefore, if a class contains multiple of the
 *                     same lambda, the coverage of them will be combined on the first instance and the rest will be
 *                     left empty.
 * <p>Extended coverage: Also covers empty space in the expression or block (according the the statements and closing
 *                       brace coverage).
 */
public class Lambdas {
    @Call
    public void test() {
        c.accept(1.0f);
        d.accept(1.0);
    }

    private Consumer<Float> c = x -> {

    };

    private Consumer<Double> d = x -> {
        return;

    };

    @Call
    public void regularLambdas() {
        Runnable r =
        () -> {

        };

        Runnable s =
        () -> {
            return;

        };

        Supplier<Integer> t =
        () -> {
            return 1;
        };

        Supplier<Long> u =
        () -> 1L;

        UnaryOperator<Integer> v =
        (i) -> {
            return i;
        };

        UnaryOperator<Integer> w =
        (i) ->
                i
                + i;

        Consumer<Integer> x =
        (i) -> {

        };

        r.run();
        s.run();
        t.get();
        u.get();
        v.apply(1);
        w.apply(1);
        x.accept(1);
    }

    @Call
    public void exception1() {
        Runnable r = () -> {
            doThrow(0);

        };
        r.run();
    }

    @Call
    public void exception2() {
        Runnable r = () -> {
            doCall();
            doThrow();

        };
        r.run();
    }

    @Call
    public void exception3() {
        Supplier s = () -> doThrow(1);
        s.get();
    }

    @Call
    public void exception4() {
        Supplier s = () ->

                doThrow(2);
        s.get();
    }
}
