import static utils.Utils.doThrow;

public class InitializerBlocks {
    /**
     * <p>static initializer block
     * <p><b>JaCoCo coverage</b>: covers the statement(s) and the line of the closing brace
     * <p><b>extended coverage</b>: covers the block
     */
    static {
        int i = 1;
    }

    // empty static block
    static {

    }

    // we don't consider static blocks throwing exceptions, because that leads to an ExceptionInInitializerError

    /**
     * <p>non-static initializer block with statement(s)
     * <p><b>JaCoCo coverage</b>: only covers the statement(s)
     * <p><b>extended coverage</b>: covers the block. if empty, the coverage depends on wheter the class has been
     *                              initialized, and whether non-static initializer blocks before it have been covered
     */
    {
        int i = 1;
    }

    // empty non-static block
    {

    }

    // non-static block throwing exception
    {
        int i = 1;
        doThrow();
    }

    // not-covered non-static block
    {

    }
}

