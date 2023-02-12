import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;
import static utils.Utils.doThrowBoolean;

public class BinaryExpressions {

    @Call
    public void regular() {
        int i =
                1
                +
                1;
    }

    @Call
    public void shortCircuit1() {
        boolean b =
                false
                &&
                true;

        ;
    }

    @Call
    public void shortCircuit1b() {
        consume(
                false
                &&
                true
        );

        ;
    }

    @Call
    public void shortCircuit2() {
        // not great
        boolean b =
                doGet(false)
                &&
                doGet(true);

        ;
    }

    @Call
    public void shortCircuit2OneLine() {
        boolean b = doGet(false) && doGet(true);

        ;
    }

    @Call
    public void shortCircuit2b() {
        // not great
        consume(
                doGet(false)
                &&
                doGet(true)
        );

        ;
    }

    @Call
    public void shortCircuit2bOneLine() {
        consume(doGet(false) && doGet(true));

        ;
    }

    @Call
    public void shortCircut3() {
        boolean d =
                doGet(false)
                &&
                MethodChain.create()
                        .get(true);

        ;
    }

    @Call
    public void shortCircut3b() {
        consume(

                doGet(false)

                &&

                MethodChain.create()

                        .get(true)
        );

        ;
    }

    @Call
    public void shortCircut4() {
        // worse
        boolean e =
                false
                &&
                MethodChain.create()
                        .get(true);

        ;
    }

    @Call
    public void shortCircut4b() {
        // worse
        consume(
                false
                &&
                MethodChain.create()
                        .get(true)
        );

        ;
    }

    @Call
    public void nestedShortCircuit() {
        boolean c =
                doGet(false)
                &&
                doGet(false)
                &&
                doGet(true);

        boolean d =
                doGet(false)
                &&
                MethodChain.create()
                        .get(false)
                &&
                MethodChain.create()
                        .get(true);
    }

    @Call
    public void exceptionInLeft1() {
        int i =
                doThrow()
                +
                1;
    }

    @Call
    public void exceptionInLeft2() {
        int i =
                doThrow()
                +
                doGet(1);
    }

    @Call
    public void exceptionInLeft3() {
        int i =
                MethodChain.create()
                        .doThrow()
                        .get(1)
                +
                1;
    }

    @Call
    public void exceptionInLeft4() {
        int i =
                MethodChain.create()
                        .doThrow()
                        .get(1)
                +
                doGet(1);
    }

    @Call
    public void exceptionInRight1() {
        int i =
                1
                +
                doThrow();
    }

    @Call
    public void exceptionInRight2() {
        int i =
                doGet(1)
                +
                doThrow();
    }

    @Call
    public void exceptionInRight3() {
        int i =
                1
                +
                MethodChain.create()
                        .doThrow()
                        .get(1);
    }

    @Call
    public void exceptionInRight4() {
        int i =
                doGet(1)
                +
                MethodChain.create()
                        .doThrow()
                        .get(1);
    }

    @Call
    public void nestedException1() {
        int i = doGet(1)
                +
                doGet(1)
                +
                doThrow();
    }

    @Call
    public void nestedException2() {
        int i = doGet(1)
                +
                doThrow()
                +
                doGet(1);
    }

}
