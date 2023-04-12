import utils.Call;
import utils.MethodChain;

import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Assertions
 * <p>JaCoCo coverage: Covers the 'assert' keyword and the condition. The assert keyword contains instructions,
 *                     which are FULLY_COVERED if the assertion threw and PARTLY_COVERED if it did not. The condition
 *                     contains branches.
 * <p>Extended coverage: Also covers the space between the keyword, check and message according to the coverage.
 */
public class Assertions {
    @Call(params = "0")
    public void passingAssertion1(int i) {
        assert true;

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion2(int i) {
        assert
                i == 0
                : "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion3(int i) {
        assert
                doGet(i) == 0
                : "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion4(int i) {
        assert
                i == 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion5(int i) {
        assert
                doGet(i) == 0
                : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion6(int i) {
        assert i == 0 : "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion7(int i) {
        assert doGet(i) == 0 : "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion8(int i) {
        assert i == 0 : "" + doGet(i);

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertion9(int i) {
        assert doGet(i) == 0 : "" + doGet(i);

        // block: ignore_end_status
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
    public void assertionWithThrowingAndCoveredExpression(int i) {
        assert

                MethodChain.create()
                        .doThrow()
                        .get(i) != 0

                : "" + doGet(i);

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

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion5(int i) {
        assert false : "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion6(int i) {
        assert false
                : doGet(i) + "";

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertion6OneLine(int i) {
        assert false : doGet(i) + "";

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertionWithoutMessage1(int i) {
        assert i == 0;

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertionWithoutMessage2(int i) {
        assert
                doGet(i) == 0;

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void passingAssertionWithoutMessage2OneLine(int i) {
        assert doGet(i) == 0;

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertionWithoutMessage1(int i) {
        assert false;

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertionWithoutMessage2(int i) {
        assert
                i != 0;

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertionWithoutMessage2OneLine(int i) {
        assert i != 0;

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertionWithoutMessage3(int i) {
        assert
                doGet(i) != 0;

        // block: ignore_end_status
    }

    @Call(params = "0", exception = AssertionError.class)
    public void throwingAssertionWithoutMessage3OneLine(int i) {
        assert doGet(i) != 0;

        // block: ignore_end_status
    }
}

