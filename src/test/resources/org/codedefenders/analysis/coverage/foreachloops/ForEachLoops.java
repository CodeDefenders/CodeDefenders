import java.util.ArrayList;
import java.util.List;

import utils.Call;
import utils.MethodChain;
import utils.ThrowingIterable;

import static utils.Utils.consume;
import static utils.Utils.doThrow;
import static utils.Utils.doThrowList;

/**
 * <p>For-each loops
 * <p>JaCoCo coverage: Covers the iterable with branch coverage: 1 branch for entering the loop, 1 branch for jumping
 *                     past (probably corresponds to a hasNext() call on the iterator). The iterable also contains
 *                     instruction coverage. If the loop body doesn't always jump, the closing brace is also covered
 *                     based on if the end of the body was reached.
 * <p>Extended coverage: Also covers the space before the iterable and before the body based on the iterable's and
 *                       body's coverage.
 */
public class ForEachLoops {
    @Call
    public void listWithElements() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (
                Integer i
                :
                list
        ) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void listWithElementsOneLine() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void emptyList() {
        List<Integer> list = new ArrayList<>();

        for (
                Integer i
                :
                list
        ) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void emptyListOneLine() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : list) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void endOfListNotReached() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (
                Integer i
                :
                list
        ) {
            break;
        }

        // block: ignore_end_status
    }

    @Call
    public void endOfListNotReachedOneLine() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            break;
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (
                Integer i
                :
                doThrowList()
        ) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIterableExprOneLine() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : doThrowList()) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInCoveredIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (
                Integer i : MethodChain.create()
                        .doThrow()
                        .get(list)
        ) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInCoveredIterableExprOneLine() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create().doThrow().get(list)) {
            consume(i);
        }

        // block: ignore_end_status
    }


    @Call
    public void test() {
        Iterable<Integer> iterable = new ThrowingIterable(0);

        for (
                Integer i
                :
                iterable
        ) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIterableAtFirstLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(0);

        for (Integer i : iterable) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInIterableAtSecondLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(1);

        for (Integer i : iterable) {
            consume(i);
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInBody() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInCoveredBody() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            consume(i);
            doThrow();
        }

        // block: ignore_end_status
    }

    // -----

    @Call
    public void emptyBodyListWithElements() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyBodyEmptyList() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : list) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emtpyBodyExceptionInIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : doThrowList()) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyBodyExceptionInCoveredIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create()
                .doThrow()
                .get(list)) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyBodyExceptionInIterableAtFirstLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(0);

        for (Integer i : iterable) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyBodyExceptionInIterableAtSecondLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(1);

        for (Integer i : iterable) {

        }

        // block: ignore_end_status
    }
}
