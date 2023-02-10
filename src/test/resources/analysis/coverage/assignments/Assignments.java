import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

public class Assignments {

    @Call
    public void nonThrowingAssignments() {
        int i = 1;

        i
            =
            2;

        i
            +=
            2;

        i
            =
            doGet(2);

        i
            +=
            doGet(2);

        MethodChain.create()
                .call()
                .field
                =
                1;

        int[] array = new int[1];

        MethodChain.create()
                .call()
                .get(array)
                [0]
                =
                1;
    }

    @Call
    public void nonThrowingAssignmentsInMethodArg() {
        int i = 1;

        consume(
                i
                =
                2
        );

        consume(
                i
                +=
                2
        );

        consume(
                i
                =
                doGet(2)
        );

        consume(
                i
                +=
                doGet(2)
        );

        consume(
                MethodChain.create()
                        .call()
                        .field
                        =
                        1
        );

        int[] array = new int[1];

        consume(
                MethodChain.create()
                        .call()
                        .get(array)
                        [0]
                        =
                        1
        );
    }

    @Call
    public void throwingValue() {
        int i = 1;

        i
            =
            doThrow();

        ;
    }

    @Call
    public void throwingValueInMethodArg() {
        int i = 1;

        consume(
                i
                =
                doThrow()
        );

        ;
    }

    @Call
    public void coveredAndThrowingTarget() {
        MethodChain.create()
                .call()
                .doThrow()
                .field

                =

                doGet(1);

        ;
    }

    @Call
    public void coveredTargetAndThrowingValue() {
        MethodChain.create()
                .call()
                .field

                =

                doThrow()

        ;
    }

    @Call
    public void coveredAndThrowingValue() {
        int i = 1;

        i

                =

        MethodChain.create()
                .call()
                .doThrow()
                .get(1)


        ;
    }

    @Call
    public void nestedAssignments() {
        int i, j;

        i
            =
            j
            =
            1;

        consume(
                i
                =
                j
                =
                1
        );


        MethodChain.create()
                .field
                =
                MethodChain.create()
                        .field
                =
                MethodChain.create()
                        .field;
    }

    @Call
    public void exceptionInOuterNestedAssignment() {
        MethodChain.create()
                .field
                =
                MethodChain.create()
                        .doThrow()
                        .field
                =
                MethodChain.create()
                        .field;
    }

    @Call
    public void exceptionInnerNestedAssignment() {
        int i;

        MethodChain.create()
                .field
                =
                i
                =
                MethodChain.create()
                        .doThrow()
                        .field;
    }
}
