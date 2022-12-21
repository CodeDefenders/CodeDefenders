import java.util.ArrayList;
import java.util.List;

import utils.Call;
import utils.MethodChain;
import utils.ThrowingIterable;

import static utils.Utils.consume;
import static utils.Utils.doThrow;
import static utils.Utils.doThrowList;

public class ForEachLoops {

    @Call
    public void listWithElements() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            consume(i);
        }
    }

    @Call
    public void emptyList() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : list) {
            consume(i);
        }
    }

    @Call
    public void endOfListNotReached() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            break;
        }
    }

    @Call
    public void exceptionInIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : doThrowList()) {
            consume(i);
        }
    }

    @Call
    public void exceptionInCoveredIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create()
                .doThrow()
                .get(list)) {
            consume(i);
        }
    }

    @Call
    public void exceptionInCoveredIterableExprOneLine() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create().doThrow().get(list)) {
            consume(i);
        }
    }

    @Call
    public void exceptionInIterableAtFirstLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(0);

        for (Integer i : iterable) {
            consume(i);
        }
    }

    @Call
    public void exceptionInIterableAtSecondLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(1);

        for (Integer i : iterable) {
            consume(i);
        }
    }

    @Call
    public void exceptionInBody() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            doThrow();
        }
    }

    @Call
    public void exceptionInCoveredBody() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {
            consume(i);
            doThrow();
        }
    }

    // -----

    @Call
    public void emptyBodyListWithElements() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        for (Integer i : list) {

        }
    }

    @Call
    public void emptyBodyEmptyList() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : list) {

        }
    }

    @Call
    public void emtpyBodyExceptionInIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : doThrowList()) {

        }
    }

    @Call
    public void emptyBodyExceptionInCoveredIterableExpr() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create()
                .doThrow()
                .get(list)) {

        }
    }

    @Call
    public void emptyBodyExceptionInCoveredIterableExprOneLine() {
        List<Integer> list = new ArrayList<>();

        for (Integer i : MethodChain.create().doThrow().get(list)) {

        }
    }

    @Call
    public void emptyBodyExceptionInIterableAtFirstLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(0);

        for (Integer i : iterable) {

        }
    }

    @Call
    public void emptyBodyExceptionInIterableAtSecondLoop() {
        Iterable<Integer> iterable = new ThrowingIterable(1);

        for (Integer i : iterable) {

        }
    }
}
