import utils.Call;

import static utils.Utils.doGet;

public class Assertions {
    @Call(params = "0")
    public void passingAssertions(int i) {
        assert
                i == 0
                : "";

        assert
                doGet(i) == 0
                : "";

        assert
                i == 0
                : "" + doGet(i);

        assert
                doGet(i) == 0
                : "" + doGet(i);
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion1(int i) {
        assert
                i != 0
                : "";

        ;
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion2(int i) {
        assert
                doGet(i) != 0
                : "";

        ;
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion3(int i) {
        assert
                i != 0
                : "" + doGet(i);

        ;
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion4(int i) {
        assert
                doGet(i) != 0
                : "" + doGet(i);

        ;
    }

    @Call(params = {"0", "1"}, exception = AssertionError.class)
    public void passingAndthrowingAssertion(int i) {
        assert
                doGet(i) != 0
                : "" + doGet(i);

        ;
    }
}

