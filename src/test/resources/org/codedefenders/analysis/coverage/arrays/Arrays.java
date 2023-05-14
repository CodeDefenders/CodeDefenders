import utils.Call;
import utils.MethodChain;

import static utils.Utils.doThrow;
import static utils.Utils.doGet;

/**
 * <p>Arrays
 * <p>JaCoCo coverage: Doesn't cover array expressions themselves. Only coverable expressions inside the bounds, index
 *                     or initializers are covered.
 * <p>Extended coverage: Covers array expressions according to the coverage of the child expressions. In the case of
 *                       exceptions, we cover the parts after the exceptional expression as NOT_COVERED.
 */
public class Arrays {

    @Call
    public void regularArrayUsage() {
        int[] i =
                new
                int[] {
                1,
                2
        };

        int[] j =
                new
                int[3];

        int k =
                i[1];
    }

    @Call
    public void exceptionInInit() {
        int[] i = new int[] {
                1,
                doThrow()
        };

        // block: ignore_end_status
    }

    @Call
    public void exceptionFromCoveredExprInInit() {
        int[] i = new int[] {
                1,
                MethodChain.create()
                        .doThrow()
                        .get(2)
        };

        // block: ignore_end_status
    }

    @Call
    public void exceptionInInitSingleLine() {
        int[] i = new int[] { 1, doThrow() };

        // block: ignore_end_status
    }

    @Call
    public void exceptionInBounds() {
        int[] i = new int[
                doThrow()
        ];

        // block: ignore_end_status
    }

    @Call
    public void exceptionFromCoveredExprInBounds() {
        int[] i = new int[
                MethodChain.create()
                        .doThrow()
                        .get(1)
        ];

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIndex() {
        int[] i = new int[2];

        int j = i[
                doThrow()
        ];

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIndexSingleLine() {
        int[] i = new int[2];

        int j = i[doThrow()];

        // block: ignore_end_status
    }

    @Call
    public void exceptionFromCoveredExprInIndex() {
        int[] i = new int[2];

        int j = i[
                MethodChain.create()
                        .doThrow()
                        .get(1)
        ];

        // block: ignore_end_status
    }

    @Call
    public void nestedInitializers() {
        int [][] i = new int[][] {
                {1, 2},
                {3, 4}
        };
    }

    @Call
    public void nestedInitializerException1() {
        int [][] i = new int[][] {
                {1, 2},
                {3, doThrow()},
                {5, 6}
        };

        // block: ignore_end_status
    }

    @Call
    public void nestedInitializersException2() {
        int [][] i = new int[][] {
                {1, 2},
                {doGet(3), doThrow()},
                {5, 6}
        };

        // block: ignore_end_status
    }

    @Call
    public void nestedInitializersException3() {
        int [][] i = new int[][] {
                {1, 2},
                {
                    3,
                    doThrow()
                },
                {5, 6}
        };

        // block: ignore_end_status
    }

    @Call
    public void nestedInitializersException4() {
        int [][] i = new int[][] {
                {1, 2},
                {
                        3,
                        MethodChain.create()
                                .doThrow()
                                .get(1)
                },
                {5, 6}
        };

        // block: ignore_end_status
    }
}
