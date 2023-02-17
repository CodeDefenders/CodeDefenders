import utils.Call;

import static utils.Utils.doGet;
import static utils.Utils.doThrow;

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

        assert i == 0 : "";

        assert doGet(i) == 0 : "";

        assert i == 0 : "" + doGet(i);

        assert doGet(i) == 0 : "" + doGet(i);
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion1(int i) {
        assert
                i != 0
                : "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion2(int i) {
        assert
                doGet(i) != 0
                : "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion3(int i) {
        assert
                i != 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion4(int i) {
        assert
                doGet(i) != 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = {"0", "1"}, exception = AssertionError.class)
    public void passingAndthrowingAssertion(int i) {
        assert
                doGet(i) != 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void assertionWithThrowingExpression1(int i) {
        assert
                doThrow() != 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void assertionWithThrowingExpression2(int i) {
        assert
                doThrow() != 0
                : "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void assertionWithThrowingMessage(int i) {
        assert
                i != 0
                : "" + doThrow();

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion1OneLine(int i) {
        assert i != 0 : "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion2OneLine(int i) {
        assert doGet(i) != 0 : "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion3OneLine(int i) {
        assert i != 0 : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion4OneLine(int i) {
        assert doGet(i) != 0 : "" + doGet(i);

        // block: ignore_end_status
    }
}

