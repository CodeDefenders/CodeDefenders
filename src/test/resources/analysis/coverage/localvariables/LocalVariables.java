import static utils.Utils.doGet;
import static utils.Utils.doThrow;

public class LocalVariables {
    /**
     * <p>local variables declarations
     * <p><b>JaCoCo coverage</b>: either covers the first line of variable name, or a (sub-)expression of the
     *                            initializer if the expression is coverable (e.g. a method call expr).
     * <p><b>extended coverage</b>: covers all lines of local variable declarations. variables without initializer are
     *                              treated like empty lines and are covered by the surrounding block as necessary.
     */
    static void localVariables() {
        // variable with initializer
        int i = 0;

        // variable with coverable initializer expression
        int j = doGet();

        // variable without initializer
        int k;

        // not-covered variable
        int l = doThrow();

        // not-covered variable
        int m = 1;
    }
}
