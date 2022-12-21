import java.util.function.Supplier;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;

import utils.Call;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class Lambdas {
    @Call
    public void test() {
        this.r.run();
        this.s.run();
    }

    private Runnable r = () -> {

    };

    private Runnable s = () -> {
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

        Supplier<Integer> u =
        () -> 1;

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
            doThrow();
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
}
