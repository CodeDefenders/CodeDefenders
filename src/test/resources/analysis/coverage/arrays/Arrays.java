import utils.Call;
import utils.MethodChain;

import static utils.Utils.doThrow;
import static utils.Utils.doGet;

public class Arrays {

    @Call
    public void regularArrayUsage() {
        int[] i = new int[] {
                1,
                2
        };

        int[] j = new int[3];

        int k =
                i[1];
    }

    @Call
    public void exceptionInInit() {
        int[] i = new int[] {
                1,
                doThrow()
        };

        ; // empty stmt to check if the coverage after the above stmt is correct
    }

    @Call
    public void exceptionFromCoveredExprInInit() {
        int[] i = new int[] {
                1,
                MethodChain.create()
                        .doThrow()
                        .get(2)
        };

        ;
    }

    @Call
    public void excpetionAndMethodCallInInit() {
        int[] i = new int[] {
                doThrow(),

                doGet(1)
        };

        ;
    }

    @Call
    public void exceptionInBounds() {
        int[] i = new int[
                doThrow()
        ];

        ;
    }

    @Call
    public void exceptionInCoveredBounds() {
        int[] i = new int[
                MethodChain.create()
                        .doThrow()
                        .get(1)
        ];

        ;
    }

    @Call
    public void exceptionInIndex() {
        int[] i = new int[2];

        int j = i[
                doThrow()
        ];

        ;
    }

    @Call
    public void exceptionInCoveredIndex() {
        int[] i = new int[2];

        int j = i[
                MethodChain.create()
                        .doThrow()
                        .get(1)
        ];

        ;
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
    }

    @Call
    public void nestedInitializersException2() {
        int [][] i = new int[][] {
                {1, 2},
                {doGet(3), doThrow()},
                {5, 6}
        };
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
    }
}
