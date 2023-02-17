import utils.Call;
import utils.MethodChain;

import static utils.Utils.doGet;
import static utils.Utils.consume;

public class UnaryExpressions {

    @Call
    public void increments() {
        int i = 0;
        int[] j = new int[1];

        i++;

        i
                ++;

        j[0]++;

        j[0]
                ++;

        doGet
                (j)[0]
                ++;
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromVariable1() {
        Integer i = null;
        i
        ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromVariable2() {
        Integer i = null;
        int j =
                i
                ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromVariable3() {
        Integer i = null;
        consume(
                i
                ++
        );

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromVariable4() {
        Boolean b = null;
        boolean c =
                !
                b;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCall1() {
        Integer[] i = new Integer[]{null};
        doGet(i)[0]
                ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCall2() {
        Integer[] i = new Integer[]{null};
        int j = doGet(i)[0]
                ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCall3() {
        Integer[] i = new Integer[]{null};
        consume(
                doGet(i)[0]
                ++
        );

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCall4() {
        Boolean[] b = new Boolean[]{null};
        boolean c =
                !
                doGet(b)[0];

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCallChain1() {
        Integer[] i = new Integer[]{null};
        MethodChain.create()
                .get(i)[0]
                ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCallChain2() {
        Integer[] i = new Integer[]{null};
        int j = MethodChain.create()
                .get(i)[0]
                ++;

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCallChain3() {
        Integer[] i = new Integer[]{null};
        consume(
                MethodChain.create()
                .get(i)[0]
                ++
        );

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionFromCallChain4() {
        Boolean[] b = new Boolean[]{null};
        boolean c =
                !
                MethodChain.create()
                        .get(b)[0];

        // block: ignore_end_status
    }

}
