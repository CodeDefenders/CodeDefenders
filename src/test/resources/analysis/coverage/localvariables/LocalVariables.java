import utils.Call;

import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Local variables declarations
 * <p>JaCoCo coverage: Either covers the first line of variable name, or a (sub-)expression of the
 *                     initializer if the initializer is coverable (e.g. a method call expr).
 * <p>extended coverage: Covers all lines of local variable declarations. Variables without initializer are
 *                       treated like empty lines and are covered by the surrounding block as necessary.
 */
public class LocalVariables {
    @Call
    public void localVariables() {
        // variable with initializer
        int i =
                0;

        // variable with coverable initializer expression
        int j =
                doGet(0);

        // variable without initializer
        int k;

        // not-covered variable
        int l =
                doThrow();

        // not-covered variable
        int m =
                1;
    }

    @Call
    public void localVariablesOneLine() {
        // variable with initializer
        int i = 0;

        // variable with coverable initializer expression
        int j = doGet(0);

        // variable without initializer
        int k;

        // not-covered variable
        int l = doThrow();

        // not-covered variable
        int m = 1;
    }

    @Call
    public void localVariableDeclWithMultipleVariables() {
        int
                i = 1,

                j = doThrow(),

                k;
    }

    @Call
    public void localVariableDeclWithMultipleVariables2() {
        int
                i = 1, j = doThrow(),

                k;
    }

    @Call
    public void uncoveredParam1() {
        Runnable s =
                () -> {};

        // block: ignore_end_status
    }

    @Call
    public void uncoveredParam2() {
        Runnable s = () -> {};

        // block: ignore_end_status
    }
}
